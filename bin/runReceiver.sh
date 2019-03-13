#!/bin/bash

# How to run it: ./runReceiver.sh 230.0.0.0 5000
java -cp . test.com.github.dosarudaniel.gsoc.TestReceiver $1 $2
