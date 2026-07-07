package com.actionsoft.apps.attachment.parser.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 信息提取结果，DocumentExtractorService 的统一返回值
 */
public class ExtractResult {

    private String documentType = "unknown";
    private String expenseType;
    private String invoiceType;
    private Map<String, Object> fields = new LinkedHashMap<>();

    public static ExtractResult unknown() {
        return new ExtractResult();
    }

    // ─── getters / setters ───

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String t) { this.documentType = t; }

    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String t) { this.expenseType = t; }

    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String t) { this.invoiceType = t; }

    public Map<String, Object> getFields() { return fields; }
    public void setFields(Map<String, Object> f) { this.fields = f; }
}
