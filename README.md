# Real-time conditions data distribution for the Online data processing of the ALICE experiment

This repository contains a pair of Java programs that send and respectively receive multicast messages. The messages are Java objects of the class Blob.
Blob class contain a random length, random content String (payload) and a separate field for a checksum of this String. 

The sender generates and send new objects at fixed time intervals (10s) and at the same time print the current time and message content on the screen.

The receiver instances also print on the screen the current time and the received message.


Requirements:
*javac
*java 

Compilation:
make 

Running:
make runReceiver # creates a Receiver process
make runSender   # creates a Sender process

