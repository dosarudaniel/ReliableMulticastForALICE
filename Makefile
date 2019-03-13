IP=230.0.0.0
PORT_NUMBER=5000

JFLAGS = -g
JC = javac
J = java

build:
	cd ./src ; find . -name \*.java | xargs $(JC) $(JFLAGS) -d ../bin

runSender:build
	$(J) -cp bin test.com.github.dosarudaniel.gsoc.TestSender $(IP) $(PORT_NUMBER)

runReceiver:build
	$(J) -cp bin test.com.github.dosarudaniel.gsoc.TestReceiver $(IP) $(PORT_NUMBER)

clean:
	rm bin/test/com/github/dosarudaniel/gsoc/*.class
	rm bin/myjava/com/github/dosarudaniel/gsoc/*.class
