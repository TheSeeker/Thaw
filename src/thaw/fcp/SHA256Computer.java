package thaw.fcp;

import java.util.Observable;

import java.io.BufferedInputStream;
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
	private short progress = 0;
	private boolean isFinished = false;

	public final static int BLOCK_SIZE = 32768; /* 32 Ko */

	public SHA256Computer(String header, String fileToHash) {
		this.file = fileToHash;
		this.headers = header;
	}


	public void run() {
		File realFile = new File(file);
		long realFileSize = realFile.length();
		
		try {
			FileInputStream in = new FileInputStream(realFile);
			BufferedInputStream bis = new BufferedInputStream(in);
			md = SHA256.getMessageDigest();
			md.reset();
			md.update(headers.getBytes("UTF-8"));
			
			byte[] buf = new byte[4096];
			int readBytes = bis.read(buf);
			while(readBytes > -1) {
				md.update(buf, 0, readBytes);
				readBytes = bis.read(buf);
				progress = (short) Math.round(readBytes * 100 / realFileSize);
				notifyObservers();
			}
			
			bis.close();
			in.close();
			
			synchronized (hashLock) {
				hash = Base64.encode(md.digest());	
			}
			isFinished = true;
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
		if(isFinished)
			return 100;
		else if(progress > 99)
			return 99;
		else 
			return progress;
	}

	/**
	 * Returns the Base64Encode of the hash
	 */
	public String getHash() {
		synchronized (hashLock) {
			return hash;	
		}
	}
	
	public boolean isFinished() {
		return isFinished;
	}
}
