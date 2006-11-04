package thaw.fcp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Observer;
import java.util.Observable;

import java.util.HashMap;

import thaw.core.Logger;

/**
 * notify() only when progress has really changes.
 * TODO: Use streams instead of writing directly the file.
 */
public class FCPClientGet extends Observable implements Observer, FCPTransferQuery {
	private int maxRetries = -1;
	private final static int PACKET_SIZE = 1024;
	private final static int BLOCK_SIZE = 32768;

	private FCPQueueManager queueManager;
	private FCPQueryManager duplicatedQueryManager;

	private String key = null;
	private String filename = null; /* Extract from the key */
	private int priority = 6;
	private int persistence = 0;
	private boolean globalQueue = false;
	private String destinationDir = null;
	private String finalPath = null;

	private int attempt = -1;
	private String status;

	private String identifier;

	private int progress; /* in pourcent */
	private int fromTheNodeProgress = 0;
	private boolean progressReliable = false;
	private long fileSize;

	private boolean running = false;
	private boolean successful = true;
	private boolean fatal = true;
	private boolean isLockOwner = false;

	private boolean alreadySaved = false;


	/**
	 * See setParameters().
	 */
	public FCPClientGet(FCPQueueManager queueManager, HashMap parameters) {
		this.queueManager = queueManager;
		this.setParameters(parameters);

		this.progressReliable = false;
		this.fromTheNodeProgress = 0;

		/* If isPersistent(), then start() won't be called, so must relisten the
		   queryManager by ourself */
		if(this.isPersistent() && this.identifier != null && !this.identifier.equals("")) {
			this.queueManager.getQueryManager().deleteObserver(this);
			this.queueManager.getQueryManager().addObserver(this);
		}

	}


	/**
	 * Used to resume query from persistent queue of the node.
	 * Think of adding this FCPClientGet as a queryManager observer.
	 * @param destinationDir if null, then a temporary file will be create (path determined only when the file is availabed ; this file will be deleted on jvm exit)
	 */
	public FCPClientGet(String id, String key, int priority,
			    int persistence, boolean globalQueue,
			    String destinationDir, String status, int progress,
			    int maxRetries,
			    FCPQueueManager queueManager) {

		this(key, priority, persistence, globalQueue, maxRetries, destinationDir);

		this.progressReliable = false;

		this.queueManager = queueManager;

		this.progress = progress;
		this.status = status;

		if(status == null) {
			Logger.warning(this, "status == null ?!");
			status = "(null)";
		}

		this.identifier = id;

		this.successful = true;
		this.running = true;

		if(progress < 100) {
			this.fromTheNodeProgress = 0;
		} else {
			this.fromTheNodeProgress = 100;
			this.progressReliable = true;
		}

	}


	/**
	 * Only for initial queries : To resume queries, use FCPClientGet(FCPQueueManager, Hashmap).
	 * @param destinationDir if null => temporary file
	 * @param persistence 0 = Forever ; 1 = Until node reboot ; 2 = Until the app disconnect
	 */
	public FCPClientGet(String key, int priority,
			    int persistence, boolean globalQueue,
			    int maxRetries,
			    String destinationDir) {


		if(globalQueue && persistence >= 2)
			globalQueue = false; /* else protocol error */

		this.progressReliable = false;
		this.fromTheNodeProgress = 0;

		this.maxRetries = maxRetries;
		this.key = key;
		this.priority = priority;
		this.persistence = persistence;
		this.globalQueue = globalQueue;
		this.destinationDir = destinationDir;

		this.progress = 0;
		this.fileSize = 0;
		this.attempt = 0;

		this.status = "Waiting";

		String cutcut[] = key.split("/");

		if(!key.endsWith("/")) {
			this.filename = cutcut[cutcut.length-1];
		} else {
			this.filename = "index.html";
		}

		Logger.debug(this, "Query for getting "+key+" created");

	}

