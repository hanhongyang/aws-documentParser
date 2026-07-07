package com.actionsoft.apps.attachment.parser.model;

import com.actionsoft.apps.attachment.parser.constant.FieldNames;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析结果，所有 Parser 的统一返回值
 */
public class ParseResult {

    private boolean success;
    private String type;
    private String fileId;
    private int size;
    private String text;
    private String parserName;
    private String sourceDocumentType;
    private Map<String, Object> meta = new LinkedHashMap<>();

    public static ParseResult ok(String fileId, String type, int size) {
        ParseResult r = new ParseResult();
        r.success = true;
        r.fileId = fileId;
        r.type = type;
        r.size = size;
        return r;
    }

    public static ParseResult fail(String type, String fileId, String msg) {
        ParseResult r = new ParseResult();
        r.success = false;
        r.type = type;
        r.fileId = fileId;
        r.meta.put(FieldNames.ERROR, msg);
        return r;
    }

    // ─── getters / setters ───

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean s) { this.success = s; }

    public String getType() { return type; }
    public void setType(String t) { this.type = t; }

    public String getFileId() { return fileId; }
    public void setFileId(String f) { this.fileId = f; }

    public int getSize() { return size; }
    public void setSize(int s) { this.size = s; }

    public String getText() { return text; }
    public void setText(String t) { this.text = t; }

    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> m) { this.meta = m; }

    public String getParserName() { return parserName; }
    public void setParserName(String n) { this.parserName = n; }

    public String getSourceDocumentType() { return sourceDocumentType; }
    public void setSourceDocumentType(String t) { this.sourceDocumentType = t; }

    public void addMeta(String key, Object value) { this.meta.put(key, value); }
}
