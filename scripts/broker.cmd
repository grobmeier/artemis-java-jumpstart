@echo off
setlocal
set "LAB_ARGS=%*"
call "%~dp0_run.cmd" broker
exit /b %errorlevel%
