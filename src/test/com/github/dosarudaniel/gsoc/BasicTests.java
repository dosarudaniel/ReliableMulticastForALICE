package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import myjava.com.github.dosarudaniel.gsoc.Utils;
import myjava.com.github.dosarudaniel.gsoc.Utils.Pair;
import myjava.com.github.dosarudaniel.gsoc.Utils.PairComparator;

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

	// Test the TreeSet
	TreeSet<Pair> treeSet = new TreeSet<>(new PairComparator());

	Pair p1 = new Pair(100, 160);
	Pair p2 = new Pair(210, 400);
	Pair p3 = new Pair(0, 100);
	Pair p4 = new Pair(400, 430);
	Pair p5 = new Pair(160, 210);

	treeSet.add(p1);
	System.out.println(treeSet);
	treeSet.add(p2);
	System.out.println(treeSet);
	treeSet.add(p3);

	System.out.println(" ");
	Iterator<Pair> iterator = treeSet.iterator();
	while (iterator.hasNext())
	    System.out.print(iterator.next());
	System.out.println(" ");

	System.out.println(treeSet);
	treeSet.add(p4);
	System.out.println(treeSet);
	treeSet.add(p5);
	System.out.println(treeSet);

	byte[] test = new byte[10];
	System.out.println(test.length);

    }

}
