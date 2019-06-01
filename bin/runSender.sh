#!/bin/bash

function usage {
    echo "usage: $0 <MULTICAST_IP_ADDRESS> <PORT_NUMBER> <FRAGMENT_MAX_PAYLOAD_SIZE> <KEY_LENGTH> <METADATA_LENGTH> <PAYLOAD_LENGTH> <NR_OF_PACKETS_TO_BE_SENT>"
    exit 1
}

if [[ $# -ne 7 ]]; then
  usage
else
  java -cp . test.com.github.dosarudaniel.gsoc.TestSender $1 $2 $3 $4 $5 $6 $7
fi
