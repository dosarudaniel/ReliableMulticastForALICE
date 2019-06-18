#!/bin/bash

cd `dirname $0`

# prepare Tomcat and compile the project
./compile.sh || exit 1

T="apache-tomcat"

TB="$T/bin"
TL="$T/lib"

# the minimal set of JARs to start Tomcat and load the servler
for jar in $TB/tomcat-juli.jar $TL/{catalina.jar,servlet-api.jar,tomcat-api.jar,tomcat-util.jar,tomcat-util-scan.jar,tomcat-coyote.jar,tomcat-jni.jar,annotations-api.jar,jaspic-api.jar,jasper.jar} ../lib/alien.jar ../lib/lazyj.jar ../lib/FarmMonitor.jar ../lib/apmon.jar; do
    jar -xf $jar
done

# remove useless (for this project) classes
rm -rf src

rm -rf META-INF LICENSE NOTICE about.html ecj.1

rm -rf org/apache/catalina/{manager,filters,ssi,users}

ls org/apache/catalina/servlets/*.class | grep -v DefaultServlet | xargs rm
ls org/apache/catalina/valves/*.class | grep -v -E -e "ValveBase|ErrorReportValve" | xargs rm
ls org/apache/catalina/session/*.class | grep -v -E -e "StandardManager|ManagerBase|TooManyActiveSessionsException" | xargs rm

rm -rf org/apache/jasper/{el,xmlparser,tagplugins,security,runtime}

ls org/apache/jasper/compiler/*.class | grep -v -E -e "JspRuntimeContext|TldCache|JspConfig|TagPluginManager|Node" | xargs rm

rm -rf org/apache/naming

for a in javax/servlet/resources/{*.dtd,*.xsd}; do
    echo -n > $a
done

ln -s ../build/classes/ch .
ln -s ../build/classes/test .
ln -s ../build/classes/myjava .

# package Sender in a single JAR
jar -cfe local.jar \
    ch.alice.o2.ccdb.webserver.LocalEmbeddedTomcat ch/alice/o2/ccdb/webserver/LocalEmbeddedTomcat.class \
    ch javax org alien lazyj lia apmon utils

# package MulticastReceiver in a single JAR
jar -cfe MulticastReceiver.jar \
    test.com.github.dosarudaniel.gsoc.TestMulticastReceiver test/com/github/dosarudaniel/gsoc/TestMulticastReceiver.class \
    test myjava


# package Sender in a single JAR
jar -cfe Sender.jar \
    test.com.github.dosarudaniel.gsoc.TestSender test/com/github/dosarudaniel/gsoc/TestSender.class \
    test myjava ch javax org alien lazyj lia apmon utils

# package BasicTests in a single JAR
jar -cfe BasicTests.jar \
    test.com.github.dosarudaniel.gsoc.BasicTests test/com/github/dosarudaniel/gsoc/BasicTests.class \
    test myjava



# Extra packages for the SQL backend

for jar in postgresql.jar mysql-connector-java-5.1.46.jar bcpkix-jdk15on-152.jar bcprov-jdk15on-152.jar; do
    jar -xf ../lib/$jar
done

rm -rf src META-INF

#jar -cfe sql.jar \
#    ch.alice.o2.ccdb.testing.SQLBenchmark ch/alice/o2/ccdb/testing/SQLBenchmark.class \
#    ch javax org lazyj

jar -cfe sql.jar \
    ch.alice.o2.ccdb.webserver.SQLBackedTomcat ch/alice/o2/ccdb/webserver/SQLBackedTomcat.class \
    ch javax org lazyj lia apmon com org utils \
    alien config trusted_authorities.jks

# further compression and remove debugging information
#pack200 --repack -G -O local.jar
#pack200 --repack -G -O sql.jar

# remove all intermediate folders
rm -rf javax org ch test myjava org lazyj alien config utils lia com org hep apmon trusted_authorities.jks