	public boolean start(FCPQueueManager queueManager) {
		this.attempt++;
		this.running = true;
		this.progress = 0;

		this.queueManager = queueManager;

		this.status = "Requesting";

		if(this.identifier == null || "".equals( this.identifier ))
			this.identifier = queueManager.getAnID() + "-"+this.filename;;

		Logger.info(this, "Requesting key : "+this.getFileKey());

		FCPMessage queryMessage = new FCPMessage();

		queryMessage.setMessageName("ClientGet");
		queryMessage.setValue("URI", this.getFileKey());
		queryMessage.setValue("Identifier", this.identifier);
		queryMessage.setValue("Verbosity", "1");
		queryMessage.setValue("MaxRetries", Integer.toString(this.maxRetries));
		queryMessage.setValue("PriorityClass", Integer.toString(this.priority));

		if(this.destinationDir != null)
			queryMessage.setValue("ClientToken", this.destinationDir);

		if(this.persistence == 0)
			queryMessage.setValue("Persistence", "forever");
		if(this.persistence == 1)
			queryMessage.setValue("Persistence", "reboot");
		if(this.persistence == 2)
			queryMessage.setValue("Persistence", "connection");

		if(this.globalQueue)
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

		FCPQueryManager queryManager = null;
		FCPMessage message = (FCPMessage)arg;

		if (o instanceof FCPQueryManager)
			queryManager = (FCPQueryManager)o;
		else
			queryManager = this.queueManager.getQueryManager(); /* default one */

		if(message.getValue("Identifier") == null
		   || !message.getValue("Identifier").equals(this.identifier))
			return;

		if("DataFound".equals( message.getMessageName() )) {
			Logger.debug(this, "DataFound!");

			if(!this.isFinished()) {
				if(!this.alreadySaved) {
					this.alreadySaved = true;

					this.fileSize = (new Long(message.getValue("DataLength"))).longValue();

					if(this.isPersistent()) {
						if(this.destinationDir != null) {

							if(!this.fileExists(this.destinationDir)) {
								this.status = "Requesting file from the node";
								this.progress = 99;
								this.running = true;
								this.successful = false;
								this.saveFileTo(this.destinationDir, false);
							} else {
								this.status = "Available";
								this.progress = 100;
								this.running = false;
								this.successful = true;
								Logger.info(this, "File already existing. Not rewrited");
							}

						} else {
							Logger.info(this, "Don't know where to put file, so file not asked to the node");
						}
					}

					this.setChanged();
					this.notifyObservers();
				}
			}

			return;
		}

		if("IdentifierCollision".equals( message.getMessageName() )) {
			Logger.notice(this, "IdentifierCollision ! Resending with another id");

			this.identifier = null;
			this.start(this.queueManager);

			this.setChanged();
			this.notifyObservers();

			return;
		}

		if("ProtocolError".equals( message.getMessageName() )) {
			Logger.debug(this, "ProtocolError !");

			if("15".equals( message.getValue("Code") )) {
				Logger.debug(this, "Unknow URI ? was probably a stop order so no problem ...");
				return;
			}

			Logger.error(this, "=== PROTOCOL ERROR === \n"+message.toString());

			this.status = "Protocol Error ("+message.getValue("CodeDescription")+")";
			this.progress = 100;
			this.running = false;
			this.successful = false;
			this.fatal = true;

			this.setChanged();
			this.notifyObservers();

			if(message.getValue("Fatal") != null &&
			   message.getValue("Fatal").equals("false")) {
				this.fatal = false;
				this.status = this.status + " (non-fatal)";
			}

			if(this.isLockOwner) {
				this.queueManager.getQueryManager().getConnection().unlockReading();
				this.queueManager.getQueryManager().getConnection().unlockWriting();
				this.isLockOwner= false;
			}

			this.queueManager.getQueryManager().deleteObserver(this);

			return;
		}

		if(message.getMessageName().equals("GetFailed")) {
			Logger.debug(this, "GetFailed !");

			if (message.getValue("RedirectURI") != null) {
				Logger.debug(this, "Redirected !");
				this.key = message.getValue("RedirectURI");
				this.status = "Redirected ...";
				this.start(this.queueManager);
				return;
			}

			Logger.warning(this, "==== GET FAILED ===\n"+message.toString());

			if(!this.isRunning()) { /* Must be a "GetFailed: cancelled by caller", so we simply ignore */
				Logger.info(this, "Cancellation confirmed.");
				return;
			}

			//removeRequest();

			int code = Integer.parseInt(message.getValue("Code"));

			if(this.maxRetries == -1 || this.attempt >= this.maxRetries || code == 25) {
			    this.status = "Failed ("+message.getValue("CodeDescription")+")";
			    this.progress = 100;
			    this.running = false;
			    this.successful = false;

			    this.fatal = true;

			    if(message.getValue("Fatal") != null &&
			       message.getValue("Fatal").equals("false")) {
				    this.fatal = false;
				    this.status = this.status + " (non-fatal)";
			    }

			    this.queueManager.getQueryManager().deleteObserver(this);
			} else {
			    this.status = "Retrying";
			    this.running = true;
			    this.successful = true;
			    this.progress = 0;
			    this.start(this.queueManager);
			}

			this.setChanged();
			this.notifyObservers();

			return;
		}

		if(message.getMessageName().equals("SimpleProgress")) {
			Logger.debug(this, "SimpleProgress !");

			this.progress = 0;

			if(message.getValue("Total") != null
			   && message.getValue("Succeeded") != null) {
				this.fileSize = Long.parseLong(message.getValue("Total"))*BLOCK_SIZE;
				long required = Long.parseLong(message.getValue("Total"));
				long succeeded = Long.parseLong(message.getValue("Succeeded"));

				this.progress = (int) ((long)((succeeded * 98) / required));

				this.status = "Fetching";

				if(message.getValue("FinalizedTotal") != null &&
				   message.getValue("FinalizedTotal").equals("true")) {
					this.progressReliable = true;
				}

				this.successful = true;
				this.running = true;

				this.setChanged();
				this.notifyObservers();
			}

			return;
		}

		if(message.getMessageName().equals("AllData")) {
			Logger.debug(this, "AllData ! : " + this.identifier);

			this.fileSize = message.getAmountOfDataWaiting();

			this.running = true;
			this.successful = true;
			this.progress = 99;
			this.status = "Writing to disk";
			Logger.info(this, "Receiving file ...");

			this.setChanged();
			this.notifyObservers();


			if(this.fetchDirectly(queryManager.getConnection(), this.fileSize, true)) {
				this.successful = true;
				this.status = "Available";
			} else {
				Logger.warning(this, "Unable to fetch correctly the file. This may create problems on socket");
				this.successful = false;
				this.status = "Error while receveing the file";
			}

			Logger.info(this, "File received");

			queryManager.getConnection().unlockReading();
			queryManager.getConnection().unlockWriting();


			this.isLockOwner= false;

			this.running = false;
			this.progress = 100;

			queryManager.deleteObserver(this);

			if (queryManager != this.queueManager.getQueryManager()) {
				this.queueManager.getQueryManager().deleteObserver(this);
				queryManager.getConnection().disconnect();
				this.duplicatedQueryManager = null;
			}

			this.setChanged();
			this.notifyObservers();


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
			if(this.dir == null) {
				Logger.warning(this, "UnlockWaiter.run() : Wtf ?");
			}

			try {
				Thread.sleep((new java.util.Random()).nextInt(1500));
			} catch(java.lang.InterruptedException e) {

			}

			while(true) {
				if(!this.connection.isReadingLocked()
				   && (!this.connection.isWritingLocked()))
					break;

				try {
					Thread.sleep(500);
				} catch(java.lang.InterruptedException e) {

				}
			}

			if(!this.connection.lockReading()) {
				/* Ah ben oué mais non */
				this.run(); /* TODO: It's dirty => To change ! */
				return;
			}

			if(!this.connection.lockWriting()) {
				/* Ah ben oué mais non */
				this.connection.unlockReading();
				this.run(); /* TODO: It's dirty => To change ! */
				return;
			}

			FCPClientGet.this.isLockOwner = true;

			Logger.debug(this, "I take the reading lock !");

			if(this.dir == null) {
				Logger.warning(this, "UnlockWaiter.run() : Wtf ?");
			}

			this.clientGet.continueSaveFileTo(this.dir);
			return;
		}
	}

