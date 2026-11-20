#!/usr/bin/env bash
# Loads variables from .env (if present), compiles, and starts CampusFix.
#   ./run.sh               start the application
#   ./run.sh --init-admin  one-time creation of the first administrator
#
# The app is started with plain "java" (not "mvn exec:java") so that the terminal can hide
# password typing.
set -euo pipefail
cd "$(dirname "$0")"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

mvn -q compile dependency:build-classpath \
    -Dmdep.outputFile=target/classpath.txt -Dmdep.includeScope=runtime

exec java -cp "target/classes:$(cat target/classpath.txt)" com.campusfix.Main "$@"
