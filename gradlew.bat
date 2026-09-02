#!/usr/bin/env sh
# Gradle wrapper launcher
APP_BASE_NAME=gradlew
DIR="$( cd "$( dirname "$0" )" >/dev/null 2>&1 && pwd )"
exec java -Xmx2g -Dfile.encoding=UTF-8 -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
