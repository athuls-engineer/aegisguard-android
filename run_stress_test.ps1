$ErrorActionPreference = "Stop"
$jdk = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin"
$androidJar = "C:\Users\athul_nuy2ni9\AppData\Local\Android\Sdk\platforms\android-36-ext19\android.jar"

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
