package com.actionsoft.apps.attachment.parser.service;

import com.actionsoft.apps.attachment.parser.constant.ErrorCodes;
import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.exception.ParseException;
import com.actionsoft.apps.attachment.parser.model.ParseResult;
import com.actionsoft.apps.attachment.parser.parser.Parser;
import com.actionsoft.apps.attachment.parser.registry.ParserRegistry;
import com.actionsoft.apps.attachment.parser.util.LogUtils;
import com.actionsoft.bpms.util.UtilIO;
import com.actionsoft.sdk.local.SDK;
import com.actionsoft.sdk.local.api.BOAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * 文档解析服务
 * <p>
 * 职责：读取附件 → 委托 ParserRegistry 查找匹配 Parser → 返回 ParseResult
 */
public class DocumentParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentParserService.class);
    private final ParserRegistry registry = ParserRegistry.getInstance();

    public ParseResult parse(String fileId) {
        try {
            BOAPI boApi = SDK.getBOAPI();
            InputStream inputStream = boApi.getFile(fileId);
            if (inputStream == null) {
                LogUtils.fileNotFound(LOGGER, fileId);
                throw new ParseException(ErrorCodes.FILE_NOT_FOUND, "文件不存在", fileId);
            }

            byte[] bytes = UtilIO.readBytesFromStream(inputStream);
            inputStream.close();

            LogUtils.fileRead(LOGGER, fileId, bytes.length);

            for (Parser parser : registry.getParsers()) {
                if (parser.supports(bytes)) {
                    LogUtils.parserStart(LOGGER, parser.getClass().getSimpleName(), fileId);
                    long start = System.currentTimeMillis();
                    ParseResult result = parser.parse(bytes, fileId);
                    result.setParserName(parser.getClass().getSimpleName());
                    result.setSourceDocumentType(result.getType());
                    long cost = System.currentTimeMillis() - start;
                    LogUtils.parserDone(LOGGER, parser.getClass().getSimpleName(),
                            fileId, result.getType(),
                            result.getText() != null ? result.getText().length() : 0, cost);
                    result.addMeta(FieldNames.PARSE_COST, cost);
                    return result;
                }
            }
            return registry.getFallback().parse(bytes, fileId);

        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            LogUtils.parserFail(LOGGER, "unknown", fileId, e);
            throw new ParseException(ErrorCodes.PARSE_ERROR, "解析失败: " + e.getMessage(), fileId, e);
        }
    }
}
