#!/usr/bin/env bash
nohup java -jar "$(dirname "$0")/javaish.jar" "$@" >/dev/null 2>&1 &