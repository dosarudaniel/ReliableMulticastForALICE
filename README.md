# Real-time conditions data distribution for the Online data processing of the ALICE experiment


![alt text](https://github.com/dosarudaniel/ReliableMulticastForALICE/blob/master/ReliableMulticast.png)



A pair of java classes were developed for this project: the Sender and the MulticastReceiver.   
These entities are using the Blob class to store complete data objects. These objects will be sent via multicast mesages from the Central repository to the Multicast receivers programs that run on the EPNs (Event Processing Node).
  
A Blob object has two important fields that store useful information: metadata and payload.   
Because a typical Blob size is around 2 MB (which is far more than the maximum payload size of UDP packets - 64KB) I had to use a fragmentation and reassembling mechanism. The FragmentedBlob class stores partial content from a Blob: either metadata, either data, depending on the Packet Type field. This object si serialized using the following structure:




Requirements:  
#todo  

## Compile:  
(Create .jar files)  
 `cd package`  
 `./package.sh`  

Clean: (removes .jar files too)   
  `make clean`  

Generate documentation:    
  `make doc`      

## Running     
Increase the receiver buffer size:    
`sudo ./kernel_configuration.sh`  
`cd package`   
(Compile)   
   
* #### the receiver:     
`./runReceiver`  

*  #### the sender:
`export MAX_PAYLOAD_SIZE=1400`  
`./runSender.sh`   
