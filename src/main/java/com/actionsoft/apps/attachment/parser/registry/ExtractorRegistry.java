package com.actionsoft.apps.attachment.parser.registry;

import com.actionsoft.apps.attachment.parser.extractor.Extractor;
import com.actionsoft.apps.attachment.parser.extractor.UnknownExtractor;
import com.actionsoft.apps.attachment.parser.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extractor 统一注册中心（单例）
 * <p>
 * 只负责保存，不负责初始化。注册由 Plugins.register() 调用。
 */
public class ExtractorRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractorRegistry.class);
    private static final ExtractorRegistry INSTANCE = new ExtractorRegistry();

    private final List<Extractor> extractors = new ArrayList<>();
    private final UnknownExtractor fallback = new UnknownExtractor();

    private ExtractorRegistry() {}

    public static ExtractorRegistry getInstance() { return INSTANCE; }

    public void register(Extractor extractor) {
        extractors.add(extractor);
        LogUtils.registerExtractor(LOGGER, extractor.getClass().getSimpleName());
    }

    public List<Extractor> getExtractors() {
        return Collections.unmodifiableList(extractors);
    }

    public Extractor getFallback() {
        return fallback;
    }
}
