# AegisGuard Autonomous Build & APK Assembly Script
$ErrorActionPreference = "Stop"

$ProjectDir = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }

# Resolve JDK
$JdkBin = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    "$env:JAVA_HOME\bin"
} elseif (Test-Path "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\javac.exe") {
    "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin"
} else {
    $javacCmd = Get-Command javac -ErrorAction SilentlyContinue
    if ($javacCmd) { Split-Path $javacCmd.Source } else { "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin" }
}

# Resolve Android SDK
$AndroidSdk = if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
    $env:ANDROID_HOME
} elseif ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) {
    $env:ANDROID_SDK_ROOT
} else {
    "$env:LOCALAPPDATA\Android\Sdk"
}

# Dynamically find latest build-tools and android.jar
$BuildToolsDir = Get-ChildItem -Path "$AndroidSdk\build-tools" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
$SdkTools = if ($BuildToolsDir) { $BuildToolsDir.FullName } else { "$AndroidSdk\build-tools\36.1.0" }

$PlatformDir = Get-ChildItem -Path "$AndroidSdk\platforms" -Directory -Filter "android-*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
$AndroidJar = if ($PlatformDir -and (Test-Path "$($PlatformDir.FullName)\android.jar")) {
    "$($PlatformDir.FullName)\android.jar"
} else {
    "$AndroidSdk\platforms\android-36-ext19\android.jar"
}

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

Write-Host "=== Step 4: Compiling Bytecode to DEX with D8 ===" -ForegroundColor Cyan
$ClassFiles = Get-ChildItem -Path $ClassesDir -Recurse -Filter "*.class" | Select-Object -ExpandProperty FullName
& cmd.exe /c "`"$D8`" --release --min-api 24 --output `"$BuildDir`" $($ClassFiles -join ' ')"
if ($LASTEXITCODE -ne 0) { throw "D8 compilation failed" }

Write-Host "=== Step 5: Packaging classes.dex into APK ===" -ForegroundColor Cyan
Push-Location $BuildDir
try {
    & $Jar uf "$UnsignedApk" "classes.dex"
    if ($LASTEXITCODE -ne 0) { throw "Jar package failed" }
} finally {
    Pop-Location
}

Write-Host "=== Step 6: 4-Byte ZipAligning APK ===" -ForegroundColor Cyan
$AlignedApk = "$BuildDir\aligned.apk"
& $Zipalign -p -f -v 4 "$UnsignedApk" "$AlignedApk" | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Zipalign failed" }

Write-Host "=== Step 7: Signing APK with V2/V3 signature scheme ===" -ForegroundColor Cyan
$Keystore = "$BuildDir\debug.keystore"
& $Keytool -genkeypair -v -keystore $Keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Keytool failed" }

$FinalApk = "$OutDir\AegisGuard-release.apk"
& cmd.exe /c "`"$ApkSigner`" sign --ks `"$Keystore`" --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android --out `"$FinalApk`" `"$AlignedApk`""
if ($LASTEXITCODE -ne 0) { throw "ApkSigner failed" }

Write-Host "Build Succeeded: $FinalApk" -ForegroundColor Green
