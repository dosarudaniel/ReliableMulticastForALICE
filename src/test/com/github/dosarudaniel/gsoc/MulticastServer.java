package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import myjava.com.github.dosarudaniel.gsoc.Blob;
import myjava.com.github.dosarudaniel.gsoc.Blob.PACKET_TYPE;
import myjava.com.github.dosarudaniel.gsoc.FragmentedBlob;

// TODO, posibil de integrat in Receiver sau Sender
public class MulticastServer {
	static final int MIN_LEN = 50;
	static final int MAX_LEN = 130;
	static final String AB = "34";

	Map<UUID, Blob> inFlight = new HashMap<>(); // uuid <-> Blob fragmentat, zonă de tranzitie până la primirea completă
												// a tuturor fragmentelor
	Map<String, Blob> currentCacheContent = new HashMap<>(); // Blob-uri complete

	public static void main(String[] args) throws NoSuchAlgorithmException, IOException {

		String payload = randomString(ThreadLocalRandom.current().nextInt(MIN_LEN, MAX_LEN));
		String key = "te";
		UUID uuid = UUID.randomUUID();
		FragmentedBlob fBlob = new FragmentedBlob(payload.getBytes(Charset.forName("UTF-8")), 257, PACKET_TYPE.DATA,
				key, uuid);
		fBlob.toBytes();

		// while (true) {
//			FragmentedBlob fragmentedBlob = ByteToFragmentedBlob(pachet);

//		    Blob blob = updateInFlight(toFragment(receive());
//		
//		    invalidateCache(blob.getKey());
//		    if (blob.isComplete()){
//		
//		        moveToCache(blob);
//		    }
		// }
	}

	// Thread incompleteBlobRecovery = new Thread();
	// daca inFlight contine obiecte care nu au fost atinse de mai mult de X millis
	// => recovery procedure pentru ele
	/**
	 * Generates a random content string of length len
	 *
	 * @param len - Length of the randomString
	 * @return String - A random content string of length len
	 */
	static String randomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			int randomNumber = ThreadLocalRandom.current().nextInt(0, AB.length());
			sb.append(AB.charAt(randomNumber));
		}

		return sb.toString();
	}
}
