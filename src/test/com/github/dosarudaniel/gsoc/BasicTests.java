package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import myjava.com.github.dosarudaniel.gsoc.Utils;

public class BasicTests {

    public static void main(String[] args) throws IOException {
	// TODO Auto-generated method stub
	Map<String, String> metadataMap = new HashMap<>();
	metadataMap.put("Inginer", "Daniel");
	metadataMap.put("a", "A");
	metadataMap.put("b", "B");
	metadataMap.put("cern", "CERN");

	System.out.println("Original HashMap");
	for (Map.Entry<String, String> entry : metadataMap.entrySet()) {
	    System.out.println("\t" + entry.getKey() + " : " + entry.getValue());
	}

	byte[] serializedMetadata = Utils.serializeMetadata(metadataMap);

	Map<String, String> metadataMap2 = Utils.deserializeMetadata(serializedMetadata);

	System.out.println("After serialization and deserialization");
	for (Map.Entry<String, String> entry : metadataMap2.entrySet()) {
	    System.out.println("\t" + entry.getKey() + " : " + entry.getValue());
	}
    }

}
