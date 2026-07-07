package com.actionsoft.apps.attachment.parser.extractor;

import com.actionsoft.apps.attachment.parser.constant.DocumentType;
import com.actionsoft.apps.attachment.parser.constant.FieldNames;
import com.actionsoft.apps.attachment.parser.model.ExtractResult;
import com.actionsoft.apps.attachment.parser.model.ParseResult;
import com.actionsoft.apps.attachment.parser.util.RegexUtils;

import java.util.Map;

/**
 * 铁路电子客票信息提取器（生产版）
 * <p>
 * 从 PDF 解析文本中提取铁路电子客票的结构化字段。
 * 每个字段支持多个正则 Pattern，从精确到模糊依次尝试。
 * 提取失败返回 null，不抛异常。
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>多 Pattern 串行匹配：每个字段 2-4 个 Pattern，从精确标签匹配到通用格式兜底</li>
 *   <li>渐进退化：优先依赖明确的中文标签，若无则退化为位置/格式启发式</li>
 *   <li>零异常：所有字段独立提取，单字段失败不影响其它字段</li>
 * </ul>
 */
public class RailwayInvoiceExtractor extends AbstractRegexExtractor {

    // ══════════════════════════════════════════════════════
    // 一、特征关键词（supports 判定）
    // ══════════════════════════════════════════════════════

    private static final String[] KEYWORDS = {
            "铁路电子客票", "电子发票（铁路电子客票）", "12306",
            "电子客票号", "车次", "乘车日期"
    };

    // ══════════════════════════════════════════════════════
    // 二、发票号码 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    /** 发票号码: 标签明确 */
    private static final String RE_INVOICE_NO_LABEL =
            "(?:发票号码|号码|发票代码|No)[：:]\\s*([A-Za-z0-9]+)";

    /** 编号: 标签模糊 */
    private static final String RE_INVOICE_NO_CODE =
            "(?:编号|票据号码|电子客票号)[：:]\\s*([A-Za-z0-9]+)";

    /** 兜底: 20 位数字（新版发票号码常见 20 位） */
    private static final String RE_INVOICE_NO_RAW = "\\b(\\d{10,25})\\b";

    // ══════════════════════════════════════════════════════
    // 三、开票日期 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_INVOICE_DATE_LABEL =
            "(?:开票日期|填发日期)[：:]\\s*(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)";

    private static final String RE_INVOICE_DATE_GENERIC =
            "(?:日期)[：:]\\s*(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2})";

    /** 兜底: 年份标签附近的日期 */
    private static final String RE_INVOICE_DATE_YEAR =
            "(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日";

    /** 兜底: 纯数字日期（2026-04-24） */
    private static final String RE_INVOICE_DATE_PLAIN =
            "(\\d{4})-(\\d{1,2})-(\\d{1,2})";

    // ══════════════════════════════════════════════════════
    // 四、金额 — 4 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_AMOUNT_LABEL =
            "(?:金额|票价|合计金额|价税合计)[：:]\\s*[￥¥]?\\s*([\\d,]+\\.\\d{2})";

    private static final String RE_AMOUNT_YUAN =
            "[￥¥]\\s*([\\d,]+\\.\\d{2})";

    private static final String RE_AMOUNT_FUZZY =
            "(?:小写|合计)[：:]?\\s*[￥¥]?\\s*([\\d,]+\\.\\d{2})";

    /** 兜底: 含中文"元"的金额 */
    private static final String RE_AMOUNT_CN =
            "([\\d,]+\\.\\d{2})\\s*元";

    // ══════════════════════════════════════════════════════
    // 五、购买方名称 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_BUYER_NAME_LABEL =
            "(?:购买方名称|名称)[：:]\\s*([\\u4e00-\\u9fa5()（）]{4,40})";

    private static final String RE_BUYER_NAME_BEFORE_TAX =
            "([\\u4e00-\\u9fa5()（）]{4,40})\\s*(?:统一社会信用代码|纳税人识别号)";

    /** 兜底: 紧邻税号前的长中文 */
    private static final String RE_BUYER_NAME_ADJACENT =
            "([\\u4e00-\\u9fa5()（）]{4,40})\\s*[0-9A-Z]{15,20}";

