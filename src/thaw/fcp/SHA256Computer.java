package thaw.fcp;

import java.util.Observer;
import java.util.Observable;

import java.io.FileInputStream;
import java.io.File;

import java.security.MessageDigest;

import freenet.crypt.SHA256;
import freenet.support.Base64;

import thaw.core.Logger;


/**
 * Automatically used by FCPClientPut.
 * You shouldn't have to bother about it
 */
public class SHA256Computer extends Observable implements Runnable {
	private MessageDigest md;

	private String hash;
	private final Object hashLock = new Object();
	private final String file;
	private final String headers;

	public final static int BLOCK_SIZE = 32768; /* 32 Ko */

	public SHA256Computer(String header, String fileToHash) {
		this.file = fileToHash;
		this.headers = header;
	}


	public void run() {
		try {
			FileInputStream in = new FileInputStream(new File(file));
			md = SHA256.getMessageDigest();
			md.reset();
			md.update(headers.getBytes("UTF-8"));
			SHA256.hash(in, md);
			
			synchronized (hashLock) {
				hash = Base64.encode(md.digest());	
			}

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
		synchronized (hashLock) {
			return hash;	
		}
	}
}
