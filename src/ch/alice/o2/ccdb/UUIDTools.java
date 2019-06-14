package ch.alice.o2.ccdb;

import java.util.UUID;

import alien.catalogue.GUID;
import alien.monitoring.MonitorFactory;

/**
 * Helper class to generate version 1 (time based) UUIDs. This class doesn't implement fully the object since the MAC address is hardcoded to a fixed value. Since for this project the objects are
 * generated on the server side and thus the MAC address would be that of the server, this field is anyway not relevant.
 *
 * @author costing
 * @since 2017-10-02
 */
public class UUIDTools {
	private static int clockSequence = MonitorFactory.getSelfProcessID();

	private static long lastTimestamp = System.nanoTime() / 100 + 122192928000000000L;

	private static long lastTimestamp2 = System.nanoTime() / 100 + 122192928000000000L;

	private static final byte[] macAddress = new byte[] { 0x11, 0x22, 0x33, 0x44, 0x55, 0x66 };

	/**
	 * @param referenceTime
	 *            timestamp to be encoded in the UUID
	 * @param address
	 *            client's address to encode in the UUID. Up to 6 bytes from it will be placed in the MAC address field. If not provided (or less than 6 bytes long, ie. IPv4) then a fixed value will
	 *            be used for the missing part.
	 * @return a time UUID with the reference time set to the reference time
	 */
	public static synchronized UUID generateTimeUUID(final long referenceTime, final byte[] address) {
		final long time = referenceTime * 10000 + 122192928000000000L;

		if (time <= lastTimestamp2 || time <= lastTimestamp) {
			clockSequence++;

			if (clockSequence >= 65535)
				clockSequence = 0;
		}

		lastTimestamp2 = time;

		final byte[] contents = new byte[16];

		int addressBytes = 0;

		if (address != null) {
			addressBytes = Math.min(address.length, 6);

			for (int i = 0; i < addressBytes; i++)
				contents[10 + i] = address[i];
		}

		for (int i = addressBytes; i < 6; i++)
			contents[10 + i] = macAddress[i];

		final int timeHi = (int) (time >>> 32);
		final int timeLo = (int) time;

		contents[0] = (byte) (timeLo >>> 24);
		contents[1] = (byte) (timeLo >>> 16);
		contents[2] = (byte) (timeLo >>> 8);
		contents[3] = (byte) (timeLo);

		contents[4] = (byte) (timeHi >>> 8);
		contents[5] = (byte) timeHi;
		contents[6] = (byte) (timeHi >>> 24);
		contents[7] = (byte) (timeHi >>> 16);

		contents[8] = (byte) (clockSequence >> 8);
		contents[9] = (byte) clockSequence;

		contents[6] &= (byte) 0x0F;
		contents[6] |= (byte) 0x10;

		contents[8] &= (byte) 0x3F;
		contents[8] |= (byte) 0x80;

		return GUID.getUUID(contents);
	}
}
