/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * Blob class - the structure of the object sent via multicast messages
 *
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */

public class Blob {
    public final static int METADATA_CODE = 0;
    public final static int DATA_CODE = 1;

    public enum PACKET_TYPE {
	METADATA, DATA;
    }

    // to be decided if this is necessary
    private ArrayList<FragmentedBlob> blobFragmentsArrayList;
    private int fragmentSize;

    private UUID uuid;
    private byte[] payloadAndMetadataChecksum;
    private String key;
    private byte[] metadata;
    private boolean[] metadataReceived;
    private boolean isMetadataComplete;
    private byte[] payload;
    private boolean isPayloadComplete;
    private boolean[] payloadReceived;

    /**
     * Parameterized constructor - creates a Blob object that contains a payload and
     * a checksum. The checksum is the sha1 of the payload.
     *
     * @param payload - The data string
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public Blob(String payload) throws NoSuchAlgorithmException, UnsupportedEncodingException {
	this.payload = payload.getBytes(Charset.forName(Utils.CHARSET));
	this.payloadAndMetadataChecksum = Utils.calculateChecksum(this.payload);
    }

    public Blob(byte[] metadata, byte[] payload, String key, UUID uuid) throws NoSuchAlgorithmException {
	this.payload = payload;
	this.payloadAndMetadataChecksum = Utils.calculateChecksum(this.payload);
	this.key = key;
	this.uuid = uuid;
	this.metadata = metadata;
    }

    public Blob() {
	this.key = "";
	this.uuid = null;
	this.metadataReceived = null;
	this.payloadReceived = null;
	this.isMetadataComplete = false;
	this.isPayloadComplete = false;

    }

    // va fi chemata de serverul UDP, pe masura ce primeste, deserializeaza un
    // fragment si vede carui Blob ii apartine
    public void notifyFragment(FragmentedBlob fragmentedBlob)
	    throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
	addFragmentedBlob(fragmentedBlob);
    }

    /*
     * Send an entire Blob via multicast messages
     * 
     */
    public void send(int maxPayloadSize, String targetIp, int port) throws NoSuchAlgorithmException, IOException {
	if (maxPayloadSize > this.payload.length + this.metadata.length) {
	    // no need to fragment the Blob
	    // TODO
	    System.out.println("This Blob can be sent without fragmenting. Implement this TODO!");
	} else {
	    /*
	     * fragment metadata
	     */
	    byte[] commonHeader = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER - Utils.SIZE_OF_FRAGMENT_OFFSET
		    - Utils.SIZE_OF_PAYLOAD_CHECKSUM];
	    byte[] packetType_byte_array = new byte[1];
	    packetType_byte_array[0] = (byte) METADATA_CODE;
	    byte[] blobMetadataLength_byte_array = ByteBuffer.allocate(4).putInt(this.metadata.length).array();
	    byte[] keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) this.key.length()).array();
	    byte[] fragmentOffset_byte_array;

	    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		// 2. 1 byte, packet type or flags - to be decided
		out.write(packetType_byte_array);
		// 3. 16 bytes, uuid
		out.write(Utils.getBytes(this.uuid));
		// 4. 4 bytes, blob metadata Length
		out.write(blobMetadataLength_byte_array);
		// 5. 2 bytes, keyLength
		out.write(keyLength_byte_array);

		commonHeader = out.toByteArray();
	    }

	    int indexMetadata = 0;

	    while (indexMetadata <= this.metadata.length) {
		int maxPayloadSize_copy = maxPayloadSize;
		if (maxPayloadSize_copy + indexMetadata > this.metadata.length) {
		    maxPayloadSize_copy = this.metadata.length - indexMetadata;
		}

		byte[] metadataFragment = new byte[maxPayloadSize_copy];
		System.arraycopy(this.metadata, indexMetadata, metadataFragment, 0, maxPayloadSize_copy);

		// byte[] payload_metadata = this.blobByteRange_metadata.get(i);
		byte[] packet = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER + metadataFragment.length
			+ this.key.length()];
		fragmentOffset_byte_array = ByteBuffer.allocate(4).putInt(indexMetadata).array();

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		    // fragment offset
		    out.write(fragmentOffset_byte_array);
		    // Common header: packet type + uuid + blob metadata Length + keyLength
		    out.write(commonHeader);
		    // payload checksum
		    out.write(Utils.calculateChecksum(metadataFragment));
		    // the key
		    out.write(this.key.getBytes());
		    // the payload metadata
		    out.write(metadataFragment);
		    // the packet checksum
		    out.write(Utils.calculateChecksum(out.toByteArray()));

		    packet = out.toByteArray();
		}
		// send the metadata packet
		Utils.sendFragmentMulticast(packet, targetIp, port);

		indexMetadata = indexMetadata + maxPayloadSize;
	    }

	    /*
	     * fragment data
	     */
	    commonHeader = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER - Utils.SIZE_OF_FRAGMENT_OFFSET
		    - Utils.SIZE_OF_PAYLOAD_CHECKSUM];

	    packetType_byte_array = new byte[1];
	    packetType_byte_array[0] = (byte) DATA_CODE;
	    byte[] blobPayloadLength_byte_array = ByteBuffer.allocate(4).putInt(this.payload.length).array();
	    keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) this.key.length()).array();

	    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		// 2. 1 byte, packet type or flags - to be decided
		out.write(packetType_byte_array);
		// 3. 16 bytes, uuid
		out.write(Utils.getBytes(this.uuid));
		// 4. 4 bytes, blob payload Length
		out.write(blobPayloadLength_byte_array);
		// 5. 2 bytes, keyLength
		out.write(keyLength_byte_array);

		commonHeader = out.toByteArray();
	    }

	    int indexPayload = 0;
	    while (indexPayload <= this.payload.length) {
		int maxPayloadSize_copy = maxPayloadSize;
		if (maxPayloadSize_copy + indexPayload > this.payload.length) {
		    maxPayloadSize_copy = this.payload.length - indexPayload;
		}

		byte[] payloadFragment = new byte[maxPayloadSize_copy];
		System.arraycopy(this.payload, indexPayload, payloadFragment, 0, maxPayloadSize_copy);

		// byte[] payload_metadata = this.blobByteRange_metadata.get(i);
		byte[] packet = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER + payloadFragment.length
			+ this.key.length()];
		fragmentOffset_byte_array = ByteBuffer.allocate(4).putInt(indexPayload).array();

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		    // fragment offset
		    out.write(fragmentOffset_byte_array);
		    // Common header: packet type + uuid + blob metadata Length + keyLength
		    out.write(commonHeader);
		    // payload checksum
		    out.write(Utils.calculateChecksum(payloadFragment));
		    // the key
		    out.write(this.key.getBytes());
		    // the payload metadata
		    out.write(payloadFragment);
		    // the packet checksum
		    out.write(Utils.calculateChecksum(out.toByteArray()));

		    packet = out.toByteArray();
		}

		// send the metadata packet
		Utils.sendFragmentMulticast(packet, targetIp, port);

		indexPayload = indexPayload + maxPayloadSize;
	    }
	}
    }

    // manual serialization
    public byte[] toBytes() throws IOException {
	byte[] blobPayloadLength_byte_array = ByteBuffer.allocate(4).putInt(this.payload.length).array();
	byte[] keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) this.key.length()).array();// putShort(this.key).array();

	try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
	    // 1. 16 bytes, uuid
	    out.write(Utils.getBytes(this.uuid));

	    // 2. 4 bytes, blob payload Length
	    out.write(blobPayloadLength_byte_array);

	    // 3. Metadata length

	    // 4. 16 bytes, (payload + metadata) checksum
	    out.write(this.payloadAndMetadataChecksum);

	    // 5. key length
	    out.write(keyLength_byte_array);

	    // 6. ? bytes, key
	    out.write(this.key.getBytes(Charset.forName("UTF-8")));

	    // 7. unknown number of bytes - metadata
	    out.write(this.metadata);

	    // 8. unknown number of bytes - the real data to be transported
	    out.write(this.payload);

	    return out.toByteArray();
	}
    }

    public boolean isComplete() {
	if (this.isPayloadComplete && this.isMetadataComplete) {
	    return true;
	}
	if (this.metadataReceived == null) {
	    return false;
	}
	boolean assumption = true;
	for (int i = this.metadataReceived.length - 1; i >= 0; i--) {
	    assumption = assumption & this.metadataReceived[i];
	    if (assumption == false) {
		return false;
	    }
	}

	if (this.payloadReceived == null) {
	    return false;
	}

	for (int i = this.payloadReceived.length - 1; i >= 0; i--) {
	    assumption = assumption & this.payloadReceived[i];
	    if (assumption == false) {
		return false;
	    }
	}

	return true;
    }

    /**
     * Assemble a Blob by adding one FragmentedBlob to it
     *
     * @param fragmentedBlob
     * @return void
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public void addFragmentedBlob(FragmentedBlob fragmentedBlob)
	    throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
	byte[] fragmentedPayload = fragmentedBlob.getPayload();
	int fragmentOffset = fragmentedBlob.getFragmentOffset();

//		String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
//		System.out.println("[" + timeStamp + "] Adding data fragment : with payload " + new String(fragmentedPayload));

	if (fragmentedBlob.getPachetType() == PACKET_TYPE.DATA) {
	    if (this.payload == null) {
		this.payload = new byte[fragmentedBlob.getBlobPayloadLength()];
		if (fragmentedBlob.getFragmentOffset() + fragmentedBlob.getPayload().length != fragmentedBlob
			.getBlobPayloadLength()) { // daca nu e ultimul fragment
		    int size = fragmentedBlob.getBlobPayloadLength() / fragmentedBlob.getPayload().length;
		    if (fragmentedBlob.getBlobPayloadLength() % fragmentedBlob.getPayload().length > 0) {
			size++;
		    }
		    this.payloadReceived = new boolean[size];
		    Arrays.fill(this.payloadReceived, false);
		} else { // this is the last fragment with data (and the only one)
		    this.isPayloadComplete = true;
		    this.payloadReceived = new boolean[1];
		    Arrays.fill(this.payloadReceived, false);
		}
		this.fragmentSize = fragmentedBlob.getPayload().length;
	    }
	    int index = fragmentedBlob.getFragmentOffset() / this.fragmentSize;
	    this.payloadReceived[index] = true;
	    System.arraycopy(fragmentedPayload, 0, this.payload, fragmentOffset, fragmentedPayload.length);
	} else if (fragmentedBlob.getPachetType() == PACKET_TYPE.METADATA) {
	    if (this.metadata == null) {
		this.metadata = new byte[fragmentedBlob.getBlobPayloadLength()];

		if (fragmentedBlob.getFragmentOffset() + fragmentedBlob.getPayload().length != fragmentedBlob
			.getBlobPayloadLength()) { // daca nu e ultimul fragment
		    int size = fragmentedBlob.getBlobPayloadLength() / fragmentedBlob.getPayload().length;
		    if (fragmentedBlob.getBlobPayloadLength() % fragmentedBlob.getPayload().length > 0) {
			size++;
		    }
		    this.metadataReceived = new boolean[size];
		    Arrays.fill(this.metadataReceived, false);
		} else { // this is the last fragment with data (and the only one)
		    this.isMetadataComplete = true;
		    this.metadataReceived = new boolean[1];
		    Arrays.fill(this.metadataReceived, false);
		}

		this.fragmentSize = fragmentedBlob.getPayload().length;
	    }
	    int index = fragmentedBlob.getFragmentOffset() / this.fragmentSize;
	    this.metadataReceived[index] = true;
	    System.arraycopy(fragmentedPayload, 0, this.metadata, fragmentOffset, fragmentedPayload.length);
	} else {
	    throw new IOException("Packet type not recognized!");
	}
    }

    public String getKey() {
	return this.key;
    }

    public void setKey(String key) {
	this.key = key;
    }

    public UUID getUuid() {
	return this.uuid;
    }

    public void setUuid(UUID uuid) {
	this.uuid = uuid;
    }

    public byte[] getMetadata() {
	return this.metadata;
    }

    public void setMetadata(byte[] metadata) {
	this.metadata = metadata;
    }

    public byte[] getPayload() {
	return this.payload;
    }

    public void setPayload(byte[] payload) {
	this.payload = payload;
    }

    @Override
    public String toString() {
	String output = "";
	output += "Blob with \n";
	output += "\t key = " + this.key + "\n";
	output += "\t uuid = " + this.uuid.toString() + "\n";
	output += "\t metadata = ";
	if (this.metadata != null) {
	    output += new String(this.metadata);
	}
	output += "\n";
	output += "\t payload = ";
	if (this.payload != null) {
	    output += new String(this.payload);
	}
	output += "\n";

	return output;
    }
}
