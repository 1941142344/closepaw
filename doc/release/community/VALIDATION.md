# 社区预览验证记录

验证日期：2026-10-26。发布材料整理日期：2026-10-27（Asia/Shanghai）。

## 构建结果

实际执行 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`，Gradle 返回 **BUILD SUCCESSFUL**。使用 Gradle 8.11.1、AGP 8.9.1、JDK 21、Android SDK 36、Build Tools 35.0.0；Java/Kotlin 字节码目标为 17。

| 项目 | 结果 |
| --- | --- |
| JVM 测试类 | 207 |
| 测试总数 | 2,106 |
| 通过 / 跳过 | 2,098 / 8 |
| 失败 / 错误 | 0 / 0 |
| Lint | 0 错误、94 警告、4 信息 |
| APK | Debug 签名，签名方案 v2 验证通过 |
| 包名 / 版本 | ai.closepaw / 0.1.0 (1) |
| 最低版本 / ABI | Android 12；arm64-v8a、armeabi-v7a、x86、x86_64 |
| APK 大小 | 235,243,594 字节 |
| TLS | 开启证书校验，INSECURE_SSL_FOR_EVAL=false |

APK SHA-256：`58f722e2e83cf9d8260a92f338d7c41180dfa209b9069354715e632a08a55d6c`。

Codex 请求构建 10 项、客户端 14 项、SSE 解析 25 项、订阅协议集成 4 项全部通过；OAuth、模型迁移、手机工具与 Termux 等原有回归也包含在全套测试中。未为此次修复关闭或删除失败测试。

## 协议覆盖

集成测试使用真实 `mobile_action` / `termux_shell` 定义和虚构认证数据，通过模拟 HTTP 传输检查同步/流式事件、工具调用到工具结果的往返、截图 detail、每次读取刷新后的认证头和真实请求模型 ID。测试验证协议适配，不代表手机执行结果。

已读取生成 APK 内的模型资产与 Termux 桥脚本，确认新配置实际打包；并用 apksigner/aapt 检查签名、包名、ABI 与系统要求。

## 尚未完成

- 真实 ChatGPT Plus 账号的在线模型调用。
- 该用户手机上的登录、点击、滑动、截图和 Termux 的完整端到端复现。
- GitHub Actions 验证不自动等同于上述本地结果；新工作流默认仅手动触发。

当前用户反馈的官方登录页面返回 `unsupported_country_region_territory`，尚未进入模型调用阶段。发布与视频不得将离线测试描述成真实账号已跑通。

Release 附带 `gradle-verification.log`、`lint-results-debug.html`、本记录和校验文件。没有附带手机屏幕、会话日志、个人目录、凭证或签名私钥。
