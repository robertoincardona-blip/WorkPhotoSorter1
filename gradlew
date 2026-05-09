#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

APP_BASE_NAME=`basename "$0"`
APP_HOME=`dirname "$0"`

DEFAULT_JVM_OPTS=""

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec "$JAVACMD" "$DEFAULT_JVM_OPTS" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