    // ══════════════════════════════════════════════════════
    // 六、统一社会信用代码 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_TAX_NO_LABEL =
            "(?:统一社会信用代码|纳税人识别号|税号)[：:]\\s*([0-9A-Z]{15,20})";

    private static final String RE_TAX_NO_CODE =
            "(?:信用代码|识别号)[：:]\\s*([0-9A-Z]{15,20})";

    /** 兜底: 18 位数字字母组合 */
    private static final String RE_TAX_NO_GENERIC = "\\b([0-9A-Z]{18})\\b";

    // ══════════════════════════════════════════════════════
    // 七、乘车人 — 4 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_PASSENGER_LABEL =
            "(?:乘车人|旅客姓名|旅客)[：:]\\s*([\\u4e00-\\u9fa5]{2,4})";

    private static final String RE_PASSENGER_NAME_ONLY =
            "(?:姓名)[：:]\\s*([\\u4e00-\\u9fa5]{2,4})";

    /** 脱敏身份证 + 姓名: "3701121989****5656 王春玺" */
    private static final String RE_PASSENGER_MASKED_ID =
            "\\d{6,10}\\*{4,8}\\d{3,4}\\s+([\\u4e00-\\u9fa5]{2,4})";

    /** 兜底: 中文名（身份证号附近 30 字符内） */
    private static final String RE_PASSENGER_ADJACENT =
            "([\\u4e00-\\u9fa5]{2,4})\\s*.{0,30}?\\b\\d{17}[0-9Xx]\\b";

    // ══════════════════════════════════════════════════════
    // 八、身份证号 — 2 个 Pattern（严格防误识别）
    // ══════════════════════════════════════════════════════

    /** 身份证显式标签（不含"号码"以免匹配发票号码) */
    private static final String RE_ID_CARD_LABEL =
            "(?:身份证号|证件号码|身份证)[：:]\\s*(\\d{15,18}[0-9Xx]?)";

    /** 脱敏身份证（直接返回脱敏字符串）: "3701121989****5656" */
    private static final String RE_ID_CARD_MASKED =
            "\\b(\\d{6,10}\\*{4,8}\\d{3,4})\\b";

    /** 兜底: 孤立 18 位数字（严格前后非数字，防止长数字串误识别） */
    private static final String RE_ID_CARD_ISOLATED =
            "(?<!\\d)(\\d{17}[0-9Xx])(?!\\d)";

    // ══════════════════════════════════════════════════════
    // 九、车站 — 多 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_FROM_STATION_LABEL =
            "(?:出发站|始发站|发站|起点站)[：:]\\s*([\\u4e00-\\u9fa5]{2,10})";

    private static final String RE_TO_STATION_LABEL =
            "(?:到达站|终点站|到站|目的站)[：:]\\s*([\\u4e00-\\u9fa5]{2,10})";

    private static final String RE_STATION_PAIR =
            "([\\u4e00-\\u9fa5]{2,10})\\s*[→\\-至到–—]\\s*([\\u4e00-\\u9fa5]{2,10})";

    /** 兜底: "站-站" 格式 */
    private static final String RE_STATION_DASH =
            "([\\u4e00-\\u9fa5]{2,6})站?\\s*-\\s*([\\u4e00-\\u9fa5]{2,6})站?";

    /** 连续站名（无分隔符）: "许昌东站漯河西站" → group1=许昌东, group2=漯河西 */
    private static final String RE_STATION_CONTINUOUS =
            "([\\u4e00-\\u9fa5]{2,6})站([\\u4e00-\\u9fa5]{2,6})站";

    // ══════════════════════════════════════════════════════
    // 十、车次 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_TRAIN_NO_LABEL =
            "(?:车次|列车|车号)[：:]\\s*([GCDKZTgcldkzt]\\d{1,4})";

    private static final String RE_TRAIN_NO_GENERIC =
            "\\b([GCDKZTgcldkzt]\\d{1,4})\\b";

    /** 兜底: 字母+数字组合（如 G2080） */
    private static final String RE_TRAIN_NO_FALLBACK =
            "\\b([A-Za-z]\\d{3,4})\\b";

