package com.actionsoft.apps.attachment.parser.util;

import com.actionsoft.apps.attachment.parser.model.BillTypeRule;
import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 费用类型检测器（统一入口，配置驱动）
 * <p>
 * 从 config/bill-type-rules.json 加载规则，失败则回退到 hardcodedRules()。
 *
 * <h3>已支持类型</h3>
 * <ul>
 *   <li>火车票</li>
 *   <li>飞机票</li>
 *   <li>出租车费</li>
 *   <li>网约车费</li>
 *   <li>加油费</li>
 *   <li>住宿费</li>
 *   <li>餐饮费</li>
 *   <li>停车费</li>
 *   <li>高速通行费</li>
 *   <li>租车费</li>
 *   <li>其它费用（兜底）</li>
 * </ul>
 */
public final class BillTypeDetector {

    private BillTypeDetector() {}

    /** 火车票 */
    public static final String BILL_TRAIN = "火车票";
    /** 飞机票 */
    public static final String BILL_FLIGHT = "飞机票";
    /** 出租车 */
    public static final String BILL_TAXI = "出租车费";
    /** 网约车 */
    public static final String BILL_RIDE_HAILING = "网约车费";
    /** 加油 */
    public static final String BILL_FUEL = "加油费";
    /** 住宿 */
    public static final String BILL_HOTEL = "住宿费";
    /** 餐饮 */
    public static final String BILL_MEAL = "餐饮费";
    /** 停车 */
    public static final String BILL_PARKING = "停车费";
    /** 高速/ETC */
    public static final String BILL_HIGHWAY = "高速通行费";
    /** 租车 */
    public static final String BILL_CAR_RENTAL = "租车费";
    /** 兜底 */
    public static final String BILL_OTHER = "其它费用";

    // ─── 运行时数据（静态初始化） ───

    private static final List<BillTypeRule> RULES;
    private static final Map<String, String> EXPENSE_MAP;
    private static final boolean CONFIG_LOADED;

    static {
        List<BillTypeRule> rules = null;
        Map<String, String> expenseMap = null;
        boolean loaded = false;

        try {
            InputStream is = BillTypeDetector.class.getClassLoader()
                    .getResourceAsStream("config/bill-type-rules.json");
            if (is != null) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                rules = JSON.parseArray(sb.toString(), BillTypeRule.class);
                expenseMap = new LinkedHashMap<>();
                for (BillTypeRule rule : rules) {
                    expenseMap.put(rule.getBillType(), rule.getExpenseType());
                }
                loaded = true;
            }
        } catch (Exception ignored) {
            // JSON 加载失败 → 使用 hardcoded fallback
        }

        if (rules == null || rules.isEmpty()) {
            rules = hardcodedRules();
        }
        if (expenseMap == null || expenseMap.isEmpty()) {
            expenseMap = hardcodedExpenseMap();
        }

        RULES = rules;
        EXPENSE_MAP = expenseMap;
        CONFIG_LOADED = loaded;
    }

    /**
     * 根据文本自动识别费用类型（BILL_TYPE）
     */
    public static String detect(String text) {
        if (text == null || text.isEmpty()) {
            return BILL_OTHER;
        }

        String t = TextUtils.fullWidthToHalf(text);

        for (BillTypeRule rule : RULES) {
            if (containsAny(t, rule.getKeywords())) {
                return rule.getBillType();
            }
        }

        return BILL_OTHER;
    }

    /**
     * 根据 BILL_TYPE 获取对应的 EXPENSE_TYPE（费用大类）
     */
    public static String detectExpenseType(String billType) {
        if (billType == null) {
            return "其他费用";
        }
        String expenseType = EXPENSE_MAP.get(billType);
        return expenseType != null ? expenseType : "其他费用";
    }

    /** 是否成功从 JSON 加载配置 */
    public static boolean isConfigLoaded() {
        return CONFIG_LOADED;
    }

    /** 获取当前生效的全部规则 */
    public static List<BillTypeRule> getRules() {
        return Collections.unmodifiableList(RULES);
    }

    // ─── 兜底规则 ───

    /**
     * 硬编码兜底规则（JSON 加载失败时使用）
     */
    private static List<BillTypeRule> hardcodedRules() {
        List<BillTypeRule> list = new ArrayList<>();

        add(list, BILL_TRAIN, "交通费",
                "铁路电子客票", "电子客票", "12306", "列车", "铁路",
                "二等座", "一等座", "商务座", "硬座", "软座", "硬卧", "软卧",
                "动车", "高铁", "车次", "乘车人", "席别");

        add(list, BILL_FLIGHT, "交通费",
                "航空运输电子客票", "航空运输", "Air Ticket",
                "航班号", "登机牌", "承运人", "舱位", "ETKT", "ETICKET");

        add(list, BILL_TAXI, "交通费",
                "出租汽车", "出租车", "Taxi", "的士", "巡游出租");

        add(list, BILL_RIDE_HAILING, "交通费",
                "滴滴", "高德打车", "曹操", "T3出行", "神州专车",
                "如祺", "首约", "花小猪", "享道", "T3");

        add(list, BILL_FUEL, "车辆费用",
                "成品油", "汽油", "柴油", "加油", "燃油", "石油", "中石油", "中石化");

        add(list, BILL_HOTEL, "住宿费",
                "酒店", "宾馆", "住宿", "Hotel",
                "入住", "退房", "客房", "旅店", "招待所");

        add(list, BILL_MEAL, "餐饮费",
                "餐饮", "餐费", "饭店", "酒楼",
                "餐厅", "火锅", "自助", "快餐", "食堂");

        add(list, BILL_PARKING, "车辆费用",
                "停车费", "停车", "停车场");

        add(list, BILL_HIGHWAY, "车辆费用",
                "高速", "ETC", "通行费", "高速公路", "收费站");

        add(list, BILL_CAR_RENTAL, "交通费",
                "租车", "汽车租赁", "车辆服务");

        return list;
    }

    /**
     * 硬编码 EXPENSE_TYPE 映射（JSON 加载失败时使用）
     */
    private static Map<String, String> hardcodedExpenseMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(BILL_TRAIN, "交通费");
        map.put(BILL_FLIGHT, "交通费");
        map.put(BILL_TAXI, "交通费");
        map.put(BILL_RIDE_HAILING, "交通费");
        map.put(BILL_CAR_RENTAL, "交通费");
        map.put(BILL_FUEL, "车辆费用");
        map.put(BILL_HOTEL, "住宿费");
        map.put(BILL_MEAL, "餐饮费");
        map.put(BILL_PARKING, "车辆费用");
        map.put(BILL_HIGHWAY, "车辆费用");
        return map;
    }

    private static void add(List<BillTypeRule> list, String billType,
                            String expenseType, String... keywords) {
        BillTypeRule rule = new BillTypeRule();
        rule.setBillType(billType);
        rule.setExpenseType(expenseType);
        rule.setKeywords(Arrays.asList(keywords));
        list.add(rule);
    }

    private static boolean containsAny(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
