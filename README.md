# AWS PaaS 附件解析服务

基于炎黄低代码平台（AWS PaaS 7.1.GA）的附件自动解析与在线预览插件应用。

## 项目介绍

在 AWS PaaS 平台上实现附件上传后自动解析票据内容的功能。用户上传 PDF 附件后，系统自动识别票据类型、提取关键字段（发票号码、日期、金额、买卖方等）、判断费用分类，并将结果回填到子表明细行中，同时生成在线预览链接供用户查看原始票据。

## 技术架构

```
┌─────────────────────────────────────────────┐
│  前端 (EndUser App)                          │
│  ├── 附件上传 → JS 事件触发 ASLP 调用         │
│  ├── 数据回填 → grid.addData()               │
│  └── 在线预览 → window.open(VIEW_URL)         │
├─────────────────────────────────────────────┤
│  ASLP: parseAttachment                       │
│  ├── DocumentParserService   (文件解析)       │
│  ├── DocumentExtractorService (信息提取)      │
│  └── BillTypeDetector        (费用分类)       │
├─────────────────────────────────────────────┤
│  平台 SDK                                     │
│  ├── SDK.getBOAPI().getFile(fileId)          │
│  ├── FormFileDao.queryById()                 │
│  └── PDFBox / POI / fastjson                 │
└─────────────────────────────────────────────┘
```

**AppId**: `com.actionsoft.apps.attachment.parser`
**ASLP认证**: AUTH_AWS_SID（跨应用调用需在 manifest.xml 声明 `<requires>` 依赖）

## 支持文件类型

| 格式 | 解析器 | 状态 |
|------|--------|------|
| PDF | PdfParser (PDFBox 2.0.36) | ✅ |
| Word (docx) | WordParser (POI 5.4.1 XWPF) | ✅ |
| Excel (xlsx) | ExcelParser (POI 5.4.1 XSSF) | ✅ |
| TXT | TxtParser (UTF-8/GBK) | ✅ |
| 图片 | ImageParser | ⏳ OCR 待实现 |

## 当前支持票据类型

| 票据类型 | Extractor | 识别关键词 | 提取字段数 |
|---------|-----------|-----------|-----------|
| 铁路电子客票 | RailwayInvoiceExtractor | 铁路电子客票, 12306, 车次, 乘车人 | 17 个 |
| 增值税电子发票 | VatInvoiceExtractor | 电子发票, 增值税电子, 价税合计, 销售方, 购买方 | 15 个 |

### 费用分类 (BillTypeDetector)

由 `BillTypeDetector.detect()` 根据文本关键词自动判定，支持：

| BILL_TYPE | EXPENSE_TYPE | 关键词示例 |
|-----------|-------------|-----------|
| 火车票 | 交通费 | 铁路电子客票, 12306, 二等座 |
| 飞机票 | 交通费 | 航空运输电子客票, 航班号, ETKT |
| 出租车费 | 交通费 | 出租汽车, Taxi |
| 网约车费 | 交通费 | 滴滴, 高德打车, T3 |
| 加油费 | 车辆费用 | 成品油, 汽油, 中石油 |
| 住宿费 | 住宿费 | 酒店, 宾馆, 入住 |
| 餐饮费 | 餐饮费 | 餐饮, 餐费, 饭店 |
| 停车费 | 车辆费用 | 停车费, 停车场 |
| 高速通行费 | 车辆费用 | 高速, ETC |
| 租车费 | 交通费 | 租车, 汽车租赁 |
| 其它费用 | 其他费用 | (兜底) |

## 架构规则

- **Extractor 职责**：票据事实字段（发票号码/日期/金额/买卖方等）、DOCUMENT_CATEGORY、INVOICE_TYPE
- **BillTypeDetector 职责**：业务费用分类 — BILL_TYPE + EXPENSE_TYPE 统一来源
- **禁止 Extractor 设置 EXPENSE_TYPE**
- **Extractor 自动注册**：新建类放入 `extractor/` 包即可，无需修改 `Plugins.java`

## ASLP 调用方式

### 端点
```
POST /r/jd?cmd=API_CALL_ASLP&aslp=parseAttachment
```

### 请求参数
```json
{
    "fileId": "附件ID (UUID)"
}
```

### 返回示例
```json
{
    "success": true,
    "data": {
        "INVOICE_TYPE": "铁路电子客票",
        "BILL_TYPE": "火车票",
        "EXPENSE_TYPE": "交通费",
        "INVOICE_NO": "26419165782000135975",
        "INVOICE_DATE": "2026-04-24",
        "AMOUNT": "29.00",
        "BUYER_NAME": "潍坊市公安局...",
        "PASSENGER_NAME": "徐钟意",
        "TRAIN_NO": "G1278",
        "SEAT_TYPE": "二等座",
        "DOCUMENT_CATEGORY": "RAILWAY_TICKET",
        "VIEW_URL": "/r/w?cmd=com.actionsoft.apps.addons.onlinedoc_filepreview&appId=...",
        "VIEW_NAME": "交通-火车票-02.pdf",
        "FILE_INFO": { "id": "...", "fileName": "...", "fileSize": "27.2 KB", ... }
    }
}
```