    /** 无词边界兜底: "G1278" 单独一行时 \b 可能不生效 */
    private static final String RE_TRAIN_NO_PLAIN =
            "(?:^|\\n|\\s)([GCDKZTgcldkzt]\\d{1,4})(?:$|\\n|\\s)";

    // ══════════════════════════════════════════════════════
    // 十一、席别/座位等级 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_SEAT_TYPE_LABEL =
            "(?:席别|座位等级|席位|席)[：:]\\s*([\\u4e00-\\u9fa5]{2,4})";

    /** 兜底: 常见席别关键词匹配 */
    private static final String RE_SEAT_TYPE_KEYWORD =
            "(二等座|一等座|商务座|特等座|硬座|软座|硬卧|软卧|高级软卧|动卧|无座)";

    private static final String RE_SEAT_TYPE_GRADE =
            "(\\w{1,2}等座|\\w{1,2}卧)";

    // ══════════════════════════════════════════════════════
    // 十二、座位号 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_SEAT_NO_LABEL =
            "(?:座位号|座位|座号)[：:]\\s*(\\d{1,2}[A-Fa-f])";

    /** 兜底: 标准位置格式 数字+字母（如 10F） */
    private static final String RE_SEAT_NO_GENERIC = "\\b(\\d{1,2}[A-Fa-f])\\b";

    private static final String RE_SEAT_NO_CN =
            "(?:(\\d{1,2})\\s*排\\s*(\\w)\\s*座|(\\d{1,2})\\s*车\\s*(\\w)\\s*座)";

    // ══════════════════════════════════════════════════════
    // 十三、铁路票组合 Pattern（优先于离散匹配）
    // ══════════════════════════════════════════════════════

    /** 一次提取开车时间+车厢号+座位号: "09:29开 03车07A号" */
    private static final String RE_TRAIN_COMBO =
            "(\\d{2}:\\d{2})\\s*开\\s*(\\d{2})\\s*车\\s*(\\d{2}[A-Fa-f])\\s*号";

    private static final String RE_DEPART_DATE_LABEL =
            "(?:乘车日期|出发日期|开车日期|发车日期)[：:]\\s*(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)";

    /** 兜底: 离开票日期最近的另一个日期 */
    private static final String RE_DEPART_DATE_GENERIC =
            "\\b(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2})\\b";

    // ══════════════════════════════════════════════════════
    // 十四、开车时间 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_DEPART_TIME_LABEL =
            "(?:开车时间|发车时间|出发时间|开点)[：:]\\s*(\\d{1,2}:\\d{2})";

    /** 兜底: 标准时间格式 HH:MM */
    private static final String RE_DEPART_TIME_GENERIC = "\\b(\\d{1,2}:\\d{2})\\b";

    /** 兜底: 中文时间 */
    private static final String RE_DEPART_TIME_CN =
            "(\\d{1,2})\\s*点\\s*(\\d{1,2})\\s*分";

    // ══════════════════════════════════════════════════════
    // 十五、车厢号 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_CAR_NO_LABEL =
            "(?:车厢号|车厢|车号)[：:]\\s*(\\d{1,2})";

    /** 兜底: 中文车厢描述 */
    private static final String RE_CAR_NO_CN = "(\\d{1,2})\\s*[号#]\\s*车厢";

    /** 兜底: 纯数字车厢号 */
    private static final String RE_CAR_NO_GENERIC = "\\b(\\d{1,2})\\s*车厢?\\b";

    /** 兜底: 无"厢"格式 "03车" */
    private static final String RE_CAR_NO_PLAIN = "\\b(\\d{1,2})\\s*车\\b";

    // ══════════════════════════════════════════════════════
    // 十六、电子客票号 — 3 个 Pattern
    // ══════════════════════════════════════════════════════

    private static final String RE_ETICKET_NO_LABEL =
            "(?:电子客票号|电子票号|客票号|E-Ticket)[：:]\\s*([A-Za-z0-9]+)";

    /** 兜底: 较长字母数字组合（支持纯数字，25 位） */
    private static final String RE_ETICKET_NO_GENERIC =
            "\\b([A-Za-z0-9]{12,30})\\b";

    /** 兜底: 以字母开头的电子客票编号 */
    private static final String RE_ETICKET_NO_PREFIX =
            "\\b([A-Z]{1,2}\\d{10,16})\\b";

