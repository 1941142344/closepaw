# ClosePaw ChatGPT Plus / Codex 兼容修复

核查日期：2026-10-26。上游：https://github.com/imoonkey/closepaw 。基线提交：`75dae2653f5a6b25d5df51ee7008b0f830de1536`。

## 发现与修复

1. **OAuth 令牌类型错误。** 原 `OAuthTokenExchange` 在授权码交换和刷新后，继续用 ID Token 请求 `requested_token=openai-api-key`，并用返回值替换 OAuth Access Token。订阅端点需要订阅 OAuth 令牌。本次移除了这个额外交换，保留浏览器 Sign In、PKCE S256、state 校验、官方授权地址及 localhost 回调。刷新响应缺少 refresh token、ID token 或 email 时保留已有字段；旧版本误存的 `sk-` 值会触发 OAuth 刷新恢复。
2. **订阅模型配置过期。** 移除 `OPENAI_CODEX` 下的 GPT-5.2、5.4、5.5 条目，加入 `gpt-6.1-sol`、`gpt-6-sol`、`gpt-6-luna`，首选 `gpt-6.1-sol`。设置和旧会话中的 `*-codex` 名称会迁移或解析到新模型。目录键 `gpt-6.1-sol-codex` 是 ClosePaw 内部名称，实际请求模型是 `gpt-6.1-sol`。
3. **工具 schema 丢失 strict 字段。** 序列化现在明确保留 `strict:false`，使 mobile_action 的目标、手势时长以及 Termux 的 cwd/env 等可选字段继续可选；显式 strict=true 也会保留。
4. **流式参数和终止状态处理不完整。** 支持 `response.function_call_arguments.done`，优先采用服务端最终参数，按 call_id 去重，并拒绝不完整 JSON。同步和流式路径共用解析器；nested error、incomplete_details、failed/incomplete/cancelled 最终状态均作为失败处理，避免失败响应被当作成功继续执行。
5. **图片输入。** 保留截图 data URL 与图像 detail，而不再统一覆盖为 auto。function_call 和 function_call_output 保留同一 call_id；后续请求携带工具结果。
6. **认证诊断。** 未使用的旧验证器也已去掉固定 GPT-5.4 推理，改为只读取官方 Codex 模型目录。登录主流程仍直接完成 OAuth，不增加推理请求。401 提示重新登录；403/模型无权限提示检查账号或换模型；订阅限额提示等待恢复。
7. **凭证边界。** 认证回调只监听 127.0.0.1；令牌与 Codex 请求禁止重定向。加密凭证存储解析失败时不再记录可能包含凭证的异常内容。代码仍使用原有 Android 加密偏好设置。

`OPENAI_API` 与其他服务商属于原项目的独立入口。本次订阅路线不读取、申请或使用 API Key。资产中独立 API 路线的旧模型条目保留，因为 ChatGPT 登录路线的退役公告不能直接当作 API 退役公告。手机自动点击、滑动、读取屏幕、Shizuku 及 Termux 的注册与执行模块均保留。

上下文窗口采用保守的 128,000 本地预算，未将 API 宣传的最大窗口直接套用到订阅账号。模型列表是已核查的内置配置；实际账号/工作区权限、灰度开放和可用额度由 ChatGPT 服务端决定。本次没有使用用户的真实账号做在线推理验证。

## 回归验证范围

- 授权码交换只发送一次 OAuth 请求，不生成 API Key；刷新保留必要字段。
- 旧版本错误令牌恢复、账号 ID 头、已有认证刷新并发保护。
- 旧模型设置迁移、打包目录默认选择、实际发送的模型 ID。
- 使用真实 mobile_action / termux_shell schema 的可选参数与 strict 标志。
- 截图内容和 detail；SSE 分块、最终参数、重复事件、错误终止状态。
- 离线模拟 HTTP 传输的同步/流式请求、每次重新取认证头、工具调用到结果回传的完整协议往返。
- 全项目 Android JVM 单元测试，包含手机操作、屏幕读取、Termux 等已有测试。

所有认证测试使用虚构令牌；没有读取、导出或上传用户的 ChatGPT 凭证，也没有使用 API 余额。

## Windows 构建修复

当前机器原先没有 JDK/Android SDK，已下载并校验 JDK 21 和 Android 命令行工具，安装 SDK 36 与 Build Tools 35.0.0；使用项目原有 Gradle 8.11.1 和 Android Gradle Plugin 8.9.1。

中文目录导致 AIDL 的 MalformedInputException，构建脚本使用临时 ASCII 盘符解决。另修复 Windows 上记忆文件 `File.renameTo` 无法覆盖已有文件的问题：优先原子移动替换，不支持原子移动时采用明确覆盖方式。路径测试改为检查文件名及父目录；跟踪测试显式声明自己的模型 fixture。这些修复没有删除测试或关闭失败检查。

工作区根目录的构建命令：

```powershell
& '.\build-tools\run-gradle.ps1' ':app:testDebugUnitTest' ':app:lintDebug' ':app:assembleDebug'
```

已有标准 Android 环境也可在源码根目录直接运行：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

APK 原始输出：`app/build/outputs/apk/debug/app-debug.apk`。该构建是 debug 签名安装包，包名 `ai.closepaw`，要求 Android 12 / API 31 以上。TLS 验证保持启用，`INSECURE_SSL_FOR_EVAL=false`。

最终测试数量、APK 校验值与安装步骤见工作区 `output/安装与验证说明.md`。

## 一手资料

- [Codex 模型及 ChatGPT 登录迁移说明](https://learn.chatgpt.com/docs/models)：优先 GPT-6.1 Sol，并按账号可用性选择其他当前模型；ChatGPT 登录路线的 GPT-5.5 退役说明。
- [Codex 认证说明](https://learn.chatgpt.com/docs/auth)：ChatGPT 订阅登录与 API Key 计费入口的区别。
- [工作区模型可用性](https://learn.chatgpt.com/docs/enterprise/workspace-model-availability)：模型权限及旧模型迁移。
- [GPT-6.1 Sol 模型说明](https://developers.openai.com/api/docs/models/gpt-6.1-sol)：模型标识、图像输入和 Responses 支持。
- [Function calling](https://developers.openai.com/api/docs/guides/function-calling)：strict 模式与可选参数要求。
- [官方 Codex 模型目录实现](https://github.com/openai/codex/blob/main/codex-rs/codex-api/src/endpoint/models.rs)：`models?client_version=...` 请求协议。
