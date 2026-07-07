package com.actionsoft.apps.attachment.parser.extractor;

import com.actionsoft.apps.attachment.parser.model.ExtractResult;
import com.actionsoft.apps.attachment.parser.model.ParseResult;

/**
 * 文档信息提取器统一接口
 * <p>
 * 每个实现类负责从特定类型的文档（如发票、身份证）中提取结构化字段。
 * 后续新增提取器只需实现此接口并注册到 DocumentExtractorService。
 */
public interface Extractor {

    /**
     * 判断此提取器是否支持该解析结果
     */
    boolean supports(ParseResult parseResult);

    /**
     * 执行信息提取
     */
    ExtractResult extract(ParseResult parseResult);
}
