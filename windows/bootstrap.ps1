# Builds installers for 64-bit AND 32-bit Windows. Downloads its own private Python 3.12 + Inno Setup (nothing to pre-install).
$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$root = $PSScriptRoot; Set-Location $root
$tools = Join-Path $root "_tools"; New-Item -ItemType Directory -Force $tools | Out-Null
function Get-File($url, $out) { if (-not (Test-Path $out)) { Write-Host "Downloading $url"; Invoke-WebRequest $url -OutFile $out -UseBasicParsing } }
$ver = "3.12.8"
$inno = Join-Path $tools "inno"
if (-not (Test-Path "$inno\ISCC.exe")) {
  $f = Join-Path $tools "innosetup.exe"; Get-File "https://jrsoftware.org/download.php/is.exe" $f
  Start-Process $f -Wait -ArgumentList "/VERYSILENT", "/SUPPRESSMSGBOXES", "/NORESTART", "/SP-", "/CURRENTUSER", "/DIR=`"$inno`""
}
foreach ($a in @(@{n = "x64"; f = "python-$ver-amd64.exe" }, @{n = "x86"; f = "python-$ver.exe" })) {
  $dir = Join-Path $tools "py_$($a.n)"
  if (-not (Test-Path "$dir\python.exe")) {
    $f = Join-Path $tools $a.f; Get-File "https://www.python.org/ftp/python/$ver/$($a.f)" $f
    Start-Process $f -Wait -ArgumentList "/quiet", "InstallAllUsers=0", "TargetDir=`"$dir`"", "Include_pip=1", "Include_tcltk=1", "Include_launcher=0", "Include_test=0", "Include_doc=0", "Shortcuts=0", "PrependPath=0", "AssociateFiles=0"
  }
  & "$dir\python.exe" "$root\build.py" --build-only --arch $a.n
  if ($LASTEXITCODE -ne 0) { throw "Build failed for $($a.n)" }
  & "$inno\ISCC.exe" "/DArch=$($a.n)" "$root\installer.iss"
  if ($LASTEXITCODE -ne 0) { throw "Installer failed for $($a.n)" }
}
Write-Host "`nDONE - installers are in the Output folder." -ForegroundColor Yellow
explorer (Join-Path $root "Output")
