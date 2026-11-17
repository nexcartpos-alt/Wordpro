@echo off
rem Builds word-pro (needs a JDK, 8 or newer). Then run: java -jar word-pro.jar
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\wordpro\*.java || exit /b 1
echo Main-Class: wordpro.Main> out\manifest.txt
jar cfm word-pro.jar out\manifest.txt -C out wordpro
echo Built word-pro.jar
