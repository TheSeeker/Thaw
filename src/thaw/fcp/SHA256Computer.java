package thaw.fcp;

import java.util.Observer;
import java.util.Observable;

import java.io.FileInputStream;

import java.security.MessageDigest;

import freenet.crypt.SHA256;
import freenet.support.Base64;

import thaw.core.Logger;


/**
 * Automatically used by FCPClientPut.
 * You shouldn't have to bother about it
 */
public class SHA256Computer extends Observable implements Runnable {
	private SHA256 sha;
	private MessageDigest md;

	private String file;
	private String hash;

	public final static int BLOCK_SIZE = 32768; /* 32 Ko */

	public SHA256Computer(String header, String fileToHash) {
		file = fileToHash;

		sha = new SHA256();

		md = sha.getMessageDigest();
		md.reset();

		try {
			md.update(header.getBytes("UTF-8"));
		} catch(java.io.UnsupportedEncodingException e) {
			md.update(header.getBytes());
		}
	}


	public void run() {
		try {
			FileInputStream in = new FileInputStream(file);

			SHA256.hash(in, md);

			hash = Base64.encode(md.digest());

			SHA256.returnMessageDigest(md);

		} catch(java.io.FileNotFoundException e) {
			Logger.error(this, "Can't hash file because: "+e.toString());
		} catch(java.io.IOException e) {
			Logger.error(this, "Can't hash file because: "+e.toString());
		}

		setChanged();
		notifyObservers();
	}


	/**
	 * In %
	 */
	public int getProgression() {
		return 0;
	}

	/**
	 * Returns the Base64Encode of the hash
	 */
	public String getHash() {
		return hash;
	}
}
