package thaw.fcp;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Observer;
import java.util.Observable;

import java.util.HashMap;

import thaw.core.Logger;

/**
 * notify() only when progress has really changes.
 * TODO: Put the fetchLock on FCPConnection. Not here.
 */
public class FCPClientGet extends Observable implements Observer, FCPTransferQuery {
	private final static int MAX_RETRIES = -1;
	private final static int PACKET_SIZE = 1024;
	private final static int BLOCK_SIZE = 32768;

	private FCPQueueManager queueManager;

	private String key = null;
	private String filename = null; /* Extract from the key */
	private int priority = 6;
	private int persistence = 0;
	private boolean globalQueue = false;
	private String destinationDir = null;

	private int attempt = -1;
	private String status;

	private String identifier;

	private int progress; /* in pourcent */
	private int fromTheNodeProgress = 0; /* I'm not sure that it's correct english ... */
	private boolean progressReliable = false;
	private long fileSize;

	private boolean running = false;
	private boolean successful = false;
	private boolean isLockOwner = false;
	

	/**
	 * See setParameters().
	 */
	public FCPClientGet(FCPQueueManager queueManager, HashMap parameters) {
		this.queueManager = queueManager;
		setParameters(parameters);

		progressReliable = false;
		fromTheNodeProgress = 0;

		/* If isPersistent(), then start() won't be called, so must relisten the
		   queryManager by ourself */
		if(isPersistent() && identifier != null && !identifier.equals("")) {
			this.queueManager.getQueryManager().deleteObserver(this);
			this.queueManager.getQueryManager().addObserver(this);
		}
		
	}


	/**
	 * Used to resume query from persistent queue of the node.
	 * Think of adding this FCPClientGet as a queryManager observer.
	 */
	public FCPClientGet(String id, String key, int priority,
			    int persistence, boolean globalQueue,
			    String destinationDir, String status, int progress,
			    FCPQueueManager queueManager) {

		this(key, priority, persistence, globalQueue, destinationDir);

		progressReliable = false;

		this.queueManager = queueManager;

		this.progress = progress;
		this.status = status;
		this.identifier = id;

		successful = false;
		running = true;

		if(progress < 100) {
			fromTheNodeProgress = 0;
		} else {
			fromTheNodeProgress = 100;
			progressReliable = true;
		}
		
	}


	/**
	 * Only for initial queries : To resume queries, use FCPClientGet(FCPQueueManager, Hashmap).
	 * @param destinationDir if null, won't be automatically saved
	 * @param persistence 0 = Forever ; 1 = Until node reboot ; 2 = Until the app disconnect
	 */
	public FCPClientGet(String key, int priority,
			    int persistence, boolean globalQueue,
			    String destinationDir) {	

	
		if(globalQueue && persistence >= 2)
			globalQueue = false; /* else protocol error */

		progressReliable = false;
		fromTheNodeProgress = 0;

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

		Logger.debug(this, "Query for getting "+key+" created");

		status = "Waiting";

	}

	public boolean start(FCPQueueManager queueManager) {
		attempt++;
		running = true;
		progress = 0;

		this.queueManager = queueManager;

		status = "Requesting";

		if(this.identifier == null || this.identifier.equals(""))
			this.identifier = queueManager.getAnID() + "-"+filename;;

		Logger.info(this, "Requesting key : "+getFileKey());

		FCPMessage queryMessage = new FCPMessage();

		queryMessage.setMessageName("ClientGet");
		queryMessage.setValue("URI", getFileKey());
		queryMessage.setValue("Identifier", identifier);
		queryMessage.setValue("Verbosity", "1");
		queryMessage.setValue("MaxRetries", "0");
		queryMessage.setValue("PriorityClass", Integer.toString(priority));

		if(destinationDir != null)
			queryMessage.setValue("ClientToken", destinationDir);

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

			if(!isFinished()) {
				status = "Available";
				fileSize = (new Long(message.getValue("DataLength"))).longValue();
				
				progress = 100;
				successful = true;
				
				if(isPersistent()) {
					if(destinationDir != null) {

						if(!fileExists(destinationDir))
							saveFileTo(destinationDir);
						else
							Logger.info(this, "File already existing. Not rewrited");
						
					} else {
						Logger.info(this, "Don't know where to put file, so file not asked to the node");
					}
				}


				fromTheNodeProgress = 100;
				progressReliable = true;
				
				setChanged();
				notifyObservers();
			}

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

			if(message.getValue("Code").equals("15")) {
				Logger.debug(this, "Unknow URI ? was probably a stop order so no problem ...");
				return;
			}
			/*
			if(message.getValue("Fatal").equals("False")) {
				Logger.debug(this, "Non-fatal protocol error");
				status = "Protocol warning ("+message.getValue("CodeDescription")+")";
				return;
			}
			*/

			Logger.error(this, "=== PROTOCOL ERROR === \n"+message.toString());

			status = "Protocol Error ("+message.getValue("CodeDescription")+")";
			progress = 100;
			running = false;
			successful = false;			

			if(message.getValue("Fatal") != null &&
			   message.getValue("Fatal").equals("false")) {
				status = status + " (non-fatal)";
			}

			if(isLockOwner) {
				queueManager.getQueryManager().getConnection().unlockReading();
				queueManager.getQueryManager().getConnection().unlockWriting();
				isLockOwner= false;
			}

			queueManager.getQueryManager().deleteObserver(this);

			setChanged();
			notifyObservers();
			
			return;
		}

