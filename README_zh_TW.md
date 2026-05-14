# WearMessenger

一款輕量級、開源的 Wear OS Telegram 客戶端。

[English](README.md) | [简体中文](README_zh_CN.md)

## 功能

- 發送和接收文字訊息
- 查看聊天記錄，支援自動載入歷史訊息（捲動到頂部載入）
- 查看圖片和影片，拍攝並發送照片
- 代理伺服器支援
- 多帳號支援
- 獨立應用（無需手機）
- 基於 Jetpack Compose 構建現代化 UI
- PIN 碼應用鎖
- 裝置會話管理（查看和終止其他裝置）
- 儲存管理（查看快取大小和清除快取）
- 雙擊回覆訊息
- Wear OS 原生通知，支援快捷回覆

## 與其他 Wear OS Telegram 客戶端對比

| 功能 | WearMessenger | Weargram | TGwear |
|------|---------------|----------|--------|
| **價格** | 免費 | 付費 ($3.49) | 免費 |
| **開源** | ✅ 是 | ❌ 否 | ✅ 是 |
| **獨立應用** | ✅ 是 | ✅ 是 | ✅ 是 |
| **最低 API** | 25 (Android 7.0) | ? | 24 (Android 7.0) |
| **語音訊息** | ❌ 否 | ✅ 是 | ✅ 是 |
| **圖片/影片** | ✅ 是 | ✅ 是 | ✅ 是 |
| **代理支援** | ✅ 是 | ? | ✅ 是 |
| **多帳號** | ✅ 是（最多 99 個） | ? | ✅ 是 |
| **錶冠旋轉** | ❌ 否 | ? | ✅ 是 |
| **應用鎖** | ✅ 是 | ? | ❌ 否 |
| **會話管理** | ✅ 是 | ? | ? |
| **儲存管理** | ✅ 是（查看+清除） | ? | ✅ 是（僅清除） |
| **雙擊回覆** | ❌ 否 | ❌ 否 | ❌ 否 |
| **Wear OS 通知** | ❌ 否 | ❌ 否 | ❌ 否 |
| **歸檔聊天** | ❌ 否 | ? | ✅ 是 |
| **APK 大小（arm64-v8a）** | ~26 MB | ~25 MB | ~43.5 MB |

*註：WearMessenger 專注於輕量級訊息體驗。*

## 安裝

1. 從 [GitHub Releases](https://github.com/NOXC-Team/WearMessenger/releases) 下載最新的 APK
2. 選擇適合您裝置的版本：
   - `app-arm64-v8a.apk` 用於 64 位 ARM 裝置（大多數現代 Wear OS 裝置）
   - `app-armeabi-v7a.apk` 用於 32 位 ARM 裝置
   - `app-universal.apk` 適用於所有架構（檔案較大）
3. 通過 ADB 安裝：
   ```bash
   adb install <apk 路徑>
   ```

## 搭建

### 前置要求

- Android Studio
- Wear OS 裝置或模擬器（API 25+）
- Telegram API 憑證

### 開始使用

1. **複製儲存庫**
   ```bash
   git clone https://github.com/NOXC-Team/WearMessenger.git
   cd WearMessenger
   ```

2. **配置 local.properties**

   將 `local.properties.example` 複製為 `local.properties` 並填入所需值：
   ```properties
   # SDK 位置（設定為您的 Android SDK 路徑）
   # sdk.dir=C:\\Users\\您的使用者名稱\\AppData\\Local\\Android\\Sdk

   # Telegram API 憑證
   api.id=1234567
   api.hash=abcdef1234567890abcdef1234567890
   ```

   您可以從 [my.telegram.org](https://my.telegram.org) 取得 API 憑證。

3. **建構和執行**

   在 Android Studio 中開啟專案，連接 Wear OS 裝置或啟動模擬器，然後點選執行。

## 許可證

本專案採用 GPL-3.0 許可證 - 詳細資訊請參閱 LICENSE 檔案。

### 重要聲明

WearMessenger 使用 [TDLib](https://github.com/tdlib/td)，即官方的 Telegram 資料庫庫。TDLib 採用 [Boost 軟體許可證](https://www.boost.org/LICENSE_1_0.txt)。本專案與 Telegram 或 Durov 無關聯、未經其贊助或認可。

## 致謝

- [TDLib](https://github.com/tdlib/td) - Telegram 資料庫庫
