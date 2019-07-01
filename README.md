# Real-time conditions data distribution for the Online data processing of the ALICE experiment


![alt text](https://github.com/dosarudaniel/ReliableMulticastForALICE/blob/master/ReliableMulticast.png)



A pair of java classes were developed for this project: the Sender and the MulticastReceiver.   
These entities are using the Blob class to store a complete data object and FragmentedBlob class for a fragmented data object. 
  
A Blob object has two important fields that store useful information: metadata and payload.   
The FragmentedBlob class stores partial content from a Blob: either metadata, either data, depending on the Packet Type field.   


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
* #### the receiver:     
`./runReceiver`  

*  #### the sender:
`export MAX_PAYLOAD_SIZE=1400`  
`./runSender.sh`   
