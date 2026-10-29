#!/bin/sh
set -eu
LAB_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
LAB_MODULE=$1
shift
LAB_JAVA=java
if [ -n "${JAVA_HOME:-}" ]; then LAB_JAVA="$JAVA_HOME/bin/java"; fi
if ! LAB_VERSION=$("$LAB_JAVA" -version 2>&1); then
  echo "Error: Java is missing. Supply JDK 21; check JAVA_HOME/PATH." >&2; exit 2
fi
LAB_MAJOR=$(printf '%s\n' "$LAB_VERSION" | sed -n 's/^[^ ]* version "\([0-9]*\).*/\1/p')
case "$LAB_MAJOR" in ''|*[!0-9]*) echo "Error: Java version not recognized; use JDK 21." >&2; exit 2;; esac
if [ "$LAB_MAJOR" -lt 21 ]; then echo "Error: Java 21 or newer required." >&2; exit 2; fi
LAB_JAR="$LAB_ROOT/lib/$LAB_MODULE.jar"
if [ ! -f "$LAB_JAR" ]; then LAB_JAR="$LAB_ROOT/$LAB_MODULE/target/$LAB_MODULE.jar"; fi
if [ ! -f "$LAB_JAR" ]; then echo "Error: Build artifact is missing. Run ./mvnw clean verify (exercises: -Ptraining)." >&2; exit 2; fi
exec "$LAB_JAVA" "-Dlab.home=$LAB_ROOT" -jar "$LAB_JAR" "$@"
