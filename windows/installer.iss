; One installer for BOTH 32-bit and 64-bit Windows: it picks the right build automatically.
[Setup]
AppName=Wingart Ctrl
AppVersion=1.1.0
AppPublisher=Wingart Studio
AppPublisherURL=https://t.me/wingartstudio
DefaultDirName={autopf}\Wingart Ctrl
DisableProgramGroupPage=yes
DisableDirPage=yes
DisableReadyPage=yes
DisableWelcomePage=yes
OutputDir=Output
OutputBaseFilename=WingartCtrl-Setup
SetupIconFile=wingart.ico
UninstallDisplayIcon={app}\wingart.ico
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=admin
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64compatible

[Files]
Source: "dist_x64\WingartCtrl.exe"; DestDir: "{app}"; Flags: ignoreversion skipifsourcedoesntexist; Check: Is64BitInstallMode
Source: "dist_x86\WingartCtrl.exe"; DestDir: "{app}"; Flags: ignoreversion skipifsourcedoesntexist; Check: not Is64BitInstallMode
Source: "wingart.ico"; DestDir: "{app}"
Source: "ViGEmBusSetup_x64.msi"; DestDir: "{tmp}"; Flags: deleteafterinstall skipifsourcedoesntexist; Check: Is64BitInstallMode
Source: "ViGEmBusSetup_x86.msi"; DestDir: "{tmp}"; Flags: deleteafterinstall skipifsourcedoesntexist; Check: not Is64BitInstallMode

[Icons]
Name: "{autoprograms}\Wingart Ctrl"; Filename: "{app}\WingartCtrl.exe"; IconFilename: "{app}\wingart.ico"
Name: "{autodesktop}\Wingart Ctrl"; Filename: "{app}\WingartCtrl.exe"; IconFilename: "{app}\wingart.ico"

[Registry]
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueName: "WingartCtrl"; ValueType: none; Flags: uninsdeletevalue

[Run]
Filename: "msiexec.exe"; Parameters: "/i ""{tmp}\ViGEmBusSetup_x64.msi"" /passive /norestart"; Check: NeedVigem64; Flags: waituntilterminated; StatusMsg: "Installing gamepad driver..."
Filename: "msiexec.exe"; Parameters: "/i ""{tmp}\ViGEmBusSetup_x86.msi"" /passive /norestart"; Check: NeedVigem86; Flags: waituntilterminated; StatusMsg: "Installing gamepad driver..."
Filename: "netsh.exe"; Parameters: "advfirewall firewall delete rule name=""Wingart Ctrl"""; Flags: runhidden
Filename: "netsh.exe"; Parameters: "advfirewall firewall add rule name=""Wingart Ctrl"" dir=in action=allow program=""{app}\WingartCtrl.exe"" enable=yes profile=any"; Flags: runhidden
Filename: "{app}\WingartCtrl.exe"; Description: "Launch Wingart Ctrl"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "taskkill.exe"; Parameters: "/F /IM WingartCtrl.exe"; Flags: runhidden; RunOnceId: "KillApp"
Filename: "netsh.exe"; Parameters: "advfirewall firewall delete rule name=""Wingart Ctrl"""; Flags: runhidden; RunOnceId: "DelFirewall"

[Code]
function VigemMissing: Boolean;
begin
  Result := not RegKeyExists(HKLM, 'SYSTEM\CurrentControlSet\Services\ViGEmBus');
end;
function NeedVigem64: Boolean;
begin
  Result := Is64BitInstallMode and VigemMissing and FileExists(ExpandConstant('{tmp}\ViGEmBusSetup_x64.msi'));
end;
function NeedVigem86: Boolean;
begin
  Result := (not Is64BitInstallMode) and VigemMissing and FileExists(ExpandConstant('{tmp}\ViGEmBusSetup_x86.msi'));
end;
function PrepareToInstall(var NeedsRestart: Boolean): String;
var rc: Integer;
begin
  Exec('taskkill.exe', '/F /IM WingartCtrl.exe', '', SW_HIDE, ewWaitUntilTerminated, rc);
  Result := '';
end;
