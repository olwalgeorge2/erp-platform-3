#!/usr/bin/env sh

DIR=$(dirname "$0")

if [ -z "$JAVA_HOME" ]; then
  JAVA_EXE=java
else
  JAVA_EXE="$JAVA_HOME/bin/java"
fi

CLASSPATH="$DIR/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVA_EXE" -Xmx64m -Xms64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
