#!/usr/bin/env sh
set -eu
rm -rf out
mkdir -p out
javac -d out src/main/java/*.java
java -cp out MarketplaceAgentLoop
