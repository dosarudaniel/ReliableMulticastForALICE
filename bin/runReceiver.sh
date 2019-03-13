#!/bin/bash

function usage {
    echo "usage: $0 <MULTICAST_IP_ADDRESS> <PORT_NUMBER>"
    exit 1
}

if [[ $# -ne 2 ]]; then
  usage
else
  java -cp . test.com.github.dosarudaniel.gsoc.TestReceiver $1 $2
fi
