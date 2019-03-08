# Real-time conditions data distribution for the Online data processing of the ALICE experiment

This repository contains a pair of Java programs that send and respectively receive multicast messages. The messages are Java objects of the class Blob.
Blob class contain a random length, random content String (payload) and a separate field for a checksum of this String.

The sender generates and send new objects at fixed time intervals (10s) and at the same time print the current time and message content on the screen.

The receiver instances also print on the screen the current time and the received message.


Requirements:
 - javac 10.0.2
 - openjdk 10.0.2

Compilation:
 `make`

Running:  
`make runReceiver` # creates a Receiver process that receives multicast messages from 230.0.0.0 on port 5000   
`make runSender`   # creates a Sender process that sends multicast messages to 230.0.0.0 using port 5000   
