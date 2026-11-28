@echo off
rem Publishes an Inspector's Gadget update as a GitHub release. Phones get it from Settings, Check for updates.
rem
rem   publish-update.bat              bump the version, build and sign the APK, commit, push, release
rem   publish-update.bat app.apk      release an APK that is already built (version read from AndroidManifest.xml)
rem
rem Needs: Git, GitHub CLI (gh auth login), and for building: JDK 17+ and the Android SDK
rem (build-tools and a platforms\android-3x folder, as installed by Android Studio).
rem Signing key: demo.keystore next to this file, or set KEYSTORE and KEYSTORE_PASS.

setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
set "REPO=jip126/inspectors-gadget"
set "BRANCH=main"
if not defined KEYSTORE set "KEYSTORE=%~dp0demo.keystore"
if not defined KEYSTORE_PASS set "KEYSTORE_PASS=android"

echo.
echo  Inspector's Gadget  ^|  Publish update
echo  ----------------------------------------
echo.

where git >nul 2>&1 || (echo Git is not installed or not on PATH. & goto fail)
where gh >nul 2>&1 || (echo GitHub CLI is not installed. Get it from https://cli.github.com & goto fail)
gh auth status >nul 2>&1 || (echo GitHub CLI is not signed in. Run: gh auth login & goto fail)
if not exist AndroidManifest.xml (echo Run this from the inspectors-gadget folder. & goto fail)

if not exist build mkdir build

rem ---------------------------------------------------------------- publish an existing APK
if not "%~1"=="" (
    set "SRC_APK=%~f1"
    if not exist "!SRC_APK!" (echo File not found: !SRC_APK! & goto fail)
    call :readversion || goto fail
    echo APK:      !SRC_APK!
    echo Version:  !NAME! ^(code !CODE!^)
    call :checktag v!NAME! || goto fail
    call :asknotes
    set /p "OK=Publish this APK as v!NAME!? [Y/n] "
    if /i "!OK!"=="n" goto cancelled
    set "APK=build\InspectorsGadget-!NAME!-!CODE!.apk"
    copy /y "!SRC_APK!" "!APK!" >nul || goto fail
    call :release || goto fail
    goto done
)

rem ---------------------------------------------------------------- build and publish
for /f "delims=" %%s in ('git status --porcelain') do (
    echo You have uncommitted changes. Commit or stash them first.
    git status --short
    goto fail
)
for /f "delims=" %%b in ('git rev-parse --abbrev-ref HEAD') do set "CUR=%%b"
if /i not "!CUR!"=="%BRANCH%" (
    echo You are on branch !CUR!, not %BRANCH%.
    set /p "OK=Publish from !CUR! anyway? [y/N] "
    if /i not "!OK!"=="y" goto cancelled
    set "BRANCH=!CUR!"
)
echo Pulling latest changes...
git pull --ff-only || (echo git pull failed. & goto fail)

call :findtools || goto fail
call :readversion || goto fail

set /a NEWCODE=CODE+1
for /f "delims=" %%v in ('powershell -NoProfile -Command "$p=[regex]::Match('!NAME!','\d+(\.\d+)*').Value; if($p -eq ''){$p='0'}; $p=$p.Split('.'); $p[$p.Length-1]=[string]([int]$p[$p.Length-1]+1); $p -join '.'"') do set "SUGGEST=%%v"
echo Current version: !NAME! ^(code !CODE!^)
set "NEWNAME="
set /p "NEWNAME=New version name [!SUGGEST!]: "
if "!NEWNAME!"=="" set "NEWNAME=!SUGGEST!"
call :checktag v!NEWNAME! || goto fail
call :asknotes

echo.
echo Setting version !NEWNAME! ^(code !NEWCODE!^)...
powershell -NoProfile -Command "$f=(Resolve-Path 'AndroidManifest.xml').Path; $m=[IO.File]::ReadAllText($f); $q=[char]34; $m=$m -replace 'versionCode=\x22\d+\x22',('versionCode='+$q+'!NEWCODE!'+$q); $m=$m -replace 'versionName=\x22.*?\x22',('versionName='+$q+'!NEWNAME!'+$q); [IO.File]::WriteAllText($f,$m)" || goto fail
set "NAME=!NEWNAME!"
set "CODE=!NEWCODE!"

call :build || (git checkout -- AndroidManifest.xml & goto fail)
set "APK=build\InspectorsGadget-!NAME!-!CODE!.apk"
copy /y build\InspectorsGadget.apk "!APK!" >nul || goto fail

echo Committing version bump...
git add AndroidManifest.xml || goto fail
git commit -q -m "Release v!NAME!" || goto fail
git push -q origin HEAD:!BRANCH! || (echo git push failed. The version bump is committed locally. & goto fail)

call :release || goto fail
goto done

rem ================================================================ subroutines

:readversion
set "CODE=" & set "NAME="
for /f "tokens=1,2" %%a in ('powershell -NoProfile -Command "$m=[IO.File]::ReadAllText((Resolve-Path 'AndroidManifest.xml').Path); [regex]::Match($m,'versionCode=\x22(\d+)\x22').Groups[1].Value + ' ' + [regex]::Match($m,'versionName=\x22([^\x22]*)\x22').Groups[1].Value"') do (
    set "CODE=%%a"
    set "NAME=%%b"
)
if "!CODE!"=="" (echo Couldn't read versionCode from AndroidManifest.xml. & exit /b 1)
if "!NAME!"=="" (echo Couldn't read versionName from AndroidManifest.xml. & exit /b 1)
exit /b 0

:checktag
gh release view %1 --repo %REPO% >nul 2>&1 && (echo Release %1 already exists on GitHub. & exit /b 1)
exit /b 0

:asknotes
set "NOTES="
set /p "NOTES=Release notes (one line, optional): "
> build\notes.txt (echo(!NOTES!)
exit /b 0

:release
echo Creating GitHub release v!NAME!...
gh release create v!NAME! "!APK!" --repo %REPO% --target !BRANCH! --title "Inspector's Gadget !NAME!" --notes-file build\notes.txt || (echo gh release create failed. & exit /b 1)
exit /b 0

:findtools
set "SDK="
if defined ANDROID_HOME if exist "%ANDROID_HOME%\build-tools" set "SDK=%ANDROID_HOME%"
if not defined SDK if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\build-tools" set "SDK=%ANDROID_SDK_ROOT%"
if not defined SDK if exist "%LOCALAPPDATA%\Android\Sdk\build-tools" set "SDK=%LOCALAPPDATA%\Android\Sdk"
if not defined SDK (echo Android SDK not found. Install Android Studio or set ANDROID_HOME. & exit /b 1)
set "BT="
for /f "delims=" %%d in ('dir /b /ad /o-n "%SDK%\build-tools" 2^>nul') do if not defined BT if exist "%SDK%\build-tools\%%d\aapt.exe" if exist "%SDK%\build-tools\%%d\d8.bat" set "BT=%SDK%\build-tools\%%d"
if not defined BT (echo No Android build-tools with aapt.exe and d8.bat in %SDK%\build-tools. & exit /b 1)
set "PLAT="
for /f "delims=" %%d in ('dir /b /ad /o-n "%SDK%\platforms\android-3*" 2^>nul') do if not defined PLAT if exist "%SDK%\platforms\%%d\android.jar" set "PLAT=%SDK%\platforms\%%d\android.jar"
if not defined PLAT (echo No Android platform android-30 or newer in %SDK%\platforms. & exit /b 1)
set "JBIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JBIN=%JAVA_HOME%\bin"
if not defined JBIN if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\javac.exe" set "JBIN=%ProgramFiles%\Android\Android Studio\jbr\bin"
if not defined JBIN for /f "delims=" %%j in ('where javac 2^>nul') do if not defined JBIN set "JBIN=%%~dpj"
if not defined JBIN (echo JDK not found. Install JDK 17+ or set JAVA_HOME. & exit /b 1)
if "!JBIN:~-1!"=="\" set "JBIN=!JBIN:~0,-1!"
if not exist "!JBIN!\jar.exe" (echo jar.exe not found in !JBIN!. Set JAVA_HOME to a full JDK. & exit /b 1)
if not exist "%KEYSTORE%" (echo Signing key not found: %KEYSTORE% & echo Copy demo.keystore into this folder so updates install over the current app. & exit /b 1)
echo Build tools: !BT!
echo Platform:    !PLAT!
echo JDK:         !JBIN!
exit /b 0

:build
echo Building...
if exist build\gen rmdir /s /q build\gen
if exist build\classes rmdir /s /q build\classes
if exist build\dex rmdir /s /q build\dex
mkdir build\gen build\classes build\dex
del /q build\app.unsigned.apk build\app.aligned.apk build\InspectorsGadget.apk build\classes.jar 2>nul

"!BT!\aapt.exe" package -f -m -J build\gen -M AndroidManifest.xml -S res -I "!PLAT!" --min-sdk-version 26 --target-sdk-version 34 -0 arsc -F build\app.unsigned.apk || (echo aapt failed. & exit /b 1)

powershell -NoProfile -Command "Get-ChildItem -Recurse -Filter *.java -Path src,build\gen | ForEach-Object { [char]34 + ($_.FullName -replace '\\','/') + [char]34 } | Set-Content -Encoding ASCII build\sources.txt" || exit /b 1
"!JBIN!\javac.exe" -nowarn -encoding UTF-8 -source 8 -target 8 -bootclasspath "!PLAT!" -Xlint:-options -d build\classes @build\sources.txt || (echo javac failed. & exit /b 1)
"!JBIN!\jar.exe" cf build\classes.jar -C build\classes . || exit /b 1

call "!BT!\d8.bat" --release --min-api 26 --lib "!PLAT!" --output build\dex build\classes.jar || (echo d8 failed. & exit /b 1)
pushd build\dex
"!BT!\aapt.exe" add ..\app.unsigned.apk classes.dex >nul
set "ERR=!errorlevel!"
popd
if not "!ERR!"=="0" (echo Adding classes.dex failed. & exit /b 1)

"!BT!\zipalign.exe" -f -p 4 build\app.unsigned.apk build\app.aligned.apk || (echo zipalign failed. & exit /b 1)
call "!BT!\apksigner.bat" sign --ks "%KEYSTORE%" --ks-pass pass:%KEYSTORE_PASS% --key-pass pass:%KEYSTORE_PASS% --out build\InspectorsGadget.apk build\app.aligned.apk || (echo apksigner failed. & exit /b 1)
call "!BT!\apksigner.bat" verify build\InspectorsGadget.apk || (echo Signature check failed. & exit /b 1)
echo Built build\InspectorsGadget.apk
exit /b 0

rem ================================================================ endings

:done
echo.
echo Published v!NAME! ^(code !CODE!^): https://github.com/%REPO%/releases/tag/v!NAME!
echo Phones get it from Settings, Check for updates.
echo.
pause
exit /b 0

:cancelled
echo Cancelled.
pause
exit /b 1

:fail
echo.
echo Publish failed.
pause
exit /b 1
