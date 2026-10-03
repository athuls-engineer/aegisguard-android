# AegisGuard Autonomous Build & APK Assembly Script
$ErrorActionPreference = "Stop"

$ProjectDir = "C:\Users\athul_nuy2ni9\.gemini\antigravity\scratch\aegisguard-android"
$JdkBin = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin"
$SdkTools = "C:\Users\athul_nuy2ni9\AppData\Local\Android\Sdk\build-tools\36.1.0"
$AndroidJar = "C:\Users\athul_nuy2ni9\AppData\Local\Android\Sdk\platforms\android-36-ext19\android.jar"

$Aapt2 = "$SdkTools\aapt2.exe"
$D8 = "$SdkTools\d8.bat"
$Zipalign = "$SdkTools\zipalign.exe"
$ApkSigner = "$SdkTools\apksigner.bat"
$Javac = "$JdkBin\javac.exe"
$Jar = "$JdkBin\jar.exe"
$Keytool = "$JdkBin\keytool.exe"

$BuildDir = "$ProjectDir\build"
$CompiledResDir = "$BuildDir\compiled_res"
$GenDir = "$BuildDir\gen"
$ClassesDir = "$BuildDir\classes"
$OutDir = "$BuildDir\outputs"

# Clean build workspace
if (Test-Path $BuildDir) { Remove-Item -Recurse -Force $BuildDir }
New-Item -ItemType Directory -Path $CompiledResDir, $GenDir, $ClassesDir, $OutDir -Force | Out-Null

Write-Host "=== Step 1: Compiling Resources with AAPT2 ===" -ForegroundColor Cyan
& $Aapt2 compile --dir "$ProjectDir\src\main\res" -o "$CompiledResDir\resources.zip"
if ($LASTEXITCODE -ne 0) { throw "AAPT2 compile failed" }

Write-Host "=== Step 2: Linking Resources with AAPT2 ===" -ForegroundColor Cyan
$UnsignedApk = "$BuildDir\unaligned.apk"
& $Aapt2 link -I $AndroidJar `
    --manifest "$ProjectDir\src\main\AndroidManifest.xml" `
    --java $GenDir `
    -o $UnsignedApk `
    --auto-add-overlay `
    "$CompiledResDir\resources.zip"
if ($LASTEXITCODE -ne 0) { throw "AAPT2 link failed" }

Write-Host "=== Step 3: Compiling Java Sources with javac 17 ===" -ForegroundColor Cyan
$JavaFiles = Get-ChildItem -Path "$ProjectDir\src\main\java", $GenDir -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& $Javac -encoding UTF-8 -source 1.8 -target 1.8 -cp $AndroidJar -d $ClassesDir $JavaFiles
if ($LASTEXITCODE -ne 0) { throw "Javac compilation failed" }

Write-Host "=== Step 4: DEXing Bytecode with d8 ===" -ForegroundColor Cyan
$ClassesJar = "$BuildDir\classes.jar"
& $Jar cf "$ClassesJar" -C "$ClassesDir" .
if ($LASTEXITCODE -ne 0) { throw "Jar creation failed" }

$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:PATH = "$JdkBin;$env:PATH"
& cmd.exe /c "$D8 --lib `"$AndroidJar`" --output `"$BuildDir`" `"$ClassesJar`""
if ($LASTEXITCODE -ne 0) { throw "D8 dexing failed" }

Write-Host "=== Step 5: Adding classes.dex into APK ===" -ForegroundColor Cyan
Push-Location $BuildDir
& $Jar uf "$UnsignedApk" classes.dex
Pop-Location

Write-Host "=== Step 6: 4-Byte ZipAligning APK ===" -ForegroundColor Cyan
$AlignedApk = "$OutDir\AegisGuard-v2.8.7-aligned.apk"
& $Zipalign -f -v 4 $UnsignedApk $AlignedApk
if ($LASTEXITCODE -ne 0) { throw "ZipAlign failed" }

Write-Host "=== Step 7: Verifying Permanent Production Keystore ===" -ForegroundColor Cyan
$KeystoreDir = "$ProjectDir\keystore"
$Keystore = "$KeystoreDir\aegisguard-release.keystore"
if (!(Test-Path $Keystore)) {
    if (!(Test-Path $KeystoreDir)) { New-Item -ItemType Directory -Path $KeystoreDir -Force | Out-Null }
    & $Keytool -genkeypair -v `
        -keystore $Keystore `
        -alias aegisguard `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass aegisguardpass -keypass aegisguardpass `
        -dname "CN=AegisGuard, OU=Security, O=Aegis, L=MountainView, ST=CA, C=US"
}

Write-Host "=== Step 8: Cryptographically Signing APK (v1, v2, v3 schemes) ===" -ForegroundColor Cyan
$FinalApk = "$OutDir\AegisGuard-v2.8.7-release.apk"
& cmd.exe /c "$ApkSigner sign --ks `"$Keystore`" --ks-pass pass:aegisguardpass --key-pass pass:aegisguardpass --ks-key-alias aegisguard --out `"$FinalApk`" `"$AlignedApk`""
if ($LASTEXITCODE -ne 0) { throw "ApkSigner failed" }

Write-Host "=== Step 9: Verifying APK Signatures ===" -ForegroundColor Cyan
& cmd.exe /c "$ApkSigner verify --verbose `"$FinalApk`""
if ($LASTEXITCODE -ne 0) { throw "Apk verification failed" }

# Copy to user's Downloads, Project root (HTTP Server), and brain artifact directory
$DownloadsApk = "C:\Users\athul_nuy2ni9\Downloads\AegisGuard-v2.8.7-release.apk"
Copy-Item -Force $FinalApk $DownloadsApk

# Copy to HTTP Server root ($ProjectDir)
Copy-Item -Force $FinalApk "$ProjectDir\AegisGuard-v2.8.7-release.apk"

$BrainDir = "C:\Users\athul_nuy2ni9\.gemini\antigravity\brain\33d1dec8-b1ae-4695-9d9a-e8d94b5ec2a3"
if (Test-Path $BrainDir) {
    Copy-Item -Force $FinalApk "$BrainDir\AegisGuard-v2.8.7-release.apk"
}

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "SUCCESS! Production APK generated at: $FinalApk" -ForegroundColor Green
Write-Host "Direct Phone Download ready at: $DownloadsApk" -ForegroundColor Green
$apkItem = Get-Item $FinalApk
Write-Host "APK File Size: $([math]::Round($apkItem.Length / 1KB, 2)) KB" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
