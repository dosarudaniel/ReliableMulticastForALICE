#!/bin/bash

cd ../../package

#start tomcat server and multicast receiver in the background
./runReceiver.sh &

#publish two hardcoded Blobs with the same "key=123_hardcoded_key"
./runSender.sh
./runSender.sh

#request the published blob
curl -i localhost:8080/Task/Detector/1/key=123_hardcoded_key

