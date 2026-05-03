# WearMessenger

A lightweight, open-source Telegram client for Wear OS.

## Features

- Send and receive text messages
- View chat history with automatic pagination (loads older messages when scrolling to top)
- Support for proxy servers
- Multiple accounts support
- Standalone app (no phone required)
- Built with Jetpack Compose for modern UI

## Comparison with other Wear OS Telegram clients

| Feature | WearMessenger | Weargram | TGwear |
|---------|---------------|----------|--------|
| **Price** | Free | Paid ($3.49) | Free |
| **Open Source** | ✅ Yes | ❌ No | ✅ Yes |
| **Standalone** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Minimum API** | 25 (Android 7.0) | ? | 24 (Android 7.0) |
| **Voice Messages** | ❌ No | ✅ Yes | ✅ Yes |
| **Images/Videos** | ❌ No | ✅ Yes | ✅ Yes |
| **Proxy Support** | ✅ Yes | ? | ? |
| **Multiple Accounts** | ✅ Yes | ? | ✅ Yes |
| **Digital Crown** | ❌ No | ? | ✅ Yes |

*Note: WearMessenger focuses on lightweight messaging experience. For full-featured experience including media, check out Weargram or TGwear.*

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

## Acknowledgments

- [TDLib](https://github.com/tdlib/td) - Telegram Database Library
