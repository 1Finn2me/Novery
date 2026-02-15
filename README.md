# Novery

<div align="center">

**A modern Android novel reader with multi-source support, TTS, and offline reading**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-blue.svg)](https://developer.android.com/about/versions/oreo)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE)

[Download](#installation) · [Features](#features) · [Screenshots](#screenshots) · [Contributing](#contributing)

</div>

---

## 📖 About

**Novery** is a free, open-source Android app for reading web novels from multiple sources. Inspired by [Tachiyomi](https://github.com/tachiyomiorg/tachiyomi) and [QuickNovel](https://github.com/LagradOst/QuickNovel), it provides a clean, customizable reading experience with **Text-to-Speech**, **offline reading**, and **personalized recommendations** — all built with **Jetpack Compose** and **Material 3**.

---

## ✨ Features

### 📚 Multi-Source Browse & Search
- **6 novel sources**: NovelFire, Webnovel, NovelBin, LibRead, RoyalRoad, NovelsOnline  
- Progressive streaming search with fuzzy matching and history  
- Enable/disable individual sources  

### 📖 Reader
- Continuous scroll or Chaptered view  
- Customizable fonts, sizing, spacing, alignment, and colors  
- Fullscreen mode with auto-hiding controls and volume key navigation  
- Bookmarks  
- Stable position tracking across sessions  

### 🎧 Text-to-Speech
- Background playback with screen off  
- Real-time sentence highlighting and auto-scroll  
- Lock screen and Bluetooth media controls  
- Automatic chapter advancement  
- Full voice selection from device TTS engines  

### 💾 Offline Reading
- Background download queue with priority levels  
- Auto-download new chapters from library novels  
- Per-novel storage management  

### 📚 Library
- Track status: Reading, Completed, On Hold, Plan to Read, Dropped  
- New chapter detection with badge indicators  
- Filtering, sorting, and batch operations  

### 🤖 Recommendations
- Tag-based matching engine that learns from your reading patterns  
- Categories: *For You*, *Because You Read X*, *From Authors You Like*  
- Configurable filters to block unwanted tags, authors, or sources  

### 🎨 Theming
- Material You dynamic colors (Android 12+)  
- Light, Dark, and AMOLED black modes  
- 7 preset themes + full custom color picker  
- Independent reader color scheme  

### 📊 Statistics & History
- Automatic reading time tracking  
- Daily reading streaks  
- History timeline grouped by date  
- Chapter completion progress  

---

## 📸 Screenshots

<div align="center" style="display: flex; flex-wrap: wrap; justify-content: center; gap: 16px;">

  <img src="https://github.com/user-attachments/assets/c0679bb9-4204-42e2-a71a-3faf9e3e4c87" alt="Onboard" width="250" />
  <img src="https://github.com/user-attachments/assets/281da9b1-4d71-41d1-89b8-264d085eebb7" alt="Library" width="250" />
  <img src="https://github.com/user-attachments/assets/b715a6ec-2a40-4aa8-bb92-609b34d7c0d4" alt="TTS" width="250" />
  <img src="https://github.com/user-attachments/assets/084957c8-2a01-4535-8318-9dfb4708ba9f" alt="Settings" width="250" />
  <img src="https://github.com/user-attachments/assets/bea55bae-24fa-4694-8dc9-9e631c1772f5" alt="Themes" width="250" />
  <img src="https://github.com/user-attachments/assets/2171786b-337c-4e26-a505-cbc15092e4b8" alt="Stats" width="250" />
  <img src="https://github.com/user-attachments/assets/349b1de7-ffb8-47b9-8d37-307392715e2c" alt="History" width="250" />
  <img src="https://github.com/user-attachments/assets/75247556-b5aa-4756-87c5-0f01e3a81803" alt="Download" width="250" />
  <img src="https://github.com/user-attachments/assets/5953d4c2-2e31-4a8b-b2f7-981628ce22ac" alt="Reader 2" width="250" />
    <img src="https://github.com/user-attachments/assets/2473aaa3-9c43-4a67-b089-6fa7a71be0c6" alt="Reader" width="250" />
  <img src="https://github.com/user-attachments/assets/1b517ca6-3e54-45e3-923a-61b5f8c0759a" alt="Reader" width="250" />

</div>


---

## 📥 Installation

### Download
1. Grab the latest APK from [Releases](../../releases)  
2. Install and grant notification permissions when prompted  

> Requires **Android 8.0+ (API 26)**

### Build from Source
```bash
git clone https://github.com/1Finn2me/Novery.git
cd Novery
./gradlew installDebug
```
Requires **Android Studio Koala+**, **JDK 11+**, **Kotlin 2.0.21**.

---

## 🛠 Tech Stack

| Component     | Technology |
|---------------|------------|
| Language      | Kotlin |
| UI            | Jetpack Compose · Material 3 |
| Architecture  | MVVM · Repository pattern |
| Database      | Room |
| Async         | Coroutines · Flow |
| Networking    | OkHttp · Jsoup |
| Images        | Coil |
| Serialization | Kotlinx Serialization · Gson |
| TTS           | Android TextToSpeech API |

---

## 🙏 Acknowledgments
- [Tachiyomi](https://github.com/tachiyomiorg/tachiyomi) — pioneered the multi-source reader concept  
- [QuickNovel](https://github.com/LagradOst/QuickNovel) — inspiration for novel-specific reader design  
- [LNReader](https://github.com/LNReader/lnreader-sources) — extensible source architecture  

---

## ⚠️ Disclaimer
Novery does not host, store, or distribute any content. The app functions as a search
engine and aggregator — it crawls and displays content from third-party websites that
are publicly accessible through any standard web browser. Novery has no affiliation
with, and no control over, the content provided by these sources.

Any legal concerns regarding content should be directed to the respective website
operators and content hosts. In cases of copyright infringement, please contact the
responsible parties or file hosts directly.

This application is intended for personal and educational use only. Users are solely
responsible for ensuring their use of the app complies with all applicable local,
national, and international laws. Use Novery at your own risk.

By using this application, you acknowledge that the developers of Novery bear no
responsibility for any content accessed through third-party sources, nor for any
consequences arising from the use of this app.
---

<div align="center">

⭐ **Star the repo if you find Novery useful!**  

[Report Bug](../../issues) · [Request Feature](../../issues)

</div>
