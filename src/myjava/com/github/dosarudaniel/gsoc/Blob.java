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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import myjava.com.github.dosarudaniel.gsoc.Utils.Pair;

/**
 * Blob class - the structure of the object sent via multicast messages
 *
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */

public class Blob {
    private static Logger logger;
    public final static byte METADATA_CODE = 0;
    public final static byte DATA_CODE = 1;
    public final static byte SMALL_BLOB_CODE = 2;

    private final UUID uuid;
    private final String key;
    private byte[] payloadChecksum = null;
    private byte[] metadataChecksum = null;
    private byte[] metadata = null;
    private byte[] payload = null;

    private final ArrayList<Pair> metadataByteRanges = new ArrayList<>();
    private final ArrayList<Pair> payloadByteRanges = new ArrayList<>();

    /**
     * Parameterized constructor - creates a Blob object to be sent that contains a
     * payload and a checksum. The checksum is the Utils.CHECKSUM_TYPE of the
     * payload.
     *
     * @param payload  - The data byte array
     * @param metadata - The metadata byte array
     * @param key      - The key string
     * @param uuid     - The UUID of the Blob
     * 
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws SecurityException
     */
    public Blob(byte[] metadata, byte[] payload, String key, UUID uuid)
	    throws NoSuchAlgorithmException, SecurityException, IOException {
	this.metadata = metadata;
	this.metadataChecksum = Utils.calculateChecksum(this.metadata);
	this.payload = payload;
	this.payloadChecksum = Utils.calculateChecksum(this.payload);
	this.key = key;
	this.uuid = uuid;
	this.metadataByteRanges.add(new Pair(0, this.metadata.length));
	this.payloadByteRanges.add(new Pair(0, this.payload.length));

	logger = Logger.getLogger(this.getClass().getCanonicalName());
	Handler fh = new FileHandler("%t/ALICE_Blob_log");
	Logger.getLogger(this.getClass().getCanonicalName()).addHandler(fh);
    }

    /**
     * Parameterized constructor - creates a Blob object to be sent that contains a
     * payload and a checksum. The checksum is the Utils.CHECKSUM_TYPE of the
     * payload.
     *
     * @param metadata - The metadata HaspMap
     * @param payload  - The data byte array
     * @param key      - The key string
     * @param uuid     - The UUID of the Blob
     * 
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws SecurityException
     */
    public Blob(Map<String, String> metadataMap, byte[] payload, String key, UUID uuid)
	    throws NoSuchAlgorithmException, SecurityException, IOException {
	this.metadata = Utils.serializeMetadata(metadataMap);
	this.metadataChecksum = Utils.calculateChecksum(this.metadata);
	this.payload = payload;
	this.payloadChecksum = Utils.calculateChecksum(this.payload);
	this.key = key;
	this.uuid = uuid;
	this.metadataByteRanges.add(new Pair(0, this.metadata.length));
	this.payloadByteRanges.add(new Pair(0, this.payload.length));

	logger = Logger.getLogger(this.getClass().getCanonicalName());
	Handler fh = new FileHandler("%t/ALICE_Blob_log");
	Logger.getLogger(this.getClass().getCanonicalName()).addHandler(fh);
    }

    /**
     * Unparameterized constructor - creates an empty Blob object that receives
     * fragmentedBlob objects and puts their content into the metadata or payload
     * members via addFragmentedBlob method
     * 
     *
     * @param payload  - The data byte array
     * @param metadata - The metadata byte array
     * @param key      - The key string
     * @param uuid     - The UUID of the Blob
     * @throws IOException
     * @throws SecurityException
     */
    public Blob(String key, UUID uuid) throws SecurityException, IOException {
	this.key = key;
	this.uuid = uuid;
	logger = Logger.getLogger(this.getClass().getCanonicalName());
	Handler fh = new FileHandler("%t/ALICE_MulticastSender_log");
	Logger.getLogger(this.getClass().getCanonicalName()).addHandler(fh);
    }

