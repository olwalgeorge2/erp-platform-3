@ECHO OFF

SET DIR=%~dp0

IF NOT DEFINED JAVA_HOME (
  SET JAVA_EXE=java.exe
) ELSE (
  SET JAVA_EXE="%JAVA_HOME%\bin\java.exe"
)

SET CLASSPATH=%DIR%\gradle\wrapper\gradle-wrapper.jar

%JAVA_EXE% -Xmx64m -Xms64m -classpath %CLASSPATH% org.gradle.wrapper.GradleWrapperMain %*
