package thaw.fcp;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Observer;
import java.util.Observable;

import java.util.HashMap;

import thaw.core.Logger;

/**
 * notify() only when progress has really changes.
 */
public class FCPClientGet extends Observable implements Observer, FCPTransferQuery {
	private final static int MAX_RETRIES = 3;
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

	private String identifier;

	private int progress; /* in pourcent */
	private long fileSize;

	private boolean running = false;
	private boolean successful = false;


	/**
	 * See setParameters().
	 */
	public FCPClientGet(FCPQueueManager queueManager, HashMap parameters) {
		this.queueManager = queueManager;
		setParameters(parameters);

		/* If isPersistent(), then start() won't be called, so must relisten the
		   queryManager by ourself */
		if(isPersistent() && identifier != null && !identifier.equals("")) {
			this.queueManager.getQueryManager().deleteObserver(this);
			this.queueManager.getQueryManager().addObserver(this);
		}
		
	}


	/**
	 * Only for initial queries : To resume queries, use FCPClientGet(FCPQueueManager, Hashmap).
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
		this.attempt = 0;
		
		if(key.indexOf('/') == key.length()-1) {
			filename = "index.html";
		} else {
			String cutcut[] = key.split("/");			
			filename = cutcut[cutcut.length-1];
		}

		Logger.debug(this, "Getting "+key);

		status = "Waiting";

	}

	public boolean start(FCPQueueManager queueManager) {
		running = true;
		progress = 0;

		this.queueManager = queueManager;

		status = "Requesting";

		if(this.identifier == null || this.identifier.equals(""))
			this.identifier = queueManager.getAnID();

		Logger.info(this, "Requesting key : "+getFileKey());

		FCPMessage queryMessage = new FCPMessage();

		queryMessage.setMessageName("ClientGet");
		queryMessage.setValue("URI", getFileKey());
		queryMessage.setValue("Identifier", identifier);
		queryMessage.setValue("Verbosity", "1");
		queryMessage.setValue("MaxRetries", "0");
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

		queueManager.getQueryManager().deleteObserver(this);
		queueManager.getQueryManager().addObserver(this);

		queueManager.getQueryManager().writeMessage(queryMessage);

		return true;
	}

	
	public void update(Observable o, Object arg) {
		FCPMessage message = (FCPMessage)arg;

		if(message.getValue("Identifier") == null
		   || !message.getValue("Identifier").equals(identifier))
			return;

		if(message.getMessageName().equals("DataFound")) {
			Logger.debug(this, "DataFound!");

			status = "Fetching";
			fileSize = (new Long(message.getValue("DataLength"))).longValue();

			
			if(isPersistent() && !isFinished()) {
				FCPMessage getRequestStatus = new FCPMessage();
				
				getRequestStatus.setMessageName("GetRequestStatus");
				getRequestStatus.setValue("Identifier", identifier);
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

			identifier = null;
			start(queueManager);
			
			setChanged();
			notifyObservers();

			return;
		}
		
		if(message.getMessageName().equals("ProtocolError")) {
			Logger.debug(this, "ProtocolError !");

			status = "Protocol Error ("+message.getValue("CodeDescription")+")";
			progress = 100;
			running = false;
			successful = false;
			
			queueManager.getQueryManager().deleteObserver(this);

			setChanged();
			notifyObservers();
			
			return;
		}

		if(message.getMessageName().equals("GetFailed")) {
			Logger.debug(this, "GetFailed !");

			int code = ((new Integer(message.getValue("Code"))).intValue());

			attempt++;

			if(attempt >= MAX_RETRIES || code == 25) {
			    status = "Failed ("+message.getValue("CodeDescription")+")";
			    progress = 100;
			    running = false;
			    successful = false;
			    queueManager.getQueryManager().deleteObserver(this);
			} else {
			    status = "Retrying";
			    start(queueManager);
			}

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

				progress = (int)((succeeded * 98) / required);

				setChanged();
				notifyObservers();
			}

			return;
		}

		if(message.getMessageName().equals("AllData")) {
			Logger.debug(this, "AllData ! : " + identifier);

			progress = 99;

			status = "Loading";

			fileSize = message.getAmountOfDataWaiting();

			status = "Writing";

			//queueManager.getQueryManager().getConnection().lockWriting();


			if(fetchDirectly(fileSize, true)) {
				successful = true;
				status = "Available";
			} else {
				Logger.warning(this, "Unable to fetch correctly the file. This may create problems on socket");
			}

			//queueManager.getQueryManager().getConnection().unlockWriting();
			
			running = false;
			progress = 100;

			queueManager.getQueryManager().deleteObserver(this);

			if(isPersistent())
				removePersistent();

			setChanged();
			notifyObservers();
			

			return;
		}

		if(message.getMessageName().equals("PersistentGet")) {			
			Logger.debug(this, "PersistentGet !");
			
			status = "Fetching";
			return;
		}
		
		Logger.warning(this, "Unknow message : "+message.getMessageName() + " !");
		
	}


	public boolean fetchDirectly(long size, boolean reallyWrite) {
		FCPConnection connection;

		File newFile = new File(getPath());
		FileOutputStream fileWriter = null;


		connection = queueManager.getQueryManager().getConnection();

		if(reallyWrite) {
			Logger.info(this, "Writing file to disk ...");
			
			try {
				fileWriter = new FileOutputStream(newFile);
			} catch(java.io.IOException e) {
				Logger.error(this, "Unable to write file on disk ... perms ? : "+e.toString());
				status = "Write error";
				return false;
			}
		} else {
			Logger.info(this, "File is supposed already written. Not rewriting.");
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

			if(reallyWrite) {
				try {
					fileWriter.write(read, 0, amount);
				} catch(java.io.IOException e) {
					Logger.error(this, "Unable to write file on disk ... out of space ? : "+e.toString());
					status = "Write error";
					return false;
				}
			}
			
			size = size - amount;
			
		}

		if(reallyWrite) {
			try {
				fileWriter.close();
			} catch(java.io.IOException e) {
				Logger.notice(this, "Unable to close correctly file on disk !? : "+e.toString());
			}
		}

		Logger.info(this, "File written");

		return true;
	}

	
	private void removePersistent() {
		FCPMessage stopMessage = new FCPMessage();
		
		stopMessage.setMessageName("RemovePersistentRequest");
		
		if(globalQueue)
			stopMessage.setValue("Global", "true");
		else
			stopMessage.setValue("Global", "false");
		
		stopMessage.setValue("Identifier", identifier);
			
		queueManager.getQueryManager().writeMessage(stopMessage);
	}


	public boolean stop(FCPQueueManager queryManager) {
		Logger.info(this, "Stop fetching of the key : "+getFileKey());

		progress = 100;
		successful = false;
		status = "Stopped";

		if(!isRunning() || isFinished()) {
			Logger.info(this, "Can't stop. Not running -> considered as failed");

			setChanged();
			notifyObservers();

			return true;
		}
		
		if(isPersistent()) {
			removePersistent();
		} else {
			Logger.warning(this, "Can't stop a non-persistent query, will continue in background ...");
			return false;
		}

		setChanged();
		notifyObservers();
		
		return true;
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
		if(progress >= 100)
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

	public void setAttempt(int x) {
		attempt = x;
	}

	public int getMaxAttempt() {
		return MAX_RETRIES;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public boolean isRunning() {
		return running;
	}

	public HashMap getParameters() {
		HashMap result = new HashMap();

		result.put("key", key);
		result.put("filename", filename);
		result.put("priority", ((new Integer(priority)).toString()));
		result.put("persistence", ((new Integer(persistence)).toString()));
		result.put("globalQueue", ((new Boolean(globalQueue)).toString()));
		result.put("destinationDir", destinationDir);
		result.put("attempt", ((new Integer(attempt)).toString()));

		if(status.indexOf("(?)") > 0) {
			String[] cut = status.split(" ");
			result.put("status", cut[0]);
		} else {
			result.put("status", status);
		}

		result.put("identifier", identifier);
		result.put("progress", ((new Integer(progress)).toString()));
		result.put("fileSize", ((new Long(fileSize)).toString()));
		result.put("running", ((new Boolean(running)).toString()));
		result.put("successful", ((new Boolean(successful)).toString()));

		return result;
	}

	public boolean setParameters(HashMap parameters) {
		
		key            = (String)parameters.get("key");

		Logger.debug(this, "Resuming key : "+key);

		filename       = (String)parameters.get("filename");
		priority       = ((new Integer((String)parameters.get("priority"))).intValue());
		persistence    = ((new Integer((String)parameters.get("persistence"))).intValue());
		globalQueue    = ((new Boolean((String)parameters.get("globalQueue"))).booleanValue());
		destinationDir = (String)parameters.get("destinationDir");
		attempt        = ((new Integer((String)parameters.get("attempt"))).intValue());
		status         = (String)parameters.get("status");
		identifier     = (String)parameters.get("identifier");

		Logger.info(this, "Resuming id : "+identifier);

		progress       = ((new Integer((String)parameters.get("progress"))).intValue());
		fileSize       = ((new Long((String)parameters.get("fileSize"))).longValue());
		running        = ((new Boolean((String)parameters.get("running"))).booleanValue());
		successful     = ((new Boolean((String)parameters.get("successful"))).booleanValue());

		if(persistence == 2 && !isFinished()) {
			progress = 0;
			status = "Waiting";
		}
		
		if(persistence < 2 && !isFinished() && identifier != null && !identifier.equals(""))
			status = status + " (?)";
		

		return true;
	}


	public boolean isPersistent() {
		return (persistence < 2);
	}


	public String getIdentifier() {
		if(identifier == null || identifier.equals(""))
			return null;

		return identifier;
	}
}