    /** 兜底: 纯数字长编号（新版电子客票号常见全数字） */
    private static final String RE_ETICKET_NO_DIGITS =
            "\\b(\\d{20,30})\\b";

    // ══════════════════════════════════════════════════════
    // 入口
    // ══════════════════════════════════════════════════════

    @Override
    public boolean supports(ParseResult parseResult) {
        if (!parseResult.isSuccess()) return false;
        String text = parseResult.getText();
        if (text == null || text.isEmpty()) return false;

        String normalized = fullWidthToHalf(text);
        // "铁路电子客票" 或其变体命中 1 个即激活；降级为任何关键词命中 2 个
        if (normalized.contains("铁路电子客票")
                || normalized.contains("铁路") && normalized.contains("电子客票")) {
            return true;
        }
        int matchCount = 0;
        for (String kw : KEYWORDS) {
            if (normalized.contains(kw)) matchCount++;
        }
        return matchCount >= 2;
    }

    @Override
    protected ExtractResult doExtract(String text, ParseResult parseResult) {
        long start = System.currentTimeMillis();
        ExtractResult result = new ExtractResult();
        result.setDocumentType(DocumentType.RAILWAY_INVOICE);
        result.setInvoiceType("铁路电子客票");    // 票据类型
        Map<String, Object> fields = result.getFields();

        // 铁路票组合 Pattern 优先 — 一次提取时间+车厢+座位
        extractTrainCombo(text, fields);

        // 按顺序提取，每个字段独立，互不影响
        safePut(fields, FieldNames.INVOICE_NO,           extractInvoiceNo(text));
        safePut(fields, FieldNames.INVOICE_DATE,         extractInvoiceDate(text));
        safePut(fields, FieldNames.AMOUNT,               extractAmount(text));
        safePut(fields, FieldNames.BUYER_NAME,           extractBuyerName(text));
        safePut(fields, FieldNames.BUYER_TAX_NO,         extractBuyerTaxNo(text));
        safePut(fields, FieldNames.PASSENGER_NAME,       extractPassengerName(text));
        safePut(fields, FieldNames.PASSENGER_ID_NO,              extractIdCard(text));
        safePut(fields, FieldNames.DEPARTURE_STATION,         extractFromStation(text));
        safePut(fields, FieldNames.ARRIVAL_STATION,           extractToStation(text));
        safePut(fields, FieldNames.TRAIN_NO,         extractTrainNumber(text));
        safePut(fields, FieldNames.SEAT_TYPE,            extractSeatType(text));
        safePut(fields, FieldNames.SEAT_NO,              extractSeatNo(text));
        extractDepartDate(text); // extracted but no matching form field — logged internally
        safePut(fields, FieldNames.BOARDING_TIME,          extractDepartTime(text));
        safePut(fields, FieldNames.CARRIAGE_NO,               extractCarNo(text));
        safePut(fields, FieldNames.TICKET_NO, extractElectronicTicketNo(text));

        long cost = System.currentTimeMillis() - start;

        // 统一提取完成日志
        logExtractResult(parseResult.getFileId(), fields, cost);

        return result;
    }

    // ══════════════════════════════════════════════════════
    // 提取方法实现
    // ══════════════════════════════════════════════════════

    private String extractInvoiceNo(String text) {
        return tryFirst(text, RE_INVOICE_NO_LABEL, RE_INVOICE_NO_CODE, RE_INVOICE_NO_RAW);
    }

    private String extractInvoiceDate(String text) {
        // 优先标签匹配
        String date = null;
        date = findFirst(RE_INVOICE_DATE_LABEL, text);
        if (date != null) return normalizeDate(date);

        date = findFirst(RE_INVOICE_DATE_GENERIC, text);
        if (date != null) return normalizeDate(date);

        // 中文标签匹配
        java.util.regex.Matcher m = RegexUtils.matcher(RE_INVOICE_DATE_YEAR, text);
        if (m.find()) {
            return m.group(1) + "-" + padZero(m.group(2)) + "-" + padZero(m.group(3));
        }

        // 纯数字日期兜底（发票 PDF 中第一个标准日期通常是开票日期）
        return findFirst(RE_INVOICE_DATE_PLAIN, text);
    }

