@echo off
setlocal
set "LAB_ROOT=%~dp0.."
set "LAB_JAVA=java"
if defined JAVA_HOME set "LAB_JAVA=%JAVA_HOME%\bin\java.exe"
"%LAB_JAVA%" -version >nul 2>&1
if errorlevel 1 (
  echo Fehler: Java fehlt. JDK 21 bereitstellen und JAVA_HOME oder PATH kontrollieren. 1>&2
  exit /b 2
)
for /f "tokens=3" %%v in ('""%LAB_JAVA%" -version 2^>^&1"') do if not defined LAB_VERSION set "LAB_VERSION=%%~v"
for /f "tokens=1 delims=." %%m in ("%LAB_VERSION%") do set "LAB_MAJOR=%%m"
if not defined LAB_MAJOR (
  echo Fehler: Java-Version nicht erkannt. JDK 21 verwenden. 1>&2
  exit /b 2
)
if %LAB_MAJOR% LSS 21 (
  echo Fehler: Java 21 oder neuer erforderlich; gefunden: %LAB_VERSION%. 1>&2
  exit /b 2
)
set "LAB_MODULE=%~1"
set "LAB_JAR=%LAB_ROOT%\lib\%LAB_MODULE%.jar"
if not exist "%LAB_JAR%" set "LAB_JAR=%LAB_ROOT%\%LAB_MODULE%\target\%LAB_MODULE%.jar"
if not exist "%LAB_JAR%" (
  echo Fehler: Build-Artefakt fehlt. Im Repository mvnw.cmd clean verify starten; Trainingsaufgaben: -Ptraining. 1>&2
  exit /b 2
)
rem Arguments are passed by the public entry points; SHIFT does not change %%*.
"%LAB_JAVA%" "-Dlab.home=%LAB_ROOT%" -jar "%LAB_JAR%" %LAB_ARGS%
set "LAB_EXIT=%errorlevel%"
endlocal & exit /b %LAB_EXIT%
