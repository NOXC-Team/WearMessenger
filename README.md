# WearMessenger

A lightweight, open-source Telegram client for Wear OS.

[简体中文](README_zh_CN.md) | [繁體中文](README_zh_TW.md)

## Features

- Send and receive text messages
- View chat history with automatic pagination (loads older messages when scrolling to top)
- View images and videos, take and send photos
- Support for proxy servers
- Multiple accounts support
- Standalone app (no phone required)
- Built with Jetpack Compose for modern UI
- App Lock with PIN protection
- Device session management (view and terminate other devices)
- Storage management (view cache size and clear cache)
- Double-tap to reply to messages
- Wear OS native notifications with quick reply

## Comparison with other Wear OS Telegram clients

| Feature | WearMessenger | Weargram | TGwear |
|---------|---------------|----------|--------|
| **Price** | Free | Paid ($3.49) | Free |
| **Open Source** | ✅ Yes | ❌ No | ✅ Yes |
| **Standalone** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Minimum API** | 25 (Android 7.0) | ? | 24 (Android 7.0) |
| **Voice Messages** | ❌ No | ✅ Yes | ✅ Yes |
| **Images/Videos** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Proxy Support** | ✅ Yes | ? | ✅ Yes |
| **Multiple Accounts** | ✅ Yes (up to 99) | ? | ✅ Yes |
| **Digital Crown** | ❌ No | ? | ✅ Yes |
| **App Lock** | ✅ Yes | ? | ❌ No |
| **Session Management** | ✅ Yes | ? | ? |
| **Storage Management** | ✅ Yes (view & clear) | ? | ✅ Yes (clear only) |
| **Double-tap to Reply** | ❌ No | ❌ No | ❌ No |
| **Wear OS Notifications** | ❌ No | ❌ No | ❌ No |
| **Archived Chats** | ❌ No | ? | ✅ Yes |
| **APK size (arm64-v8a)** | ~26 MB | ~25 MB | ~43.5 MB |

*Note: WearMessenger focuses on lightweight messaging experience.*

## Installation

1. Download the latest APK from [GitHub Releases](https://github.com/NOXC-Team/WearMessenger/releases)
2. Choose the appropriate version for your device:
   - `app-arm64-v8a.apk` for 64-bit ARM devices (most modern Wear OS devices)
   - `app-armeabi-v7a.apk` for 32-bit ARM devices
   - `app-universal.apk` for all architectures (larger file size)
3. Install via ADB:
   ```bash
   adb install <path-to-apk>
   ```

## Setup

### Prerequisites

- Android Studio
- Wear OS device or emulator (API 25+)
- Telegram API credentials

### Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/NOXC-Team/WearMessenger.git
   cd WearMessenger
   ```

2. **Configure local.properties**

   Copy `local.properties.example` to `local.properties` and fill in the required values:
   ```properties
   # SDK location (set this to your Android SDK path)
   # sdk.dir=C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk

   # API credentials for Telegram
   api.id=1234567
   api.hash=abcdef1234567890abcdef1234567890
   ```

   You can get your API credentials from [my.telegram.org](https://my.telegram.org).

3. **Build and run**

   Open the project in Android Studio, connect your Wear OS device or start an emulator, then click Run.

## License

This project is licensed under the GPL-3.0 License - see the LICENSE file for details.

### Important Note

WearMessenger uses [TDLib](https://github.com/tdlib/td), the official Telegram Database Library. TDLib is licensed under the [Boost Software License](https://www.boost.org/LICENSE_1_0.txt). This project is not affiliated with, sponsored by, or endorsed by Telegram or Durov.

## Acknowledgments

- [TDLib](https://github.com/tdlib/td) - Telegram Database Library
