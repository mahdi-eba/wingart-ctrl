@echo off
cd /d %~dp0
python -m pip install pystray pillow vgamepad
python -m pip install winrt-runtime winrt-Windows.Foundation winrt-Windows.Foundation.Collections winrt-Windows.Storage.Streams winrt-Windows.Networking.Sockets winrt-Windows.Devices.Bluetooth winrt-Windows.Devices.Bluetooth.Rfcomm winrt-Windows.Networking winrt-Windows.Devices.Enumeration winrt-Windows.Storage
python wingart_ctrl.py
pause
