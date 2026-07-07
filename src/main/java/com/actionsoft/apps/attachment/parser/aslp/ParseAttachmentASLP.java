package com.actionsoft.apps.attachment.parser.aslp;

import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.exception.ParseException;
import com.actionsoft.apps.attachment.parser.model.ExtractResult;
import com.actionsoft.apps.attachment.parser.model.ParseResult;
import com.actionsoft.apps.attachment.parser.service.DocumentExtractorService;
import com.actionsoft.apps.attachment.parser.service.DocumentParserService;
import com.actionsoft.apps.attachment.parser.util.BillTypeDetector;
import com.actionsoft.apps.attachment.parser.util.LogUtils;
import com.actionsoft.apps.resource.interop.aslp.ASLP;
import com.actionsoft.bpms.commons.formfile.dao.FormFileDao;
import com.actionsoft.bpms.commons.formfile.model.delegate.FormFile;
import com.actionsoft.bpms.commons.mvc.view.ResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 附件解析 ASLP（入口）
 * <p>
 * 职责：接收参数 → 调用 DocumentParserService → 调用 DocumentExtractorService → 返回
 * <p>
 * 返回格式：{ "success": true, "data": { "FIELD_NAME": "value", ... } }
 * data 中所有 key 与表单 fieldName 完全一致，未识别字段返回空字符串 ""。
 */
