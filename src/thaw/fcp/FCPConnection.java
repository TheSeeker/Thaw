package thaw.fcp;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
/* import java.io.BufferedReader; */
import java.io.BufferedInputStream;
import java.util.Observable;

/* Should be the only real dep of the FCP package */
import thaw.core.Logger;


/**
 * This object manages directly the socket attached to the node.
 * After being instanciated, you should commit it to the FCPQueryManager, and then
 * commit the FCPQueryManager to the FCPQueueManager.
 * Call observer when connected / disconnected.
 * WARNING: This FCP implement don't guarantee that messages are sent in the same order than initally put
 *          if the lock on writting is not set !
 * TODO: Add functions socketToFile(long size, File file) / fileToSocket(File file)
 */
public class FCPConnection extends Observable {
	/** If == true, then will print on stdout
	 * all fcp input / output.
	 */
	private final static boolean DEBUG_MODE = true;
	private final static int MAX_RECV = 1024;

	private byte[] recvBytes = new byte[MAX_RECV]; /* global to avoid each time free() / malloc() */

	private FCPBufferedStream bufferedOut = null;
	private int maxUploadSpeed = 0;

	private String nodeAddress = null;
	private int port = 0;

	private Socket socket = null;
	private InputStream in = null;
	private OutputStream out = null;

	private BufferedInputStream reader = null;

	private long rawBytesWaiting = 0;


	private boolean lockWriting = false;
	private boolean lockReading = false;

	private long lastWrite = 0; /* real writes ; System.currentTimeMillis() */


	/**
	 * Don't connect. Call connect() for that.
	 * @param maxUploadSpeed in KB: -1 means no limit
	 */
	public FCPConnection(String nodeAddress,
			     int port,
			     int maxUploadSpeed)
	{
		if(DEBUG_MODE) {
			Logger.notice(this, "DEBUG_MODE ACTIVATED");
		}

		setNodeAddress(nodeAddress);
		setNodePort(port);
		setMaxUploadSpeed(maxUploadSpeed);
	}


	public void setNodeAddress(String nodeAddress) {
		this.nodeAddress = nodeAddress;
	}

	public void setNodePort(int port) {
		this.port = port;
	}

	public void setMaxUploadSpeed(int max) {
		this.maxUploadSpeed = max;
	}
	
	
	public void disconnect() {
		try {
		    if(isConnected())
			    socket.close();
		    else {
			    Logger.info(this, "Disconnect(): Already disconnected.");
		    }
		} catch(java.io.IOException e) {
			Logger.warning(this, "Unable to close cleanly the connection : "+e.toString());
		}

		socket = null;
		in = null;
		out = null;
		if(bufferedOut != null) {
			bufferedOut.stopSender();
			bufferedOut = null;
		}

		setChanged();
		notifyObservers();
	}


	/**
	 * If already connected, disconnect before connecting.
	 * @return true if successful
	 */
	public boolean connect() {
		if(nodeAddress == null || port == 0) {
			Logger.warning(this, "Address or port not defined ! Unable to connect\n");
			return false;
		}
		
		Logger.info(this, "Connection to "+nodeAddress+":"+ Integer.toString(port) +"...");

		if(socket != null && !socket.isClosed())
			disconnect();

		try {
			socket = new Socket(nodeAddress, port);

		} catch(java.net.UnknownHostException e) {
			Logger.error(this, "Error while trying to connect to "+nodeAddress+":"+port+" : "+
				     e.toString());
			socket = null;
			return false;
		} catch(java.io.IOException e) {
			Logger.error(this, "Error while trying to connect to "+nodeAddress+":"+port+" : "+
				     e.toString());
			socket = null;
			return false;
		}
		
		if(!socket.isConnected()) {
			Logger.warning(this, "Unable to connect, but no exception ?! WTF ?\n");
			Logger.warning(this, "Will try to continue ...\n");
		}
		
		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch(java.io.IOException e) {
			Logger.error(this, "Socket and connection established, but unable to get in/output streams ?! : "+e.toString());
			return false;
		}

		reader = new BufferedInputStream(in);
		bufferedOut = new FCPBufferedStream(this, maxUploadSpeed);
		bufferedOut.startSender();

		Logger.info(this, "Connected");

		setChanged();
		notifyObservers();
		
		return true;
	}

	public boolean isOutputBufferEmpty() {
		return bufferedOut.isOutputBufferEmpty();
	}
	
	public boolean isConnected() {
		if(socket == null)
			return false;
		else
			return socket.isConnected();
	}


	public synchronized boolean lockWriting() {
		if(lockWriting) {
			Logger.notice(this, "Writing already locked! You can't lock it !");
			return false;
		}

		Logger.debug(this, "Lock writing ...");
		lockWriting = true;

		return true;
	}
	
	public synchronized boolean lockReading() {
		if(lockReading) {
			Logger.notice(this, "Reading already locked! You can't lock it !");
			return false;
		}

		Logger.debug(this, "Lock reading");
		lockReading = true;

		return true;
	}

	public synchronized void unlockWriting() {
		if(!lockWriting) {
			Logger.notice(this, "Writing already unlocked !");
			return;
		}

		Logger.debug(this, "Unlock writting");
		lockWriting = false;
	}

	public synchronized void unlockReading() {
		if(!lockReading) {
			Logger.notice(this, "Reading already unlocked !");
			return;
		}

		Logger.debug(this, "Unlock reading");
		lockReading = false;
	}

