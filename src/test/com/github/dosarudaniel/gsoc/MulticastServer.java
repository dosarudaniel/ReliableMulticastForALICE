package test.com.github.dosarudaniel.gsoc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import myjava.com.github.dosarudaniel.gsoc.Blob;

// TODO, posibil de integrat in Receiver sau Sender
public class MulticastServer {

	Map<UUID, Blob> inFlight = new HashMap<>(); // uuid <-> Blob fragmentat, zonă de tranzitie până la primirea completă
												// a tuturor fragmentelor
	Map<String, Blob> currentCacheContent = new HashMap<>(); // Blob-uri complete

	public static void main(String[] args) {
		while (true) {
//			FragmentedBlob fragmentedBlob = ByteToFragmentedBlob(pachet);

//		    Blob blob = updateInFlight(toFragment(receive());
//		
//		    invalidateCache(blob.getKey());
//		    if (blob.isComplete()){
//		
//		        moveToCache(blob);
//		    }
		}
	}

	Thread incompleteBlobRecovery = new Thread();
	// daca inFlight contine obiecte care nu au fost atinse de mai mult de X millis
	// => recovery procedure pentru ele

}
