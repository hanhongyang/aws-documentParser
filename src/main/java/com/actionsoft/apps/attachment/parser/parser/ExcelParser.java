package com.actionsoft.apps.attachment.parser.parser;

import com.actionsoft.apps.attachment.parser.constant.DocumentType;
import com.actionsoft.apps.attachment.parser.constant.ErrorCodes;
import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.exception.ParseException;
import com.actionsoft.apps.attachment.parser.model.ParseResult;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;

/**
 * Excel XLSX 解析器（Apache POI）
 */
public class ExcelParser implements Parser {

    @Override
    public String getType() { return DocumentType.XLSX; }

    @Override
    public boolean supports(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        if (!WordParser.isZip(bytes)) return false;
        String s = new String(bytes, 0, Math.min(2048, bytes.length), java.nio.charset.StandardCharsets.UTF_8);
        return s.contains("xl/");
    }

    @Override
    public ParseResult parse(byte[] bytes, String fileId) {
        ParseResult result = ParseResult.ok(fileId, DocumentType.XLSX, bytes.length);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            int sheets = workbook.getNumberOfSheets();
            result.addMeta(FieldNames.SHEETS, sheets);
            result.setText(new XSSFExcelExtractor(workbook).getText());
        } catch (Exception e) {
            throw new ParseException(ErrorCodes.PARSE_ERROR,
                    "XLSX 解析失败: " + e.getMessage(), fileId, e);
        }
        return result;
    }
}
