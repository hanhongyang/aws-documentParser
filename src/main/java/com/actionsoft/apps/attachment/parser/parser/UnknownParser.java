package com.actionsoft.apps.attachment.parser.parser;

import com.actionsoft.apps.attachment.parser.constant.DocumentType;
import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.model.ParseResult;

/**
 * 兜底解析器 — 当没有匹配的 Parser 时使用
 */
public class UnknownParser implements Parser {

    @Override
    public String getType() { return DocumentType.UNKNOWN; }

    @Override
    public boolean supports(byte[] bytes) {
        return true; // 兜底，永远匹配
    }

    @Override
    public ParseResult parse(byte[] bytes, String fileId) {
        ParseResult result = ParseResult.ok(fileId, DocumentType.UNKNOWN, bytes.length);
        result.addMeta(FieldNames.SUPPORTED, false);
        result.addMeta(FieldNames.MSG, "不支持的文件类型");
        return result;
    }
}
