$ErrorActionPreference = "Stop"

$jdk = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    "$env:JAVA_HOME\bin"
} elseif (Test-Path "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot\bin\javac.exe") {
    "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot\bin"
} elseif (Test-Path "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\javac.exe") {
    "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin"
} else {
    $javacCmd = Get-Command javac -ErrorAction SilentlyContinue
    if ($javacCmd) { Split-Path $javacCmd.Source } else { "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot\bin" }
}

$androidJar = if ($env:ANDROID_HOME -and (Test-Path "$env:ANDROID_HOME\platforms\android-36-ext19\android.jar")) {
    "$env:ANDROID_HOME\platforms\android-36-ext19\android.jar"
} elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk\platforms\android-36-ext19\android.jar") {
    "$env:LOCALAPPDATA\Android\Sdk\platforms\android-36-ext19\android.jar"
} else {
    $platforms = Get-ChildItem -Path "$env:LOCALAPPDATA\Android\Sdk\platforms" -Directory -Filter "android-*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    "$($platforms.FullName)\android.jar"
}

Write-Host "Using JDK: $jdk" -ForegroundColor Cyan
Write-Host "Using Android JAR: $androidJar" -ForegroundColor Cyan

Write-Host "=== Compiling Stress Test Suite with javac 17 ===" -ForegroundColor Cyan
New-Item -ItemType Directory -Path 'build\test_classes' -Force | Out-Null
& "$jdk\javac.exe" -encoding UTF-8 -cp "$androidJar" -d 'build\test_classes' `
    src\main\java\org\aegisguard\android\DnsPacketParser.java `
    src\main\java\org\aegisguard\android\FilterEngine.java `
    src\test\java\org\aegisguard\android\AegisGuardStressTest.java
if ($LASTEXITCODE -ne 0) { throw "Compilation failed" }

Write-Host "=== Launching AegisGuard 100,000 Unique Scenario Stress Test ===" -ForegroundColor Cyan
& "$jdk\java.exe" -cp "build\test_classes;$androidJar" org.aegisguard.android.AegisGuardStressTest
if ($LASTEXITCODE -ne 0) { throw "Stress test failed" }
