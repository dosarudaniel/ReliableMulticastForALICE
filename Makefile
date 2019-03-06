build:
	javac -g src/* -d . 

runSender:
	java TestSender

runReceiver:
	java TestReceiver

clean:
	rm *.class
