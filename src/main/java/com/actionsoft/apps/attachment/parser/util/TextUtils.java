package com.actionsoft.apps.attachment.parser.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * 文本处理工具类
 * <p>
 * 提供全角半角转换、空白压缩、换行统一、金额/日期标准化等通用文本预处理能力。
 * 所有方法均为静态，无副作用。
 */
public final class TextUtils {

    private TextUtils() {}

    // ─── 全角半角转换 ───

    /**
     * 全角字符转半角（英文、数字、符号）
     * <p>
     * 全角范围 FF01-FF5E，对应半角 21-7E，偏移量 0xFEE0
     */
    public static String fullWidthToHalf(String text) {
        if (text == null || text.isEmpty()) return text;
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= '\uFF01' && chars[i] <= '\uFF5E') {
                chars[i] = (char) (chars[i] - 0xFEE0);
            } else if (chars[i] == '\u3000') { // 全角空格
                chars[i] = ' ';
            }
        }
        return new String(chars);
    }

    // ─── 空白处理 ───

    /** 将连续空白字符压缩为单个空格 */
    public static String compressBlank(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replaceAll("\\s+", " ").trim();
    }

    /** 移除所有空白字符 */
    public static String removeBlank(String text) {
        if (text == null) return null;
        return text.replaceAll("\\s+", "");
    }

    // ─── 换行处理 ───

    /** 统一换行符为 \n */
    public static String normalizeNewline(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    // ─── 冒号兼容 ───

    /** 中文冒号（：）统一转换为英文冒号（:） */
    public static String normalizeColon(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replace('\uff1a', ':');
    }

    /** 中文逗号（，）统一转换为英文逗号（,） */
    public static String normalizeComma(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replace('\uff0c', ',');
    }

    // ─── 金额标准化 ───

    private static final Pattern MONEY_CLEAN = Pattern.compile("[￥¥,，\\s]");

    /**
     * 金额字符串标准化
     * <p>
     * 移除 ¥￥,，和空白字符，保留数字和小数点。
     * 例如 "¥ 1,234.56" → "1234.56"
     */
    public static String normalizeMoney(String text) {
        if (text == null || text.isEmpty()) return text;
        return MONEY_CLEAN.matcher(text).replaceAll("");
    }

    // ─── 日期标准化 ───

    /** 中文日期分隔符（年月日）统一转为 - */
    public static String normalizeDate(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replace('年', '-')
                   .replace('月', '-')
                   .replace('日', ' ')
                   .trim();
    }

    // ─── 组合预处理 ───

    /**
     * 标准预处理流水线：半角转换 → 换行统一 → 冒号兼容 → 逗号兼容 → 空白压缩
     * <p>
     * 这是大多数 Extractor 的第一步操作
     */
    public static String preprocess(String text) {
        if (text == null || text.isEmpty()) return text;
        String result = fullWidthToHalf(text);
        result = normalizeNewline(result);
        result = normalizeColon(result);
        result = normalizeComma(result);
        result = compressBlank(result);
        return result;
    }

    // ─── Unicode 标准化 ───

    /** NFC 标准化（兼容全角/半角引起的视觉差异） */
    public static String nfc(String text) {
        if (text == null) return null;
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }
}
