package com.actionsoft.apps.attachment.parser.extractor;

import com.actionsoft.apps.attachment.parser.model.ExtractResult;
import com.actionsoft.apps.attachment.parser.model.ParseResult;

/**
 * 默认提取器 — 当没有匹配的 Extractor 时使用
 */
public class UnknownExtractor implements Extractor {

    @Override
    public boolean supports(ParseResult parseResult) {
        return true; // 兜底，永远匹配
    }

    @Override
    public ExtractResult extract(ParseResult parseResult) {
        return ExtractResult.unknown();
    }
}