	public boolean saveFileTo(String dir) {
		return this.saveFileTo(dir, true);
	}

	public synchronized boolean saveFileTo(String dir, boolean checkStatus) {
		this.fromTheNodeProgress = 0;

		Logger.info(this, "Saving file to '"+dir+"'");

		if(dir == null) {
			Logger.warning(this, "saveFileTo() : Can't save to null.");
			return false;
		}

		this.destinationDir = dir;


		if(checkStatus && (!this.isFinished() || !this.isSuccessful())) {
			Logger.warning(this, "Unable to fetch a file not finished");
			return false;
		}

		if(!this.isPersistent()) {
			Logger.warning(this, "Not persistent, so unable to ask");
			return false;
		}

		Logger.info(this, "Duplicating socket ...");

		this.duplicatedQueryManager = this.queueManager.getQueryManager().duplicate(this.identifier);
		this.duplicatedQueryManager.addObserver(this);

		Logger.info(this, "Waiting for socket  ...");
		this.status = "Waiting for socket availability ...";
		this.progress = 99;
		this.running = true;

		this.setChanged();
		this.notifyObservers();


		Thread fork = new Thread(new UnlockWaiter(this, this.duplicatedQueryManager.getConnection(), dir));
		fork.start();

		return true;
	}

	public synchronized boolean continueSaveFileTo(String dir) {

		Logger.info(this, "Asking file '"+this.filename+"' to the node...");

		this.destinationDir = dir;

		this.status = "Requesting file";
		this.progress = 99;
		this.running = true;
		this.setChanged();
		this.notifyObservers();

		if(this.destinationDir == null) {
			Logger.warning(this, "saveFileTo() : Wtf ?");
		}

		FCPMessage getRequestStatus = new FCPMessage();

		getRequestStatus.setMessageName("GetRequestStatus");
		getRequestStatus.setValue("Identifier", this.identifier);
		if(this.globalQueue)
			getRequestStatus.setValue("Global", "true");
		else
			getRequestStatus.setValue("Global", "false");
		getRequestStatus.setValue("OnlyData", "true");

		this.duplicatedQueryManager.writeMessage(getRequestStatus, false);

		return true;
	}


