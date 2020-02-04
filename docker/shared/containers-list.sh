#!/usr/bin/env bash

#
## bash environment
#

if test "$BASH" = "" || "$BASH" -uc "a=();true \"\${a[@]}\"" 2>/dev/null; then
    # Bash 4.4, Zsh
    set -euo pipefail
else
    # Bash 4.3 and older chokes on empty arrays with set -u.
    set -eo pipefail
fi
shopt -s nullglob globstar





#
## script environment
#

TOMCAT_IMAGE="tomcat"

DEFAULT_TOMCAT_TAG="8.5-jdk8-openjdk-slim"

TOMCAT_DEBIAN_TAGS=(
    "8.5-jdk11-openjdk-slim"
    "8.5.47-jdk11-openjdk-slim"
    "9.0-jdk11-openjdk-slim"
    "9.0.27-jdk11-openjdk-slim"

    "8.0-jre8-slim"
    "8.5-jdk8-openjdk-slim"
    "8.5.46-jdk8-openjdk-slim"
    "9.0-jdk8-openjdk-slim"
)

TOMCAT_ALPINE_TAGS=(
    "8.5.34-jre8-alpine"
)
