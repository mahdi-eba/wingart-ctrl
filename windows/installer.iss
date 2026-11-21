#ifndef Arch
#define Arch "x64"
#endif
[Setup]
AppName=Wingart Ctrl
AppVersion=1.0.0
AppPublisher=Wingart Studio
AppPublisherURL=https://t.me/wingartstudio
DefaultDirName={autopf}\Wingart Ctrl
DisableProgramGroupPage=yes
OutputDir=Output
OutputBaseFilename=WingartCtrl-Setup-{#Arch}
SetupIconFile=wingart.ico
UninstallDisplayIcon={app}\wingart.ico
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=admin
WizardStyle=modern
#if Arch == "x64"
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
#endif

[Tasks]
Name: "desktopicon"; Description: "Desktop shortcut"
Name: "autostart"; Description: "Start with Windows"
Name: "vigem"; Description: "Install gamepad driver (ViGEmBus) - needed for the Game tab"

[Files]
Source: "dist_{#Arch}\WingartCtrl.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "wingart.ico"; DestDir: "{app}"
Source: "ViGEmBusSetup_{#Arch}.msi"; DestDir: "{tmp}"; Flags: deleteafterinstall skipifsourcedoesntexist; Tasks: vigem

[Icons]
Name: "{autoprograms}\Wingart Ctrl"; Filename: "{app}\WingartCtrl.exe"; IconFilename: "{app}\wingart.ico"
Name: "{autodesktop}\Wingart Ctrl"; Filename: "{app}\WingartCtrl.exe"; IconFilename: "{app}\wingart.ico"; Tasks: desktopicon

[Registry]
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "WingartCtrl"; ValueData: """{app}\WingartCtrl.exe"" --tray"; Tasks: autostart; Flags: uninsdeletevalue

[Run]
Filename: "msiexec.exe"; Parameters: "/i ""{tmp}\ViGEmBusSetup_{#Arch}.msi"" /passive /norestart"; Tasks: vigem; Check: HaveMsi; Flags: waituntilterminated; StatusMsg: "Installing gamepad driver..."
Filename: "netsh.exe"; Parameters: "advfirewall firewall add rule name=""Wingart Ctrl"" dir=in action=allow program=""{app}\WingartCtrl.exe"" enable=yes profile=any"; Flags: runhidden
Filename: "{app}\WingartCtrl.exe"; Description: "Launch Wingart Ctrl"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "netsh.exe"; Parameters: "advfirewall firewall delete rule name=""Wingart Ctrl"""; Flags: runhidden

[Code]
function HaveMsi: Boolean;
begin
  Result := FileExists(ExpandConstant('{tmp}\ViGEmBusSetup_{#Arch}.msi'));
end;