	public boolean isWritingLocked() {
		return lockWriting;
	}

	public boolean isReadingLocked() {
		return lockReading;
	}

	/**
	 * Doesn't check the lock state ! You have to manage it yourself.
	 */
	public boolean rawWrite(byte[] data) {
		if(bufferedOut != null)
			return bufferedOut.write(data);
		else {
			Logger.notice(this, "rawWrite(), bufferedOut == null ? Socket closed ?");
			return false;
		}
	}

	/**
	 * Should be call by FCPBufferedStream. Not you.
	 */
	public boolean realRawWrite(byte[] data) {
		if(out != null && socket != null && socket.isConnected()) {
			try {
				lastWrite = System.currentTimeMillis();
				out.write(data);
			} catch(java.io.IOException e) {
				Logger.warning(this, "Unable to write() on the socket ?! : "+ e.toString());
				disconnect();
				return false;
			}
		} else {
			Logger.warning(this, "Cannot write if disconnected !\n");
			return false;
		}

		return true;
	}

	
	public boolean isWriting() {
		if( !isConnected() )
			return false;

		return ( isWritingLocked() || ((System.currentTimeMillis() - lastWrite) < 300) );
	}

	public boolean write(String toWrite) {
		return write(toWrite, true);
	}

	public boolean write(String toWrite, boolean checkLock) {

		if(checkLock && isWritingLocked()) {
			Logger.verbose(this, "Writting lock, unable to write.");
		}

		while(checkLock && isWritingLocked()) {
			try {
				Thread.sleep(200);
			} catch(java.lang.InterruptedException e) {
				/* On s'en fout, mais alors d'une force ... */
			}
		}

		Logger.asIt(this, "Thaw >>> Node :");
		Logger.asIt(this, toWrite);


		if(out != null && socket != null && socket.isConnected()) {
			try {
				bufferedOut.write(toWrite.getBytes("UTF-8"));
			} catch(java.io.UnsupportedEncodingException e) {
				Logger.error(this, "UNSUPPORTED ENCODING EXCEPTION : UTF-8");
				bufferedOut.write(toWrite.getBytes());
			}
		} else {
			Logger.warning(this, "Cannot write if disconnected !\n");
			return false;
		}
		return true;
	}

	/**
	 * For security : FCPQueryManager uses this function to tells FCPConnection
	 * how many raw bytes are waited (to avoid to *serious* problems).
	 */
	public void setRawDataWaiting(long waiting) {
		rawBytesWaiting = waiting;
	}

	/**
	 * @see read(int, byte[])
	 */
	public int read(byte[] buf) {
		return read(buf.length, buf);
	}

	/**
	 * @param lng Obsolete.
	 */
	public synchronized int read(int lng, byte[] buf) {
		int rdBytes = 0;
		try {
			rdBytes = reader.read(buf);

			rawBytesWaiting = rawBytesWaiting - rdBytes;

			//Logger.verbose(this, "Remaining: "+rawBytesWaiting);

			return rdBytes;
		} catch(java.io.IOException e) {
			Logger.error(this, "IOException while reading raw bytes on socket, will probably cause troubles");
			Logger.error(this, e.getMessage() + ":" +e.getCause());
			System.exit(3);
			return -2; /* -1 can mean eof */
		}

	}

	/**
	 * Read a line.
	 * @return null if disconnected or error
	 */
	public String readLine() {
		


		/* SECURITY */
		if(rawBytesWaiting > 0) {
			Logger.error(this, "RAW BYTES STILL WAITING ON SOCKET. THIS IS ABNORMAL.");
			Logger.error(this, "Will drop them.");
			
			while(rawBytesWaiting > 0) {
				int to_read = 1024;
				
				if(to_read > rawBytesWaiting)
					to_read = (int)rawBytesWaiting;

				byte[] read = new byte[to_read];
				read(to_read, read);

				rawBytesWaiting = rawBytesWaiting - to_read;
			}

		}



		String result;
		
		if(in != null && reader != null && socket != null && socket.isConnected()) {
			try {
				for(int i = 0; i < recvBytes.length ; i++)
					recvBytes[i] = 0;

				result = "";
				
				int c = 0;
				int i = 0; /* position in recvBytes */

				while(c != '\n' && i < recvBytes.length) {
					c = reader.read();

					if(c == -1) {
						if(isConnected())
							Logger.error(this, "Unable to read but still connected");
						else
							Logger.notice(this, "Disconnected");

						disconnect(); /* will warn everybody */

						return null;
					}
					
					if(c == '\n')
						break;

					//result = result + new String(new byte[] { (byte)c });

					recvBytes[i] = (byte)c;
					i++;
				}

				result = new String(recvBytes, 0, i, "UTF-8");

				if(DEBUG_MODE) {
					if(result.matches("[\\-\\ \\?.a-zA-Z0-9\\,~%@/_=\\[\\]\\(\\)]*"))
						Logger.asIt(this, "Thaw <<< Node : "+result);
					else
						Logger.asIt(this, "Thaw <<< Node : Unknow chars in message. Not displayed");
				}

				
				return result;

			} catch (java.io.IOException e) {
				if(isConnected()) {
					Logger.error(this, "IOException while reading but still connected, wtf? : "+e.toString());
				} else
					Logger.notice(this, "IOException. Disconnected.");

				disconnect();

				return null;
			}
		} else {
			Logger.warning(this, "Cannot read if disconnected => null");
		}

		return null;
	}

}
