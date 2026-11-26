#!/usr/bin/env bash
# Builds the app if needed and starts it.
cd "$(dirname "$0")"
if [ ! -f secure-vault.jar ]; then
    ./build.sh || exit 1
fi
java -jar secure-vault.jar
