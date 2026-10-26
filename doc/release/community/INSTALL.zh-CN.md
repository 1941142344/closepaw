# 安装与排错

本文件适用于 `v0.1.0-codex-compat.1` 社区预览版。APK 包名 `ai.closepaw`，Android 版本显示为 `0.1.0`（versionCode 1）；预览标签用于区分本次社区发布。

## 下载与校验

从 [本仓库预览版 Release](https://github.com/1941142344/closepaw/releases/tag/v0.1.0-codex-compat.1) 下载 APK 和 `SHA256SUMS.txt`。要求 Android 12 及以上。

文件名：`ClosePaw-ChatGPT-Plus-debug.apk`；大小：235,243,594 字节。

SHA-256：

```text
58f722e2e83cf9d8260a92f338d7c41180dfa209b9069354715e632a08a55d6c
```

Windows 可用 `Get-FileHash .\ClosePaw-ChatGPT-Plus-debug.apk -Algorithm SHA256` 校验。附带的 Debug APK 不是上游签名的生产版。

## 第一次使用

1. 将 APK 传到手机，从文件管理器打开，按系统提示允许安装此来源。
2. 若已有同包名应用而安装失败，先检查是否签名不同。先备份需要保留的数据，再自行决定是否卸载旧版；卸载会清除本地数据。不要导出登录令牌。
3. 启动 ClosePaw，按引导开启无障碍服务和悬浮窗，并按需调整电池优化。
4. 在模型设置进入 **Sign In with ChatGPT/Codex**。本人在 `auth.openai.com` 完成授权；不需要 OpenAI API Key。
5. 选择账号实际可用的模型，优先检查 GPT-6.1 Sol。目录配置不保证账号权限；无法访问某模型时，以服务端错误和账号可用列表为准。
6. 新建会话，先尝试：“打开系统设置，读取首页前 3 个可见的设置分类，然后返回主屏幕。不要修改任何设置。”观察实际工具步骤与结果。

选择云模型时，任务和必要的屏幕内容会发给模型服务。首次测试请使用无个人数据的系统界面。

## Termux

安装兼容的 F-Droid Termux 并手动打开一次，回到 ClosePaw 的 Termux 设置，按页面完成桥接设置及 RUN_COMMAND 授权。成功状态应为 Ready；会话开始时才决定是否注册 `termux_shell`。

桥接需要 `allow-external-apps` 和必要的软件包。若 ROM 阻止后台启动 Termux，手动打开 Termux 后再执行设置。详见 [完整 Termux 文档](../../main/app/termux_shell.md)。

## `unsupported_country_region_territory`

该错误来自官方 OAuth 授权页，发生在浏览器回调之前。出现时不能判定模型或工具调用已成功。改模型、购买 API 额度或重编 APK不能解除该服务端限制。

先在同一手机、同一浏览器检查能否正常登录 ChatGPT。若实际处于官方支持地区，可按官方建议清理相关 Cookie、使用无痕窗口或其他浏览器；仍失败请联系 OpenAI 支持。若所在地区不受支持，客户端无法改变服务提供范围。

- [ChatGPT 支持地区](https://help.openai.com/en/articles/7947663-chatgpt-supported-countries)
- [官方地区错误排查](https://help.openai.com/en/articles/9131992-chatgpt-and-api-services-in-unsupported-countries-and-territories)

请从 ClosePaw 重新发起新的授权，避免复用已超时的授权页面。不要公开完整授权链接或回调参数。

## 其他问题

| 现象 | 排查 |
| --- | --- |
| 401 | 在应用中重新登录，勿把令牌复制给他人 |
| 403 / model_not_found | 核对账号、工作区与模型权限，使用账号实际可用模型 |
| 429 / usage limit | 按订阅服务给出的限额和恢复时间处理 |
| 授权完成但应用等待 | 保持应用运行，从同一手机浏览器完成回调；重新发起授权，检查 localhost 回调是否被网络工具干扰 |
| 无法点击或读取屏幕 | 检查无障碍服务是否启用；安全界面或受保护应用可能无法读取 |
| Termux 未出现 | 检查设置是否 Ready，并新建会话；查看原项目的 Termux 排错文档 |

报告问题只提交脱敏错误码和复现步骤。源代码测试采用虚构令牌；本次未采集或上传个人 ChatGPT 凭证。
