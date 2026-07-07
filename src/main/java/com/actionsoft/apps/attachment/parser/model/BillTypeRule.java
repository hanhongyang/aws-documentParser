package com.actionsoft.apps.attachment.parser.model;

import java.util.List;

/**
 * 费用类型规则实体 — JSON 配置驱动
 */
public class BillTypeRule {

    private String billType;
    private String expenseType;
    private List<String> keywords;

    public String getBillType() { return billType; }
    public void setBillType(String v) { this.billType = v; }

    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String v) { this.expenseType = v; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> v) { this.keywords = v; }
}
