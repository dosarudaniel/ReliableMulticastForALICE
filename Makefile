IP=230.0.0.0
PORT_NUMBER=5000

build:
	javac -g src/* -d .

runSender:
	java TestSender $(IP) $(PORT_NUMBER)

runReceiver:
	java TestReceiver $(IP) $(PORT_NUMBER)

clean:
	rm *.class
