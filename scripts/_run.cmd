@echo off
setlocal
set "LAB_ROOT=%~dp0.."
set "LAB_JAVA=java"
if defined JAVA_HOME set "LAB_JAVA=%JAVA_HOME%\bin\java.exe"
rem Capture stderr directly; nested FOR /F command quoting loses the redirection on Windows.
set "LAB_VERSION_FILE=%TEMP%\artemis-java-version-%RANDOM%-%RANDOM%.txt"
"%LAB_JAVA%" -version >"%LAB_VERSION_FILE%" 2>&1
if errorlevel 1 (
  del /q "%LAB_VERSION_FILE%" >nul 2>&1
  echo Error: Java is missing. Supply JDK 21 and check JAVA_HOME or PATH. 1>&2
  exit /b 2
)
for /f "usebackq tokens=2,3" %%a in ("%LAB_VERSION_FILE%") do if "%%a"=="version" set "LAB_VERSION=%%~b"
del /q "%LAB_VERSION_FILE%" >nul 2>&1
for /f "tokens=1 delims=." %%m in ("%LAB_VERSION%") do set "LAB_MAJOR=%%m"
if not defined LAB_MAJOR (
  echo Error: Java version not recognized. Use JDK 21. 1>&2
  exit /b 2
)
if %LAB_MAJOR% LSS 21 (
  echo Error: Java 21 or newer required; found: %LAB_VERSION%. 1>&2
  exit /b 2
)
set "LAB_MODULE=%~1"
if "%LAB_MODULE%"=="doctor-build" goto buildDoctor
set "LAB_JAR=%LAB_ROOT%\lib\%LAB_MODULE%.jar"
if not exist "%LAB_JAR%" set "LAB_JAR=%LAB_ROOT%\%LAB_MODULE%\target\%LAB_MODULE%.jar"
if not exist "%LAB_JAR%" (
  echo Error: Build artifact is missing. Run mvnw.cmd clean verify in the repository; training exercises: -Ptraining. 1>&2
  exit /b 2
)
rem Arguments are passed by the public entry points; SHIFT does not change %%*.
rem Use UTF-8 for the Java process, then restore this console's previous code page.
for /f "tokens=2 delims=:" %%c in ('chcp') do set "LAB_CODEPAGE=%%c"
chcp 65001 >nul
"%LAB_JAVA%" "-Dlab.home=%LAB_ROOT%" -jar "%LAB_JAR%" %LAB_ARGS%
set "LAB_EXIT=%errorlevel%"
if defined LAB_CODEPAGE chcp %LAB_CODEPAGE% >nul
endlocal & exit /b %LAB_EXIT%

:buildDoctor
set "LAB_JAVAC=javac.exe"
if defined JAVA_HOME set "LAB_JAVAC=%JAVA_HOME%\bin\javac.exe"
"%LAB_JAVAC%" -version >nul 2>&1
if errorlevel 1 (
  echo Error: A full JDK 21 is required, not a JRE. Check JAVA_HOME/bin/javac. 1>&2
  exit /b 2
)
"%LAB_JAVA%" "-Dlab.home=%LAB_ROOT%" "%LAB_ROOT%\scripts\BuildDoctor.java"
exit /b %errorlevel%
