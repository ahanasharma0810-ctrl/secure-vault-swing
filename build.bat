@echo off
REM Builds secure-vault.jar without Maven (needs JDK 11 or newer).
setlocal
cd /d "%~dp0"

if exist out rmdir /s /q out
mkdir out\classes

echo Compiling...
javac --release 11 -encoding UTF-8 -sourcepath src\main\java -d out\classes src\main\java\com\securevault\Main.java
if errorlevel 1 goto :error

(
echo Main-Class: com.securevault.Main
echo Implementation-Title: Secure Vault
echo Implementation-Version: 1.0.0
) > out\manifest.txt
jar cfm secure-vault.jar out\manifest.txt -C out\classes .
if errorlevel 1 goto :error

echo Done: secure-vault.jar
echo Run it with: java -jar secure-vault.jar
exit /b 0

:error
echo Build failed. Make sure the JDK (not just the JRE) is installed and on PATH.
exit /b 1
