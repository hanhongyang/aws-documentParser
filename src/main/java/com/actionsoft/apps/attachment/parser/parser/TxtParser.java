package com.actionsoft.apps.attachment.parser.parser;

import com.actionsoft.apps.attachment.parser.constant.DocumentType;
import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.model.ParseResult;

/**
 * 纯文本解析器（UTF-8 优先，兼容 GBK）
 */
public class TxtParser implements Parser {

    @Override
    public String getType() { return DocumentType.TXT; }

    @Override
    public boolean supports(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return false;
        int limit = Math.min(4000, bytes.length);
        for (int i = 0; i < limit; i++) {
            if ((bytes[i] & 0xFF) == 0) return false;
        }
        return true;
    }

    @Override
    public ParseResult parse(byte[] bytes, String fileId) {
        ParseResult result = ParseResult.ok(fileId, DocumentType.TXT, bytes.length);
        String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        String encoding = "UTF-8";
        if (text.contains("\uFFFD")) {
            try {
                text = new String(bytes, "GBK");
                encoding = "GBK";
            } catch (Exception ignored) {
                encoding = "UTF-8(含乱码)";
            }
        }
        result.setText(text);
        result.addMeta(FieldNames.ENCODING, encoding);
        return result;
    }
}
