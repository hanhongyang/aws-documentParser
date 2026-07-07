package com.actionsoft.apps.attachment.parser.extractor;

import com.actionsoft.apps.attachment.parser.model.ExtractResult;
import com.actionsoft.apps.attachment.parser.model.ParseResult;
import com.actionsoft.apps.attachment.parser.util.RegexUtils;
import com.actionsoft.apps.attachment.parser.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 基于正则表达式的抽象 Extractor
 * <p>
 * 提供公共工具方法，子类只需实现 supports() 和 extract()。
 * 内置安全取值、正则查找、文本预处理等能力。
 *
 * <h3>子类实现示例</h3>
 * <pre>
 * public class RailwayInvoiceExtractor extends AbstractRegexExtractor {
 *     // 只需要实现 supports() 和 doExtract() 两个方法
 * }
 * </pre>
 */
public abstract class AbstractRegexExtractor implements Extractor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // ══════════════════════════════════════════════════
    // 抽象方法 — 子类必须实现
    // ══════════════════════════════════════════════════

    /**
     * 执行具体的信息提取逻辑
     * <p>
     * 调用时文本已经过 {@link #preprocess} 预处理
     */
    protected abstract ExtractResult doExtract(String text, ParseResult parseResult);

    // ══════════════════════════════════════════════════
    // 模板方法 — 提供默认处理流程，子类可覆盖
    // ══════════════════════════════════════════════════

    @Override
    public ExtractResult extract(ParseResult parseResult) {
        String rawText = parseResult.getText();
        if (rawText == null || rawText.isEmpty()) {
            logger.warn("[附件解析服务] 解析文本为空，跳过提取: fileId={}", parseResult.getFileId());
            return ExtractResult.unknown();
        }
        String text = preprocess(rawText);
        try {
            return doExtract(text, parseResult);
        } catch (Exception e) {
            logger.error("[附件解析服务] 信息提取异常: fileId={}, error={}",
                    parseResult.getFileId(), e.getMessage(), e);
            return ExtractResult.unknown();
        }
    }

    /**
     * 文本预处理 — 默认使用标准流水线，子类可覆盖
     */
    protected String preprocess(String rawText) {
        return TextUtils.preprocess(rawText);
    }

    // ══════════════════════════════════════════════════
    // 公共工具方法 — 所有子类直接使用
    // ══════════════════════════════════════════════════

    /** 首次正则匹配 */
    protected String findFirst(String regex, String text) {
        return RegexUtils.findFirst(regex, text);
    }

    /** 全部正则匹配 */
    protected List<String> findAll(String regex, String text) {
        return RegexUtils.findAll(regex, text);
    }

    /** 查找并标准化日期 */
    protected String findDate(String regex, String text) {
        return RegexUtils.findDate(regex, text);
    }

    /** 查找并标准化金额 */
    protected String findMoney(String regex, String text) {
        return RegexUtils.findMoney(regex, text);
    }

    /** 安全取列表元素 */
    protected <T> T safeGet(List<T> list, int index) {
        return RegexUtils.safeGet(list, index);
    }

    /** 全角转半角 */
    protected String fullWidthToHalf(String text) {
        return TextUtils.fullWidthToHalf(text);
    }

    /** 压缩连续空白 */
    protected String compressBlank(String text) {
        return TextUtils.compressBlank(text);
    }

    /** 移除空白 */
    protected String removeBlank(String text) {
        return TextUtils.removeBlank(text);
    }

    /** 统一换行 */
    protected String normalizeNewline(String text) {
        return TextUtils.normalizeNewline(text);
    }

    /** 冒号兼容 */
    protected String normalizeColon(String text) {
        return TextUtils.normalizeColon(text);
    }

    /** 金额标准化 */
    protected String normalizeMoney(String text) {
        return TextUtils.normalizeMoney(text);
    }

    /** 日期标准化 */
    protected String normalizeDate(String text) {
        return TextUtils.normalizeDate(text);
    }
}
