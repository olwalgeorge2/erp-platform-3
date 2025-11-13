@echo off
rem Convenient Gradle wrapper for Windows
rem This script ensures the correct Java version is used

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

java -Xmx64m -Xms64m -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*