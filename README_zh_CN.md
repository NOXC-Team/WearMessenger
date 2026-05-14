# WearMessenger

一款轻量级、开源的 Wear OS Telegram 客户端。

[English](README.md) | [繁體中文](README_zh_TW.md)

## 功能

- 发送和接收文字消息
- 查看聊天记录，支持自动加载历史消息（滚动到顶部加载）
- 查看图片和视频，拍摄并发送照片
- 代理服务器支持
- 多账号支持
- 独立应用（无需手机）
- 基于 Jetpack Compose 构建现代化 UI
- PIN 码应用锁
- 设备会话管理（查看和终止其他设备）
- 存储管理（查看缓存大小和清除缓存）
- 双击回复消息
- Wear OS 原生通知，支持快捷回复

## 与其他 Wear OS Telegram 客户端对比

| 功能 | WearMessenger | Weargram | TGwear |
|------|---------------|----------|--------|
| **价格** | 免费 | 付费 ($3.49) | 免费 |
| **开源** | ✅ 是 | ❌ 否 | ✅ 是 |
| **独立应用** | ✅ 是 | ✅ 是 | ✅ 是 |
| **最低 API** | 25 (Android 7.0) | ? | 24 (Android 7.0) |
| **语音消息** | ❌ 否 | ✅ 是 | ✅ 是 |
| **图片/视频** | ✅ 是 | ✅ 是 | ✅ 是 |
| **代理支持** | ✅ 是 | ? | ✅ 是 |
| **多账号** | ✅ 是（最多 99 个） | ? | ✅ 是 |
| **表冠旋转** | ❌ 否 | ? | ✅ 是 |
| **应用锁** | ✅ 是 | ? | ❌ 否 |
| **会话管理** | ✅ 是 | ? | ? |
| **存储管理** | ✅ 是（查看+清除） | ? | ✅ 是（仅清除） |
| **双击回复** | ❌ 否 | ❌ 否 | ❌ 否 |
| **Wear OS 通知** | ❌ 否 | ❌ 否 | ❌ 否 |
| **归档聊天** | ❌ 否 | ? | ✅ 是 |
| **APK 大小（arm64-v8a）** | ~26 MB | ~25 MB | ~43.5 MB |

*注：WearMessenger 专注于轻量级消息体验。*

## 安装

1. 从 [GitHub Releases](https://github.com/NOXC-Team/WearMessenger/releases) 下载最新的 APK
2. 选择适合您设备的版本：
   - `app-arm64-v8a.apk` 用于 64 位 ARM 设备（大多数现代 Wear OS 设备）
   - `app-armeabi-v7a.apk` 用于 32 位 ARM 设备
   - `app-universal.apk` 适用于所有架构（文件较大）
3. 通过 ADB 安装：
   ```bash
   adb install <apk 路径>
   ```

## 搭建

### 前置要求

- Android Studio
- Wear OS 设备或模拟器（API 25+）
- Telegram API 凭证

### 开始使用

1. **克隆仓库**
   ```bash
   git clone https://github.com/NOXC-Team/WearMessenger.git
   cd WearMessenger
   ```

2. **配置 local.properties**

   将 `local.properties.example` 复制为 `local.properties` 并填入所需值：
   ```properties
   # SDK 位置（设置为您的 Android SDK 路径）
   # sdk.dir=C:\\Users\\您的用户名\\AppData\\Local\\Android\\Sdk

   # Telegram API 凭证
   api.id=1234567
   api.hash=abcdef1234567890abcdef1234567890
   ```

   您可以从 [my.telegram.org](https://my.telegram.org) 获取 API 凭证。

3. **构建和运行**

   在 Android Studio 中打开项目，连接 Wear OS 设备或启动模拟器，然后点击运行。

## 许可证

本项目采用 GPL-3.0 许可证 - 详细信息请参阅 LICENSE 文件。

### 重要声明

WearMessenger 使用 [TDLib](https://github.com/tdlib/td)，即官方的 Telegram 数据库库。TDLib 采用 [Boost 软件许可证](https://www.boost.org/LICENSE_1_0.txt)。本项目与 Telegram 或 Durov 无关联、未经其赞助或认可。

## 致谢

- [TDLib](https://github.com/tdlib/td) - Telegram 数据库库
