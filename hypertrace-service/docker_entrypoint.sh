#!/bin/sh
set -e
exec java $JAVA_OPTS -classpath '/app/resources:/app/classes:/app/libs/*' org.hypertrace.core.serviceframework.PlatformServiceLauncher