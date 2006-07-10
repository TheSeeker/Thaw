package thaw.fcp;

import java.util.Observable;

import thaw.core.Logger;

/**
 * Manage all fcp messages (see corresponding object of each kind of query).
 * Call observers each type a new message is received. The given object is
 * the 
 */
public class FCPQueryManager extends Observable implements Runnable {
	private Thread me;

	private FCPConnection connection;
	private FCPMessage latestMessage;


	public FCPQueryManager(FCPConnection connection) {
		me = null;
		latestMessage = null;
		setConnection(connection);
	}

	/**
	 * If you call yourself this function, you will probably have to call
	 * resetQueue() of FCPQueueManager.
	 */
	public void setConnection(FCPConnection connection) {
		this.connection = connection;
	}

	/**
	 * Try to not directly call functions from FCPConnection.
	 */
	public FCPConnection getConnection() {
		return connection;
	}

	public synchronized boolean writeMessage(FCPMessage message) {
		return connection.write(message.toString());
	}

	/**
	 * Blocking until a message is reveived.
	 * More exactly, read until "Data\n" or "EndMessage\n" is read.
	 */
	public FCPMessage readMessage() {
		String whatsUp = new String("");
		FCPMessage result = new FCPMessage();
		boolean withData;
		
		withData = false;

		while(true) {

			String read = new String("");

			read = connection.readLine();
			
			if(read == null) {
				Logger.notice(this, "readLine() returned null => disconnected ?");
				return null;
			}

			if(read.equals("Data")) {
				withData = true;
				break;
			}

			if(read.equals("EndMessage")) {
				break;
			}

			whatsUp = whatsUp + read + "\n";
		}

		Logger.verbose(this, "Parsing message ...");

		result.loadFromRawMessage(whatsUp);

		if(withData) {
			long dataWaiting = (new Long(result.getValue("DataLength"))).longValue();
			connection.setRawDataWaiting(dataWaiting);
			Logger.info(this, "Achtung data: "+(new Long(dataWaiting)).toString());			
		}

		return result;
	}


	/**
	 * Will listen in loop for new incoming messages.
	 */
	public void run() {

		while(true) {
			latestMessage = readMessage();
			
			Logger.debug(this, "Message received. Notifying observers");

			if(latestMessage != null) {
				setChanged();
				notifyObservers(latestMessage);
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
			me = new Thread(this);
			me.start();
		} else {
			Logger.warning(this, "Not connected, so not listening on the socket");
		}
	}

}

