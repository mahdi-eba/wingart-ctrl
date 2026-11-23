# Wingart Ctrl  v1.1.0   |   وینگارت کنترل
Phone -> Windows laptop: trackpad, keyboard, media (YouTube), Xbox gamepad. Wi-Fi + Bluetooth. EN / FA.
Telegram: https://t.me/wingartstudio

## Builds (automatic, on GitHub)
Every time files are pushed, GitHub builds everything. Open the repo -> **Releases**:
* `Wingart Ctrl - latest APK`  -> WingartCtrl.apk (Android)
* `Wingart Ctrl - Windows installer (32 + 64 bit)` -> WingartCtrl-Setup.exe
  ONE file for every Windows PC: it detects 32/64-bit itself, installs the gamepad driver, creates the desktop icon and starts the app.

## Install
1. PC: run WingartCtrl-Setup.exe (UAC -> Yes). The app opens and shows a PIN + Wi-Fi address.
2. Phone: install WingartCtrl.apk, enter the PIN. Wi-Fi: it finds the laptop by itself.
   Bluetooth: pair the laptop in the phone's Bluetooth settings first, then pick it in the app.
3. Updates: install the new APK / Setup over the old ones (same signing key, nothing to uninstall).

## Troubleshooting
* PC log file: %APPDATA%\WingartCtrl.log  (Bluetooth errors are written there; the PC window also shows the reason).
* Phone: the refresh button (top bar / game screen) releases every key and gamepad button on the PC.