    /**
     * 金额提取：先取标签匹配的最大值（通常含税合计），再退化为 ¥ 匹配
     */
    private String extractAmount(String text) {
        String amount = tryFirst(text, RE_AMOUNT_LABEL, RE_AMOUNT_FUZZY, RE_AMOUNT_YUAN, RE_AMOUNT_CN);
        if (amount != null) {
            amount = normalizeMoney(amount);
        }
        return amount;
    }

    private String extractBuyerName(String text) {
        String name = tryFirst(text, RE_BUYER_NAME_LABEL, RE_BUYER_NAME_BEFORE_TAX, RE_BUYER_NAME_ADJACENT);
        if (name != null) {
            name = name.trim().replaceAll("\\s+", "");
        }
        return name;
    }

    private String extractBuyerTaxNo(String text) {
        return tryFirst(text, RE_TAX_NO_LABEL, RE_TAX_NO_CODE, RE_TAX_NO_GENERIC);
    }

    private String extractPassengerName(String text) {
        return tryFirst(text, RE_PASSENGER_LABEL, RE_PASSENGER_NAME_ONLY,
                RE_PASSENGER_MASKED_ID, RE_PASSENGER_ADJACENT);
    }

    private String extractIdCard(String text) {
        // Step 1: 显式标签匹配（仅身份证特定标签，不含"号码"以免匹配发票号码）
        String id = findFirst(RE_ID_CARD_LABEL, text);
        if (id != null) return id;

        // Step 2: 脱敏格式（3701121989****5656），直接返回脱敏串
        id = findFirst(RE_ID_CARD_MASKED, text);
        if (id != null) return id;

        // Step 3: 孤立 18 位（严格前后非数字，最后兜底）
        return findFirst(RE_ID_CARD_ISOLATED, text);
    }

    private String extractFromStation(String text) {
        String station = findFirst(RE_FROM_STATION_LABEL, text);
        if (station != null) return station;

        java.util.regex.Matcher m = RegexUtils.matcher(RE_STATION_PAIR, text);
        if (m.find()) return m.group(1);

        m = RegexUtils.matcher(RE_STATION_CONTINUOUS, text);
        if (m.find()) return m.group(1);

        m = RegexUtils.matcher(RE_STATION_DASH, text);
        if (m.find()) return m.group(1);

        return null;
    }

    private String extractToStation(String text) {
        String station = findFirst(RE_TO_STATION_LABEL, text);
        if (station != null) return station;

        java.util.regex.Matcher m = RegexUtils.matcher(RE_STATION_PAIR, text);
        if (m.find()) return m.group(2);

        m = RegexUtils.matcher(RE_STATION_CONTINUOUS, text);
        if (m.find()) return m.group(2);

        m = RegexUtils.matcher(RE_STATION_DASH, text);
        if (m.find()) return m.group(2);

        return null;
    }

    private String extractTrainNumber(String text) {
        return tryFirst(text, RE_TRAIN_NO_LABEL, RE_TRAIN_NO_GENERIC, RE_TRAIN_NO_FALLBACK, RE_TRAIN_NO_PLAIN);
    }

    private String extractSeatType(String text) {
        // 优先标签匹配
        String seat = findFirst(RE_SEAT_TYPE_LABEL, text);
        if (seat != null) return seat;

        // 关键词匹配
        seat = findFirst(RE_SEAT_TYPE_KEYWORD, text);
        if (seat != null) return seat;

        // 等级格式匹配
        seat = findFirst(RE_SEAT_TYPE_GRADE, text);
        return seat;
    }

    private String extractSeatNo(String text) {
        return tryFirst(text, RE_SEAT_NO_LABEL, RE_SEAT_NO_GENERIC);
    }

