# build.ps1 — downloads Gradle if needed, builds SkyUtils.jar, copies to Downloads
# Run from the EmpireMod folder: powershell -ExecutionPolicy Bypass -File build.ps1

$ErrorActionPreference = "Stop"
$modDir   = $PSScriptRoot
$distDir  = "$modDir\build\libs"
$download = "$env:TEMP\gradle"

# ── 1. Verify Java 21+ ────────────────────────────────────────────────────────
try {
    $javaVer = (java -version 2>&1 | Select-String 'version "(\d+)' |
        ForEach-Object { $_.Matches.Groups[1].Value }) -as [int]
    if ($javaVer -lt 21) { throw "Java $javaVer found but 21+ required." }
    Write-Host "Java $javaVer found."
} catch {
    Write-Host ""
    Write-Host "Java 21 not found. Install it from:"
    Write-Host "  https://adoptium.net/temurin/releases/?version=21"
    Write-Host "Then re-run this script."
    exit 1
}

# ── 2. Get Gradle (download if gradlew not present) ───────────────────────────
if (-not (Test-Path "$modDir\gradlew.bat")) {
    Write-Host "Downloading Gradle 8.8..."
    $gradleZip = "$download\gradle.zip"
    $gradleDir = "$download\gradle-8.8"
    New-Item -ItemType Directory -Force -Path $download | Out-Null
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-8.8-bin.zip" `
        -OutFile $gradleZip -UseBasicParsing
    Expand-Archive -Path $gradleZip -DestinationPath $download -Force
    $gradleExe = "$gradleDir\bin\gradle.bat"

    Write-Host "Generating Gradle wrapper..."
    Push-Location $modDir
    & $gradleExe wrapper --gradle-version 8.8 | Out-Null
    Pop-Location
}

# ── 3. Build ──────────────────────────────────────────────────────────────────
Write-Host "Building SkyUtils mod (first run downloads Minecraft — may take a few minutes)..."
Push-Location $modDir
& ".\gradlew.bat" build --no-daemon
Pop-Location

if ($LASTEXITCODE -ne 0) { Write-Host "Build failed."; exit 1 }

# ── 4. Copy JAR to Downloads ──────────────────────────────────────────────────
$jar = Get-ChildItem "$distDir\SkyUtils-*.jar" | Where-Object { $_.Name -notmatch "sources" } | Select-Object -First 1
if ($jar) {
    $dest = "$env:USERPROFILE\Downloads\SkyUtils.jar"
    Copy-Item $jar.FullName $dest -Force
    Write-Host ""
    Write-Host "Done! JAR copied to: $dest"
    Write-Host "Drop it in .minecraft\mods\ and place empire client.exe in .minecraft\skyutils\"
} else {
    Write-Host "Build succeeded but JAR not found in $distDir"
}
