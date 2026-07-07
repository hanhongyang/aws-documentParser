package com.actionsoft.apps.attachment.parser.constant;

/**
 * 错误码常量
 */
public final class ErrorCodes {

    private ErrorCodes() {}

    /** 参数缺失 */
    public static final String MISSING_PARAM = "MISSING_PARAM";

    /** 文件不存在 */
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";

    /** 文件读取失败 */
    public static final String FILE_READ_ERROR = "FILE_READ_ERROR";

    /** 解析失败 */
    public static final String PARSE_ERROR = "PARSE_ERROR";

    /** 提取失败 */
    public static final String EXTRACT_ERROR = "EXTRACT_ERROR";

    /** 不支持的类型 */
    public static final String UNSUPPORTED_TYPE = "UNSUPPORTED_TYPE";

    /** 未知错误 */
    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
}
