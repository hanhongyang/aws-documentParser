package com.actionsoft.apps.attachment.parser.parser;

import com.actionsoft.apps.attachment.parser.constant.DocumentType;
import com.actionsoft.apps.attachment.parser.constant.ErrorCodes;
import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.exception.ParseException;
import com.actionsoft.apps.attachment.parser.model.ParseResult;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;

/**
 * Word DOCX 解析器（Apache POI）
 */
public class WordParser implements Parser {

    @Override
    public String getType() { return DocumentType.DOCX; }

    @Override
    public boolean supports(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        if (!isZip(bytes)) return false;
        String s = new String(bytes, 0, Math.min(2048, bytes.length), java.nio.charset.StandardCharsets.UTF_8);
        return s.contains("word/");
    }

    static boolean isZip(byte[] bytes) {
        return (bytes[0] & 0xFF) == 0x50 && (bytes[1] & 0xFF) == 0x4B
            && (bytes[2] & 0xFF) == 0x03 && (bytes[3] & 0xFF) == 0x04;
    }

    @Override
    public ParseResult parse(byte[] bytes, String fileId) {
        ParseResult result = ParseResult.ok(fileId, DocumentType.DOCX, bytes.length);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            result.setText(extractor.getText());
            result.addMeta(FieldNames.PARAGRAPHS, document.getParagraphs().size());
        } catch (Exception e) {
            throw new ParseException(ErrorCodes.PARSE_ERROR,
                    "DOCX 解析失败: " + e.getMessage(), fileId, e);
        }
        return result;
    }
}
