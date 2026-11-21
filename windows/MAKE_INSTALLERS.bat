@echo off
title Wingart Ctrl - Make installers
echo This downloads its own Python + Inno Setup (needs internet, ~5-10 min). Your own Python is not used.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bootstrap.ps1"
pause
