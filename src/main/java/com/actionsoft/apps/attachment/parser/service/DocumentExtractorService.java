package com.actionsoft.apps.attachment.parser.service;

import com.actionsoft.apps.attachment.parser.extractor.Extractor;
import com.actionsoft.apps.attachment.parser.extractor.UnknownExtractor;
import com.actionsoft.apps.attachment.parser.model.ExtractResult;
import com.actionsoft.apps.attachment.parser.model.ParseResult;
import com.actionsoft.apps.attachment.parser.registry.ExtractorRegistry;
import com.actionsoft.apps.attachment.parser.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文档信息提取服务
 * <p>
 * 委托 ExtractorRegistry 管理提取器，遍历 supports() 查找第一个匹配的执行。
 * UnknownExtractor 仅作为 fallback 兜底使用，不参与主循环遍历。
 */
public class DocumentExtractorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentExtractorService.class);
    private final ExtractorRegistry registry = ExtractorRegistry.getInstance();

    /**
     * 从解析结果中提取结构化信息
     */
    public ExtractResult extract(ParseResult parseResult) {
        for (Extractor extractor : registry.getExtractors()) {
            // UnknownExtractor 仅作 fallback，不参与主循环匹配
            if (extractor instanceof UnknownExtractor) continue;
            if (extractor.supports(parseResult)) {
                String name = extractor.getClass().getSimpleName();
                LogUtils.extractorStart(LOGGER, name, parseResult.getFileId());
                long start = System.currentTimeMillis();
                ExtractResult result = extractor.extract(parseResult);
                long cost = System.currentTimeMillis() - start;
                LogUtils.extractorDone(LOGGER, name, parseResult.getFileId(),
                        result.getDocumentType(), result.getFields().size(), cost);
                return result;
            }
        }
        LogUtils.extractorSkip(LOGGER, parseResult.getFileId());
        return registry.getFallback().extract(parseResult);
    }
}
