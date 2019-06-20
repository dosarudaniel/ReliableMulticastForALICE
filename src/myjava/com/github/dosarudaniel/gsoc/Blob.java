/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
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
    SingletonLogger singletonLogger = new SingletonLogger();
    Logger logger = this.singletonLogger.getLogger();

    public final static byte METADATA_CODE = 0;
    public final static byte DATA_CODE = 1;
    public final static byte SMALL_BLOB_CODE = 2;

    private final UUID uuid;
    private final String key;
    private byte[] payloadChecksum = null;
    private byte[] metadataChecksum = null;
    private byte[] metadata = null;
    private byte[] payload = null;
    private Timestamp timestamp;

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
    }

    /**
     * Send method - fragment (if necessary) and send the missingBlock from metadata
     * or payload as packetType parameter specifies
     *
     * @param maxPayloadSize - the maximum payload supported by a fragmented packet
     * @param missingBlock   - the interval to be sent via multicast from metadata
     *                       or payload
     * @param packetType     - specify what kind of data is missing so that it
     *                       should be send: METADATA_CODE or DATA_CODE
     * @param targetIp       - Destination multicast IP
     * @param port           - Socket port number
     * 
     * @throws NoSuchAlgorithmException, IOException
     */
    public void send(int maxPayloadSize, Pair missingBlock, byte packetType, String targetIp, int port)
	    throws IOException, NoSuchAlgorithmException {

	if (packetType == METADATA_CODE) {
	    // fragment [missingBlock.first, missingBlock.second]
	    // build packet
	    // Utils.sendFragmentMulticast(packet, targetIp, port);
	    byte[] metadataToSend = new byte[missingBlock.second - missingBlock.first];
	    System.arraycopy(this.metadata, missingBlock.first, metadataToSend, 0,
		    missingBlock.second - missingBlock.first);
	    /*
	     * fragment metadata
	     */
	    byte[] commonHeader = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER - Utils.SIZE_OF_FRAGMENT_OFFSET
		    - Utils.SIZE_OF_PAYLOAD_CHECKSUM];
	    // 2. 1 byte, packet type or flags
	    commonHeader[0] = METADATA_CODE;
	    // 3. 16 bytes, uuid
	    System.arraycopy(Utils.getBytes(this.uuid), 0, commonHeader, Utils.SIZE_OF_PACKET_TYPE, Utils.SIZE_OF_UUID);
	    // 4. 4 bytes, blob metadata Length
	    System.arraycopy(Utils.intToByteArray(this.metadata.length), 0, commonHeader,
		    Utils.SIZE_OF_PACKET_TYPE + Utils.SIZE_OF_UUID, Utils.SIZE_OF_BLOB_PAYLOAD_LENGTH);
	    // 5. 2 bytes, keyLength
	    System.arraycopy(Utils.shortToByteArray((short) this.key.getBytes().length), 0, commonHeader,
		    Utils.SIZE_OF_PACKET_TYPE + Utils.SIZE_OF_UUID + Utils.SIZE_OF_BLOB_PAYLOAD_LENGTH,
		    Utils.SIZE_OF_KEY_LENGTH);

	    int indexMetadata = 0;

	    while (indexMetadata < metadataToSend.length) {
		int maxPayloadSize_copy = maxPayloadSize;
		if (maxPayloadSize_copy + indexMetadata > metadataToSend.length) {
		    maxPayloadSize_copy = metadataToSend.length - indexMetadata;
		}

		byte[] packet = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER + maxPayloadSize_copy
			+ this.key.getBytes().length];

		// 1. fragment offset
		System.arraycopy(Utils.intToByteArray(indexMetadata), 0, packet, Utils.FRAGMENT_OFFSET_START_INDEX,
			Utils.SIZE_OF_FRAGMENT_OFFSET);
		// Fields 2,3,4,5 from commonHeader:packet type, uuid, blob metadata Length,
		// keyLength
		System.arraycopy(commonHeader, 0, packet, Utils.PACKET_TYPE_START_INDEX, commonHeader.length);
		// payload checksum
		System.arraycopy(this.metadataChecksum, 0, packet, Utils.PAYLOAD_CHECKSUM_START_INDEX,
			Utils.SIZE_OF_PAYLOAD_CHECKSUM);
		// the key
		System.arraycopy(this.key.getBytes(), 0, packet, Utils.KEY_START_INDEX, this.key.getBytes().length);
		// the payload metadata
		System.arraycopy(metadataToSend, indexMetadata, packet,
			Utils.KEY_START_INDEX + this.key.getBytes().length, maxPayloadSize_copy);
		// the packet checksum
		System.arraycopy(
			Utils.calculateChecksum(
				Arrays.copyOfRange(packet, 0, packet.length - Utils.SIZE_OF_PACKET_CHECKSUM)),
			0, packet, Utils.KEY_START_INDEX + this.key.getBytes().length + maxPayloadSize_copy,
			Utils.SIZE_OF_PACKET_CHECKSUM);
		// send the metadata packet
		Utils.sendFragmentMulticast(packet, targetIp, port);

		indexMetadata = indexMetadata + maxPayloadSize;
	    }

	} else if (packetType == DATA_CODE) {
	    // fragment [missingBlock.first, missingBlock.second]
	    // build packet
	    // Utils.sendFragmentMulticast(packet, targetIp, port);
	    byte[] payloadToSend = new byte[missingBlock.second - missingBlock.first];
	    System.arraycopy(this.payload, missingBlock.first, payloadToSend, 0,
		    missingBlock.second - missingBlock.first);
	    /*
	     * fragment metadata
	     */
	    byte[] commonHeader = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER - Utils.SIZE_OF_FRAGMENT_OFFSET
		    - Utils.SIZE_OF_PAYLOAD_CHECKSUM];
	    // 2. 1 byte, packet type or flags
	    commonHeader[0] = DATA_CODE;
	    // 3. 16 bytes, uuid
	    System.arraycopy(Utils.getBytes(this.uuid), 0, commonHeader, Utils.SIZE_OF_PACKET_TYPE, Utils.SIZE_OF_UUID);
	    // 4. 4 bytes, blob payload Length
	    System.arraycopy(Utils.intToByteArray(this.payload.length), 0, commonHeader,
		    Utils.SIZE_OF_PACKET_TYPE + Utils.SIZE_OF_UUID, Utils.SIZE_OF_BLOB_PAYLOAD_LENGTH);
	    // 5. 2 bytes, keyLength
	    System.arraycopy(Utils.shortToByteArray((short) this.key.getBytes().length), 0, commonHeader,
		    Utils.SIZE_OF_PACKET_TYPE + Utils.SIZE_OF_UUID + Utils.SIZE_OF_BLOB_PAYLOAD_LENGTH,
		    Utils.SIZE_OF_KEY_LENGTH);

	    int indexPayload = 0;

	    while (indexPayload < payloadToSend.length) {
		int maxPayloadSize_copy = maxPayloadSize;
		if (maxPayloadSize_copy + indexPayload > payloadToSend.length) {
		    maxPayloadSize_copy = payloadToSend.length - indexPayload;
		}

		byte[] packet = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER + maxPayloadSize_copy
			+ this.key.getBytes().length];

		// 1. fragment offset
		System.arraycopy(Utils.intToByteArray(indexPayload), 0, packet, Utils.FRAGMENT_OFFSET_START_INDEX,
			Utils.SIZE_OF_FRAGMENT_OFFSET);
		// Fields 2,3,4,5 from commonHeader:packet type, uuid, blob metadata Length,
		// keyLength
		System.arraycopy(commonHeader, 0, packet, Utils.PACKET_TYPE_START_INDEX, commonHeader.length);
		// payload checksum
		System.arraycopy(this.payloadChecksum, 0, packet, Utils.PAYLOAD_CHECKSUM_START_INDEX,
			Utils.SIZE_OF_PAYLOAD_CHECKSUM);
		// the key
		System.arraycopy(this.key.getBytes(), 0, packet, Utils.KEY_START_INDEX, this.key.getBytes().length);
		// the payload metadata
		System.arraycopy(payloadToSend, indexPayload, packet,
			Utils.KEY_START_INDEX + this.key.getBytes().length, maxPayloadSize_copy);
		// the packet checksum
		System.arraycopy(
			Utils.calculateChecksum(
				Arrays.copyOfRange(packet, 0, packet.length - Utils.SIZE_OF_PACKET_CHECKSUM)),
			0, packet, Utils.KEY_START_INDEX + this.key.getBytes().length + maxPayloadSize_copy,
			Utils.SIZE_OF_PACKET_CHECKSUM);

		// send the metadata packet
		Utils.sendFragmentMulticast(packet, targetIp, port);

		indexPayload = indexPayload + maxPayloadSize;
	    }
	} else {
	    throw new IOException("Packet type not recognized!");
	}
    }

    /**
     * Send method - fragments a blob into smaller serialized fragmentedBlobs and
     * sends them via UDP multicast. Reads the maxPayloadSize from a file <--TODO
     *
     * @param targetIp - Destination multicast IP
     * @param port     - Socket port number
     * @throws IOException, NoSuchAlgorithmException
     */
    public void send(String targetIp, int port) throws NoSuchAlgorithmException, IOException {
	String maxPayloadSizeEnvValue = System.getenv("MAX_PAYLOAD_SIZE");
	int maxPayloadSize = 1200;
	try {
	    maxPayloadSize = Integer.parseInt(maxPayloadSizeEnvValue);
	} catch (NumberFormatException e) {
	    this.logger.log(Level.WARNING, "Environment variable MAX_PAYLOAD_SIZE was not set or is not a number.");
	}

	if (maxPayloadSize > this.payload.length + this.metadata.length) {
	    // no need to fragment the Blob
	    byte[] metadataAndPayload = new byte[this.payload.length + this.metadata.length];

	    System.arraycopy(this.metadata, 0, metadataAndPayload, 0, this.metadata.length);
	    System.arraycopy(this.payload, 0, metadataAndPayload, this.metadata.length, this.payload.length);

	    byte[] packet = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER + metadataAndPayload.length
		    + this.key.getBytes().length];
	    // Fill the fields:
	    // 1. fragment offset will always be zero
	    System.arraycopy(Utils.intToByteArray(0), 0, packet, Utils.FRAGMENT_OFFSET_START_INDEX,
		    Utils.SIZE_OF_FRAGMENT_OFFSET);

	    // 2. 1 byte, packet type
	    packet[Utils.PACKET_TYPE_START_INDEX] = SMALL_BLOB_CODE;

	    // 3. 16 bytes, uuid
	    System.arraycopy(Utils.getBytes(this.uuid), 0, packet, Utils.UUID_START_INDEX, Utils.SIZE_OF_UUID);

	    // 4. 4 bytes, blob payload + metadata Length
	    System.arraycopy(Utils.intToByteArray(this.payload.length), 0, packet,
		    Utils.BLOB_PAYLOAD_LENGTH_START_INDEX, Utils.SIZE_OF_BLOB_PAYLOAD_LENGTH);

	    // 5. 2 bytes, keyLength
	    System.arraycopy(Utils.shortToByteArray((short) this.key.getBytes().length), 0, packet,
		    Utils.KEY_LENGTH_START_INDEX, Utils.SIZE_OF_KEY_LENGTH);

	    // 6. payload checksum
	    System.arraycopy(this.payloadChecksum, 0, packet, Utils.PAYLOAD_CHECKSUM_START_INDEX,
		    Utils.SIZE_OF_PAYLOAD_CHECKSUM);

	    // 7. the key
	    System.arraycopy(this.key.getBytes(), 0, packet, Utils.KEY_START_INDEX, this.key.getBytes().length);

	    // 8. the payload and metadata
	    System.arraycopy(metadataAndPayload, 0, packet, Utils.KEY_START_INDEX + this.key.getBytes().length,
		    metadataAndPayload.length);

	    // 9. the packet checksum
	    System.arraycopy(
		    Utils.calculateChecksum(
			    Arrays.copyOfRange(packet, 0, packet.length - Utils.SIZE_OF_PACKET_CHECKSUM)),
		    0, packet, Utils.KEY_START_INDEX + this.key.getBytes().length + metadataAndPayload.length,
		    Utils.SIZE_OF_PACKET_CHECKSUM);

	    // send the metadata packet
	    Utils.sendFragmentMulticast(packet, targetIp, port);
	} else {
	    send(maxPayloadSize, new Pair(0, this.metadata.length), METADATA_CODE, targetIp, port);
	    send(maxPayloadSize, new Pair(0, this.payload.length), DATA_CODE, targetIp, port);
	}
    }

    /**
     * isComplete method - checks if a Blob is completely received
     *
     * @return boolean true if the Blob is Complete
     * 
     * @throws NoSuchAlgorithmException, IOException
     */
    public boolean isComplete() throws IOException, NoSuchAlgorithmException {
	if (this.metadata == null || this.payload == null) {
		System.out.println("case 1");
		return false;
	}

	// Check byte ranges size:
	if (this.payloadByteRanges.size() != 1 || this.metadataByteRanges.size() != 1) {
		System.out.println("case 2: " + this.payloadByteRanges.size() + " " + this.metadataByteRanges.size());
		System.out.println(this.payloadByteRanges);
	    return false;
	}
	// Check byte ranges metadata:
	if (this.metadataByteRanges.get(0).first != 0
		|| this.metadataByteRanges.get(0).second != this.metadata.length) {
		System.out.println("case 3");
	    return false;
	}

	// Check byte ranges payload:
	if (this.payloadByteRanges.get(0).first != 0 || this.payloadByteRanges.get(0).second != this.payload.length) {
		System.out.println("case 4");
	    return false;
	}

	// Verify checksums
	if (!Arrays.equals(this.payloadChecksum, Utils.calculateChecksum(this.payload))) {
		System.out.println("case 5");
	    throw new IOException("Payload checksum failed");
	}

	if (!Arrays.equals(this.metadataChecksum, Utils.calculateChecksum(this.metadata))) {
		System.out.println("case 6");
	    throw new IOException("Metadata checksum failed");
	}
	System.out.println("case 77");
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
		this.logger.log(Level.WARNING,
			"metadata and payload byte arrays should be null for an empty SMALL BLOB");
	    }
	} else {
	    throw new IOException("Packet type not recognized!");
	}
    }

    public ArrayList<Pair> getMissingMetadataBlocks() {
	if (this.metadata == null) {
	    return null;
	}

	Collections.sort(this.metadataByteRanges, new Utils.PairComparator());

	ArrayList<Pair> missingBlocks = new ArrayList<>();

	for (int i = 0; i < this.metadataByteRanges.size() - 1; i++) {
	    if (this.metadataByteRanges.get(i).second != this.metadataByteRanges.get(i + 1).first) {
		missingBlocks
			.add(new Pair(this.metadataByteRanges.get(i).second, this.metadataByteRanges.get(i + 1).first));
	    }
	}

	return missingBlocks;

    }

    public ArrayList<Pair> getMissingPayloadBlocks() {
	if (this.payload == null) {
	    return null;
	}

	Collections.sort(this.payloadByteRanges, new Utils.PairComparator());

	ArrayList<Pair> missingBlocks = new ArrayList<>();

	for (int i = 0; i < this.payloadByteRanges.size() - 1; i++) {
	    if (this.payloadByteRanges.get(i).second != this.payloadByteRanges.get(i + 1).first) {
		missingBlocks
			.add(new Pair(this.payloadByteRanges.get(i).second, this.payloadByteRanges.get(i + 1).first));
	    }
	}

	return missingBlocks;
    }

    public void addByteRange(byte[] data, Pair missingBlock) {
	if (this.payload == null) {
	    // getting the entire payload blob
	    this.payload = new byte[data.length];
	}
	System.arraycopy(data, 0, this.payload, missingBlock.first, data.length);
    }

    public String getKey() {
	return this.key;
    }

    public UUID getUuid() {
	return this.uuid;
    }

    public Map<String, String> getMetadataMap() {
	return Utils.deserializeMetadata(this.metadata);
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

    public Timestamp getTimestamp() {
	return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
	this.timestamp = timestamp;
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

    @Override
    public boolean equals(Object o) {

	// If the object is compared with itself then return true
	if (o == this) {
	    return true;
	}

	/*
	 * Check if o is an instance of Complex or not "null instanceof [type]" also
	 * returns false
	 */
	if (!(o instanceof Blob)) {
	    return false;
	}

	// typecast o to Complex so that we can compare data members
	Blob blob = (Blob) o;

	// Verify payload
	if (!this.key.equals(blob.getKey())) {
	    return false;
	}

	// Verify uuid
	if (!this.uuid.equals(blob.getUuid())) {
	    return false;
	}

	// Verify payload
	if (!Arrays.equals(this.payload, blob.getPayload())) {
	    return false;
	}

	// Verify metadata
	if (!Arrays.equals(this.metadata, blob.getMetadata())) {
	    return false;
	}

	return true;
    }
}
