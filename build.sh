#!/bin/sh
# Builds word-pro (needs JDK 11+). Run: ./build.sh && java -jar word-pro.jar
set -e
rm -rf out && mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')
printf 'Main-Class: wordpro.Main\n' > out/manifest.txt
jar cfm word-pro.jar out/manifest.txt -C out wordpro
echo "Built word-pro.jar"
