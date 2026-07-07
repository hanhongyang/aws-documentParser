package com.actionsoft.apps.attachment.parser.extractor;

import com.actionsoft.apps.attachment.parser.constant.DocumentCategory;
import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.model.ExtractResult;
import com.actionsoft.apps.attachment.parser.model.ParseResult;

/**
 * 增值税电子发票信息提取器
 * <p>
 * 从 PDF 解析文本中提取增值税电子发票的结构化字段。
 * 支持电子普票和电子专票的常见格式。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>设置 DOCUMENT_CATEGORY = VAT_INVOICE</li>
 *   <li>设置 INVOICE_TYPE = "增值税电子发票"</li>
 *   <li>提取发票事实字段（发票号码、日期、金额、买卖方等）</li>
 *   <li><b>不设置 EXPENSE_TYPE</b> — 费用分类由 BillTypeDetector 统一负责</li>
 * </ul>
 */
public class VatInvoiceExtractor extends AbstractRegexExtractor {

    private static final String[] KEYWORDS = {
            "电子发票", "增值税电子", "价税合计", "销售方", "购买方"
    };

    // ─── 发票号码 ───
    private static final String RE_INVOICE_NO_LABEL =
            "(?:发票号码|No|NO|编号)[：:]\\s*([A-Za-z0-9\\-]+)";
    private static final String RE_INVOICE_NO_GENERIC =
            "\\b(\\d{20}|\\d{12}|\\d{10})\\b";

    // ─── 开票日期 ───
    private static final String RE_INVOICE_DATE_LABEL =
            "(?:开票日期|填开日期)[：:]\\s*(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)";
    private static final String RE_INVOICE_DATE_PLAIN =
            "(\\d{4})-(\\d{1,2})-(\\d{1,2})";

    // ─── 销售方 ───
    private static final String RE_SELLER_NAME =
            "(?:销售方名称|销方名称|收款人)[：:]\\s*([\\u4e00-\\u9fa5()（）]{4,40})";
    private static final String RE_SELLER_TAX_NO =
            "(?:销售方.*?纳税人识别号|销方.*?纳税识别号|销方.*?税号)[：:]\\s*([0-9A-Z]{15,20})";

    // ─── 购买方 ───
    private static final String RE_BUYER_NAME_LABEL =
            "(?:购买方名称|名称)[：:]\\s*([\\u4e00-\\u9fa5()（）]{4,40})";
    private static final String RE_BUYER_TAX_NO_LABEL =
            "(?:购买方.*?纳税人识别号|统一社会信用代码|纳税人识别号)[：:]\\s*([0-9A-Z]{15,20})";

    // ─── 金额 ───
    private static final String RE_AMOUNT =
            "(?:金额合计|合计金额|金额|小写)[：:]\\s*[￥¥]?\\s*([\\d,]+\\.\\d{2})";
    private static final String RE_TAX_AMOUNT =
            "(?:税额合计|税额|税款)[：:]\\s*[￥¥]?\\s*([\\d,]+\\.\\d{2})";
    private static final String RE_TOTAL_AMOUNT_SMALL =
            "(?:价税合计.*?小写)[：:]\\s*[￥¥]?\\s*([\\d,]+\\.\\d{2})";
    private static final String RE_TOTAL_AMOUNT_GENERIC =
            "(?:价税合计|总金额)[：:]\\s*[￥¥]?\\s*([\\d,]+\\.\\d{2})";
    private static final String RE_TOTAL_BACKUP =
            "[￥¥]\\s*([\\d,]+\\.\\d{2})";

    // ─── 货物/服务名称 ───
    private static final String RE_ITEM_NAME =
            "(?:货物或应税劳务[、.]服务名称|项目名称|商品名称)[：:]\\s*(.+?)(?:\\n|规格|单位|数量)";
    private static final String RE_ITEM_STAR =
            "\\*([\\u4e00-\\u9fa5]+)\\*";

    // ─── 备注 / 开票人 ───
    private static final String RE_REMARK =
            "(?:备注)[：:]\\s*(.+?)(?:\\n|收款人|复核|开票人)";
    private static final String RE_ISSUER_NAME =
            "(?:开票人)[：:]\\s*([\\u4e00-\\u9fa5a-zA-Z0-9]{2,10})";

    // ══════════════════════════════════════════════════════