	private boolean fileExists(String dir) {
		this.destinationDir = dir;
		File newFile = new File(this.getPath());
		return newFile.exists();
	}


	private boolean fetchDirectly(FCPConnection connection, long size, boolean reallyWrite) {
		String file = this.getPath();
		File newFile = null;
		OutputStream outputStream = null;

		if (file != null) {
			newFile = new File(file);
		} else {
			try {
				Logger.info(this, "Using temporary file");
				newFile = File.createTempFile("thaw_", ".tmp");
				finalPath = newFile.getPath();
				newFile.deleteOnExit();
			} catch(java.io.IOException e) {
				Logger.error(this, "Error while creating temporary file: "+e.toString());
			}
		}

		if(reallyWrite) {
			Logger.info(this, "Getting file from node ... ");

			try {
				outputStream = new FileOutputStream(newFile);
			} catch(java.io.IOException e) {
				Logger.error(this, "Unable to write file on disk ... disk space / perms ? : "+e.toString());
				this.status = "Write error";
				return false;
			}

		} else {
			Logger.info(this, "File is supposed already written. Not rewriting.");
		}

		/* size == bytes remaining on socket */
		long origSize = size;
		long startTime = System.currentTimeMillis();

		boolean writingSuccessful = true;

		while(size > 0) {

			int packet = PACKET_SIZE;
			byte[] read;
			int amount;

			if(size < (long)packet)
				packet = (int)size;

			read = new byte[packet];

			amount = connection.read(packet, read);

			if(amount <= -1) {
				Logger.error(this, "Socket closed, damn !");
				this.status = "Read error";
				this.successful = false;
				writingSuccessful = false;
				break;
			}

			if(reallyWrite) {
				try {
					outputStream.write(read, 0, amount);
				} catch(java.io.IOException e) {
					Logger.error(this, "Unable to write file on disk ... out of space ? : "+e.toString());
					this.status = "Write error";
					writingSuccessful = false;
					this.successful = false;
					return false;
				}
			}

			size = size - amount;

			if( System.currentTimeMillis() >= (startTime+3000)) {
				this.status = "Writing to disk";
				this.fromTheNodeProgress = (int) (((origSize-size) * 100) / origSize);
				this.setChanged();
				this.notifyObservers();
				startTime = System.currentTimeMillis();
			}

		}

		this.fromTheNodeProgress = 100;

		if(reallyWrite) {
			try {
				outputStream.close();

				if(!writingSuccessful && newFile != null)
					newFile.delete();

			} catch(java.io.IOException e) {
				Logger.notice(this, "Unable to close correctly file on disk !? : "+e.toString());
			}
		}

		Logger.info(this, "File written");


		return true;
	}



