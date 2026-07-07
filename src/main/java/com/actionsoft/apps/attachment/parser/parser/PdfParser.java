package com.actionsoft.apps.attachment.parser.parser;

import com.actionsoft.apps.attachment.parser.constant.DocumentType;
import com.actionsoft.apps.attachment.parser.constant.ErrorCodes;
import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.exception.ParseException;
import com.actionsoft.apps.attachment.parser.model.ParseResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PDF 解析器（Apache PDFBox）
 */
public class PdfParser implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfParser.class);

    @Override
    public String getType() { return DocumentType.PDF; }

    @Override
    public boolean supports(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        return (bytes[0] & 0xFF) == 0x25 && (bytes[1] & 0xFF) == 0x50
            && (bytes[2] & 0xFF) == 0x44 && (bytes[3] & 0xFF) == 0x46;
    }

    @Override
    public ParseResult parse(byte[] bytes, String fileId) {
        ParseResult result = ParseResult.ok(fileId, DocumentType.PDF, bytes.length);
        try (PDDocument document = PDDocument.load(bytes)) {
            int pages = document.getNumberOfPages();
            result.addMeta(FieldNames.PAGES, pages);
            PDFTextStripper stripper = new PDFTextStripper();
            result.setText(stripper.getText(document));
        } catch (Exception e) {
            throw new ParseException(ErrorCodes.PARSE_ERROR,
                    "PDF 解析失败: " + e.getMessage(), fileId, e);
        }
        return result;
    }
}
