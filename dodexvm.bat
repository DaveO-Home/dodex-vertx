@echo off
REM
REM It appears the all graalvm dependencies are resolved from the location of native-image.
REM So you can build the executable from the vertx directory. Since dodex-vertx is a server
REM a java vm(JAVA_HOME) is required to execute.   
REM

set LIB=.\build\install\dodex-vertx\lib
set CD2=
for /F %%p in ('dir /b %LIB%\*.jar') DO call :append %%p

<Install Directory>\graalvm\bin\native-image io.vertx.core.Launcher -jar .\build\libs\dodex-vertx-3.8.4.jar  -cp .\build\classes\java/main;%CD2% --initialize-at-build-time=org.slf4j,org.apache.commons.logging --allow-incomplete-classpath --initialize-at-run-time=io.netty.handler.ssl.ConscryptAlpnSslEngine

goto end

:append

echo.%1|findstr /C:"jin*.jar" >nul 2>&1
if errorlevel 1 (
   set CD2=%CD2%;%LIB%\%1
)

:end

