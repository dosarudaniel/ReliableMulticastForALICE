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

	public int getFragmentOffset() {
		return this.fragmentOffset;
	}

	public void setFragmentOffset(int fragmentOffset) {
		this.fragmentOffset = fragmentOffset;
	}

}
