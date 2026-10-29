@echo off
setlocal
pushd "%~dp0.."
call mvnw.cmd clean verify %*
set "LAB_EXIT=%errorlevel%"
popd
endlocal & exit /b %LAB_EXIT%