## 前端调用方式

```javascript
// 附件字段「文件保存后」事件
$.ajax({
    url: '/r/jd',
    data: {
        cmd: 'API_CALL_ASLP',
        aslp: 'parseAttachment',
        fileId: fileId
    },
    success: function(resp) {
        var data = resp.data;
        grid.addData(function(context) {
            context.rowData.VIEW_URL  = data.VIEW_URL;
            context.rowData.VIEW_NAME = data.VIEW_NAME;
            context.rowData.AMOUNT    = data.AMOUNT;
            // ...
        });
    }
});
```

## 在线预览方案

VIEW_URL 使用平台 onlinedoc_filepreview 服务：

```
/r/w?cmd=com.actionsoft.apps.addons.onlinedoc_filepreview
  &appId={appId}
  &sourceGroupValue={fileId}
  &sourceFileValue={boItemName}
  &sourceRepositoryName=!form-ui-file-
  &isShowDefaultToolbar=true
  &isPDFCovertPNG=0
  &isDecode=true
  ...
```

前端通过 `window.open(VIEW_URL)` 打开预览窗口。

## 本地开发环境

- **JDK**: Java 17（编译目标 Java 8）
- **构建工具**: Gradle / javac
- **平台版本**: AWS PaaS 7.1.GA
- **编码**: UTF-8

### 编译

```bash
# 方式1: Gradle
./gradlew jar copyToPlatform

# 方式2: javac
javac --release 8 -encoding UTF-8 -cp "libs/*" -d build/classes src/main/java/**/*.java
jar cf attachment-parser.jar -C build/classes .
```

### 依赖

| 依赖 | 用途 | 范围 |
|------|------|------|
| aws-sdk-7.1.GA.jar | AWS PaaS SDK | compileOnly |
| aws-infrastructure-common-7.1.GA.jar | 平台基础设施 | compileOnly |
| aws-infrastructure-core-7.1.GA.jar | 平台核心 | compileOnly |
| pdfbox-2.0.36.jar | PDF 解析 | 平台提供 |
| poi-5.4.1.jar / poi-ooxml-5.4.1.jar | Office 解析 | 平台提供 |
| fastjson-1.2.83.jar | JSON 配置解析 | 平台提供 |
| slf4j-api | 日志 | 平台提供 |

## 部署方式

```bash
# 1. 编译 jar
./gradlew jar

# 2. 复制到平台插件目录
cp build/libs/attachment-parser.jar \
   E:/中软科/炎黄/app/apps/install/com.actionsoft.apps.attachment.parser/lib/

# 3. 更新 manifest.xml 的 buildNo（触发重载）
#    <buildNo>N</buildNo>  →  <buildNo>N+1</buildNo>

# 4. 重启平台 或 等待 AppLibListener Timer 扫描
```

## 项目结构

```
src/main/java/com/actionsoft/apps/attachment/parser/
├── aslp/
│   └── ParseAttachmentASLP.java      # ASLP 入口
├── constant/
│   ├── DocumentCategory.java         # 文档分类枚举
│   ├── DocumentType.java             # 文档类型常量
│   ├── ErrorCodes.java               # 错误码
│   └── FieldNames.java               # 字段名常量
├── exception/
│   └── ParseException.java           # 解析异常
├── extractor/
│   ├── AbstractRegexExtractor.java   # 抽象提取器基类
│   ├── Extractor.java                # 提取器接口
│   ├── RailwayInvoiceExtractor.java  # 铁路电子客票提取器
│   ├── UnknownExtractor.java         # 兜底提取器
│   └── VatInvoiceExtractor.java      # 增值税电子发票提取器
├── mapper/
│   └── FormFieldMapper.java          # 字段映射
├── model/
│   ├── BillTypeRule.java             # 费用分类规则实体
│   ├── ExtractResult.java            # 提取结果
│   └── ParseResult.java              # 解析结果
├── parser/
│   ├── Parser.java                   # 解析器接口
│   ├── PdfParser.java                # PDF 解析器
│   ├── WordParser.java               # Word 解析器
│   ├── ExcelParser.java              # Excel 解析器
│   ├── TxtParser.java                # TXT 解析器
│   ├── ImageParser.java              # 图片解析器
│   └── UnknownParser.java            # 兜底解析器
├── registry/
│   ├── ExtractorRegistry.java        # 提取器注册
│   └── ParserRegistry.java           # 解析器注册
├── service/
│   ├── DocumentExtractorService.java # 提取服务
│   └── DocumentParserService.java    # 解析服务
├── util/
│   ├── BillTypeDetector.java         # 费用类型检测器
│   ├── ExtractorAutoScanner.java     # 提取器自动扫描
│   ├── LogUtils.java                 # 日志工具
│   ├── RegexUtils.java               # 正则工具
│   └── TextUtils.java                # 文本工具
└── Plugins.java                      # 插件注册入口
```

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v0.1.0 | 2026-07-07 | 首个稳定版：PDF解析、铁路电子客票/增值税发票识别、自动报销明细生成、在线预览 |
