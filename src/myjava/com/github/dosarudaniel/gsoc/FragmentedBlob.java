package myjava.com.github.dosarudaniel.gsoc;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class FragmentedBlob extends Blob {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int fragmentOffset;

	public FragmentedBlob(String payload) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		super(payload);
	}

	public FragmentedBlob(byte[] payload, int fragmentOffset, PACHET_TYPE pachetType, String key)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		super(payload, pachetType, key);
		this.fragmentOffset = fragmentOffset;
	}

	public FragmentedBlob(byte[] serialisedFragmentedBlob) {
		super(serialisedFragmentedBlob);
		// TODO
	}

	public int getFragmentOffset() {
		return this.fragmentOffset;
	}

	public void setFragmentOffset(int fragmentOffset) {
		this.fragmentOffset = fragmentOffset;
	}

}
