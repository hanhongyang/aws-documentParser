package com.actionsoft.apps.attachment.parser.exception;

/**
 * 附件解析统一异常
 * <p>
 * 所有 Parser、Extractor、Service 统一抛出此异常。
 * ParseAttachmentASLP 最外层捕获并返回友好 JSON。
 */
public class ParseException extends RuntimeException {

    private final String errorCode;
    private final String fileId;

    public ParseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fileId = null;
    }

    public ParseException(String errorCode, String message, String fileId) {
        super(message);
        this.errorCode = errorCode;
        this.fileId = fileId;
    }

    public ParseException(String errorCode, String message, String fileId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.fileId = fileId;
    }

    public String getErrorCode() { return errorCode; }
    public String getFileId() { return fileId; }
}
