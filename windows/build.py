"""Builds WingartCtrl.exe for the architecture of the Python running this script (x64 or x86)."""
import importlib.util, os, shutil, subprocess, sys, tempfile, traceback

HERE = os.path.dirname(os.path.abspath(__file__)); os.chdir(HERE)
BUILD_ONLY = "--build-only" in sys.argv
ARCH = sys.argv[sys.argv.index("--arch") + 1] if "--arch" in sys.argv else ("x64" if sys.maxsize > 2**32 else "x86")
os.environ["VGAMEPAD_SKIP_VIGEMBUS_INSTALL"] = "true"   # the installer ships the driver; don't pop it up while building

def run(cmd, optional=False):
    print("\n>", " ".join(cmd), flush=True)
    ok = subprocess.call(cmd) == 0
    if not ok and not optional: raise SystemExit("FAILED: " + " ".join(cmd))
    return ok
def pip(*pkgs, optional=False): return run([sys.executable, "-m", "pip", "install", "--upgrade", *pkgs], optional)
def spec(m): return importlib.util.find_spec(m)

def main():
    print("Python", sys.version.split()[0], ARCH)
    if sys.platform != "win32": raise SystemExit("Run this on Windows.")
    if subprocess.call([sys.executable, "-m", "pip", "--version"]) != 0: run([sys.executable, "-m", "ensurepip", "--upgrade"])
    pip("pystray", "pillow", "pyinstaller")
    pip("vgamepad", optional=True)
    pip("winrt-runtime", "winrt-Windows.Foundation", "winrt-Windows.Foundation.Collections", "winrt-Windows.Storage.Streams",
        "winrt-Windows.Networking.Sockets", "winrt-Windows.Devices.Bluetooth", "winrt-Windows.Devices.Bluetooth.Rfcomm", optional=True)
    cmd = [sys.executable, "-m", "PyInstaller", "--noconsole", "--onefile", "--clean", "--noconfirm", "--name", "WingartCtrl",
           "--distpath", f"dist_{ARCH}", "--workpath", f"build_{ARCH}", "--icon", "wingart.ico", "--add-data", "wingart.ico;."]
    if spec("vgamepad"): cmd += ["--collect-all", "vgamepad"]
    else: print("NOTE: vgamepad missing -> Game tab disabled")
    if spec("winrt"): cmd += ["--collect-submodules", "winrt"]
    else: print("NOTE: winrt missing -> Bluetooth disabled (Wi-Fi still works)")
    run(cmd + ["wingart_ctrl.py"])
    built = os.path.join(HERE, f"dist_{ARCH}", "WingartCtrl.exe")
    if not os.path.exists(built): raise SystemExit(f"{built} was not created")
    vg = spec("vgamepad"); msi = None
    if vg and vg.submodule_search_locations:
        msi = os.path.join(list(vg.submodule_search_locations)[0], "win", "vigem", "install", ARCH, f"ViGEmBusSetup_{ARCH}.msi")
        if os.path.exists(msi): shutil.copy2(msi, os.path.join(HERE, f"ViGEmBusSetup_{ARCH}.msi"))
    print(f"\nBuilt: {built}")
    if BUILD_ONLY: return
    target = os.path.join(os.environ["LOCALAPPDATA"], "Programs", "Wingart Ctrl"); os.makedirs(target, exist_ok=True)
    exe = os.path.join(target, "WingartCtrl.exe"); ico = os.path.join(target, "wingart.ico")
    shutil.copy2(built, exe); shutil.copy2("wingart.ico", ico)
    ps = f'''$exe="{exe}"; $ico="{ico}"
$d=[Environment]::GetFolderPath('Desktop'); $m=Join-Path $env:APPDATA 'Microsoft\\Windows\\Start Menu\\Programs'
foreach($p in @($d,$m)){{ $s=(New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $p 'Wingart Ctrl.lnk'))
$s.TargetPath=$exe; $s.IconLocation=$ico; $s.WorkingDirectory=Split-Path $exe; $s.Save() }}'''
    f = os.path.join(tempfile.gettempdir(), "wingart_shortcut.ps1"); open(f, "w").write(ps)
    run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", f], optional=True)
    if msi and os.path.exists(msi): run(["msiexec", "/i", msi, "/passive", "/norestart"], optional=True)
    os.startfile(exe)

if __name__ == "__main__":
    try: main()
    except SystemExit as e: print("\n" + str(e)); sys.exit(1)
    except Exception: traceback.print_exc(); sys.exit(1)
