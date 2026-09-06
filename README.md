# AI PDF Scanner – Scan, OCR & PDF
*Developed & Published by **FX Signal Lab***

A complete, production-grade Android application engineered with modern Android development standards for publishing on the Google Play Store (2026/2027 compliant).

---

## 📱 Highlights & Features

- **Modern Jetpack Compose UI**: 100% Material Design 3 with dynamic theme support (System, Light, Dark).
- **CameraX High-Performance Scanning**: Flash torch toggle, auto-focus, grid guides, and multi-page batch scanning.
- **Perspective Warping & Edge Detection**: Interactive 8-point quad manipulation with real-time perspective correction and document filters (Magic Color, B&W, Grayscale).
- **Local On-Device OCR**: Offline text recognition powered by Google ML Kit Vision.
- **AI Document Assistant**: Integrated Gemini API client for document summarization, entity extraction (dates, amounts, emails), multi-language translation, and contextual Q&A (with local offline heuristic fallback).
- **Comprehensive PDF Utility Suite**:
  - PDF Compressor
  - PDF Merger
  - PDF to Image converter
  - Photo to PDF creator
  - Native Android printing service & system share sheet integration
- **Robust Local Persistence**: Android Room Database with foreign key cascade deletions, full-text search query index, and a trash recovery system.
- **Monetization & Privacy**: Built-in Google AdMob SDK support (Banner, Interstitial) with dedicated Test Mode toggle and PRO ad-free unlock mode.
- **GitHub Actions CI/CD**: Automated pipeline to build and package debug APK artifacts on push or pull request.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (BOM 2024.09.00)
- **Architecture**: Clean Architecture / MVVM (`ScanSessionViewModel`, `StateFlow`, Coroutines)
- **Camera**: CameraX (Camera2, Lifecycle, View)
- **Database**: Room Database with KSP (`AppDatabase`, `DocumentDao`, `ScannedDocument`, `ScannedPage`)
- **Computer Vision**: ML Kit Text Recognition (On-Device), Android Graphics Matrix Warp & Luminance Thresholding
- **PDF Engine**: Android `PdfDocument`, `PdfRenderer`, and `PrintManager`
- **Image Loading**: Coil Compose
- **Networking**: OkHttp & Kotlinx Serialization for Gemini REST API

---

## 🚀 Building the Project

### Prerequisites
- Android Studio Ladybug / Koala or newer
- JDK 17
- Android SDK Platform 35 (minSdk 24, targetSdk 35)

### Command Line
```bash
# Build Debug APK
./gradlew assembleDebug

# Output location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚙️ Configuration & Secrets

API Keys and secrets can be configured via:
1. **AI Studio Secrets Panel / BuildConfig**: `GEMINI_API_KEY`
2. **In-App Settings**: Enter custom API keys directly within the app's Settings screen.
3. **AdMob IDs**: Configured in `com.example.core.ads.AdMobConfig.kt` with user-provided IDs:
   - App ID: `ca-app-pub-4545041470404090~4069818828`
   - Banner ID: `ca-app-pub-4545041470404090/5129676649`
   - Interstitial ID: `ca-app-pub-4545041470404090/5836417730`
   - Rewarded ID: `ca-app-pub-4545041470404090/6987747805`
   - Test mode switch available in Settings for safe development and Play Console verification.

---

## 📄 License
Copyright © 2026 FX Signal Lab. All Rights Reserved.
