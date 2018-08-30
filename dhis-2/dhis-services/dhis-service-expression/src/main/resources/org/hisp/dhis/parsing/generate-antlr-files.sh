#!/bin/bash

# After editing Expression.g4, this script generates the ANLTR derived files
# that are stored in org.hisp.parsing.generated
#
# The generated Java files are then compiled as part of the normal build
# and saved to Github. ANLTR also generates *.interp and *.tokens files that
# are not needed for the build and are ignored by DHIS2 .gitignore.
#
# Before running this script, you should have antlr-4.7.1-complete.jar on
# your system, as from http://www.antlr.org/download/antlr-4.7.1-complete.jar.
#
# Supply this file with path as a command argument to this script. Example:
#
# ./generate-antlr-files.sh /usr/local/lib/antlr-4.7.1-complete.jar

java -jar $1 -o ../../../../../java/org/hisp/dhis/parsing/generated/ -no-listener -visitor -package org.hisp.dhis.parsing.generated Expression.g4
