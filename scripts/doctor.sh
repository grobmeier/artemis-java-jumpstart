#!/bin/sh
if [ "${1:-runtime}" = build ]; then
  exec "$(dirname -- "$0")/_run.sh" doctor-build
fi
exec "$(dirname -- "$0")/lab.sh" doctor "$@"