		if(message.getMessageName().equals("GetFailed")) {
			Logger.debug(this, "GetFailed !");

			if(!isRunning()) { /* Must be a "GetFailed: cancelled by caller", so we simply ignore */
				Logger.info(this, "Cancellation confirmed.");
				return;
			}

			//removeRequest();

			int code = Integer.parseInt(message.getValue("Code"));

			if(MAX_RETRIES == -1 || attempt >= MAX_RETRIES || code == 25) {
			    status = "Failed ("+message.getValue("CodeDescription")+")";
			    progress = 100;
			    running = false;
			    successful = false;
			    
			    if(message.getValue("Fatal") != null &&
			       message.getValue("Fatal").equals("false")) {
				    status = status + " (non-fatal)";
			    }

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

				progress = (int)((succeeded * 99) / required);

				status = "Fetching";

				if(message.getValue("FinalizedTotal") != null &&
				   message.getValue("FinalizedTotal").equals("true")) {
					progressReliable = true;
				}

				setChanged();
				notifyObservers();
			}

			return;
		}

		if(message.getMessageName().equals("AllData")) {
			Logger.debug(this, "AllData ! : " + identifier);

			fileSize = message.getAmountOfDataWaiting();

			status = "Writing to disk";
			Logger.info(this, "Receiving file ...");

			setChanged();
			notifyObservers();


			if(fetchDirectly(getPath(), fileSize, true)) {
				successful = true;
				status = "Available";
			} else {
				Logger.warning(this, "Unable to fetch correctly the file. This may create problems on socket");
			}
			
			Logger.info(this, "File received");

			queueManager.getQueryManager().getConnection().unlockReading();
			queueManager.getQueryManager().getConnection().unlockWriting();
			isLockOwner= false;
			
			running = false;
			progress = 100;

			queueManager.getQueryManager().deleteObserver(this);

			setChanged();
			notifyObservers();
			

			return;
		}

		if(message.getMessageName().equals("PersistentGet")) {			
			Logger.debug(this, "PersistentGet !");
			
			return;
		}
		