	public boolean removeRequest() {
		FCPMessage stopMessage = new FCPMessage();

		if(!this.isPersistent()) {
			Logger.notice(this, "Can't remove non persistent request.");
			return false;
		}

		if(this.identifier == null) {
			Logger.notice(this, "Can't remove non-started queries");
			return true;
		}

		stopMessage.setMessageName("RemovePersistentRequest");

		if(this.globalQueue)
			stopMessage.setValue("Global", "true");
		else
			stopMessage.setValue("Global", "false");

		stopMessage.setValue("Identifier", this.identifier);

		this.queueManager.getQueryManager().writeMessage(stopMessage);

		this.running = false;

		return true;
	}

	public boolean pause(FCPQueueManager queryManager) {
		/* TODO ? : Reduce the priority
		   instead of stopping */

		Logger.info(this, "Pausing fetching of the key : "+this.getFileKey());

		this.removeRequest();

		this.progress = 0;
		this.running = false;
		this.successful = false;
		this.status = "Delayed";

		this.setChanged();
		this.notifyObservers();

		return true;

	}

	public boolean stop(FCPQueueManager queryManager) {
		Logger.info(this, "Stop fetching of the key : "+this.getFileKey());

		if(!this.removeRequest()) {
			return false;
		}

		this.progress = 100;
		this.running = false;
		this.successful = false;
		this.fatal = true;
		this.status = "Stopped";
		this.setChanged();
		this.notifyObservers();

		return true;
	}


	public void updatePersistentRequest(boolean clientToken) {
		if(!this.isPersistent())
			return;

		FCPMessage msg = new FCPMessage();

		msg.setMessageName("ModifyPersistentRequest");
		msg.setValue("Global", Boolean.toString(this.globalQueue));
		msg.setValue("Identifier", this.identifier);
		msg.setValue("PriorityClass", Integer.toString(this.priority));

		if(clientToken && this.destinationDir != null)
			msg.setValue("ClientToken", this.destinationDir);

		this.queueManager.getQueryManager().writeMessage(msg);

	}


	public int getThawPriority() {
		return this.priority;
	}

	public int getFCPPriority() {
		return this.priority;
	}

	public void setFCPPriority(int prio) {
		Logger.info(this, "Setting priority to "+Integer.toString(prio));

		this.priority = prio;

		this.setChanged();
		this.notifyObservers();
	}

