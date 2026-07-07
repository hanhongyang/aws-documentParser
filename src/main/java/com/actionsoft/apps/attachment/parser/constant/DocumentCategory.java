package com.actionsoft.apps.attachment.parser.constant;

/**
 * 文档分类枚举 — 程序级路由标识
 */
public enum DocumentCategory {

    RAILWAY_TICKET("铁路电子客票"),
    FLIGHT_TICKET("航空运输电子客票"),
    VAT_INVOICE("增值税电子发票"),
    HOTEL_INVOICE("酒店住宿发票"),
    FUEL_INVOICE("加油发票"),
    CAR_RENTAL_INVOICE("租车发票"),
    TAXI_INVOICE("出租车发票"),
    RIDE_HAILING_INVOICE("网约车行程单"),
    CATERING_INVOICE("餐饮发票"),
    PARKING_INVOICE("停车发票"),
    TOLL_INVOICE("高速通行费发票"),
    OTHER("未知");

    private final String displayName;

    DocumentCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
