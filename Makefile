IP=230.0.0.0
PORT_NUMBER=5000

JFLAGS = -g
JC = javac
J = java
JD = javadoc
DOC = documentation


build:
	cd ./src ; find . -name \*.java | xargs $(JC) $(JFLAGS) -d ../bin

runSender:
	$(J) -cp bin test.com.github.dosarudaniel.gsoc.TestSender $(IP) $(PORT_NUMBER)
	
runBurstSender:
	$(J) -cp bin test.com.github.dosarudaniel.gsoc.TestBurstSender $(IP) $(PORT_NUMBER)

runReceiver:
	$(J) -cp bin test.com.github.dosarudaniel.gsoc.TestReceiver $(IP) $(PORT_NUMBER)

runBasicTests:build
	$(J) -cp bin test.com.github.dosarudaniel.gsoc.BasicTests

runMulticastServer:
	$(J) -cp bin test.com.github.dosarudaniel.gsoc.TestMulticastServer $(IP) $(PORT_NUMBER)

doc:
	$(JD) src/test/com/github/dosarudaniel/gsoc/* \
		src/myjava/com/github/dosarudaniel/gsoc/* -d $(DOC)

clean:
	rm -rf $(DOC)
	rm -rf bin/test
	rm -rf bin/myjava
	rm -rf build/classes/ch/*
	rm -rf build/classes/test/*
	rm -rf build/classes/myjava/*