	public int getQueryType() {
		return 1;
	}

	public String getStatus() {
		return this.status;
	}

	public int getProgression() {
		return this.progress;
	}

	public boolean isProgressionReliable() {
		if(this.progress == 0 || this.progress >= 99)
			return true;

		return this.progressReliable;
	}

	public String getFileKey() {
		return this.key;
	}

	public boolean isFinished() {
		if(this.progress >= 100)
			return true;

		return false;
	}

	public long getFileSize() {
		return this.fileSize;
	}

	public String getPath() {
		if (finalPath != null)
			return finalPath;

		if(this.destinationDir != null)
			return this.destinationDir + File.separator + this.filename;

		return null;
	}

	public String getFilename() {
		return this.filename;
	}

	public int getAttempt() {
		return this.attempt;
	}

	public void setAttempt(int x) {
		this.attempt = x;

		if(x == 0) {
			/* We suppose it's a restart */
			this.progress = 0;
		}
	}

	public int getMaxAttempt() {
		return this.maxRetries;
	}

	public boolean isSuccessful() {
		return this.successful;
	}

	public boolean isFatallyFailed() {
		return ((!this.successful) && this.fatal);
	}

	public boolean isRunning() {
		return this.running;
	}

	public HashMap getParameters() {
		HashMap result = new HashMap();

		result.put("URI", this.key);
		result.put("Filename", this.filename);
		result.put("Priority", Integer.toString(this.priority));
		result.put("Persistence", Integer.toString(this.persistence));
		result.put("Global", Boolean.toString(this.globalQueue));
		result.put("ClientToken", this.destinationDir);
		result.put("Attempt", Integer.toString(this.attempt));

		result.put("status", this.status);

       		result.put("Identifier", this.identifier);
		result.put("Progress", Integer.toString(this.progress));
		result.put("FileSize", Long.toString(this.fileSize));
		result.put("Running", Boolean.toString(this.running));
		result.put("Successful", Boolean.toString(this.successful));
		result.put("MaxRetries", Integer.toString(this.maxRetries));

		return result;
	}

	public boolean setParameters(HashMap parameters) {

		this.key            = (String)parameters.get("URI");

		Logger.debug(this, "Resuming key : "+this.key);

		this.filename       = (String)parameters.get("Filename");
		this.priority       = Integer.parseInt((String)parameters.get("Priority"));
		this.persistence    = Integer.parseInt((String)parameters.get("Persistence"));
		this.globalQueue    = Boolean.valueOf((String)parameters.get("Global")).booleanValue();
		this.destinationDir = (String)parameters.get("ClientToken");
		this.attempt        = Integer.parseInt((String)parameters.get("Attempt"));
		this.status         = (String)parameters.get("Status");
		this.identifier     = (String)parameters.get("Identifier");

		Logger.info(this, "Resuming id : "+this.identifier);

		this.progress       = Integer.parseInt((String)parameters.get("Progress"));
		this.fileSize       = Long.parseLong((String)parameters.get("FileSize"));
		this.running        = Boolean.valueOf((String)parameters.get("Running")).booleanValue();
		this.successful     = Boolean.valueOf((String)parameters.get("Successful")).booleanValue();
		this.maxRetries     = Integer.parseInt((String)parameters.get("MaxRetries"));

		if(this.persistence == 2 && !this.isFinished()) {
			this.progress = 0;
			this.running = false;
			this.status = "Waiting";
		}

		return true;
	}


	public boolean isPersistent() {
		return (this.persistence < 2);
	}


	public String getIdentifier() {
		if(this.identifier == null || this.identifier.equals(""))
			return null;

		return this.identifier;
	}

	public boolean isGlobal() {
		return this.globalQueue;
	}

	public int getTransferWithTheNodeProgression() {
		return this.fromTheNodeProgress;
	}
}
