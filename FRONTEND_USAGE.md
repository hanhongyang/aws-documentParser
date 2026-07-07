# 附件解析服务 - 前端调用指南

## 功能说明

通过 ASLP 服务 `parseAttachment` 解析平台附件，返回文件元数据（文件名、大小、类型等）。

## ASLP 地址

```
aslp://com.actionsoft.apps.attachment.parser/parseAttachment
```

## 调用参数

```json
{
  "fileId": "平台附件ID"
}
```

## 响应格式

```json
{
  "result": "ok",
  "data": {
    "ok": true,
    "fileId": "文件ID",
    "fileName": "文件名.pdf",
    "size": 123456,
    "contentType": "application/pdf"
  }
}
```

## 前端调用示例（表单事件）

在表单设计器中，为**附件字段**的"文件保存后"事件绑定以下 JS：

```javascript
// 获取上传文件的 fileId
var fileId = null;

if (event && event.fileId) {
    fileId = event.fileId;
} else if (event && event.data && event.data.fileId) {
    fileId = event.data.fileId;
} else if (this && this.getVal) {
    var val = this.getVal();
    if (Array.isArray(val) && val.length > 0) {
        var firstFile = val[0];
        fileId = (typeof firstFile === 'object') ? firstFile.fileId : firstFile;
    }
}

if (!fileId) {
    console.warn('[附件解析] 未获取到 fileId');
    return;
}

console.log('[附件解析] 开始解析文件:', fileId);

// 调用 ASLP
formApi.api.awsuiaxios.post({
    url: "jd",
    data: {
        cmd: "API_CALL_ASLP",
        aslp: "aslp://com.actionsoft.apps.attachment.parser/parseAttachment",
        authentication: formApi.api.getSid(),
        sourceAppId: window.awsAppId || "当前应用ID",
        params: JSON.stringify({ fileId: fileId })
    },
    alert: false
}).then(function(response) {
    console.log('[附件解析] 调用成功:', response);
    
    if (response && response.result === "ok") {
        var data = response.data;
        // 将结果写入指定字段（根据实际字段名修改）
        formApi.ui("parseResult").setVal(
            '文件名: ' + (data.fileName || '未知') + 
            ', 大小: ' + data.size + ' bytes' +
            ', 类型: ' + (data.contentType || '未知')
        );
        console.log('[附件解析] 已将结果写入字段');
    } else {
        formApi.ui("parseResult").setVal('解析失败: ' + (response ? response.msg : '未知错误'));
        console.error('[附件解析] 业务错误:', response);
    }
}).catch(function(error) {
    console.error('[附件解析] 调用失败:', error);
    formApi.ui("parseResult").setVal('调用失败: ' + (error.message || '网络错误'));
});
```

## 依赖声明

如果其他应用需要调用此 ASLP，需在其 `manifest.xml` 中声明依赖：

```xml
<requires>
    <require appId="com.actionsoft.apps.attachment.parser" notActiveHandler="error"/>
</requires>
```

## 注意事项

- ⚠️ **认证要求**：调用方用户必须有有效的 Session ID
- ⚠️ **权限配置**：确保调用方应用已声明依赖
- ⚠️ **异步调用**：ASLP 调用是异步的，结果会在稍后写入字段
- ⚠️ **cmd 参数**：必须使用 `"API_CALL_ASLP"`（大写）