    /**
     * 铁路票组合提取 — "09:29开 03车07A号" → departTime + carNo + seatNo
     * <p>
     * 优先于离散的 extractDepartTime/extractCarNo/extractSeatNo。
     * 匹配成功时将结果直接写入 fields map。
     */
    private void extractTrainCombo(String text, Map<String, Object> fields) {
        java.util.regex.Matcher m = RegexUtils.matcher(RE_TRAIN_COMBO, text);
        if (!m.find()) return;

        // group(1)=departTime, group(2)=carNo, group(3)=seatNo
        String departTime = m.group(1);                    // "09:29"
        String carNo      = m.group(2);                    // "03"
        String seatNo     = m.group(3).toUpperCase();       // "07A"

        if (departTime != null && !departTime.isEmpty()) fields.put(FieldNames.BOARDING_TIME, departTime);
        if (carNo      != null && !carNo.isEmpty())      fields.put(FieldNames.CARRIAGE_NO,      carNo);
        if (seatNo     != null && !seatNo.isEmpty())     fields.put(FieldNames.SEAT_NO,     seatNo);
    }

    private String extractDepartDate(String text) {
        String date = findFirst(RE_DEPART_DATE_LABEL, text);
        if (date != null) return normalizeDate(date);

        // 兜底: 取第2个日期（第1个通常是开票日期），如果只有一个日期则返回null
        java.util.regex.Matcher m = RegexUtils.matcher(RE_DEPART_DATE_GENERIC, text);
        int count = 0;
        while (m.find()) {
            count++;
            if (count == 2) {
                return normalizeDate(m.group(1));
            }
        }
        // 只有一个日期 → 可能是没有独立乘车日期，返回 null 而非猜测
        return null;
    }

    private String extractDepartTime(String text) {
        String time = tryFirst(text, RE_DEPART_TIME_LABEL, RE_DEPART_TIME_GENERIC);

        // 兜底: 中文时间 "14点30分"
        if (time == null) {
            java.util.regex.Matcher m = RegexUtils.matcher(RE_DEPART_TIME_CN, text);
            if (m.find()) {
                time = padZero(m.group(1)) + ":" + padZero(m.group(2));
            }
        }

        return time;
    }

    private String extractCarNo(String text) {
        return tryFirst(text, RE_CAR_NO_LABEL, RE_CAR_NO_CN, RE_CAR_NO_GENERIC, RE_CAR_NO_PLAIN);
    }

    private String extractElectronicTicketNo(String text) {
        return tryFirst(text, RE_ETICKET_NO_LABEL, RE_ETICKET_NO_GENERIC, RE_ETICKET_NO_PREFIX, RE_ETICKET_NO_DIGITS);
    }

    // ══════════════════════════════════════════════════════
    // 工具方法
    // ══════════════════════════════════════════════════════

    /**
     * 依次尝试多个正则，直到匹配成功
     */
    private String tryFirst(String text, String... regexes) {
        for (String regex : regexes) {
            String result = findFirst(regex, text);
            if (result != null) return result;
        }
        return null;
    }

    /**
     * 安全填充字段（null 不存入 map）
     */
    private void safePut(Map<String, Object> fields, String key, String value) {
        if (value != null && !value.isEmpty()) {
            fields.put(key, value);
        }
    }

    /**
     * 补零（月/日/时/分）
     */
    private String padZero(String s) {
        if (s == null) return "00";
        if (s.length() == 1) return "0" + s;
        return s;
    }

    /**
     * 统一提取完成日志
     */
    private void logExtractResult(String fileId, Map<String, Object> fields, long costMs) {
        String[] allFields = {
            FieldNames.INVOICE_NO, FieldNames.INVOICE_DATE, FieldNames.AMOUNT,
            FieldNames.BUYER_NAME, FieldNames.BUYER_TAX_NO, FieldNames.PASSENGER_NAME,
            FieldNames.PASSENGER_ID_NO, FieldNames.DEPARTURE_STATION, FieldNames.ARRIVAL_STATION,
            FieldNames.TRAIN_NO, FieldNames.SEAT_TYPE, FieldNames.SEAT_NO,
            FieldNames.BOARDING_TIME, FieldNames.CARRIAGE_NO,
            FieldNames.TICKET_NO
        };

        java.util.List<String> successList = new java.util.ArrayList<>();
        java.util.List<String> missingList = new java.util.ArrayList<>();
        for (String f : allFields) {
            if (fields.containsKey(f)) {
                successList.add(f);
            } else {
                missingList.add(f);
            }
        }
        logger.info("[附件解析服务] RailwayInvoiceExtractor 提取完成: fileId={}, successFields={}, missingFields={}, cost={}ms",
                fileId, successList, missingList, costMs);
    }
}
