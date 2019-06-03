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
import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;

import myjava.com.github.dosarudaniel.gsoc.Utils.Pair;
import myjava.com.github.dosarudaniel.gsoc.Utils.PairComparator;

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
    public final static int SMALL_BLOB_CODE = 2;

    public enum PACKET_TYPE {
	METADATA, DATA, SMALL_BLOB_CODE;
    }

    private UUID uuid;
    private byte[] payloadAndMetadataChecksum;
    private String key;
    private byte[] metadata;
    private byte[] payload;

    private TreeSet<Utils.Pair> metadataByteRanges;
    private TreeSet<Utils.Pair> payloadByteRanges;

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
     */
    public Blob(byte[] metadata, byte[] payload, String key, UUID uuid) throws NoSuchAlgorithmException {
	this.metadata = metadata;
	this.payload = payload;
	this.key = key;
	this.uuid = uuid;
	byte[] metadataAndPayload = new byte[metadata.length + payload.length];
	System.arraycopy(metadata, 0, metadataAndPayload, 0, metadata.length);
	System.arraycopy(payload, 0, metadataAndPayload, metadata.length, payload.length);
	this.payloadAndMetadataChecksum = Utils.calculateChecksum(metadataAndPayload);
	this.metadataByteRanges = new TreeSet<>(new PairComparator());
	this.metadataByteRanges.add(new Pair(0, this.metadata.length));
	this.payloadByteRanges = new TreeSet<>(new PairComparator());
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
     */
    public Blob() {
	this.metadata = null;
	this.payload = null;
	this.key = "";
	this.uuid = null;
	this.payloadAndMetadataChecksum = null;
	this.metadataByteRanges = new TreeSet<>(new PairComparator());
	this.payloadByteRanges = new TreeSet<>(new PairComparator());
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

	    byte[] commonHeader = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER - Utils.SIZE_OF_FRAGMENT_OFFSET
		    - Utils.SIZE_OF_PAYLOAD_CHECKSUM];
	    byte[] packetType_byte_array = new byte[1];
	    packetType_byte_array[0] = (byte) SMALL_BLOB_CODE;

	    byte[] blobPayloadLength_byte_array = ByteBuffer.allocate(4).putInt(this.payload.length).array();
	    byte[] keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) this.key.length()).array();
	    byte[] fragmentOffset_byte_array;

	    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		// 2. 1 byte, packet type or flags - to be decided
		out.write(packetType_byte_array);
		// 3. 16 bytes, uuid
		out.write(Utils.getBytes(this.uuid));
		// 4. 4 bytes, blob payload + metadata Length
		out.write(blobPayloadLength_byte_array);
		// 5. 2 bytes, keyLength
		out.write(keyLength_byte_array);

		commonHeader = out.toByteArray();
	    }

	    byte[] metadataAndPayloadFragment = new byte[this.payload.length + this.metadata.length];

	    System.arraycopy(this.metadata, 0, metadataAndPayloadFragment, 0, this.metadata.length);
	    System.arraycopy(this.payload, 0, metadataAndPayloadFragment, this.metadata.length, this.payload.length);

	    byte[] packet = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER
		    + metadataAndPayloadFragment.length + this.key.length()];
	    // Offset zero
	    fragmentOffset_byte_array = ByteBuffer.allocate(4).putInt(0).array();

	    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		// fragment offset
		out.write(fragmentOffset_byte_array);
		// Common header: packet type + uuid + blob metadata Length + keyLength
		out.write(commonHeader);
		// payload checksum
		out.write(this.payloadAndMetadataChecksum);
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

	    while (indexMetadata < this.metadata.length) {
		int maxPayloadSize_copy = maxPayloadSize;
		if (maxPayloadSize_copy + indexMetadata > this.metadata.length) {
		    maxPayloadSize_copy = this.metadata.length - indexMetadata;
		}

		byte[] metadataFragment = new byte[maxPayloadSize_copy];
		System.arraycopy(this.metadata, indexMetadata, metadataFragment, 0, maxPayloadSize_copy);
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
	    while (indexPayload < this.payload.length) {
		int maxPayloadSize_copy = maxPayloadSize;
		if (maxPayloadSize_copy + indexPayload > this.payload.length) {
		    maxPayloadSize_copy = this.payload.length - indexPayload;
		}

		byte[] payloadFragment = new byte[maxPayloadSize_copy];
		System.arraycopy(this.payload, indexPayload, payloadFragment, 0, maxPayloadSize_copy);

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

    /**
     * toBytes method - serialize a Blob into a fragmentedBlob with (metadata and
     * payload)
     * 
     *
     * @param maxPayloadSize - The maximum payload size of the serialized
     *                       fragmentedBlob
     * @param targetIp       - Destination multicast IP
     * @param port           - Socket port number
     * 
     * @throws NoSuchAlgorithmException, IOException
     */
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
	if (this.metadata == null || this.payload == null) {
	    return false;
	}

	Iterator<Pair> iterator = this.metadataByteRanges.iterator();
	int index = 0;
	while (iterator.hasNext()) {
	    Pair pair = iterator.next();
	    if (index != pair.first) {
		return false;
	    }
	    index = pair.second;
	}

	if (index != this.metadata.length) {
	    return false;
	}

	iterator = this.payloadByteRanges.iterator();
	index = 0;
	while (iterator.hasNext()) {
	    Pair pair = iterator.next();
	    if (index != pair.first) {
		return false;
	    }
	    index = pair.second;
	}

	if (index != this.payload.length) {
	    return false;
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

	if (fragmentedBlob.getPachetType() == PACKET_TYPE.DATA) {
	    if (this.payload == null) {
		this.payload = new byte[fragmentedBlob.getBlobPayloadLength()];
	    }
	    System.arraycopy(fragmentedPayload, 0, this.payload, fragmentOffset, fragmentedPayload.length);
	    Pair pair = new Pair(fragmentOffset, fragmentOffset + fragmentedPayload.length);
	    this.payloadByteRanges.add(pair);
	} else if (fragmentedBlob.getPachetType() == PACKET_TYPE.METADATA) {
	    if (this.metadata == null) {
		this.metadata = new byte[fragmentedBlob.getBlobPayloadLength()];
	    }
	    System.arraycopy(fragmentedPayload, 0, this.metadata, fragmentOffset, fragmentedPayload.length);
	    Pair pair = new Pair(fragmentOffset, fragmentOffset + fragmentedPayload.length);
	    this.metadataByteRanges.add(pair);
	} else if (fragmentedBlob.getPachetType() == PACKET_TYPE.SMALL_BLOB_CODE) {
	    if (this.metadata == null && this.payload == null) {
		int metadataLength = fragmentedPayload.length - fragmentedBlob.getBlobPayloadLength();
		int payloadLength = fragmentedBlob.getBlobPayloadLength();

		this.metadata = new byte[metadataLength];
		this.payload = new byte[payloadLength];

		if (fragmentOffset != 0) { // sanity check
		    // log error
		}
		System.arraycopy(fragmentedPayload, 0, this.metadata, fragmentOffset, metadataLength);
		System.arraycopy(fragmentedPayload, metadataLength, this.payload, fragmentOffset, payloadLength);

		Pair pairPayloadByteRange = new Pair(0, payloadLength);
		Pair pairMetadataByteRange = new Pair(0, metadataLength);
		this.payloadByteRanges.add(pairPayloadByteRange);
		this.metadataByteRanges.add(pairMetadataByteRange);
	    } else {
		// log this error "metadata and payload should be null"
		System.out.println("metadata and payload should be null");
	    }
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