    /**
     * Send method - fragments a blob into smaller serialized fragmentedBlobs and
     * sends them via UDP multicast. Reads the maxPayloadSize from a file <--TODO
     *
     * @param targetIp - Destination multicast IP
     * @param port     - Socket port number
     * 
     * @throws NoSuchAlgorithmException, IOException
     */
    public void send(String targetIp, int port) throws NoSuchAlgorithmException, IOException {
	// TODO: get maxPayloadSize from a file
	int maxPayloadSize = 1500;
	send(maxPayloadSize, targetIp, port);
    }

    /**
     * Send method - fragments a blob into smaller serialized fragmentedBlobs and
     * sends them via UDP multicast
     * 
     *
     * @param maxPayloadSize - The maximum payload size of the serialized
     *                       fragmentedBlob
     * @param targetIp       - Destination multicast IP
     * @param port           - Socket port number
     * 
     * @throws NoSuchAlgorithmException, IOException
     */
    public void send(int maxPayloadSize, String targetIp, int port) throws NoSuchAlgorithmException, IOException {
	if (maxPayloadSize > this.payload.length + this.metadata.length) {
	    // no need to fragment the Blob

	    byte[] packetType_byte_array = new byte[] { SMALL_BLOB_CODE };

	    byte[] blobPayloadLength_byte_array = ByteBuffer.allocate(4).putInt(this.payload.length).array();
	    byte[] keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) this.key.getBytes().length).array();
	    byte[] fragmentOffset_byte_array;

	    byte[] metadataAndPayloadFragment = new byte[this.payload.length + this.metadata.length];

	    System.arraycopy(this.metadata, 0, metadataAndPayloadFragment, 0, this.metadata.length);
	    System.arraycopy(this.payload, 0, metadataAndPayloadFragment, this.metadata.length, this.payload.length);

