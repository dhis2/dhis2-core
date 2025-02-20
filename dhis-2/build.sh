#!/bin/sh

# Requires maven to be on the classpath
# Skips test phase

mvn clean install --batch-mode --no-transfer-progress -DskipTests=true


