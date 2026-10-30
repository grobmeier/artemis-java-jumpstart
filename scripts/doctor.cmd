@echo off
if /i "%~1"=="build" goto build
call "%~dp0lab.cmd" doctor %*
exit /b %errorlevel%

:build
call "%~dp0_run.cmd" doctor-build
exit /b %errorlevel%