	    byte[] packet = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER
		    + metadataAndPayloadFragment.length + this.key.getBytes().length];
	    // Offset zero
	    fragmentOffset_byte_array = ByteBuffer.allocate(4).putInt(0).array();

	    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		// fragment offset
		out.write(fragmentOffset_byte_array);
		// 2. 1 byte, packet type or flags - to be decided
		out.write(packetType_byte_array);
		// 3. 16 bytes, uuid
		out.write(Utils.getBytes(this.uuid));
		// 4. 4 bytes, blob payload + metadata Length
		out.write(blobPayloadLength_byte_array);
		// 5. 2 bytes, keyLength
		out.write(keyLength_byte_array);
		// payload checksum
		out.write(this.payloadChecksum);
		// the key
		out.write(this.key.getBytes());
		// the payload metadata
		out.write(metadataAndPayloadFragment);
		// the packet checksum
		out.write(Utils.calculateChecksum(out.toByteArray()));

		packet = out.toByteArray();
	    }
	    // send the metadata packet
	    Utils.sendFragmentMulticast(packet, targetIp, port);
	} else {
	    /*
	     * fragment metadata
	     */
	    byte[] commonHeader = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER - Utils.SIZE_OF_FRAGMENT_OFFSET
		    - Utils.SIZE_OF_PAYLOAD_CHECKSUM];
	    byte[] packetType_byte_array = new byte[] { METADATA_CODE };
	    byte[] blobMetadataLength_byte_array = ByteBuffer.allocate(4).putInt(this.metadata.length).array();
	    byte[] keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) this.key.getBytes().length).array();
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

	    while (indexMetadata < this.metadata.length) {
		int maxPayloadSize_copy = maxPayloadSize;
		if (maxPayloadSize_copy + indexMetadata > this.metadata.length) {
		    maxPayloadSize_copy = this.metadata.length - indexMetadata;
		}

		byte[] metadataFragment = new byte[maxPayloadSize_copy];
		System.arraycopy(this.metadata, indexMetadata, metadataFragment, 0, maxPayloadSize_copy);
		byte[] packet = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER + metadataFragment.length
			+ this.key.getBytes().length];
		fragmentOffset_byte_array = ByteBuffer.allocate(4).putInt(indexMetadata).array();

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		    // fragment offset
		    out.write(fragmentOffset_byte_array);
		    // Common header: packet type + uuid + blob metadata Length + keyLength
		    out.write(commonHeader);
		    // payload checksum
		    out.write(this.metadataChecksum);
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

	    packetType_byte_array = new byte[] { DATA_CODE };
	    byte[] blobPayloadLength_byte_array = ByteBuffer.allocate(4).putInt(this.payload.length).array();
	    keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) this.key.getBytes().length).array();

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
	    while (indexPayload < this.payload.length) {
		int maxPayloadSize_copy = maxPayloadSize;
		if (maxPayloadSize_copy + indexPayload > this.payload.length) {
		    maxPayloadSize_copy = this.payload.length - indexPayload;
		}

		byte[] payloadFragment = new byte[maxPayloadSize_copy];
		System.arraycopy(this.payload, indexPayload, payloadFragment, 0, maxPayloadSize_copy);

		byte[] packet = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER + payloadFragment.length
			+ this.key.getBytes().length];
		fragmentOffset_byte_array = ByteBuffer.allocate(4).putInt(indexPayload).array();

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		    // fragment offset
		    out.write(fragmentOffset_byte_array);
		    // Common header: packet type + uuid + blob metadata Length + keyLength
		    out.write(commonHeader);
		    // payload checksum
		    out.write(this.payloadChecksum);
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

    public boolean isComplete() throws IOException, NoSuchAlgorithmException {
	if (this.metadata == null || this.payload == null) {
	    return false;
	}

	// Check byte ranges size:
	if (this.payloadByteRanges.size() != 1 || this.payloadByteRanges.size() != 1) {
	    return false;
	}
	// Check byte ranges metadata:
	if (this.metadataByteRanges.get(0).first != 0
		|| this.metadataByteRanges.get(0).second != this.metadata.length) {
	    return false;
	}

	// Check byte ranges payload:
	if (this.payloadByteRanges.get(0).first != 0 || this.payloadByteRanges.get(0).second != this.payload.length) {
	    return false;
	}

	// Verify checksums
	if (!Arrays.equals(this.payloadChecksum, Utils.calculateChecksum(this.payload))) {
	    throw new IOException("Payload checksum failed");
	}

	if (!Arrays.equals(this.metadataChecksum, Utils.calculateChecksum(this.metadata))) {
	    throw new IOException("Metadata checksum failed");
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
	Pair pair = new Pair(fragmentOffset, fragmentOffset + fragmentedPayload.length);

	if (fragmentedBlob.getPachetType() == DATA_CODE) {
	    if (this.payload == null) {
		this.payload = new byte[fragmentedBlob.getblobDataLength()];
		this.payloadChecksum = fragmentedBlob.getPayloadChecksum();
	    }
	    if (this.payload.length != fragmentedBlob.getblobDataLength()) { // Another fragment
		throw new IOException("payload.length should have size = " + fragmentedBlob.getblobDataLength());
	    }
	    System.arraycopy(fragmentedPayload, 0, this.payload, fragmentOffset, fragmentedPayload.length);

	    int index = -1;
	    for (int i = 0; i < this.payloadByteRanges.size(); i++) {
		if (this.payloadByteRanges.get(i).second == pair.first) {
		    index = i;
		    this.payloadByteRanges.set(i, new Pair(this.payloadByteRanges.get(i).first, pair.second));
		    break;
		} else if (this.payloadByteRanges.get(i).first == pair.second) {
		    index = i;
		    this.payloadByteRanges.set(i, new Pair(pair.first, this.payloadByteRanges.get(i).second));
		    break;
		}
	    }

	    if (index == -1) { // a new element at the end
		this.payloadByteRanges.add(pair);
	    } else {
		// check if element at index i can be merged with another one
		for (int i = 0; i < this.payloadByteRanges.size() && i != index; i++) {
		    if (this.payloadByteRanges.get(i).first == this.payloadByteRanges.get(index).second) {
			this.payloadByteRanges.set(i, new Pair(this.payloadByteRanges.get(index).first,
				this.payloadByteRanges.get(i).second));
			this.payloadByteRanges.remove(index);
			break;
		    } else if (this.payloadByteRanges.get(i).second == this.payloadByteRanges.get(index).first) {
			this.payloadByteRanges.set(i, new Pair(this.payloadByteRanges.get(i).first,
				this.payloadByteRanges.get(index).second));
			this.payloadByteRanges.remove(index);
			break;
		    }
		}
	    }

	} else if (fragmentedBlob.getPachetType() == METADATA_CODE) {
	    if (this.metadata == null) {
		this.metadata = new byte[fragmentedBlob.getblobDataLength()];
		this.metadataChecksum = fragmentedBlob.getPayloadChecksum(); // metadata == payload
	    }
	    if (this.metadata.length != fragmentedBlob.getblobDataLength()) { // Another fragment
		throw new IOException("metadata.length should have size = " + fragmentedBlob.getblobDataLength());
	    }
	    System.arraycopy(fragmentedPayload, 0, this.metadata, fragmentOffset, fragmentedPayload.length);

	    int index = -1;
	    for (int i = 0; i < this.metadataByteRanges.size(); i++) {
		if (this.metadataByteRanges.get(i).second == pair.first) {
		    index = i;
		    this.metadataByteRanges.set(i, new Pair(this.metadataByteRanges.get(i).first, pair.second));
		    break;
		} else if (this.metadataByteRanges.get(i).first == pair.second) {
		    index = i;
		    this.metadataByteRanges.set(i, new Pair(pair.first, this.metadataByteRanges.get(i).second));
		    break;
		}
	    }

	    if (index == -1) { // a new element at the end
		this.metadataByteRanges.add(pair);
	    } else {
		// check if element at index i can be merged with another one
		for (int i = 0; i < this.metadataByteRanges.size() && i != index; i++) {
		    if (this.metadataByteRanges.get(i).first == this.metadataByteRanges.get(index).second) {
			this.metadataByteRanges.set(i, new Pair(this.metadataByteRanges.get(index).first,
				this.metadataByteRanges.get(i).second));
			this.metadataByteRanges.remove(index);
			break;
		    } else if (this.metadataByteRanges.get(i).second == this.metadataByteRanges.get(index).first) {
			this.metadataByteRanges.set(i, new Pair(this.metadataByteRanges.get(i).first,
				this.metadataByteRanges.get(index).second));
			this.metadataByteRanges.remove(index);
			break;
		    }
		}
	    }
	} else if (fragmentedBlob.getPachetType() == SMALL_BLOB_CODE) {
	    if (this.metadata == null && this.payload == null) {
		int metadataLength = fragmentedPayload.length - fragmentedBlob.getblobDataLength();
		int payloadLength = fragmentedBlob.getblobDataLength();
		this.metadata = new byte[metadataLength];
		this.payload = new byte[payloadLength];

		System.arraycopy(fragmentedPayload, 0, this.metadata, fragmentOffset, metadataLength);
		System.arraycopy(fragmentedPayload, metadataLength, this.payload, fragmentOffset, payloadLength);
		this.payloadChecksum = fragmentedBlob.getPayloadChecksum();
		this.metadataChecksum = Utils.calculateChecksum(this.metadata);
		this.payloadByteRanges.add(new Pair(0, payloadLength));
		this.metadataByteRanges.add(new Pair(0, metadataLength));
	    } else {
		logger.log(Level.WARNING, "metadata and payload byte arrays should be null for an empty SMALL BLOB");
	    }
	} else {
	    throw new IOException("Packet type not recognized!");
	}

    }

    public String getKey() {
	return this.key;
    }

    public UUID getUuid() {
	return this.uuid;
    }

    public Map<String, String> getMetadata() {
	return Utils.deserializeMetadata(this.metadata);
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
