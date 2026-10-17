@echo off
title apigrab
setlocal enabledelayedexpansion
cls

:: 启动主类
set APP_MAINCLASS=com.gitee.apigrab.admin.ApigrabApplication

:: 应用根目录
set APP_HOME=%~dp0
set APP_HOME=%APP_HOME%\..\
cd %APP_HOME%
set APP_HOME=%cd%

set APP_LIB_PATH=%APP_HOME%\lib
set APP_CONF_PATH=%APP_HOME%\conf

:: java 可执行文件：优先 JAVA_HOME
if not "%JAVA_HOME%"=="" (
  set JAVA=%JAVA_HOME%\bin\java
) else (
  set JAVA=java
)

:: JVM 参数，可通过环境变量 JAVA_OPTS 覆盖
if "%JAVA_OPTS%"=="" (
  set JAVA_OPTS=-server -Xms1024m -Xmx2048m -Dfile.encoding=UTF-8 -Djava.awt.headless=true
)

echo System Information:
echo ********************************************************
echo APP_HOME=%APP_HOME%
echo APP_MAINCLASS=%APP_MAINCLASS%
echo CLASSPATH=%APP_CONF_PATH%;%APP_LIB_PATH%\*
echo ********************************************************

echo Starting %APP_MAINCLASS% ...
"%JAVA%" -cp "%APP_CONF_PATH%;%APP_LIB_PATH%\*" %JAVA_OPTS% %APP_MAINCLASS% --spring.config.additional-location=file:%APP_CONF_PATH%/

pause
