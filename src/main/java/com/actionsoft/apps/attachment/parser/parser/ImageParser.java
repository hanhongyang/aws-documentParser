package com.actionsoft.apps.attachment.parser.parser;

import com.actionsoft.apps.attachment.parser.constant.DocumentType;
import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.model.ParseResult;

/**
 * 图片解析器（暂未实现 OCR）
 */
public class ImageParser implements Parser {

    @Override
    public String getType() { return DocumentType.IMAGE; }

    @Override
    public boolean supports(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        int b0 = bytes[0] & 0xFF, b1 = bytes[1] & 0xFF, b2 = bytes[2] & 0xFF, b3 = bytes[3] & 0xFF;
        return (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) || (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47);
    }

    @Override
    public ParseResult parse(byte[] bytes, String fileId) {
        String sub = (((bytes[0] & 0xFF) == 0xFF) ? DocumentType.JPG : DocumentType.PNG);
        ParseResult result = ParseResult.ok(fileId, sub, bytes.length);
        result.addMeta(FieldNames.SUPPORTED, false);
        result.addMeta(FieldNames.MSG, "图片 OCR 暂未实现");
        return result;
    }
}
