package com.actionsoft.apps.attachment.parser.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式工具类
 * <p>
 * 提供 Pattern 缓存、Matcher 安全调用、空值处理、公共 Regex 常量。
 * 避免每次调用都 compile Pattern，提升性能。
 */
public final class RegexUtils {

    private RegexUtils() {}

    /** Pattern 缓存，线程安全 */
    private static final Map<String, Pattern> CACHE = Collections.synchronizedMap(new HashMap<>());

    // ─── 公共 Regex 常量 ───

    /** 金额：¥ + 数字 + 可选小数 */
    public static final String RE_MONEY = "[￥¥]\\s*\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?";

    /** 日期：YYYY-MM-DD / YYYY/MM/DD / YYYY年MM月DD日 */
    public static final String RE_DATE = "\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?";

    /** 日期：含中文前缀（如 开票日期:2023-01-01） */
    public static final String RE_DATE_LABEL = "(?:日期|开票日期|填发日期)[：:]\\s*(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)";

    /** 金额：含标签（如 金额:¥123.45） */
    public static final String RE_AMOUNT_LABEL = "(?:金额|合计金额|价税合计|小写)[：:]\\s*([￥¥]?\\s*[\\d,.]+)";

    /** 编号：常见发票编号格式 */
    public static final String RE_INVOICE_NO = "(?:(?:发票)?号码|编号|发票代码)[：:]\\s*([A-Za-z0-9\\-]+)";

    /** 税率：数字 + % */
    public static final String RE_TAX_RATE = "(\\d{1,2})\\s*%";

    /** 纳税人识别号：18位 */
    public static final String RE_TAX_ID = "[0-9A-Z]{15,20}";

    // ─── 缓存获取 ───

    /**
     * 获取或编译 Pattern（带缓存）
     */
    public static Pattern pattern(String regex) {
        return CACHE.computeIfAbsent(regex, Pattern::compile);
    }

    /**
     * 获取或编译 Pattern（忽略大小写 + 带缓存）
     */
    public static Pattern patternI(String regex) {
        return CACHE.computeIfAbsent("(?i)" + regex, k -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
    }

    // ─── Matcher 安全方法 ───

    /**
     * 安全创建 Matcher，输入为 null 时返回空匹配的 Matcher
     */
    public static Matcher matcher(String regex, String text) {
        if (text == null) text = "";
        return pattern(regex).matcher(text);
    }

    // ─── 查找方法 ───

    /**
     * 首次匹配 — 返回第一个捕获组（group 1），无匹配返回 null
     */
    public static String findFirst(String regex, String text) {
        Matcher m = matcher(regex, text);
        if (m.find()) {
            String result = m.groupCount() > 0 ? m.group(1) : m.group();
            return result != null ? result.trim() : null;
        }
        return null;
    }

    /**
     * 首次匹配 — 返回完整匹配（group 0），无匹配返回 null
     */
    public static String findFirstFull(String regex, String text) {
        Matcher m = matcher(regex, text);
        if (m.find()) {
            return m.group().trim();
        }
        return null;
    }

    /**
     * 全部匹配 — 返回所有匹配的 group 1（或 group 0）列表
     */
    public static List<String> findAll(String regex, String text) {
        List<String> results = new ArrayList<>();
        Matcher m = matcher(regex, text);
        while (m.find()) {
            String value = m.groupCount() > 0 ? m.group(1) : m.group();
            if (value != null) results.add(value.trim());
        }
        return results;
    }

    /**
     * 首次匹配的日期 — 标准化为 yyyy-MM-dd 格式
     */
    public static String findDate(String regex, String text) {
        String raw = findFirst(regex, text);
        if (raw == null) return null;
        return TextUtils.normalizeDate(raw).replaceAll("\\s+", "");
    }

    /**
     * 首次匹配的金额 — 标准化为纯数字格式
     */
    public static String findMoney(String regex, String text) {
        String raw = findFirst(regex, text);
        if (raw == null) return null;
        return TextUtils.normalizeMoney(raw);
    }

    // ─── 列表安全取值 ───

    /**
     * 安全获取列表第 N 个元素，越界或列表为 null 返回 null
     */
    public static <T> T safeGet(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    // ─── 缓存管理 ───

    /** 清空 Pattern 缓存 */
    public static void clearCache() {
        CACHE.clear();
    }
}
