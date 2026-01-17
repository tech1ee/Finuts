#!/bin/sh

##############################################################################
##
##  Gradle wrapper script for POSIX
##
##############################################################################

set -e

APP_HOME="$(cd "$(dirname "$0")" > /dev/null && pwd)"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec java $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
