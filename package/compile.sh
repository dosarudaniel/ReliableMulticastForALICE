#!/bin/bash

cd `dirname $0`

# make sure Tomcat is available
./download-tomcat.sh || exit 1

# same directory structure as for Eclipse
mkdir -p ../build/classes/

# add all Tomcat JARs to the classpath
T=apache-tomcat

CLASSPATH=

for jar in $T/bin/*.jar $T/lib/*.jar ../lib/lazyj.jar ../lib/alien.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

export CLASSPATH

# and compile the project
find ../src -name \*.java | xargs javac -source 8 -target 8 -d ../build/classes
