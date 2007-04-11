package thaw.fcp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import thaw.core.FreenetURIHelper;
import thaw.core.Logger;

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
	private long maxSize = 0;

	private boolean running = false;
	private boolean successful = true;
	private boolean writingSuccessful = true;
	private boolean fatal = true;
	private boolean isLockOwner = false;

	private boolean alreadySaved = false;

	private boolean noDDA = false;

	/**
	 * See setParameters().
	 */
	public FCPClientGet(final FCPQueueManager queueManager, final HashMap parameters) {
		this.queueManager = queueManager;
		setParameters(parameters);

		progressReliable = false;
		fromTheNodeProgress = 0;

		/* If isPersistent(), then start() won't be called, so must relisten the
		   queryManager by ourself */
		if(isPersistent() && (identifier != null) && !identifier.equals("")) {
			this.queueManager.getQueryManager().deleteObserver(this);
			this.queueManager.getQueryManager().addObserver(this);
		}

	}


	/**
	 * Used to resume query from persistent queue of the node.
	 * Think of adding this FCPClientGet as a queryManager observer.
	 * @param destinationDir if null, then a temporary file will be create (path determined only when the file is availabed ; this file will be deleted on jvm exit)
	 */
	public FCPClientGet(final String id, final String key, final int priority,
			    final int persistence, final boolean globalQueue,
			    final String destinationDir, String status, final int progress,
			    final int maxRetries,
			    final FCPQueueManager queueManager) {

		this(key, priority, persistence, globalQueue, maxRetries, destinationDir);

		progressReliable = false;

		this.queueManager = queueManager;

		this.progress = progress;
		this.status = status;

		if(status == null) {
			Logger.warning(this, "status == null ?!");
			status = "(null)";
		}

		identifier = id;

		successful = true;
		running = true;

		if(progress < 100) {
			fromTheNodeProgress = 0;
		} else {
			fromTheNodeProgress = 100;
			progressReliable = true;
		}

	}


	/**
	 * See the other entry point
	 * @param noDDA refuse the use of DDA (if true, request must be *NOT* *PERSISTENT*)
	 */
	public FCPClientGet(final String key, final int priority,
			    final int persistence, boolean globalQueue,
			    final int maxRetries,
			    String destinationDir,
			    boolean noDDA) {
		this(key, priority, persistence, globalQueue, maxRetries, destinationDir);
		this.noDDA = noDDA;
	}


	/**
	 * Entry point:
	 * Only for initial queries : To resume queries, use FCPClientGet(FCPQueueManager, Hashmap).
	 * @param destinationDir if null => temporary file
	 * @param persistence 0 = Forever ; 1 = Until node reboot ; 2 = Until the app disconnect
	 */
	public FCPClientGet(final String key, final int priority,
			    final int persistence, boolean globalQueue,
			    final int maxRetries,
			    String destinationDir) {


		if(globalQueue && (persistence >= 2))
			globalQueue = false; /* else protocol error */

		progressReliable = false;
		fromTheNodeProgress = 0;

		this.maxRetries = maxRetries;
		this.key = key;
		this.priority = priority;
		this.persistence = persistence;
		this.globalQueue = globalQueue;
		this.destinationDir = destinationDir;

		progress = 0;
		fileSize = 0;
		attempt = 0;

		status = "Waiting";

		filename = FreenetURIHelper.getFilenameFromKey(key);

		try {
			filename = java.net.URLDecoder.decode(filename, "UTF-8");
		} catch (final java.io.UnsupportedEncodingException e) {
			Logger.warning(this, "UnsupportedEncodingException (UTF-8): "+e.toString());
		}

		Logger.debug(this, "Query for getting "+key+" created");

	}


	/**
	 * See the other entry point
	 * @param noDDA refuse the use of DDA (if true, request must be *NOT* *PERSISTENT*)
	 */
	public FCPClientGet(final String key, final int priority,
			    final int persistence, boolean globalQueue,
			    final int maxRetries,
			    String destinationDir,
			    long maxSize,
			    boolean noDDA) {
		this(key, priority, persistence, globalQueue, maxRetries, destinationDir, maxSize);
		this.noDDA = noDDA;
	}

	/**
	 * Another entry point allowing to specify a max size
	 */
	public FCPClientGet(final String key, final int priority,
			    final int persistence, boolean globalQueue,
			    final int maxRetries,
			    String destinationDir,
			    long maxSize) {
		this(key, priority, persistence, globalQueue, maxRetries,
		     destinationDir);
		this.maxSize = maxSize;
	}


	public boolean start(final FCPQueueManager queueManager) {
		attempt++;
		running = true;
		progress = 0;

		this.queueManager = queueManager;

		if (finalPath == null && destinationDir == null) {
			if ((destinationDir = System.getProperty("java.io.tmpdir")) == null) {
				Logger.error(this, "Unable to find temporary directory ! Will create troubles !");
				destinationDir = "";
			}
			else
				Logger.notice(this, "Using temporary file: "+getPath());
		}

		status = "Requesting";

		if((identifier == null) || "".equals( identifier ))
			identifier = queueManager.getAnID() + "-"+filename;;

		Logger.debug(this, "Requesting key : "+getFileKey());

		final FCPMessage queryMessage = new FCPMessage();

		queryMessage.setMessageName("ClientGet");
		queryMessage.setValue("URI", getFileKey());
		queryMessage.setValue("Identifier", identifier);
		queryMessage.setValue("Verbosity", "1");
		queryMessage.setValue("MaxRetries", Integer.toString(maxRetries));
		queryMessage.setValue("PriorityClass", Integer.toString(priority));

		if (maxSize > 0)
			queryMessage.setValue("MaxSize", Long.toString(maxSize));

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

		if (!queueManager.getQueryManager().getConnection().isLocalSocket() && !noDDA)
			queryMessage.setValue("ReturnType", "direct");
		else {
			queryMessage.setValue("ReturnType", "disk");
			queryMessage.setValue("Filename", getPath());

			if (getPath() == null) {
				Logger.error(this, "getPath() returned null ! Will create troubles !");
			}
		}

		queueManager.getQueryManager().deleteObserver(this);
		queueManager.getQueryManager().addObserver(this);

		return queueManager.getQueryManager().writeMessage(queryMessage);
	}


	public void update(final Observable o, final Object arg) {

		FCPQueryManager queryManager = null;
		final FCPMessage message = (FCPMessage)arg;

		if (o instanceof FCPQueryManager)
			queryManager = (FCPQueryManager)o;
		else
			queryManager = queueManager.getQueryManager(); /* default one */

		if((message.getValue("Identifier") == null)
		   || !message.getValue("Identifier").equals(identifier))
			return;

		if("DataFound".equals( message.getMessageName() )) {
			Logger.debug(this, "DataFound!");

			if(!isFinished()) {
				if(!alreadySaved) {
					alreadySaved = true;

					fileSize = (new Long(message.getValue("DataLength"))).longValue();

					if(isPersistent()
					   || (queueManager.getQueryManager().getConnection().isLocalSocket() && !noDDA)) {
						if(destinationDir != null) {
							if(!fileExists()
							   && !(queueManager.getQueryManager().getConnection().isLocalSocket() && !noDDA)) {
								status = "Requesting file from the node";
								progress = 99;
								running = true;
								successful = true;
								writingSuccessful = false;
								saveFileTo(destinationDir, false);
							} else {
								status = "Available";
								progress = 100;
								running = false;
								successful = true;
								writingSuccessful = true;
								Logger.info(this, "File already existing. Not rewrited");
							}

						} else {
							Logger.info(this, "Don't know where to put file, so file not asked to the node");
						}
					}
				}

				setChanged();
				notifyObservers();
			}

			return;
		}

		if("IdentifierCollision".equals( message.getMessageName() )) {
			Logger.notice(this, "IdentifierCollision ! Resending with another id");

			identifier = null;
			start(queueManager);

			setChanged();
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

			status = "Protocol Error ("+message.getValue("CodeDescription")+")";
			progress = 100;
			running = false;
			successful = false;
			fatal = true;

			if((message.getValue("Fatal") != null) &&
			   message.getValue("Fatal").equals("false")) {
				fatal = false;
				status = status + " (non-fatal)";
			}

			if(isLockOwner) {
				if (duplicatedQueryManager != null)
					duplicatedQueryManager.getConnection().removeFromWriterQueue();
				isLockOwner= false;
			}

			setChanged();
			notifyObservers();

			queueManager.getQueryManager().deleteObserver(this);

			return;
		}

		if(message.getMessageName().equals("GetFailed")) {
			Logger.debug(this, "GetFailed !");

			if (message.getValue("RedirectURI") != null) {
				Logger.debug(this, "Redirected !");
				key = message.getValue("RedirectURI");
				status = "Redirected ...";
				start(queueManager);
				return;
			}

			Logger.warning(this, "==== GET FAILED ===\n"+message.toString());

			if(!isRunning()) { /* Must be a "GetFailed: cancelled by caller", so we simply ignore */
				Logger.info(this, "Cancellation confirmed.");
				return;
			}

			//removeRequest();

			final int code = Integer.parseInt(message.getValue("Code"));

			if((maxRetries == -1) || (attempt >= maxRetries) || (code == 25)) {
			    status = "Failed ("+message.getValue("CodeDescription")+")";
			    progress = 100;
			    running = false;
			    successful = false;

			    fatal = true;

			    if((message.getValue("Fatal") != null) &&
			       message.getValue("Fatal").equals("false")) {
				    fatal = false;
				    status = status + " (non-fatal)";
			    }

			    queueManager.getQueryManager().deleteObserver(this);
			} else {
			    status = "Retrying";
			    running = true;
			    successful = true;
			    progress = 0;
			    start(queueManager);
			}

			setChanged();
			this.notifyObservers();

			return;
		}

		if(message.getMessageName().equals("SimpleProgress")) {
			Logger.debug(this, "SimpleProgress !");

			progress = 0;

			if((message.getValue("Total") != null)
			   && (message.getValue("Succeeded") != null)) {
				fileSize = Long.parseLong(message.getValue("Total"))*FCPClientGet.BLOCK_SIZE;
				final long required = Long.parseLong(message.getValue("Total"));
				final long succeeded = Long.parseLong(message.getValue("Succeeded"));

				progress = (int) ((long)((succeeded * 98) / required));

				status = "Fetching";

				if((message.getValue("FinalizedTotal") != null) &&
				   message.getValue("FinalizedTotal").equals("true")) {
					progressReliable = true;
				}

				successful = true;
				running = true;
			}

			setChanged();
			this.notifyObservers();

			return;
		}

		if(message.getMessageName().equals("AllData")) {
			Logger.debug(this, "AllData ! : " + identifier);

			fileSize = message.getAmountOfDataWaiting();

			running = true;
			successful = true;
			progress = 99;
			status = "Writing to disk";
			Logger.info(this, "Receiving file ...");

			setChanged();
			this.notifyObservers();

			successful = true;

			if(fetchDirectly(queryManager.getConnection(), fileSize, true)) {
				writingSuccessful = true;
				status = "Available";
			} else {
				Logger.warning(this, "Unable to fetch correctly the file. This may create problems on socket");
				writingSuccessful = false;
				status = "Error while receveing the file";
			}

			Logger.info(this, "File received");

			if (duplicatedQueryManager != null)
				duplicatedQueryManager.getConnection().removeFromWriterQueue();

			isLockOwner= false;

			running = false;
			progress = 100;

			queryManager.deleteObserver(this);

			if (queryManager != queueManager.getQueryManager()) {
				queueManager.getQueryManager().deleteObserver(this);
				queryManager.getConnection().disconnect();
				duplicatedQueryManager = null;
			}

			setChanged();
			notifyObservers();


			return;
		}

		if(message.getMessageName().equals("PersistentGet")) {
			Logger.debug(this, "PersistentGet !");
			setChanged();
			notifyObservers();
			return;
		}

		Logger.warning(this, "Unknow message : "+message.getMessageName() + " !");
	}


	private class UnlockWaiter implements Runnable {
		FCPClientGet clientGet;
		FCPConnection c;
		String dir;

		public UnlockWaiter(final FCPClientGet clientGet, final FCPConnection c, final String dir) {
			this.clientGet = clientGet;
			this.dir = dir;
			this.c = c;
		}

		public void run() {
			c.addToWriterQueue();
			isLockOwner = true;

			Logger.debug(this, "I take the lock !");

			if(dir == null) {
				Logger.warning(this, "UnlockWaiter.run() : Wtf ?");
			}

			clientGet.continueSaveFileTo(dir);
			return;
		}
	}

	public boolean saveFileTo(final String dir) {
		return saveFileTo(dir, true);
	}

	public synchronized boolean saveFileTo(final String dir, final boolean checkStatus) {
		fromTheNodeProgress = 0;

		Logger.info(this, "Saving file to '"+dir+"'");

		if(dir == null) {
			Logger.warning(this, "saveFileTo() : Can't save to null.");
			return false;
		}

		destinationDir = dir;


		if(checkStatus && (!isFinished() || !isSuccessful())) {
			Logger.warning(this, "Unable to fetch a file not finished");
			return false;
		}

		if(!isPersistent()) {
			Logger.warning(this, "Not persistent, so unable to ask");
			return false;
		}

		Logger.info(this, "Duplicating socket ...");

		duplicatedQueryManager = queueManager.getQueryManager().duplicate(identifier);
		duplicatedQueryManager.addObserver(this);

		Logger.info(this, "Waiting for socket  ...");
		status = "Waiting for socket availability ...";
		progress = 99;
		running = true;

		setChanged();
		this.notifyObservers();


		final Thread fork = new Thread(new UnlockWaiter(this, duplicatedQueryManager.getConnection(), dir));
		fork.start();

		return true;
	}

	public synchronized boolean continueSaveFileTo(final String dir) {

		Logger.info(this, "Asking file '"+filename+"' to the node...");

		destinationDir = dir;

		status = "Requesting file";
		progress = 99;
		running = true;
		setChanged();
		this.notifyObservers();

		if(destinationDir == null) {
			Logger.warning(this, "saveFileTo() : Wtf ?");
		}

		final FCPMessage getRequestStatus = new FCPMessage();

		getRequestStatus.setMessageName("GetRequestStatus");
		getRequestStatus.setValue("Identifier", identifier);
		if(globalQueue)
			getRequestStatus.setValue("Global", "true");
		else
			getRequestStatus.setValue("Global", "false");
		getRequestStatus.setValue("OnlyData", "true");

		duplicatedQueryManager.writeMessage(getRequestStatus, false);

		return true;
	}


	private boolean fileExists() {
		final File newFile = new File(getPath());
		return newFile.exists();
	}


	private boolean fetchDirectly(final FCPConnection connection, long size, final boolean reallyWrite) {
		final String file = getPath();
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
			} catch(final java.io.IOException e) {
				Logger.error(this, "Error while creating temporary file: "+e.toString());
			}
		}

		if(reallyWrite) {
			Logger.info(this, "Getting file from node ... ");

			try {
				outputStream = new FileOutputStream(newFile);
			} catch(final java.io.IOException e) {
				Logger.error(this, "Unable to write file on disk ... disk space / perms ? : "+e.toString());
				status = "Write error";
				return false;
			}

		} else {
			Logger.info(this, "File is supposed already written. Not rewriting.");
		}

		/* size == bytes remaining on socket */
		final long origSize = size;
		long startTime = System.currentTimeMillis();

		writingSuccessful = true;

		while(size > 0) {

			int packet = FCPClientGet.PACKET_SIZE;
			byte[] read;
			int amount;

			if(size < (long)packet)
				packet = (int)size;

			read = new byte[packet];

			amount = connection.read(packet, read);

			if(amount <= -1) {
				Logger.error(this, "Socket closed, damn !");
				status = "Read error";
				writingSuccessful = false;
				break;
			}

			if(reallyWrite) {
				try {
					outputStream.write(read, 0, amount);
				} catch(final java.io.IOException e) {
					Logger.error(this, "Unable to write file on disk ... out of space ? : "+e.toString());
					status = "Unable to fetch / disk probably full !";
					writingSuccessful = false;
					return false;
				}
			}

			size = size - amount;

			if( System.currentTimeMillis() >= (startTime+3000)) {
				status = "Writing to disk";
				fromTheNodeProgress = (int) (((origSize-size) * 100) / origSize);
				setChanged();
				this.notifyObservers();
				startTime = System.currentTimeMillis();
			}

		}

		fromTheNodeProgress = 100;

		if(reallyWrite) {
			try {
				outputStream.close();

				if(!writingSuccessful && (newFile != null))
					newFile.delete();

			} catch(final java.io.IOException e) {
				Logger.notice(this, "Unable to close correctly file on disk !? : "+e.toString());
			}
		}

		Logger.info(this, "File written");


		return true;
	}



	public boolean removeRequest() {
		final FCPMessage stopMessage = new FCPMessage();

		if(!isPersistent()) {
			Logger.notice(this, "Can't remove non persistent request.");
			return false;
		}

		if(identifier == null) {
			Logger.notice(this, "Can't remove non-started queries");
			return true;
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

	public boolean pause(final FCPQueueManager queryManager) {
		/* TODO ? : Reduce the priority
		   instead of stopping */

		Logger.info(this, "Pausing fetching of the key : "+getFileKey());

		removeRequest();

		progress = 0;
		running = false;
		successful = false;
		status = "Delayed";

		setChanged();
		this.notifyObservers();

		return true;

	}

	public boolean stop(final FCPQueueManager queryManager) {
		Logger.info(this, "Stop fetching of the key : "+getFileKey());

		if(!removeRequest())
			return false;

		if (progress < 100)
			successful = false;

		progress = 100;
		running = false;
		fatal = true;
		status = "Stopped";
		setChanged();
		this.notifyObservers();

		return true;
	}


	public void updatePersistentRequest(final boolean clientToken) {
		if(!isPersistent())
			return;

		final FCPMessage msg = new FCPMessage();

		msg.setMessageName("ModifyPersistentRequest");
		msg.setValue("Global", Boolean.toString(globalQueue));
		msg.setValue("Identifier", identifier);
		msg.setValue("PriorityClass", Integer.toString(priority));

		if(clientToken && (destinationDir != null))
			msg.setValue("ClientToken", destinationDir);

		queueManager.getQueryManager().writeMessage(msg);

	}


	public int getThawPriority() {
		return priority;
	}

	public int getFCPPriority() {
		return priority;
	}

	public void setFCPPriority(final int prio) {
		Logger.info(this, "Setting priority to "+Integer.toString(prio));

		priority = prio;

		setChanged();
		this.notifyObservers();
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
		if((progress == 0) || (progress >= 99))
			return true;

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
		if (finalPath != null)
			return finalPath;

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

	public void setAttempt(final int x) {
		attempt = x;

		if(x == 0) {
			/* We suppose it's a restart */
			progress = 0;
		}
	}

	public int getMaxAttempt() {
		return maxRetries;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public boolean isWritingSuccessful() {
		return writingSuccessful;
	}

	public boolean isFatallyFailed() {
		return ((!successful) && fatal);
	}

	public boolean isRunning() {
		return running;
	}

	public HashMap getParameters() {
		final HashMap result = new HashMap();

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
		result.put("MaxRetries", Integer.toString(maxRetries));

		return result;
	}

	public boolean setParameters(final HashMap parameters) {

		key            = (String)parameters.get("URI");

		Logger.debug(this, "Resuming key : "+key);

		filename       = (String)parameters.get("Filename");
		priority       = Integer.parseInt((String)parameters.get("Priority"));
		persistence    = Integer.parseInt((String)parameters.get("Persistence"));
		globalQueue    = Boolean.valueOf((String)parameters.get("Global")).booleanValue();
		destinationDir = (String)parameters.get("ClientToken");
		attempt        = Integer.parseInt((String)parameters.get("Attempt"));
		status         = (String)parameters.get("Status");
		identifier     = (String)parameters.get("Identifier");

		Logger.info(this, "Resuming id : "+identifier);

		progress       = Integer.parseInt((String)parameters.get("Progress"));
		fileSize       = Long.parseLong((String)parameters.get("FileSize"));
		running        = Boolean.valueOf((String)parameters.get("Running")).booleanValue();
		successful     = Boolean.valueOf((String)parameters.get("Successful")).booleanValue();
		maxRetries     = Integer.parseInt((String)parameters.get("MaxRetries"));

		if((persistence == 2) && !isFinished()) {
			progress = 0;
			running = false;
			status = "Waiting";
		}

		return true;
	}


	public boolean isPersistent() {
		return (persistence < 2);
	}


	public String getIdentifier() {
		if((identifier == null) || identifier.equals(""))
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
