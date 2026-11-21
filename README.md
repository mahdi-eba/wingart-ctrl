# Wingart Ctrl  v1.0.0   |   وینگارت کنترل
Phone -> Windows laptop: trackpad, keyboard, media (YouTube), Xbox gamepad. Wi-Fi + Bluetooth. EN / FA.
Telegram: https://t.me/wingartstudio

## Get the Android APK (no Android Studio needed)
1. Create a free account at github.com -> New repository (name: wingart-ctrl).
2. "uploading an existing file" -> drag in EVERYTHING inside this folder (including the hidden .github folder). Commit.
3. Actions tab -> "Build Android APK" -> wait ~4-6 min (or Run workflow).
4. Easiest download: repo main page -> Releases (right side) -> "Wingart Ctrl - latest APK" -> WingartCtrl.apk.
   Or: open the finished run -> Artifacts -> WingartCtrl-Android-APK (zip) -> unzip.
5. On the phone: open the APK -> allow "Install unknown apps" for your browser/file manager -> Install.

## Windows installer
Actions tab -> "Build Wingart Ctrl (Windows)" -> Artifacts: x64 (most PCs) / x86 (old 32-bit) -> Setup.exe.
Or locally: double-click windows\MAKE_INSTALLERS.bat.

## Use
1. Run Wingart Ctrl on the laptop (icon in tray). It shows a PIN and your Wi-Fi address.
2. Phone: enter the PIN. Wi-Fi: "Find my laptop". Bluetooth: pair in system settings first, then tap the laptop.
