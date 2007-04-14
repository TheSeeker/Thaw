package thaw.fcp;

import java.util.Observer;
import java.util.Observable;

import java.io.FileInputStream;

import freenet.crypt.SHA256;
import freenet.support.Base64;

import thaw.core.Logger;

public class SHA256Computer extends Observable implements Runnable {
	private SHA256 sha;

	private String file;

	public SHA256Computer(String header, String fileToHash) {
		file = fileToHash;

		sha = new SHA256();

		sha.update(header.getBytes());
	}


	public void run() {
		try {
			FileInputStream in = new FileInputStream(file);

			byte[] raw = new byte[32768]; /* 32 Ko */

			while(in.available() > 0) {
				in.read(raw);
				sha.update(raw);
			}

			in.close();
		} catch(java.io.FileNotFoundException e) {
			Logger.error(this, "Can't hash file because: "+e.toString());
			sha = null;
		} catch(java.io.IOException e) {
			Logger.error(this, "Can't hash file because: "+e.toString());
			sha = null;
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
		if (sha != null)
			return Base64.encode(sha.digest());
		else {
			Logger.warning(this, "No hash !");
			return null;
		}
	}
}