    @Override
    public boolean supports(ParseResult parseResult) {
        if (!parseResult.isSuccess()) return false;
        String text = parseResult.getText();
        if (text == null || text.isEmpty()) return false;

        String t = fullWidthToHalf(text);
        int matchCount = 0;
        for (String kw : KEYWORDS) {
            if (t.contains(kw)) matchCount++;
        }
        return matchCount >= 1;
    }

    @Override
    protected ExtractResult doExtract(String text, ParseResult parseResult) {
        long start = System.currentTimeMillis();
        ExtractResult result = new ExtractResult();

        // 文档分类 + 票据类型（Extractor 负责）
        result.setDocumentType(DocumentCategory.VAT_INVOICE.name());
        result.setInvoiceType("增值税电子发票");

        // 发票事实字段提取
        safePut(result, FieldNames.INVOICE_NO,    extractInvoiceNo(text));
        safePut(result, FieldNames.INVOICE_DATE,  extractInvoiceDate(text));
        safePut(result, FieldNames.SELLER_NAME,   findFirst(RE_SELLER_NAME, text));
        safePut(result, FieldNames.SELLER_TAX_NO, findFirst(RE_SELLER_TAX_NO, text));
        safePut(result, FieldNames.BUYER_NAME,    findFirst(RE_BUYER_NAME_LABEL, text));
        safePut(result, FieldNames.BUYER_TAX_NO,  findFirst(RE_BUYER_TAX_NO_LABEL, text));

        safePut(result, FieldNames.AMOUNT,        normalizeMoney(findFirst(RE_AMOUNT, text)));
        safePut(result, FieldNames.TAX_AMOUNT,    normalizeMoney(findFirst(RE_TAX_AMOUNT, text)));
        safePut(result, FieldNames.TOTAL_AMOUNT,  extractTotalAmount(text));
        safePut(result, FieldNames.ITEM_NAME,     extractItemName(text));
        safePut(result, FieldNames.REMARK,        findFirst(RE_REMARK, text));
        safePut(result, FieldNames.ISSUER_NAME,   findFirst(RE_ISSUER_NAME, text));

        long cost = System.currentTimeMillis() - start;
        logger.info("[附件解析服务] VatInvoiceExtractor 提取完成: fileId={}, fields={}, cost={}ms",
                parseResult.getFileId(), result.getFields().keySet(), cost);

        return result;
    }

    // ══════════════════════════════════════════════════════
    // 提取方法
    // ══════════════════════════════════════════════════════

    private String extractInvoiceNo(String text) {
        String no = findFirst(RE_INVOICE_NO_LABEL, text);
        if (no != null) return no;
        return findFirst(RE_INVOICE_NO_GENERIC, text);
    }

    private String extractInvoiceDate(String text) {
        String date = findFirst(RE_INVOICE_DATE_LABEL, text);
        if (date != null) return normalizeDate(date);

        java.util.regex.Matcher m = com.actionsoft.apps.attachment.parser.util.RegexUtils
                .matcher(RE_INVOICE_DATE_PLAIN, text);
        if (m.find()) {
            return m.group(1) + "-" + padZero(m.group(2)) + "-" + padZero(m.group(3));
        }
        return null;
    }

    private String extractTotalAmount(String text) {
        // ① 价税合计（小写）
        String total = normalizeMoney(findFirst(RE_TOTAL_AMOUNT_SMALL, text));
        if (total != null) return total;

        // ② 价税合计 / 总金额
        total = normalizeMoney(findFirst(RE_TOTAL_AMOUNT_GENERIC, text));
        if (total != null) return total;

        // ③ ¥金额兜底
        return normalizeMoney(findFirst(RE_TOTAL_BACKUP, text));
    }

    private String extractItemName(String text) {
        // ① 标签匹配
        String name = findFirst(RE_ITEM_NAME, text);
        if (name != null) {
            name = name.trim();
            if (name.length() > 40) {
                name = name.substring(0, 40);
            }
        }

        // ② *中文* 格式兜底
        if (name == null) {
            name = findFirst(RE_ITEM_STAR, text);
        }
        return name;
    }

    // ══════════════════════════════════════════════════════
    // 工具方法
    // ══════════════════════════════════════════════════════

    private String padZero(String s) {
        if (s == null) return "00";
        if (s.length() == 1) return "0" + s;
        return s;
    }

    /**
     * 安全填充字段（null 或空字符串不存入 map）
     */
    private void safePut(ExtractResult result, String key, String value) {
        if (value != null && !value.isEmpty()) {
            result.getFields().put(key, value);
        }
    }
}
