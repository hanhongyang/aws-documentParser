package com.actionsoft.apps.attachment.parser.util;

import org.slf4j.Logger;

/**
 * 统一日志工具
 * <p>
 * 所有 Parsers、Extractors、Service 通过此类输出日志，
 * 保证格式统一、便于后续日志分析。
 */
public final class LogUtils {

    private LogUtils() {}

    private static final String PREFIX = "[附件解析服务]";

    // ═══ ParseAttachmentASLP ═══

    public static void aslpEnter(Logger log, Object params) {
        log.info("{} 进入 ASLP，params={}", PREFIX, params);
    }

    // ═══ DocumentParserService ═══

    public static void fileRead(Logger log, String fileId, int size) {
        log.info("{} 文件读取成功: fileId={}, size={} bytes", PREFIX, fileId, size);
    }

    public static void fileNotFound(Logger log, String fileId) {
        log.warn("{} 文件不存在: fileId={}", PREFIX, fileId);
    }

    // ═══ Parser ═══

    public static void parserStart(Logger log, String parserName, String fileId) {
        log.info("{} 开始解析: parser={}, fileId={}", PREFIX, parserName, fileId);
    }

    public static void parserDone(Logger log, String parserName, String fileId,
                                   String type, int textLen, long costMs) {
        log.info("{} 解析完成: parser={}, fileId={}, type={}, textLength={}, cost={}ms",
                PREFIX, parserName, fileId, type, textLen, costMs);
    }

    public static void parserFail(Logger log, String parserName, String fileId, Throwable e) {
        log.error("{} 解析失败: parser={}, fileId={}, error={}", PREFIX, parserName, fileId, e.getMessage(), e);
    }

    // ═══ Extractor ═══

    public static void extractorStart(Logger log, String extractorName, String fileId) {
        log.info("{} 开始提取: extractor={}, fileId={}", PREFIX, extractorName, fileId);
    }

    public static void extractorDone(Logger log, String extractorName, String fileId,
                                      String docType, int fieldCount, long costMs) {
        log.info("{} 提取完成: extractor={}, fileId={}, documentType={}, fieldCount={}, cost={}ms",
                PREFIX, extractorName, fileId, docType, fieldCount, costMs);
    }

    public static void extractorSkip(Logger log, String fileId) {
        log.info("{} 未匹配到 Extractor，使用 UnknownExtractor: fileId={}", PREFIX, fileId);
    }

    // ═══ Registry ═══

    public static void registerParser(Logger log, String type, String className) {
        log.info("{} 已注册 Parser: type={}, class={}", PREFIX, type, className);
    }

    public static void registerExtractor(Logger log, String className) {
        log.info("{} 已注册 Extractor: class={}", PREFIX, className);
    }

    // ═══ 性能 ═══

    public static void costSummary(Logger log, String fileId, long parseCost,
                                    long extractCost, long totalCost) {
        log.info("{} 耗时统计: fileId={}, parseCost={}ms, extractCost={}ms, totalCost={}ms",
                PREFIX, fileId, parseCost, extractCost, totalCost);
    }
}
