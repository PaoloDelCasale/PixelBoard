# PixelBoard Stable Release (v18.3.1 / Patch v1.0.6)

## Summary
PixelBoard v18.3.1 (Patch Bundle v1.0.6) brings the rock-solid in-line AI feature set directly to Google's latest **Gboard v18.3.1 Release** (`18.3.1.977415014-release-arm64-v8a`) along with critical idle battery optimizations for Gemini Rambler Voice Typing.

---

## What's New & Changed

### 1. Updated to Gboard v18.3.1 Release Base
- Rebased the primary stable build from Gboard 18.0.3 to official **Gboard 18.3.1 Release** (`18.3.1.977415014-release-arm64-v8a`, 81.2 MB).
- Fully supports arm64-v8a devices running Android 10 through Android 16 Preview.
- Backward compatibility maintained for Gboard 18.0.3.

### 2. Patch Bundle v1.0.6
- **Rambler not activating after an automatic reboot (Direct Boot) fixed**: after a reboot that unlocks the phone automatically (for example Samsung's automatic restart or RAM Plus restart, which do not ask for the PIN), the system can start the keyboard process about a second *before* the user storage is unlocked. Gboard and the patches decide the dictation setup in that window and keep it for the whole life of the process, so Rambler stayed off (`AgenticDictationExtension.onActivate` never appeared) until the keyboard process was restarted by hand. A process started after the unlock was not affected. When the first patched flag read happens while the user is still locked, the keyboard now waits for `ACTION_USER_UNLOCKED`, drops its cached dictation decisions and, if Advanced Voice is enabled, restarts its own process 2 seconds later (the same as a manual force-stop); processes started after the unlock are not touched. The dictation-selection cache is also no longer filled while the user is locked. Log lines under the `GboardPatches` tag with the `[unlock-guard]` prefix show what happened (credits: @PaoloDelCasale for the device investigation).
- Not reproducible by hand without root: it needs a reboot that unlocks automatically. Please report if Rambler still fails to start after one.

### 2a. Patch Bundle v1.0.5
- **Dictation language-download retry loop fixed (battery)**: on Gboard 18.3.1 Release, when a keyboard language (for example `en-US` next to `it-IT`) has no offline speech pack installed, Gboard requested the pack download over and over. The speech service refuses silent downloads from the renamed package but still completes the request, so each completion re-ran the eligibility check, which requested the download again, about every 6.5 ms while the keyboard was open (about 1,800 download jobs and SbG checks per opening, hundreds of `Can't silent download model` errors per second in Google TTS, roughly 15% of a CPU core). The first request for each language and source is now let through and identical repeats within 10 minutes are dropped; concurrent identical requests are decided atomically. Verified on device: 2 download jobs at start-up and none afterwards, idle CPU about 0.04%. Progress is visible in logcat under the `GboardPatches` tag (`allowed language download …` / `suppressed repeated language download …`) (credits: @PaoloDelCasale for the device logs and testing).
- Known limitations: `GoogleAsrService` is released 20 seconds after the keyboard is hidden (a fixed grace period in stock Gboard). The advanced dictation stack still reports `NON_ELIGIBLE_NATIVE_SPLIT_UNAVAILABLE` on the 18.3.1 Release base, so Advanced Voice (Rambler) may not be active there.

### 2b. Patch Bundle v1.0.4
- Bumped patch bundle to **v1.0.4** with official bytecode mappings and compatibility for Gboard 18.3.1 Release.
- **Rambler ASR Battery & Service Lifecycle Fix**: Eliminated background `GoogleAsrService` (`com.google.android.tts`) service binder persistence after closing the keyboard by enforcing `immediately_end_dictation_on_keyboard_hidden=true` and unforcing `enable_sticky_mic_background` (credits: @PaoloDelCasale).
- Reverted experimental V2 prompt keyboard overrides to restore the stable in-line writing tools architecture.
- Proofread and style chips (Rephrase, Emojify, Formal, Casual, Concise, Elaborate) render directly above the keyboard layout.
- Eliminated all errors associated with multi-role prompt splitting, empty draft payloads, and prompt bundle network failures.
- Zero "Could not suggest" or "Showing error" states during text transformation requests.

### 3. Gemini "Rambler" Natural Voice Typing
- Unlocked Google's state-of-the-art Rambler dictation model.
- Real-time speech cleanup: automatically filters hesitation, stutters, and filler words ("um", "ah", "like").
- Real-time thought correction: seamlessly corrects self-revisions during dictation.
- Context-aware auto-punctuation and proper noun capitalization.

### 4. Standalone Coexistence & Signature Bypass
- Packaged under independent application ID `com.akshaykadam.pixelboard` with app label `PixelBoard` for safe side-by-side use alongside factory Gboard.
- Signature verification bypassed so patched APKs pass integrity and whitelist checks without root.

---

## Release Assets

| File | Size | Description |
| :--- | :--- | :--- |
| **PixelBoard.apk** | ~82 MB | Ready-to-install signed Stable APK (v18.3.1 release base) |
| **PixelBoard.mpp** | ~1.4 MB | Standalone patch bundle (v1.0.6) for Morphe Manager |

---

## Installation

### Option 1: Direct APK Installation (Recommended)
1. Download `PixelBoard.apk` directly to your Android device.
2. Tap the downloaded APK in your file manager or browser downloads.
3. Allow installation from unknown sources if prompted, and complete installation.
4. Enable PixelBoard in **Settings > System > Languages & input > On-screen keyboard**.

### Option 2: ADB Sideload
```bash
adb install -r PixelBoard.apk
adb shell am force-stop com.akshaykadam.pixelboard
```

### Option 3: Patch via Morphe Manager
Add the custom patch source in Morphe Manager:
```text
https://raw.githubusercontent.com/Akshayykadam/PixelBoard/main/patches-bundle.json
```
Select stock Gboard `v18.3.1.977415014-release-arm64-v8a` (or `v18.0.3`) and apply patches.

---

## Compatibility
- Target Package: `com.google.android.inputmethod.latin`
- Target Versions:
  - `18.3.1.977415014-release-arm64-v8a` (Recommended)
  - `18.3.1.977415014-beta-arm64-v8a`
  - `18.0.3.954559732-release-arm64-v8a`
- Architecture: `arm64-v8a`
- Minimum Android Version: Android 10 (API 29)+
