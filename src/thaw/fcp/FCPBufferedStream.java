package thaw.fcp;

import thaw.core.Logger;

/**
 * Only used by FCPConnection. Except special situation, you shouldn't have to use it directly.
 * Currently only used for output. (shouldn't be really usefull for input).
 * Some data are sent each 'INTERVAL' (in ms).
 */
public class FCPBufferedStream implements Runnable {
	private FCPConnection connection;
	private int maxUploadSpeed;

	private byte outputBuffer[];

	public final static int OUTPUT_BUFFER_SIZE = 102400;
	public final static int INTERVAL = 200;

	private int waiting = 0; /* amount of data stored in the buffer */
	private int readCursor = 0; /* indicates where the nex read will be */
	private int writeCursor = 0; /* indicates where the next write will be */

	private Thread tractopelle = null;
	private boolean running = true;
	private int packetSize = 0;



	public FCPBufferedStream(FCPConnection connection,
				 int maxUploadSpeed) {
		this.connection = connection;
		this.maxUploadSpeed = maxUploadSpeed;

		if(maxUploadSpeed >= 0) {
			this.outputBuffer = new byte[OUTPUT_BUFFER_SIZE];
			this.packetSize = (maxUploadSpeed * 1024) / (1000/INTERVAL);
		}
	}

	/**
	 * Add to the buffer. Can block if buffer is full !
	 * Never send more than OUTPUT_BUFFER_SIZE.
	 */
	public synchronized boolean write(byte[] data) {
		if(this.maxUploadSpeed == -1) {
			return this.connection.realRawWrite(data);
		}

		while(this.waiting + data.length > OUTPUT_BUFFER_SIZE) {
			this.sleep(INTERVAL);
		}

		this.waiting += data.length;

		for(int i = 0 ; i < data.length ; i++) {
			this.outputBuffer[this.writeCursor] = data[i];

			this.writeCursor++;

			if(this.writeCursor >= OUTPUT_BUFFER_SIZE)
				this.writeCursor = 0;
		}

		return true;
	}

	/**
	 * @see #write(byte[])
	 */
	public boolean write(String data) {
		try {
			return this.write(data.getBytes("UTF-8"));
		} catch(java.io.UnsupportedEncodingException e) {
			Logger.error(this, "UNSUPPORTED ENCODING EXCEPTION : UTF-8");
			return this.write(data.getBytes());
		}
	}

	/**
	 * extract from the buffer
	 */
	private boolean readOutputBuffer(byte[] data) {
		for(int i = 0; i < data.length ; i++) {
			data[i] = this.outputBuffer[this.readCursor];

			this.readCursor++;

			if(this.readCursor >= OUTPUT_BUFFER_SIZE)
				this.readCursor = 0;
		}

		this.waiting -= data.length;

		return true;
	}

	/**
	 * wait for the buffer being empty.
	 */
	public void flush() {
		while(this.waiting > 0) {
			this.sleep(INTERVAL);
		}
	}


	public void run() {
		byte[] data;

		while(this.running) { /* Wild and freeeeeee */
			if(this.waiting > 0) {
				int to_read = this.packetSize;

				if(this.waiting < to_read)
					to_read = this.waiting;

				data = new byte[to_read];

				this.readOutputBuffer(data);

				this.connection.realRawWrite(data);
			}

			this.sleep(INTERVAL);
		}
	}

	/**
	 * Start the thread sending data from the buffer to the OutputStream (socket).
	 */
	public boolean startSender() {
		this.running = true;

		if(this.maxUploadSpeed < 0) {
			Logger.notice(this, "startSender(): No upload limit. Not needed");
			return false;
		}

		if(this.tractopelle == null) {
			this.tractopelle = new Thread(this);
			this.tractopelle.start();
			return true;
		} else {
			Logger.notice(this, "startSender(): Already started");
			return false;
		}
	}


	public boolean stopSender() {
		this.running = false;
		this.tractopelle = null;
		return true;
	}

	public boolean isOutputBufferEmpty() {
		return (this.waiting == 0);
	}

	public boolean isOutputBufferFull() {
		return (this.maxUploadSpeed < 0 || this.waiting >= (OUTPUT_BUFFER_SIZE-1));
	}

	/**
	 * Just ignore the InterruptedException.
	 */
	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch(java.lang.InterruptedException e) {
			/* just iggnnnnnooored */
		}
	}
}
