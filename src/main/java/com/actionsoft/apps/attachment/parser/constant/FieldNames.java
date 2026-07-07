package com.actionsoft.apps.attachment.parser.constant;

/**
 * 字段名常量（与表单 fieldName 完全一致）
 */
public final class FieldNames {

    private FieldNames() {}

    // ─── 表单字段（与 XML fieldName 完全一致） ───

    /** 附件 */
    public static final String FILE = "FILE";

    /** 解析结果 */
    public static final String RESULT = "RESULT";

    /** 发票类型 */
    public static final String INVOICE_TYPE = "INVOICE_TYPE";

    /** 发票号码 */
    public static final String INVOICE_NO = "INVOICE_NO";

    /** 开票日期 */
    public static final String INVOICE_DATE = "INVOICE_DATE";

    /** 金额 */
    public static final String AMOUNT = "AMOUNT";

    /** 购方名称 */
    public static final String BUYER_NAME = "BUYER_NAME";

    /** 费用类型（大类） */
    public static final String EXPENSE_TYPE = "EXPENSE_TYPE";

    /** 费用类型（具体类型：火车票/飞机票/出租车费等） */
    public static final String BILL_TYPE = "BILL_TYPE";

    /** 税号 */
    public static final String BUYER_TAX_NO = "BUYER_TAX_NO";

    /** 乘车人 */
    public static final String PASSENGER_NAME = "PASSENGER_NAME";

    /** 身份证 */
    public static final String PASSENGER_ID_NO = "PASSENGER_ID_NO";

    /** 车次 */
    public static final String TRAIN_NO = "TRAIN_NO";

    /** 坐席 */
    public static final String SEAT_TYPE = "SEAT_TYPE";

    /** 出发站 */
    public static final String DEPARTURE_STATION = "DEPARTURE_STATION";

    /** 到达站 */
    public static final String ARRIVAL_STATION = "ARRIVAL_STATION";

    /** 车厢 */
    public static final String CARRIAGE_NO = "CARRIAGE_NO";

    /** 座位 */
    public static final String SEAT_NO = "SEAT_NO";

    /** 乘车时间 */
    public static final String BOARDING_TIME = "BOARDING_TIME";

    /** 电子客票号 */
    public static final String TICKET_NO = "TICKET_NO";

    /** 来源类型 */
    public static final String SOURCE_TYPE = "SOURCE_TYPE";

    /** 解析器名称 */
    public static final String PARSER_NAME = "PARSER_NAME";

    /** 解析耗时 */
    public static final String PARSE_DURATION = "PARSE_DURATION";

    /** 附件完整信息（供前端 $dataExtend.FIELD.extends.fileList 使用） */
    public static final String FILE_INFO = "FILE_INFO";

    /** 销售方名称 */
    public static final String SELLER_NAME = "SELLER_NAME";

    /** 销售方税号 */
    public static final String SELLER_TAX_NO = "SELLER_TAX_NO";

    /** 税额 */
    public static final String TAX_AMOUNT = "TAX_AMOUNT";

    /** 价税合计（含税总金额） */
    public static final String TOTAL_AMOUNT = "TOTAL_AMOUNT";

    /** 货物/服务名称 */
    public static final String ITEM_NAME = "ITEM_NAME";

    /** 备注 */
    public static final String REMARK = "REMARK";

    /** 开票人 */
    public static final String ISSUER_NAME = "ISSUER_NAME";

    /** 文档分类（RAILWAY_TICKET/VAT_INVOICE等） */
    public static final String DOCUMENT_CATEGORY = "DOCUMENT_CATEGORY";

    /** 在线预览 URL */
    public static final String VIEW_URL = "VIEW_URL";

    /** 附件原始文件名 */
    public static final String VIEW_NAME = "VIEW_NAME";

    // ─── 内部 meta 字段 ───

    public static final String SUPPORTED = "supported";
    public static final String MSG = "msg";
    public static final String ERROR = "error";
    public static final String PAGES = "pages";
    public static final String PARAGRAPHS = "paragraphs";
    public static final String SHEETS = "sheets";
    public static final String ENCODING = "encoding";
    public static final String PARSE_COST = "parseCost";
    public static final String EXTRACT_COST = "extractCost";
    public static final String TOTAL_COST = "totalCost";
    public static final String TEXT = "text";
    public static final String TYPE = "type";
    public static final String SIZE = "size";
}
