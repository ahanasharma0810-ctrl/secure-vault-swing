#!/usr/bin/env bash
# Builds secure-vault.jar without Maven (needs JDK 11 or newer).
set -e
cd "$(dirname "$0")"

rm -rf out
mkdir -p out/classes

echo "Compiling..."
javac --release 11 -encoding UTF-8 -sourcepath src/main/java -d out/classes \
      src/main/java/com/securevault/Main.java

printf 'Main-Class: com.securevault.Main\nImplementation-Title: Secure Vault\nImplementation-Version: 1.0.0\n' > out/manifest.txt
jar cfm secure-vault.jar out/manifest.txt -C out/classes .

echo "Done: secure-vault.jar"
echo "Run it with: java -jar secure-vault.jar"
