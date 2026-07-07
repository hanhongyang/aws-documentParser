package com.actionsoft.apps.attachment.parser.mapper;

import com.actionsoft.apps.attachment.parser.constant.FieldNames;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表单字段映射器
 * <p>
 * 统一维护 JSON 字段名 → 页面字段编码的映射关系。
 * 当前阶段采用 1:1 映射，后续可在此处调整为 1:N 或 N:1。
 * <p>
 * 所有业务代码通过此类获取字段标识，禁止硬编码字段字符串。
 */
public final class FormFieldMapper {

    private FormFieldMapper() {}

    private static final Map<String, String> FIELD_MAP = new LinkedHashMap<>();

    static {
        // 铁路电子客票表单字段
        FIELD_MAP.put(FieldNames.INVOICE_NO,       FieldNames.INVOICE_NO);
        FIELD_MAP.put(FieldNames.INVOICE_DATE,     FieldNames.INVOICE_DATE);
        FIELD_MAP.put(FieldNames.AMOUNT,           FieldNames.AMOUNT);
        FIELD_MAP.put(FieldNames.BUYER_NAME,       FieldNames.BUYER_NAME);
        FIELD_MAP.put(FieldNames.BUYER_TAX_NO,     FieldNames.BUYER_TAX_NO);
        FIELD_MAP.put(FieldNames.PASSENGER_NAME,   FieldNames.PASSENGER_NAME);
        FIELD_MAP.put(FieldNames.PASSENGER_ID_NO,  FieldNames.PASSENGER_ID_NO);
        FIELD_MAP.put(FieldNames.DEPARTURE_STATION,FieldNames.DEPARTURE_STATION);
        FIELD_MAP.put(FieldNames.ARRIVAL_STATION,  FieldNames.ARRIVAL_STATION);
        FIELD_MAP.put(FieldNames.TRAIN_NO,         FieldNames.TRAIN_NO);
        FIELD_MAP.put(FieldNames.SEAT_TYPE,        FieldNames.SEAT_TYPE);
        FIELD_MAP.put(FieldNames.SEAT_NO,          FieldNames.SEAT_NO);
        FIELD_MAP.put(FieldNames.BOARDING_TIME,    FieldNames.BOARDING_TIME);
        FIELD_MAP.put(FieldNames.CARRIAGE_NO,      FieldNames.CARRIAGE_NO);
        FIELD_MAP.put(FieldNames.TICKET_NO,        FieldNames.TICKET_NO);
        FIELD_MAP.put(FieldNames.EXPENSE_TYPE,     FieldNames.EXPENSE_TYPE);
        FIELD_MAP.put(FieldNames.BILL_TYPE,        FieldNames.BILL_TYPE);
        FIELD_MAP.put(FieldNames.SOURCE_TYPE,      FieldNames.SOURCE_TYPE);
        FIELD_MAP.put(FieldNames.PARSER_NAME,      FieldNames.PARSER_NAME);
        FIELD_MAP.put(FieldNames.PARSE_DURATION,   FieldNames.PARSE_DURATION);
        FIELD_MAP.put(FieldNames.RESULT,           FieldNames.RESULT);

        // 通用
        FIELD_MAP.put(FieldNames.SUPPORTED,        FieldNames.SUPPORTED);
        FIELD_MAP.put(FieldNames.MSG,              FieldNames.MSG);
        FIELD_MAP.put(FieldNames.ERROR,            FieldNames.ERROR);
        FIELD_MAP.put(FieldNames.PAGES,            FieldNames.PAGES);
        FIELD_MAP.put(FieldNames.PARAGRAPHS,       FieldNames.PARAGRAPHS);
        FIELD_MAP.put(FieldNames.SHEETS,           FieldNames.SHEETS);
        FIELD_MAP.put(FieldNames.ENCODING,         FieldNames.ENCODING);
        FIELD_MAP.put(FieldNames.PARSE_COST,       FieldNames.PARSE_COST);
        FIELD_MAP.put(FieldNames.EXTRACT_COST,     FieldNames.EXTRACT_COST);
        FIELD_MAP.put(FieldNames.TOTAL_COST,       FieldNames.TOTAL_COST);
        FIELD_MAP.put(FieldNames.INVOICE_TYPE,     FieldNames.INVOICE_TYPE);
    }

    public static Map<String, String> getFieldMap() {
        return Collections.unmodifiableMap(FIELD_MAP);
    }

    public static String toPageField(String jsonField) {
        return FIELD_MAP.getOrDefault(jsonField, jsonField);
    }
}
