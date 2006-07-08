package thaw.fcp;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Observer;
import java.util.Observable;

import thaw.core.Logger;

/**
 * notify() only when progress has really changes.
 */
public class FCPClientGet extends Observable implements Observer, FCPQuery {
	private final static String MAX_RETRIES = "3";
	private final static int PACKET_SIZE = 1024;
	private final static int BLOCK_SIZE = 32768;

	private FCPQueueManager queueManager;

	private String key = null;
	private String filename = null; /* Extract from the key */
	private int priority = 6;
	private int persistence = 0;
	private boolean globalQueue = false;
	private String destinationDir = null;

	private int attempt = 0;
	private String status;

	private String id;

	private int progress; /* in pourcent */
	private long fileSize;

	/**
	 * @param persistence 0 = Forever ; 1 = Until node reboot ; 2 = Until the app disconnect
	 */
	public FCPClientGet(String key, int priority,
			    int persistence, boolean globalQueue,
			    String destinationDir) {

		if(globalQueue && persistence >= 2)
			globalQueue = false; /* else protocol error */

		this.key = key;
		this.priority = priority;
		this.persistence = persistence;
		this.globalQueue = globalQueue;
		this.destinationDir = destinationDir;

		this.progress = 0;
		this.fileSize = 0;
		
		String cutcut[] = key.split("/");
		
		filename = cutcut[cutcut.length-1];

		if(filename.equals("") || filename.indexOf('.') < 0) {
			filename = "index.html";
		}
		
		Logger.debug(this, "Getting "+key);

		status = "Waiting";

	}

	public boolean start(FCPQueueManager queueManager) {
		this.queueManager = queueManager;

		status = "Requesting";

		this.id = queueManager.getAnID();

		Logger.info(this, "Requesting key : "+getFileKey());

		FCPMessage queryMessage = new FCPMessage();

		queryMessage.setMessageName("ClientGet");
		queryMessage.setValue("URI", getFileKey());
		queryMessage.setValue("Identifier", id);
		queryMessage.setValue("Verbosity", "1");
		queryMessage.setValue("MaxRetries", MAX_RETRIES);
		queryMessage.setValue("PriorityClass", (new Integer(priority)).toString());

		if(persistence == 0)
			queryMessage.setValue("Persistence", "forever");
		if(persistence == 1)
			queryMessage.setValue("Persistence", "reboot");
		if(persistence == 2)
			queryMessage.setValue("Persistence", "connection");

		if(globalQueue)
			queryMessage.setValue("Global", "true");
		else
			queryMessage.setValue("Global", "false");

		queryMessage.setValue("ReturnType", "direct");

		queueManager.getQueryManager().addObserver(this);

		queueManager.getQueryManager().writeMessage(queryMessage);

		return true;
	}

	
	public void update(Observable o, Object arg) {
		FCPMessage message = (FCPMessage)arg;

		if(message.getValue("Identifier") == null
		   || !message.getValue("Identifier").equals(id)) {
			if(message.getValue("Identifier") != null)
				Logger.verbose(this, "Not for us : "+message.getValue("Identifier"));
			else
				Logger.verbose(this, "Not for us");
			return;
		} else {
			Logger.verbose(this, "For us for us !");
		}

		if(message.getMessageName().equals("DataFound")) {
			Logger.debug(this, "DataFound!");

			status = "Fetching";
			fileSize = (new Long(message.getValue("DataLength"))).longValue();

			
			if(globalQueue) {
				FCPMessage getRequestStatus = new FCPMessage();
				
				getRequestStatus.setMessageName("GetRequestStatus");
				getRequestStatus.setValue("Identifier", id);
				if(globalQueue)
					getRequestStatus.setValue("Global", "true");
				else
					getRequestStatus.setValue("Global", "false");
				getRequestStatus.setValue("OnlyData", "true");
				
				queueManager.getQueryManager().writeMessage(getRequestStatus);
			}
			

			setChanged();
			notifyObservers();

			return;
		}

		if(message.getMessageName().equals("IdentifierCollision")) {
			Logger.notice(this, "IdentifierCollision ! Resending with another id");

			start(queueManager);
			
			setChanged();
			notifyObservers();

			return;
		}
		
		if(message.getMessageName().equals("ProtocolError")) {
			Logger.debug(this, "ProtocolError !");

			status = "Protocol Error";
			progress = 100;

			queueManager.getQueryManager().deleteObserver(this);

			setChanged();
			notifyObservers();
			
			return;
		}

		if(message.getMessageName().equals("GetFailed")) {
			Logger.debug(this, "GetFailed !");

			status = "Failed";
			progress = 100;

			queueManager.getQueryManager().deleteObserver(this);

			setChanged();
			notifyObservers();

			return;
		}

		if(message.getMessageName().equals("SimpleProgress")) {
			Logger.debug(this, "SimpleProgress !");

			progress = 0;

			if(message.getValue("Total") != null
			   && message.getValue("Succeeded") != null) {
				fileSize = ((new Long(message.getValue("Total"))).longValue())*BLOCK_SIZE;
				long required = (new Long(message.getValue("Total"))).longValue();
				long succeeded = (new Long(message.getValue("Succeeded"))).longValue();

				status = "Fetching";

				progress = (int)((succeeded * 100) / required);

				setChanged();
				notifyObservers();
			}

			return;
		}

		if(message.getMessageName().equals("AllData")) {
			Logger.debug(this, "AllData !");

			status = "Loading";

			fileSize = (new Long(message.getValue("DataLength"))).longValue();

			status = "Available";

			fetchDirectly(fileSize);

			progress = 100;

			queueManager.getQueryManager().deleteObserver(this);

			return;
		}

		if(message.getMessageName().equals("PersistentGet")) {
			/* Should not bother us */
			return;
		}

		Logger.warning(this, "Unknow message : "+message.getMessageName() + " !");

	}


