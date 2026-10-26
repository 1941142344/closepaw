# ClosePaw：ChatGPT / Codex 社区兼容修复版

让 Android 手机通过自然语言执行操作：读取屏幕、点击、滑动，以及通过 Termux 执行脚本。本仓库基于 [imoonkey/closepaw](https://github.com/imoonkey/closepaw)，本次工作聚焦 ChatGPT 订阅登录路线的兼容修复。原项目、界面设计和手机自动化能力归原作者与贡献者所有。

**状态：社区预览版。** 源码与离线回归已验证；真实 ChatGPT Plus 登录、账号模型权限、实机完整任务仍待验证。没有使用用户的 ChatGPT 凭证做在线测试。不承诺任意 Plus 账号均可使用目录中的模型。

[下载预览 APK](https://github.com/1941142344/closepaw/releases/tag/v0.1.0-codex-compat.1) · [安装与排错](doc/release/community/INSTALL.zh-CN.md) · [技术分析](doc/dev/chatgpt-plus-compatibility.md) · [演示分镜和口播](doc/release/community/bilibili-demo.md) · [验证记录](doc/release/community/VALIDATION.md)

## 这次修复了什么

| 范围 | 本分支变更 |
| --- | --- |
| ChatGPT 登录 | 保留官方浏览器 OAuth、PKCE、state 和本地回调；移除将 OAuth 令牌交换成 API Key 的旧逻辑 |
| 模型迁移 | 订阅目录优先配置 `gpt-6.1-sol`，另列 `gpt-6-sol`、`gpt-6-luna`；旧 GPT-5.2/5.4/5.5 Codex 设置迁移到新默认值 |
| 请求协议 | 使用 Codex Responses 路线；保留图像 detail、工具 strict 标志和 call_id 关联 |
| 工具响应 | 处理 SSE 最终参数、重复事件和失败终止状态，避免不完整响应触发手机操作 |
| 凭证刷新 | 保留刷新响应省略的字段，修复旧版误存令牌的恢复逻辑，避免记录敏感内容 |
| Windows | 修复记忆文件替换及跨平台路径测试；提供中文路径的构建说明 |

模型名称出现在目录中只说明客户端已配置对应请求，不代表账号已经获得访问权限。旧模型在独立 API 路线的条目没有按 ChatGPT 订阅公告盲目删除。

## 保留的能力

- Android 无障碍服务读取界面、点击和滑动；截图或混合感知模式。
- 悬浮胶囊显示进度、暂停和停止。
- Termux 桥接与 `termux_shell`，可选 Shizuku 后台虚拟显示。
- 原有多服务商架构；本预览的推荐路径是 **Sign In with ChatGPT/Codex**，无需 OpenAI API Key。

应用及操作逻辑在手机运行；选择云模型时，任务文本、必要的屏幕内容或截图会发送到所选模型服务。不要把“手机端运行”理解成“模型离线推理”或“屏幕数据永不离开手机”。

## 安装

要求 Android 12 / API 31 以上。在本仓库 Release 中下载 `ClosePaw-ChatGPT-Plus-debug.apk`，按 [安装文档](doc/release/community/INSTALL.zh-CN.md) 完成权限设置。APK 为 Debug 签名，与上游正式版的签名可能不同，不能保证覆盖安装。

在模型设置选择 ChatGPT/Codex Sign In，由本人在官方页面登录，然后选择账号实际可用的模型。当前已观察到官方授权页返回 `unsupported_country_region_territory`；此时尚未进入模型请求阶段，请查看安装文档的对应排错段。

## 构建与测试

准备 JDK 17+、Android SDK 36 与 Build Tools 35.0.0，在源码目录运行：

```powershell
.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Linux/macOS 使用 `./gradlew`。APK 输出为 `app/build/outputs/apk/debug/app-debug.apk`。Windows 中文目录建议将源码放到 ASCII 路径，或参照 [Windows 构建帮助脚本](scripts/community-gradle.ps1) 创建临时盘符。脚本使用本机已安装的 JDK/SDK，不下载工具或修改全局环境。

2026-10-26 的完整验证：2,106 项测试，2,098 通过，8 跳过，0 失败；Lint 0 错误、94 警告、4 信息。结果来自本地构建，不代表 GitHub CI 或实机登录已通过。[完整验证范围](doc/release/community/VALIDATION.md)

## 演示与反馈

录制建议、三组可复制任务、逐段口播、登录受阻时的替代分镜，以及 B 站标题/简介/置顶评论见 [演示文档](doc/release/community/bilibili-demo.md)。请用自己的实录替代示意镜头，并按实测结果描述能力。

反馈可以包含系统版本、APK 版本、模型 ID、错误码和脱敏后的复现步骤。不要提交访问令牌、refresh token、完整授权回调链接、聊天记录或个人屏幕数据。

## 来源与许可证

- 上游：[imoonkey/closepaw](https://github.com/imoonkey/closepaw)，基线 `75dae2653f5a6b25d5df51ee7008b0f830de1536`。
- 本分支改动：[MODIFICATIONS.md](MODIFICATIONS.md)。保留上游历史、[Apache-2.0 LICENSE](LICENSE) 和 [NOTICE](NOTICE)。
- APK 包含上游引入的 LiquidAI LEAP SDK。该组件有独立专有许可，不因本仓库使用 Apache-2.0 而成为 Apache 组件。见 [二进制组件条款](doc/release/community/DISTRIBUTION.md) 和应用内开源许可列表。
- 本项目与 OpenAI、ChatGPT、Codex 无官方关联，不提供账号、代登录或订阅额度。