public class ParseAttachmentASLP implements ASLP {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseAttachmentASLP.class);
    private static final DocumentParserService PARSER_SERVICE = new DocumentParserService();
    private static final DocumentExtractorService EXTRACTOR_SERVICE = new DocumentExtractorService();

    /**
     * 所有需要出现在 data 中的表单字段（按表单布局顺序）
     */
    private static final String[] FORM_FIELDS = {
        FieldNames.INVOICE_TYPE,
        FieldNames.EXPENSE_TYPE,
        FieldNames.INVOICE_NO,
        FieldNames.INVOICE_DATE,
        FieldNames.AMOUNT,
        FieldNames.BUYER_NAME,
        FieldNames.BUYER_TAX_NO,
        FieldNames.PASSENGER_NAME,
        FieldNames.PASSENGER_ID_NO,
        FieldNames.TRAIN_NO,
        FieldNames.SEAT_TYPE,
        FieldNames.DEPARTURE_STATION,
        FieldNames.ARRIVAL_STATION,
        FieldNames.BOARDING_TIME,
        FieldNames.CARRIAGE_NO,
        FieldNames.SEAT_NO,
        FieldNames.TICKET_NO,
        FieldNames.SOURCE_TYPE,
        FieldNames.PARSER_NAME,
        FieldNames.PARSE_DURATION,
        FieldNames.RESULT
    };

    @Override
    public ResponseObject call(Map<String, Object> params) {
        long totalStart = System.currentTimeMillis();
        String fileId = (String) params.get("fileId");

        LogUtils.aslpEnter(LOGGER, params);

        if (fileId == null || fileId.isEmpty()) {
            return ResponseObject.newErrResponse("缺少必需参数: fileId");
        }

        try {
            // 1. 解析文件
            long parseStart = System.currentTimeMillis();
            ParseResult parseResult = PARSER_SERVICE.parse(fileId);
            long parseCost = System.currentTimeMillis() - parseStart;

            // 2. 提取信息
            long extractStart = System.currentTimeMillis();
            ExtractResult extractResult = EXTRACTOR_SERVICE.extract(parseResult);
            long extractCost = System.currentTimeMillis() - extractStart;

            long totalCost = System.currentTimeMillis() - totalStart;
            LogUtils.costSummary(LOGGER, fileId, parseCost, extractCost, totalCost);

            // 3. 构建 data（所有字段默认空字符串，再从提取结果填充）
            Map<String, Object> extractFields = extractResult.getFields();
            Map<String, Object> data = new LinkedHashMap<>();
            for (String field : FORM_FIELDS) {
                Object val = extractFields.get(field);
                data.put(field, val != null ? String.valueOf(val) : "");
            }

            // 顶层字段（不来自 Extractor 的 fields map）
            data.put(FieldNames.RESULT, parseResult.getText() != null ? parseResult.getText() : "");
            data.put(FieldNames.INVOICE_TYPE, extractResult.getInvoiceType() != null ? extractResult.getInvoiceType() : "");

            // BILL_TYPE + EXPENSE_TYPE 统一由 BillTypeDetector 生成
            // Extractor 不负责费用分类（禁止设置 EXPENSE_TYPE）
            String billType = BillTypeDetector.detect(parseResult.getText());
            data.put(FieldNames.BILL_TYPE, billType);
            data.put(FieldNames.EXPENSE_TYPE, BillTypeDetector.detectExpenseType(billType));

            // DOCUMENT_CATEGORY（程序级文档分类标识）
            String docCategory = extractResult.getDocumentType();
            data.put(FieldNames.DOCUMENT_CATEGORY, docCategory != null && !docCategory.isEmpty()
                    ? docCategory : com.actionsoft.apps.attachment.parser.constant.DocumentCategory.OTHER.name());

            data.put(FieldNames.SOURCE_TYPE, parseResult.getSourceDocumentType() != null ? parseResult.getSourceDocumentType() : "");
            data.put(FieldNames.PARSER_NAME, parseResult.getParserName() != null ? parseResult.getParserName() : "");
            data.put(FieldNames.PARSE_DURATION, String.valueOf(totalCost));

            // 5. 查询附件信息（只查一次，复用）
            FormFileDao dao = new FormFileDao();
            FormFile formFile = (FormFile) dao.queryById(fileId);
            data.put(FieldNames.FILE_INFO, buildFileInfo(formFile, fileId));

            // 6. 构建 VIEW_URL 和 VIEW_NAME
            if (formFile != null) {
                data.put(FieldNames.VIEW_NAME, nvl(formFile.getFileName()));
                data.put(FieldNames.VIEW_URL, buildViewUrl(formFile));
            } else {
                data.put(FieldNames.VIEW_NAME, "");
                data.put(FieldNames.VIEW_URL, "");
            }

            // 7. 返回
            ResponseObject response = ResponseObject.newOkResponse();
            response.put("success", true);
            response.put("data", data);
            return response;

        } catch (ParseException e) {
            long totalCost = System.currentTimeMillis() - totalStart;
            LogUtils.parserFail(LOGGER, "ASLP", e.getFileId(), e);

            ResponseObject response = ResponseObject.newErrResponse(e.getMessage());
            response.put("success", false);
            response.put("errorCode", e.getErrorCode());
            response.put("detail", e.getMessage());
            return response;
        } catch (Exception e) {
            long totalCost = System.currentTimeMillis() - totalStart;
            LOGGER.error("[附件解析服务] 未知异常: fileId={}, error={}", fileId, e.getMessage(), e);

            ResponseObject response = ResponseObject.newErrResponse("系统内部错误");
            response.put("success", false);
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("detail", e.getMessage());
            return response;
        }
    }

    /**
     * 根据 fileId 查询平台附件表（AWS_FORM_FILE），返回完整文件信息对象。
     * <p>
     * 前端附件组件读取的是 {@code $dataExtend.FIELD.extends.fileList}，
     * 仅传 fileId 无法显示附件，必须返回完整的 fileList 对象。
     */
    private Map<String, Object> buildFileInfo(FormFile formFile, String fileId) {
        Map<String, Object> info = new LinkedHashMap<>();
        if (fileId == null || fileId.isEmpty() || formFile == null) return info;

        // 基本信息
        info.put("id",              nvl(formFile.getId()));
        info.put("fileName",        nvl(formFile.getFileName()));
        info.put("fileSize",        formatFileSize(formFile.getFileSize()));
        info.put("fileType",        getFileType(formFile.getFileName()));
        info.put("fileImgIcon",     getFileIcon(getFileType(formFile.getFileName())));
        info.put("createDate",      formatDate(formFile.getCreateDate()));
        info.put("createUser",      nvl(formFile.getCreateUser()));
        info.put("appId",           nvl(formFile.getAppId()));
        info.put("boName",          nvl(formFile.getBoName()));
        info.put("boItemName",      nvl(formFile.getBoItemName()));

        // 下载 URL
        info.put("fileDownloadUrl", buildDownloadUrl(fileId));

        // 平台 UI 需要的固定字段
        info.put("checked",          false);
        info.put("hasHistoryVersion", false);

        return info;
    }

    private String nvl(String val) { return val != null ? val : ""; }

    private String formatDate(java.sql.Timestamp ts) {
        if (ts == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private String getFileIcon(String fileType) {
        switch (fileType) {
            case "pdf":  return "&#xe7d5;";
            case "doc":
            case "docx": return "&#xe7c2;";
            case "xls":
            case "xlsx": return "&#xe7c7;";
            case "ppt":
            case "pptx": return "&#xe7c4;";
            case "txt":  return "&#xe7d6;";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":  return "&#xe7cf;";
            case "zip":
            case "rar":
            case "7z":   return "&#xe7d2;";
            default:     return "&#xe7d6;";
        }
    }

    private String buildDownloadUrl(String fileId) {
        return "/r/uf?repositoryName=!form-ui-file-&fileId=" + fileId;
    }

    /**
     * 构建 onlinedoc_filepreview 预览 URL
     */
    private String buildViewUrl(FormFile formFile) {
        String appId = nvl(formFile.getAppId());
        String fileName = nvl(formFile.getFileName());
        String boItemName = nvl(formFile.getBoItemName());
        String fileId = nvl(formFile.getId());

        String encodedFileName = urlEncode(fileName);

        String url = "/r/w?cmd=com.actionsoft.apps.addons.onlinedoc_filepreview"
                + "&appId=" + appId
                + "&fileNameOriginal=" + encodedFileName
                + "&sourceFileName=" + encodedFileName
                + "&sourceGroupValue=" + fileId
                + "&sourceFileValue=" + boItemName
                + "&sourceRepositoryName=!form-ui-file-"
                + "&sourceAppId=" + appId
                + "&isShowDefaultToolbar=true"
                + "&isCopy=true"
                + "&isPrint=false"
                + "&isDownload=true"
                + "&isEncrypt=true"
                + "&isShowBackbtn=false"
                + "&isPDFCovertPNG=0"
                + "&isDecode=true"
                + "&bucketFolder="
                + "&msaSvcId=awspaas";

        LOGGER.info("[附件解析服务] buildViewUrl: fileId={}, appId={}, boItemName={}, fileName={}, url={}",
                fileId, appId, boItemName, fileName, url);
        return url;
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
