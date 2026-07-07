package com.actionsoft.apps.attachment.parser.parser;

import com.actionsoft.apps.attachment.parser.model.ParseResult;

/**
 * 文件解析器统一接口
 */
public interface Parser {

    /**
     * 返回此解析器处理的文件类型标识
     */
    String getType();

    /**
     * 判断此解析器是否支持该文件（基于 magic bytes）
     */
    boolean supports(byte[] bytes);

    /**
     * 执行解析
     */
    ParseResult parse(byte[] bytes, String fileId);
}
