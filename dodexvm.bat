@echo off

set LIB=<dodex-vertx install>\build\install\dodex-vertx\lib
set CD2=
for /F %%p in ('dir /b %LIB%\*.jar') DO call :append %%p

native-image io.vertx.core.Launcher -jar <dodex-vertx install>\build\libs\dodex-vertx-3.8.4.jar  -cp <dodex-vertx>\build\classes\java/main;%CD2% --initialize-at-build-time=org.slf4j,org.apache.commons.logging
goto end

:append

echo.%1|findstr /C:"jin*.jar" >nul 2>&1
if errorlevel 1 (
   set CD2=%CD2%;%LIB%\%1
)

:end

