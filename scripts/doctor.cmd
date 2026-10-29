@echo off
call "%~dp0lab.cmd" doctor %*
exit /b %errorlevel%