		Logger.warning(this, "Unknow message : "+message.getMessageName() + " !");
		
	}


	private class UnlockWaiter implements Runnable {
		FCPClientGet clientGet;
		FCPConnection connection;
		String dir;

		public UnlockWaiter(FCPClientGet clientGet, FCPConnection connection, String dir) {
			this.clientGet = clientGet;
			this.dir = dir;
			this.connection = connection;
		}

		public void run() {
			if(dir == null) {
				Logger.warning(this, "UnlockWaiter.run() : Wtf ?");
			}

			while(true) {
				if(!connection.isReadingLocked()
				   && (!connection.isWritingLocked()))
					break;

				try {
					Thread.sleep(200);
				} catch(java.lang.InterruptedException e) {

				}
			}
			
			if(!connection.lockReading()) {
				/* Ah ben oué mais non */
				run();
				return;
			}

			if(!connection.lockWriting()) {
				/* Ah ben oué mais non */
				connection.unlockReading();
				run();
				return;
			}

			isLockOwner = true;

			Logger.debug(this, "I take the reading lock !");

			if(dir == null) {
				Logger.warning(this, "UnlockWaiter.run() : Wtf ?");
			}

			clientGet.continueSaveFileTo(this.dir);
			return;
		}
	}

	public synchronized boolean saveFileTo(String dir) {
		fromTheNodeProgress = 0;
		if(dir == null) {
			Logger.warning(this, "saveFileTo() : Can't save to null.");
			return false;
		}

		destinationDir = dir;


		if(!isFinished() || !isSuccessful()) {
			Logger.warning(this, "Unable to fetch a file not finished");
			return false;
		}

		if(!isPersistent()) {
			Logger.warning(this, "Not persistent, so unable to ask");
			return false;
		}


		Logger.info(this, "Waiting socket avaibility ...");
		status = "Waiting socket avaibility ...";

		setChanged();
		notifyObservers();


		Thread fork = new Thread(new UnlockWaiter(this, queueManager.getQueryManager().getConnection(), dir));
		fork.start();

		return true;
	}

	public synchronized boolean continueSaveFileTo(String dir) {
		Logger.debug(this, "Asking file to the node...");

		destinationDir = dir;

		status = "Requesting file";

		setChanged();
		notifyObservers();

		if(destinationDir == null) {
			Logger.warning(this, "saveFileTo() : Wtf ?");
		}

		FCPMessage getRequestStatus = new FCPMessage();
		
		getRequestStatus.setMessageName("GetRequestStatus");
		getRequestStatus.setValue("Identifier", identifier);
		if(globalQueue)
			getRequestStatus.setValue("Global", "true");
		else
			getRequestStatus.setValue("Global", "false");
		getRequestStatus.setValue("OnlyData", "true");

		
		
		queueManager.getQueryManager().writeMessage(getRequestStatus, false);

		return true;
	}


	private boolean fileExists(String dir) {
		destinationDir = dir;
		File newFile = new File(getPath());
		return newFile.exists();
	}



	private boolean fetchDirectly(String file, long size, boolean reallyWrite) {
		FCPConnection connection;

		File newFile = new File(file);
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
		long origSize = size;
		long startTime = System.currentTimeMillis();

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
			
			if( System.currentTimeMillis() >= (startTime+3000)) {
				fromTheNodeProgress = (int) (((origSize-size) * 100) / origSize);
				setChanged();
				notifyObservers();
				startTime = System.currentTimeMillis();
			}

		}

		fromTheNodeProgress = 100;

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

	
	public boolean removeRequest() {
		FCPMessage stopMessage = new FCPMessage();
		
		if(!isPersistent()) {
			Logger.info(this, "Can't remove non persistent request.");
			return false;
		}

		stopMessage.setMessageName("RemovePersistentRequest");
		
		if(globalQueue)
			stopMessage.setValue("Global", "true");
		else
			stopMessage.setValue("Global", "false");
		
		stopMessage.setValue("Identifier", identifier);
		
		queueManager.getQueryManager().writeMessage(stopMessage);

		running = false;

		return true;
	}

	public boolean pause(FCPQueueManager queryManager) {
		/* TODO ? : Reduce the priority 
		   instead of stopping */

		Logger.info(this, "Pausing fetching of the key : "+getFileKey());

		removeRequest();

		progress = 0;
		successful = false;
		status = "Delayed";

		setChanged();
		notifyObservers();
		
		return true;

	}

	public boolean stop(FCPQueueManager queryManager) {
		Logger.info(this, "Stop fetching of the key : "+getFileKey());

		if(!removeRequest()) {
			return false;
		}

		progress = 100;
		successful = false;
		status = "Stopped";
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

	public boolean isProgressionReliable() {
		return progressReliable;
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
		if(destinationDir != null)
			return destinationDir + File.separator + filename;

		return null;
	}

	public String getFilename() {
		return filename;
	}

	public int getAttempt() {
		return attempt;
	}

	public void setAttempt(int x) {
		attempt = x;

		if(x == 0) {
			/* We suppose it's a restart */
			progress = 0;
		}
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

		result.put("URI", key);
		result.put("Filename", filename);
		result.put("Priority", Integer.toString(priority));
		result.put("Persistence", Integer.toString(persistence));
		result.put("Global", Boolean.toString(globalQueue));
		result.put("ClientToken", destinationDir);
		result.put("Attempt", Integer.toString(attempt));

		result.put("status", status);

       		result.put("Identifier", identifier);
		result.put("Progress", Integer.toString(progress));
		result.put("FileSize", Long.toString(fileSize));
		result.put("Running", Boolean.toString(running));
		result.put("Successful", Boolean.toString(successful));

		return result;
	}

	public boolean setParameters(HashMap parameters) {
		
		key            = (String)parameters.get("URI");

		Logger.debug(this, "Resuming key : "+key);

		filename       = (String)parameters.get("Filename");
		priority       = Integer.parseInt((String)parameters.get("Priority"));
		persistence    = Integer.parseInt((String)parameters.get("Persistence"));
		globalQueue    = Boolean.valueOf((String)parameters.get("Global"));
		destinationDir = (String)parameters.get("ClientToken");
		attempt        = Integer.parseInt((String)parameters.get("Attempt"));
		status         = (String)parameters.get("Status");
		identifier     = (String)parameters.get("Identifier");

		Logger.info(this, "Resuming id : "+identifier);

		progress       = Integer.parseInt((String)parameters.get("Progress"));
		fileSize       = Long.parseLong((String)parameters.get("FileSize"));
		running        = Boolean.valueOf((String)parameters.get("Running"));
		successful     = Boolean.valueOf((String)parameters.get("Successful"));

		if(persistence == 2 && !isFinished()) {
			progress = 0;
			status = "Waiting";
		}
		
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

	public boolean isGlobal() {
		return globalQueue;
	}

	public int getTransferWithTheNodeProgression() {
		return fromTheNodeProgress;
	}
}
