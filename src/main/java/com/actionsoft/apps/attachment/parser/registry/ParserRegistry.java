package com.actionsoft.apps.attachment.parser.registry;

import com.actionsoft.apps.attachment.parser.parser.Parser;
import com.actionsoft.apps.attachment.parser.parser.UnknownParser;
import com.actionsoft.apps.attachment.parser.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser 统一注册中心（单例）
 * <p>
 * 只负责保存，不负责初始化。注册由 Plugins.register() 调用。
 */
public class ParserRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParserRegistry.class);
    private static final ParserRegistry INSTANCE = new ParserRegistry();

    private final List<Parser> parsers = new ArrayList<>();
    private final UnknownParser fallback = new UnknownParser();

    private ParserRegistry() {}

    public static ParserRegistry getInstance() { return INSTANCE; }

    public void register(Parser parser) {
        parsers.add(parser);
        LogUtils.registerParser(LOGGER, parser.getType(), parser.getClass().getSimpleName());
    }

    public List<Parser> getParsers() {
        return Collections.unmodifiableList(parsers);
    }

    public Parser getFallback() {
        return fallback;
    }
}
