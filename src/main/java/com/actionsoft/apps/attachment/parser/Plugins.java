package com.actionsoft.apps.attachment.parser;

import com.actionsoft.apps.listener.PluginListener;
import com.actionsoft.apps.resource.AppContext;
import com.actionsoft.apps.resource.plugin.profile.AWSPluginProfile;
import com.actionsoft.apps.resource.plugin.profile.ASLPPluginProfile;
import com.actionsoft.apps.resource.plugin.profile.HttpASLP;
import com.actionsoft.apps.attachment.parser.aslp.ParseAttachmentASLP;
import com.actionsoft.apps.attachment.parser.extractor.RailwayInvoiceExtractor;
import com.actionsoft.apps.attachment.parser.parser.*;
import com.actionsoft.apps.attachment.parser.registry.ExtractorRegistry;
import com.actionsoft.apps.attachment.parser.registry.ParserRegistry;
import com.actionsoft.apps.attachment.parser.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 附件解析服务 - 插件注册类
 * <p>
 * 职责：注册 ASLP 服务、Parser、Extractor
 */
public class Plugins implements PluginListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Plugins.class);

    @Override
    public List<AWSPluginProfile> register(AppContext context) {
        LogUtils.aslpEnter(LOGGER, "Plugins.register()");

        // ─── 注册 Parsers ───
        ParserRegistry parserReg = ParserRegistry.getInstance();
        parserReg.register(new PdfParser());
        parserReg.register(new WordParser());
        parserReg.register(new ExcelParser());
        parserReg.register(new TxtParser());
        parserReg.register(new ImageParser());

        // ─── 注册 Extractors ───
        ExtractorRegistry extractorReg = ExtractorRegistry.getInstance();
        extractorReg.register(new RailwayInvoiceExtractor());

        // ─── 注册 ASLP 服务 ───
        List<AWSPluginProfile> list = new ArrayList<>();
        list.add(new ASLPPluginProfile(
            "parseAttachment",
            ParseAttachmentASLP.class.getName(),
            "解析附件：读取平台附件并提取文档内容",
            new HttpASLP(HttpASLP.AUTH_AWS_SID)
        ));

        LOGGER.info("[附件解析服务] 已注册 {} 个 ASLP 服务", list.size());
        LOGGER.info("[附件解析服务] ===== Plugins.register() 完成 =====");
        return list;
    }
}
