package thaw.fcp;

import java.util.Observable;

import thaw.core.Logger;
import thaw.core.ThawThread;

/**
 * Manage all fcp messages (see corresponding object of each kind of query).
 * Call observers each type a new message is received. The given object is
 * the message.
 */
public class FCPQueryManager extends Observable implements Runnable {
	private Thread me;

	private FCPConnection connection;


	public FCPQueryManager(final FCPConnection connection) {
		me = null;
		setConnection(connection);
	}

	/**
	 * If you call yourself this function, you will probably have to call
	 * resetQueue() of FCPQueueManager.
	 */
	public void setConnection(final FCPConnection connection) {
		this.connection = connection;
	}

	/**
	 * Try to not directly call functions from FCPConnection.
	 */
	public FCPConnection getConnection() {
		return connection;
	}

	public boolean writeMessage(final FCPMessage message) {
		return connection.write(message.toString());
	}

	public boolean writeMessage(final FCPMessage message, final boolean checkLock) {
		return connection.write(message.toString(), checkLock);

	}

	/**
	 * Blocking until a message is reveived.
	 * More exactly, read until "Data\n" or "EndMessage\n" is read.
	 */
	public FCPMessage readMessage() {
		String whatsUp = new String("");
		final FCPMessage result = new FCPMessage();
		boolean withData;

		withData = false;

		while(true) {

			String read = new String("");

			read = connection.readLine();

			if(read == null) {
				Logger.notice(this, "readLine() returned null => disconnected ?");
				return null;
			}

			if("Data".equals( read )) {
				withData = true;
				break;
			}

			if("EndMessage".equals( read )) {
				break;
			}

			whatsUp = whatsUp + read + "\n";
		}

		Logger.verbose(this, "Parsing message ...");

		result.loadFromRawMessage(whatsUp);

		if(withData) {
			final long dataWaiting = (new Long(result.getValue("DataLength"))).longValue();
			connection.setRawDataWaiting(dataWaiting);
			Logger.info(this, "Achtung data: "+(new Long(dataWaiting)).toString());
		}

		return result;
	}


	public class Notifier implements Runnable {
		FCPMessage msg;

		public Notifier(FCPMessage msg) {
			this.msg = msg;
		}

		public void run() {
			try {
				setChanged();
				notifyObservers(msg);
			} catch(final Exception e) {
				/* it's really bad ... because if data are waiting on the socket ... */
				Logger.error(this, "EXCEPTION FROM ONE OF LISTENER : "+e.toString());
				Logger.error(this, "ERROR : "+e.getMessage());
				e.printStackTrace();
			}
		}
	}



	public class WatchDog implements Runnable {
		public final static int TIMEOUT = 10000;

		Runnable runnable;

		public WatchDog(Runnable runnable) {
			this.runnable = runnable;
		}

		private boolean isRunning(Thread th) {
			return (th.isAlive());
		}

		public void run() {
			Thread th = new ThawThread(runnable, "FCP message processing", this);
			th.start();

			for (int i = 0 ; i < TIMEOUT && isRunning(th) ; i += 300) {
				try {
					Thread.sleep(100);
				} catch(InterruptedException e) {
					/* \_o< */
				}
			}

			while (isRunning(th)) {
				Logger.warning(this, "Notifier thread ('"+th.toString()+"') seems to be blocked !!");
				try { Thread.sleep(TIMEOUT); } catch(InterruptedException e) { /* \_o< */ }
			}
		}

	}

	/**
	 * Multithreading allow the use of a watchdog. It's useful to debug,
	 * but I recommand to desactivate it for a normal use.
	 */
	public final static boolean MULTITHREADED = false;

	/**
	 * Will listen in loop for new incoming messages.
	 */
	public void run() {
		while(true) {
			FCPMessage latestMessage;

			/* note : if multithreaded, stop reading when a thread is writing,
			 *        else reading, parsing and answering messages while a thread is 
			 *        sending a big file may generate a lot of threads (and warnings because
			 *        of a possible freeze)
			 */
			latestMessage = readMessage();
			if (MULTITHREADED) {
				connection.addToWriterQueue();
				connection.removeFromWriterQueue();
			}

			Logger.verbose(this, "Message received. Notifying observers");

			if(latestMessage != null) {
				/*
				 * can't multithread if data are waiting
				 */
				if (MULTITHREADED && latestMessage.getAmountOfDataWaiting() == 0) {
					Thread notifierTh = new ThawThread(new WatchDog(new Notifier(latestMessage)),
								       "FCP message processing watchdog",
								       this);
					notifierTh.start();
				} else {
					try {
						setChanged();
						notifyObservers(latestMessage);
					} catch(final Exception e) {
						/* it's really bad ... because if data are waiting on the socket ... */
						Logger.error(this, "EXCEPTION FROM ONE OF LISTENER : "+e.toString());
						Logger.error(this, "ERROR : "+e.getMessage());
						e.printStackTrace();
					}

				}
			} else {
				Logger.info(this, "Stopping listening");
				return;
			}
		}

	}


	/**
	 * Create the thread listening for incoming message.
	 */
	public void startListening() {
		if(connection.isConnected()) {
			me = new ThawThread(this, "FCP socket listener", this);
			me.start();
		} else {
			Logger.warning(this, "Not connected, so not listening on the socket");
		}
	}


	/**
	 * This function is mainly used by FCPClientGet to have a separate socket to transfer the files.
	 * If FCPConnection is allowed to duplicate itself, then it will duplicate it and create a dedicated FCPQueryManager for.
	 * A FCPClientHello is sent with the given id.
	 * @return This object if it cannot duplicate FCPConnection
	 */
	public FCPQueryManager duplicate(final String connectionId) {
		FCPConnection newConnection;
		FCPQueryManager queryManager;

		newConnection = connection.duplicate();

		if (newConnection == connection)
			return this;

		queryManager = new FCPQueryManager(newConnection);

		queryManager.startListening();

		final FCPClientHello clientHello = new FCPClientHello(queryManager, connectionId);

		if (!clientHello.start(null)) {
			Logger.warning(this, "ID already used ?! Using initial socket ...");
			newConnection.disconnect();
			return this;
		}

		return queryManager;
	}

}

