package thaw.fcp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import thaw.core.Logger;

public class FCPClientGet extends Observable
	implements Observer, FCPTransferQuery {

	public final static int DEFAULT_PRIORITY = 4;
	public final static int DEFAULT_MAX_RETRIES = -1;
	public final static int PERSISTENCE_FOREVER           = 0;
	public final static int PERSISTENCE_UNTIL_NODE_REBOOT = 1;
	public final static int PERSISTENCE_UNTIL_DISCONNECT  = 2;


	private int maxRetries = -1;
	private final static int PACKET_SIZE = 1024;
	private final static int BLOCK_SIZE = 16384;

	private FCPQueueManager queueManager;
	private FCPQueryManager duplicatedQueryManager;

	private String key = null;
	private String filename = null; /* Extract from the key */
	private int priority = DEFAULT_PRIORITY;
	private int persistence = PERSISTENCE_FOREVER;
	private boolean globalQueue = true;
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
	private boolean noRedir = false;

	private FCPTestDDA testDDA = null;

	/* used when redirected */
	private boolean restartIfFailed = false;

	private int protocolErrorCode = -1;
	private int getFailedCode = -1;


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
	 * @param destinationDir if null, then a temporary file will be create
	 *                       (path determined only when the file is availabed ;
	 *                       this file will be deleted on jvm exit)
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

		/* FIX : This is a fix for the files inserted by Frost */
		/* To remove when bback will do his work correctly */
		if (filename == null && id != null) {
			Logger.notice(this, "Fixing Frost key filename");
			String[] split = id.split("-");

			if (split.length >= 2) {
				filename = "";
				for (int i = 1 ; i < split.length; i++)
					filename += split[i];
			}
		}
		/* /FIX */


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
	 * Entry point: Only for initial queries
	 * @param destinationDir if null => temporary file
	 * @param persistence PERSISTENCE_FOREVER ; PERSISTENCE_UNTIL_NODE_REBOOT ; PERSISTENCE_UNTIL_DISCONNECT
	 */
	public FCPClientGet(final String key, final int priority,
			    final int persistence, boolean globalQueue,
			    final int maxRetries,
			    String destinationDir) {


		if (globalQueue && (persistence >= PERSISTENCE_UNTIL_DISCONNECT))
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
		attempt  = 0;

		status = "Waiting";

		filename = FreenetURIHelper.getFilenameFromKey(key);

		if (filename == null) {
			Logger.warning(this, "Nameless key !!");
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

	/**
	 * won't follow the redirections
	 */
	public void setNoRedirectionFlag(boolean noRedir) {
		this.noRedir = noRedir;
	}


	public boolean start(final FCPQueueManager queueManager) {
		attempt++;
		running = true;
		progress = 0;

		this.queueManager = queueManager;

		if (queueManager.getQueryManager().getConnection().isLocalSocket()
		    && !noDDA
		    && (destinationDir != null || finalPath != null)) {

			if (destinationDir == null)
				destinationDir = new File(finalPath).getAbsoluteFile().getParent();

			testDDA = new FCPTestDDA(destinationDir, false, true);
			testDDA.addObserver(this);
			return testDDA.start(queueManager);
		}

		return sendClientGet();
	}

	public boolean sendClientGet() {

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
		queryMessage.setValue("URI", key);
		queryMessage.setValue("Identifier", identifier);
		queryMessage.setValue("Verbosity", "1");
		queryMessage.setValue("MaxRetries", Integer.toString(maxRetries));
		queryMessage.setValue("PriorityClass", Integer.toString(priority));

		if (maxSize > 0)
			queryMessage.setValue("MaxSize", Long.toString(maxSize));

		if(destinationDir != null)
			queryMessage.setValue("ClientToken", destinationDir);

		if(persistence == PERSISTENCE_FOREVER)
			queryMessage.setValue("Persistence", "forever");
		if(persistence == PERSISTENCE_UNTIL_NODE_REBOOT)
			queryMessage.setValue("Persistence", "reboot");
		if(persistence == PERSISTENCE_UNTIL_DISCONNECT)
			queryMessage.setValue("Persistence", "connection");

		if(globalQueue)
			queryMessage.setValue("Global", "true");
		else
			queryMessage.setValue("Global", "false");

		if (!queueManager.getQueryManager().getConnection().isLocalSocket() || noDDA)
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
		if (o == testDDA) {
			if (!testDDA.mayTheNodeWrite())
				noDDA = true;

			sendClientGet();

			return;
		}


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
							if (!fileExists()
							    && !(queueManager.getQueryManager().getConnection().isLocalSocket() && !noDDA)
							    && queueManager.getQueryManager().getConnection().getAutoDownload()) {
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

			if ("4".equals(message.getValue("Code"))) {
				Logger.warning(this, "The node reported an invalid key. Please check the following key\n"+
					       key);
			}

			if("15".equals( message.getValue("Code") )) {
				Logger.debug(this, "Unknow URI ? was probably a stop order so no problem ...");
				return;
			}

			Logger.error(this, "=== PROTOCOL ERROR === \n"+message.toString());

			protocolErrorCode = Integer.parseInt(message.getValue("Code"));

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


		/* we assume that the change is not about the clientToken */
		if ("PersistentRequestModified".equals(message.getMessageName())) {
			if (message.getValue("PriorityClass") == null) {
				Logger.warning(this, "No priority specified ?! Message ignored.");
			} else {
				priority = Integer.parseInt(message.getValue("PriorityClass"));
			}
			return;
		}

		if ("PersistentRequestRemoved".equals(message.getMessageName())) {
			status = "Removed";

			if (!isFinished()) {
				progress = 100;
				running = false;
				successful = false;
				fatal = true;
			}

			Logger.info(this, "PersistentRequestRemoved >> Removing from the queue");
			queueManager.getQueryManager().deleteObserver(this);
			queueManager.remove(this);

			setChanged();
			notifyObservers();
			return;
		}


		if ("GetFailed".equals(message.getMessageName())) {
			Logger.debug(this, "GetFailed !");

			if (message.getValue("RedirectURI") != null && !noRedir) {
				Logger.debug(this, "Redirected !");
				key = message.getValue("RedirectURI");
				status = "Redirected ...";
				if (queueManager.isOur(message.getValue("Identifier"))) {
					restartIfFailed = true;
					stop(queueManager);
				} else {
					Logger.debug(this, "Not our transfer ; we don't touch");
				}
			}

			if (restartIfFailed) {
				restartIfFailed = false;
				start(queueManager);
				return;
			}

			Logger.notice(this, "==== GET FAILED ===\n"+message.toString());


			if(!isRunning()) { /* Must be a "GetFailed: cancelled by caller", so we simply ignore */
				Logger.info(this, "Cancellation confirmed.");
				return;
			}

			//removeRequest();

			getFailedCode = Integer.parseInt(message.getValue("Code"));

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

			setChanged();
			this.notifyObservers();

			return;
		}

		if ("SimpleProgress".equals(message.getMessageName())) {
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

		if ("AllData".equals(message.getMessageName())) {
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
				successful = true;
				status = "Available";
			} else {
				Logger.warning(this, "Unable to fetch correctly the file. This may create problems on socket");
				if (writingSuccessful) { /* if we forgot to set the correct values */
					writingSuccessful = false;
					successful = false;
					status = "Error while receveing the file";
				}
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

		if ("PersistentGet".equals(message.getMessageName())) {
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

		if (globalQueue) {
			duplicatedQueryManager = queueManager.getQueryManager().duplicate(identifier);
			duplicatedQueryManager.addObserver(this);
		} else { /* won't duplicate ; else it will use another id */
			duplicatedQueryManager = queueManager.getQueryManager();
		}

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
				status = "Unable to read data from the node";
				writingSuccessful = false;
				successful = false;
				try {
					outputStream.close();
				} catch(java.io.IOException ex) {
					Logger.error(this, "Unable to close the file cleanly : "+ex.toString());
					Logger.error(this, "Things seem to go wrong !");
				}
				newFile.delete();
				return false;
			}

			if(reallyWrite) {
				try {
					outputStream.write(read, 0, amount);
				} catch(final java.io.IOException e) {
					Logger.error(this, "Unable to write file on disk ... out of space ? : "+e.toString());
					status = "Unable to fetch / disk probably full !";
					writingSuccessful = false;
					successful = false;
					try {
						outputStream.close();
					} catch(java.io.IOException ex) {
						Logger.error(this, "Unable to close the file cleanly : "+ex.toString());
						Logger.error(this, "Things seem to go wrong !");
					}
					newFile.delete();
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

		if (!restartIfFailed) {
			setChanged();
			this.notifyObservers();
		}

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
		// TODO : It's fix due to Frost
		//        => to remove when it will become unneeded

		if (filename != null && key != null
		    && key.startsWith("CHK@")
		    && key.indexOf('/') < 0) {
			return key + "/" + filename;
		}

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
		String path = null;

		if (finalPath != null)
			path = finalPath;
		else if(destinationDir != null)
			path = destinationDir + File.separator + filename;

		if (path != null)
			path = path.replaceAll("\\|", "-");

		return path;
	}

	public String getFilename() {
		return filename.replaceAll("\\|", "-");
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

		if((persistence == PERSISTENCE_UNTIL_DISCONNECT) && !isFinished()) {
			progress = 0;
			running = false;
			status = "Waiting";
		}

		return true;
	}


	public boolean isPersistent() {
		return (persistence < PERSISTENCE_UNTIL_DISCONNECT);
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

	public void notifyChange() {
		setChanged();
		notifyObservers();
	}

	public int getGetFailedCode() {
		return getFailedCode;
	}

	public int getProtocolErrorCode() {
		return protocolErrorCode;
	}
}
