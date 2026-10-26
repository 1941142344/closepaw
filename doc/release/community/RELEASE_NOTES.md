# ClosePaw ChatGPT/Codex 社区兼容修复预览 1

基于 [imoonkey/closepaw](https://github.com/imoonkey/closepaw) 的社区二次开发，基线 `75dae2653f5a6b25d5df51ee7008b0f830de1536`。保留上游历史、LICENSE 与 NOTICE。本构建不是上游或 OpenAI 官方发行版。

## 修复

- 保留 Sign In with ChatGPT/Codex、PKCE 与 state 校验，移除额外的 API Key 交换。订阅路线无需 OpenAI API Key。
- 订阅目录默认配置 `gpt-6.1-sol`，另列 `gpt-6-sol`、`gpt-6-luna`，迁移旧 GPT-5.2/5.4/5.5 Codex 设置。
- 修复工具 schema strict 标志、截图 detail、SSE 最终参数、call_id 关联、重复事件与异常终止处理。
- 修复凭证刷新字段保留与旧令牌恢复，收紧回调、重定向和敏感日志处理。
- 保留手机点击、滑动、屏幕读取、悬浮胶囊、Termux 和 Shizuku；补充 Windows 文件替换兼容修复。

## 验证与已知限制

本地 Gradle `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 成功。2,106 项测试：2,098 通过、8 跳过、0 失败、0 错误；Lint 0 错误、94 警告、4 信息。APK 验签通过，TLS 校验开启。

测试使用虚构凭证与模拟传输。**真实 ChatGPT Plus 在线调用与该用户手机的端到端任务仍待验证。** 已观察到官方授权页返回 `unsupported_country_region_territory`；此时尚未到模型请求阶段，客户端无法解除服务端地区限制。模型目录不保证任意账号访问权限，实际权限与限额以服务端为准。

## 安装与附件

- Android 12+；APK 为 Debug 签名；包名 `ai.closepaw`，应用版本 `0.1.0` / code 1。
- 下载 `ClosePaw-ChatGPT-Plus-debug.apk`，校验 `SHA256SUMS.txt`，按 `INSTALL.zh-CN.md` 设置权限和本人登录。
- 与上游正式版签名可能不同，覆盖安装可能失败。先保留本地数据，再自行决定是否卸载旧版；卸载会清除本地数据。
- APK 大小 235,243,594 字节；SHA-256：`58f722e2e83cf9d8260a92f338d7c41180dfa209b9069354715e632a08a55d6c`。
- 附带本地 Gradle 日志、Lint 报告、验证说明、安装说明与 B 站演示稿。签名私钥和个人登录凭证不包含在附件中。

APK 包含上游引入的专有 LEAP SDK，适用独立 [LEAP 条款](https://leap.liquid.ai/terms)；请阅读附带 `DISTRIBUTION.md`。其余许可证与上游通知保留。

[中文说明](https://github.com/1941142344/closepaw/blob/main/README.zh-CN.md) · [技术分析](https://github.com/1941142344/closepaw/blob/main/doc/dev/chatgpt-plus-compatibility.md) · [演示与口播](https://github.com/1941142344/closepaw/blob/main/doc/release/community/bilibili-demo.md)