	public void fetchDirectly(long size) {
		FCPConnection connection;
		File newFile = new File(getPath());
		FileOutputStream fileWriter;

		connection = queueManager.getQueryManager().getConnection();

		Logger.info(this, "Writing file to disk ...");

		try {
			fileWriter = new FileOutputStream(newFile);
		} catch(java.io.IOException e) {
			Logger.error(this, "Unable to write file on disk ... perms ? : "+e.toString());
			status = "Write error";
			return;
		}

		/* size == bytes remaining on socket */
		while(size > 0) {
			int packet = PACKET_SIZE;
			byte[] read;
			int amount;
			
			if(size < (long)packet)
				packet = (int)size;

			read = new byte[packet];

			amount = connection.read(packet, read);

			if(amount <= -1) {
				Logger.error(this, "Socket closed ?!");
				status = "Read error";
				break;
			}

			try {
				fileWriter.write(read, 0, amount);
			} catch(java.io.IOException e) {
				Logger.error(this, "Unable to write file on disk ... out of space ? : "+e.toString());
				status = "Write error";
				return;
			}
			
			size = size - amount;
			
		}
		
		try {
			fileWriter.close();
		} catch(java.io.IOException e) {
			Logger.notice(this, "Unable to close correctly file on disk !? : "+e.toString());
		}

		Logger.info(this, "File written");

	}


	public boolean stop(FCPQueueManager queryManager) {
		Logger.info(this, "*TODO* stop()  *TODO*");
		/* TODO */
		return false;
	}

	public int getThawPriority() {
		return priority;
	}

	public int getQueryType() {
		return 1;
	}
	
	public String getStatus() {
		return status;
	}

	public int getProgression() {
		return progress;
	}

	public String getFileKey() {
		return key;
	}

	public boolean isFinished() {
		if(progress >= 99)
			return true;

		return false;
	}

	public long getFileSize() {
		return fileSize;
	}

	public String getPath() {
		return destinationDir + File.separator + filename;
	}


	public int getAttempt() {
		return attempt;
	}
}
