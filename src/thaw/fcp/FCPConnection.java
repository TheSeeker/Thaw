package thaw.fcp;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.util.Observable;

/* Should be the only real dep of the FCP package */
import thaw.core.Logger;


/**
 * This object manages directly the socket attached to the node.
 * After being instanciated, you should commit it to the FCPQueryManager, and then
 * commit the FCPQueryManager to the FCPQueueManager.
 * Call observer when connected / disconnected.<br/>
 * WARNING: This FCP implement don't guarantee that messages are sent in the same order than initally put
 *          if the lock on writting is not set !<br/>
 * TODO: Add functions socketToStream(long size, OutputStream file) / streamToSocket(InputStream file)
 */
public class FCPConnection extends Observable {
	/** If == true, then will print on stdout
	 * all fcp input / output.
	 */
	private final static boolean DEBUG_MODE = false;
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

	private boolean duplicationAllowed = true;


	/**
	 * Don't connect. Call connect() for that.
	 * @param maxUploadSpeed in KB: -1 means no limit
	 * @param duplicationAllowed FCPClientGet and FCPClientPut will be allowed to open a separate socket to transfer the files
	 */
	public FCPConnection(String nodeAddress,
			     int port,
			     int maxUploadSpeed,
			     boolean duplicationAllowed)
	{
		if(DEBUG_MODE) {
			Logger.notice(this, "DEBUG_MODE ACTIVATED");
		}

		maxUploadSpeed = -1;

		this.setNodeAddress(nodeAddress);
		this.setNodePort(port);
		this.setMaxUploadSpeed(maxUploadSpeed);
		this.setDuplicationAllowed(duplicationAllowed);

		this.lockWriting = false;
		this.lockReading = false;
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

	public void setDuplicationAllowed(boolean allowed) {
		this.duplicationAllowed = allowed;
	}

	public void disconnect() {
		try {
		    if(this.isConnected())
			    this.socket.close();
		    else {
			    Logger.info(this, "Disconnect(): Already disconnected.");
		    }
		} catch(java.io.IOException e) {
			Logger.warning(this, "Unable to close cleanly the connection : "+e.toString() +" ; "+e.getMessage());
		}

		this.socket = null;
		this.in = null;
		this.out = null;
		if(this.bufferedOut != null) {
			this.bufferedOut.stopSender();
			this.bufferedOut = null;
		}

		this.setChanged();
		this.notifyObservers();
	}


	/**
	 * If already connected, disconnect before connecting.
	 * @return true if successful
	 */
	public boolean connect() {
		if(this.nodeAddress == null || this.port == 0) {
			Logger.warning(this, "Address or port not defined ! Unable to connect\n");
			return false;
		}

		Logger.info(this, "Connection to "+this.nodeAddress+":"+ Integer.toString(this.port) +"...");

		if(this.socket != null && !this.socket.isClosed())
			this.disconnect();

		try {
			this.socket = new Socket(this.nodeAddress, this.port);

		} catch(java.net.UnknownHostException e) {
			Logger.error(this, "Error while trying to connect to "+this.nodeAddress+":"+this.port+" : "+
				     e.toString());
			this.socket = null;
			return false;
		} catch(java.io.IOException e) {
			Logger.error(this, "Error while trying to connect to "+this.nodeAddress+":"+this.port+" : "+
				     e.toString() + " ; "+e.getMessage());
			this.socket = null;
			return false;
		}

		if(!this.socket.isConnected()) {
			Logger.warning(this, "Unable to connect, but no exception ?! WTF ?\n");
			Logger.warning(this, "Will try to continue ...\n");
		}

		try {
			this.in = this.socket.getInputStream();
			this.out = this.socket.getOutputStream();
		} catch(java.io.IOException e) {
			Logger.error(this, "Socket and connection established, but unable to get in/output streams ?! : "+e.toString()+ " ; "+e.getMessage() );
			return false;
		}

		this.reader = new BufferedInputStream(this.in);
		this.bufferedOut = new FCPBufferedStream(this, this.maxUploadSpeed);
		this.bufferedOut.startSender();

		this.rawBytesWaiting = 0;
		this.lockWriting = false;
		this.lockReading = false;
		this.lastWrite = 0;

		Logger.info(this, "Connected");

		this.setChanged();
		this.notifyObservers();

		return true;
	}

	public boolean isOutputBufferEmpty() {
		return this.bufferedOut.isOutputBufferEmpty();
	}

	public boolean isConnected() {
		if(this.socket == null)
			return false;
		else
			return this.socket.isConnected();
	}


	public synchronized boolean lockWriting() {
		if(this.lockWriting) {
			Logger.notice(this, "Writing already locked! You can't lock it !");
			return false;
		}

		Logger.debug(this, "Lock writing ...");
		this.lockWriting = true;

		return true;
	}

	public synchronized boolean lockReading() {
		if(this.lockReading) {
			Logger.notice(this, "Reading already locked! You can't lock it !");
			return false;
		}

		Logger.debug(this, "Lock reading");
		this.lockReading = true;

		return true;
	}

	public synchronized void unlockWriting() {
		if(!this.lockWriting) {
			Logger.notice(this, "Writing already unlocked !");
			return;
		}

		Logger.debug(this, "Unlock writting");
		this.lockWriting = false;
	}

	public synchronized void unlockReading() {
		if(!this.lockReading) {
			Logger.notice(this, "Reading already unlocked !");
			return;
		}

		Logger.debug(this, "Unlock reading");
		this.lockReading = false;
	}

	public boolean isWritingLocked() {
		return this.lockWriting;
	}

	public boolean isReadingLocked() {
		return this.lockReading;
	}

	/**
	 * Doesn't check the lock state ! You have to manage it yourself.
	 */
	public boolean rawWrite(byte[] data) {
		if(this.bufferedOut != null)
			return this.bufferedOut.write(data);
		else {
			Logger.notice(this, "rawWrite(), bufferedOut == null ? Socket closed ?");
			this.disconnect();
			return false;
		}
	}

	/**
	 * Should be call by FCPBufferedStream. Not you.
	 */
	public boolean realRawWrite(byte[] data) {
		if(this.out != null && this.socket != null && this.socket.isConnected()) {
			try {
				this.lastWrite = System.currentTimeMillis();

				this.out.write(data);
				this.out.flush();
			} catch(java.io.IOException e) {
				Logger.warning(this, "Unable to write() on the socket ?! : "+ e.toString()+ " ; "+e.getMessage());
				this.disconnect();
				return false;
			}
		} else {
			Logger.warning(this, "Cannot write if disconnected !\n");
			return false;
		}

		return true;
	}


	public boolean isWriting() {
		if( !this.isConnected() )
			return false;

		return ( this.isWritingLocked() || ((System.currentTimeMillis() - this.lastWrite) < 300) );
	}

	public boolean write(String toWrite) {
		return this.write(toWrite, true);
	}

	public boolean write(String toWrite, boolean checkLock) {

		if(checkLock && this.isWritingLocked()) {
			Logger.verbose(this, "Writting lock, unable to write.");
		}

		while(checkLock && this.isWritingLocked()) {
			try {
				Thread.sleep(200);
			} catch(java.lang.InterruptedException e) {
				/* On s'en fout, mais alors d'une force ... */
			}
		}

		Logger.asIt(this, "Thaw >>> Node :");
		Logger.asIt(this, toWrite);


		if(this.out != null && this.socket != null && this.socket.isConnected()) {
			try {
				this.bufferedOut.write(toWrite.getBytes("UTF-8"));
			} catch(java.io.UnsupportedEncodingException e) {
				Logger.error(this, "UNSUPPORTED ENCODING EXCEPTION : UTF-8");
				this.bufferedOut.write(toWrite.getBytes());
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
		this.rawBytesWaiting = waiting;
	}

	/**
	 * @see #read(int, byte[])
	 */
	public int read(byte[] buf) {
		return this.read(buf.length, buf);
	}

	/**
	 * @param lng Obsolete.
	 * @return -1 Disconnection.
	 */
	public synchronized int read(int lng, byte[] buf) {
		int rdBytes = 0;
		try {
			rdBytes = this.reader.read(buf);

			if(rdBytes < 0) {
				Logger.error(this, "Error while reading on the socket => disconnection");
				this.disconnect();
			}

			this.rawBytesWaiting = this.rawBytesWaiting - rdBytes;

			//Logger.verbose(this, "Remaining: "+rawBytesWaiting);

			return rdBytes;
		} catch(java.io.IOException e) {
			Logger.error(this, "IOException while reading raw bytes on socket => disconnection:");
			Logger.error(this, "   =========");
			Logger.error(this, e.getMessage() + ":");
			if (e.getCause() != null)
				Logger.error(this, e.getCause().toString());
			Logger.error(this, e.getMessage() );
			Logger.error(this, "   =========");

			this.disconnect();
			return -2; /* -1 can mean eof */
		}

	}

	/**
	 * Read a line.
	 * @return null if disconnected or error
	 */
	public String readLine() {

		/* SECURITY */
		if(this.rawBytesWaiting > 0) {
			Logger.error(this, "RAW BYTES STILL WAITING ON SOCKET. THIS IS ABNORMAL.");
			Logger.error(this, "Will drop them.");

			while(this.rawBytesWaiting > 0) {
				int to_read = 1024;

				if(to_read > this.rawBytesWaiting)
					to_read = (int)this.rawBytesWaiting;

				byte[] read = new byte[to_read];
				this.read(to_read, read);

				this.rawBytesWaiting = this.rawBytesWaiting - to_read;
			}

		}



		String result;

		if(this.in != null && this.reader != null && this.socket != null && this.socket.isConnected()) {
			try {
				for(int i = 0; i < this.recvBytes.length ; i++)
					this.recvBytes[i] = 0;

				result = "";

				int c = 0;
				int i = 0; /* position in recvBytes */

				while(c != '\n' && i < this.recvBytes.length) {
					c = this.reader.read();

					if(c == -1) {
						if(this.isConnected())
							Logger.error(this, "Unable to read but still connected");
						else
							Logger.notice(this, "Disconnected");

						this.disconnect(); /* will warn everybody */

						return null;
					}

					if(c == '\n')
						break;

					//result = result + new String(new byte[] { (byte)c });

					this.recvBytes[i] = (byte)c;
					i++;
				}

				result = new String(this.recvBytes, 0, i, "UTF-8");

				if(DEBUG_MODE) {
					if(result.matches("[\\-\\ \\?.a-zA-Z0-9\\,~%@/_=\\[\\]\\(\\)]*"))
						Logger.asIt(this, "Thaw <<< Node : "+result);
					else
						Logger.asIt(this, "Thaw <<< Node : Unknow chars in message. Not displayed");
				}


				return result;

			} catch (java.io.IOException e) {
				if(this.isConnected())
					Logger.error(this, "IOException while reading but still connected, wtf? : "+e.toString()+ " ; "+e.getMessage() );
				else
					Logger.notice(this, "IOException. Disconnected. : "+e.toString() + " ; "+e.getMessage());

				this.disconnect();

				return null;
			}
		} else {
			Logger.warning(this, "Cannot read if disconnected => null");
		}

		return null;
	}


	/**
	 * If duplicationAllowed, returns a copy of this object, using a different socket and differents lock / buffer.
	 * If !duplicationAllowed, returns this object.
	 * The duplicate socket is just connected but not initialized (ClientHello, etc).
	 */
	public FCPConnection duplicate() {
		if (!this.duplicationAllowed)
			return this;

		Logger.info(this, "Duplicating connection to the node ...");

		FCPConnection newConnection;

		newConnection = new FCPConnection(this.nodeAddress, this.port, -1, this.duplicationAllowed); /* upload limit is useless here, since we can't do a global limit on all the connections */

		if (!newConnection.connect()) {
			Logger.warning(this, "Unable to duplicate socket !");
			return this;
		}

		return newConnection;
	}

}
