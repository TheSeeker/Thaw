package thaw.fcp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import thaw.core.Logger;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;


public class FCPClientGet extends FCPTransferQuery implements Observer {

	private int maxRetries = -1;
	private final static int PACKET_SIZE = 65536;

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

	private int fromTheNodeProgress = 0;
	private long fileSize;
	private long maxSize = 0;

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
		super((String)parameters.get("Identifier"), false);
		
		this.queueManager = queueManager;
		setParameters(parameters);

		fromTheNodeProgress = 0;

		/* If isPersistent(), then start() won't be called, so must relisten the
		   queryManager by ourself */
		if(isPersistent() && (getIdentifier() != null) && !getIdentifier().equals("")) {
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
	protected FCPClientGet(final String id, final String key, final int priority,
			    final int persistence, final boolean globalQueue,
			    final String destinationDir, String status,
			    final int maxRetries,
			    final FCPQueueManager queueManager) {

		this(key, priority, persistence, globalQueue, maxRetries, destinationDir);

		this.queueManager = queueManager;

		this.status = status;

		if(status == null) {
			Logger.warning(this, "status == null ?!");
			status = "(null)";
		}

		setIdentifier(id);


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

		setStatus(true, false, false);
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
		super(null, false);

		if (globalQueue && (persistence >= PERSISTENCE_UNTIL_DISCONNECT))
			globalQueue = false; /* else protocol error */

		fromTheNodeProgress = 0;

		this.maxRetries = maxRetries;
		this.key = key;
		this.priority = priority;
		this.persistence = persistence;
		this.globalQueue = globalQueue;
		this.destinationDir = destinationDir;

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
		
		setStatus(true, false, false);

		this.queueManager = queueManager;
		
		/* TODO : seems to be true sometimes => find why */ 
		if (queueManager == null
				|| queueManager.getQueryManager() == null
				|| queueManager.getQueryManager().getConnection() == null)
			return false;

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

		if((getIdentifier() == null) || "".equals( getIdentifier() ))
			setIdentifier(queueManager.getAnID() + "-"+filename);

		Logger.debug(this, "Requesting key : "+getFileKey());

		final FCPMessage queryMessage = new FCPMessage();

		queryMessage.setMessageName("ClientGet");
		queryMessage.setValue("URI", key);
		queryMessage.setValue("Identifier", getIdentifier());
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
		   || !message.getValue("Identifier").equals(getIdentifier()))
			return;

		if("DataFound".equals( message.getMessageName() )) {
			Logger.debug(this, "DataFound!");

			if(!isFinished()) {
				if(!alreadySaved) {
					alreadySaved = true;

					fileSize = Long.parseLong(message.getValue("DataLength"));
					

					if(isPersistent()
					   || (queueManager.getQueryManager().getConnection().isLocalSocket() && !noDDA)) {
						if(destinationDir != null) {
							if (!fileExists()
							    && !(queueManager.getQueryManager().getConnection().isLocalSocket() && !noDDA)
							    && queueManager.getQueryManager().getConnection().getAutoDownload()) {
								status = "Requesting file from the node";
								
								writingSuccessful = false;
								saveFileTo(destinationDir, false);

							} else {
								status = "Available";
								setStatus(false, true, true);
								writingSuccessful = true;
								Logger.notice(this, "Download finished => File already existing. Not rewrited");
							}

						} else {
							setStatus(false, true, true);
							status = "Available but not downloaded";
							writingSuccessful = true;
							Logger.notice(this, "Download finished => Don't know where to put file, so file not asked to the node");
						}
					} else {
						/* we do nothing : the request is not persistent, so we should get a AllData */
					}
				}

				notifyChange();
			}

			return;
		}

		if("IdentifierCollision".equals( message.getMessageName() )) {
			Logger.notice(this, "IdentifierCollision ! Resending with another id");

			setIdentifier(null);
			start(queueManager);

			notifyChange();

			return;
		}
		
		if ("PersistentGet".equals(message.getMessageName())) {
			/* not our problem */
			return;
		}

		if("ProtocolError".equals( message.getMessageName() )) {
			Logger.debug(this, "ProtocolError !");
			
			if ("25".equals(message.getValue("Code"))
					&& queueManager.getQueryManager().getConnection().isLocalSocket()
					&& !noDDA
				    && (destinationDir != null || finalPath != null)) {

					if (destinationDir == null)
						destinationDir = new File(finalPath).getAbsoluteFile().getParent();

				testDDA = new FCPTestDDA(destinationDir, false, true);
				testDDA.addObserver(this);
				testDDA.start(queueManager);
				
				return;
			}

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

			setStatus(false, true, false);

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

			notifyChange();

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
				setStatus(false, true, false);
				fatal = true;
			}

			Logger.info(this, "PersistentRequestRemoved >> Removing from the queue");
			queueManager.getQueryManager().deleteObserver(this);
			queueManager.remove(this);

			notifyChange();
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
					stop(queueManager, false);
				} else {
					Logger.debug(this, "Not our transfer ; we don't touch");
				}
			}

			if (restartIfFailed) {
				restartIfFailed = false;
				start(queueManager);
				return;
			}

			if (!"13".equals(message.getValue("Code"))) /* if != of Data Not Found */
				Logger.notice(this, "GetFailed : "+message.getValue("CodeDescription"));


			if(!isRunning()) { /* Must be a "GetFailed: cancelled by caller", so we simply ignore */
				Logger.info(this, "Cancellation confirmed.");
				return;
			}

			//removeRequest();

			getFailedCode = Integer.parseInt(message.getValue("Code"));

			status = "Failed ("+message.getValue("CodeDescription")+")";
			setStatus(false, true, false);

			fatal = true;

			if((message.getValue("Fatal") != null) &&
			   message.getValue("Fatal").equals("false")) {
				fatal = false;
				status = status + " (non-fatal)";
			}

			notifyChange();

			return;
		}

		if ("SimpleProgress".equals(message.getMessageName())) {
			Logger.debug(this, "SimpleProgress !");

			if (message.getValue("Total") != null
					&& message.getValue("Required") != null
					&& message.getValue("Succeeded") != null) {

				fileSize = Long.parseLong(message.getValue("Required"))*FCPClientGet.BLOCK_SIZE;

				final long total = Long.parseLong(message.getValue("Total"));
				final long required = Long.parseLong(message.getValue("Required"));
				final long succeeded = Long.parseLong(message.getValue("Succeeded"));

				boolean progressReliable = false;
				
				if((message.getValue("FinalizedTotal") != null) &&
						   message.getValue("FinalizedTotal").equals("true")) {
					progressReliable = true;
				}

				status = "Fetching";
				setBlockNumbers(required, total, succeeded, progressReliable);
				setStatus(true, false, false);

			} else {
				setBlockNumbers(-1, -1, -1, false);
			}

			notifyChange();

			return;
		}

		if ("AllData".equals(message.getMessageName())) {
			Logger.debug(this, "AllData ! : " + getIdentifier());

			fileSize = message.getAmountOfDataWaiting();

			setStatus(true, false, false);
			setStartupTime(Long.valueOf(message.getValue("StartupTime")).longValue());
			setCompletionTime(Long.valueOf(message.getValue("CompletionTime")).longValue());

			status = "Writing to disk";
			Logger.info(this, "Receiving file ...");

			notifyChange();

			if(fetchDirectly(queryManager.getConnection(), fileSize, true)) {
				Logger.info(this, "File received");

				writingSuccessful = true;
				status = "Available";
			} else {
				Logger.warning(this, "Unable to fetch correctly the file. This may create problems on socket");
				writingSuccessful = false;
				status = "Error while receveing the file";
			}

			if (duplicatedQueryManager != null)
				duplicatedQueryManager.getConnection().removeFromWriterQueue();

			isLockOwner= false;
			
			setStatus(false, true, writingSuccessful);

			queryManager.deleteObserver(this);

			if (queryManager != queueManager.getQueryManager()) {
				queueManager.getQueryManager().deleteObserver(this);
				queryManager.getConnection().disconnect();
				duplicatedQueryManager = null;
			}

			notifyChange();

			return;
		}

		Logger.warning(this, "Unknow message : "+message.getMessageName() + " !");
	}


	private class UnlockWaiter implements ThawRunnable {
		FCPClientGet clientGet;
		FCPConnection c;
		String dir;

		public UnlockWaiter(final FCPClientGet clientGet, final FCPConnection c, final String dir) {
			this.clientGet = clientGet;
			this.dir = dir;
			this.c = c;
		}

		private Thread th;
		private boolean waiting = false;

		public void run() {
			synchronized(this) {
				waiting = true;
			}

			c.addToWriterQueue();

			synchronized(this) {
				waiting = false;

				if (Thread.interrupted()) {
					c.removeFromWriterQueue();
					return;
				}
			}

			isLockOwner = true;

			Logger.debug(this, "I take the lock !");

			if(dir == null) {
				Logger.warning(this, "UnlockWaiter.run() : Wtf ?");
			}

			clientGet.continueSaveFileTo(dir);
			return;
		}


		public void setThread(Thread th) {
			synchronized(this) {
				this.th = th;
			}
		}

		/* race-conditions may happen but "shits happen" */
		public void stop() {
			synchronized(this) {
				if (waiting)
					th.interrupt();
			}
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
			duplicatedQueryManager = queueManager.getQueryManager().duplicate(getIdentifier());
			duplicatedQueryManager.addObserver(this);
		} else { /* won't duplicate ; else it will use another id */
			duplicatedQueryManager = queueManager.getQueryManager();
		}

		Logger.info(this, "Waiting for socket  ...");
		status = "Waiting for socket availability ...";
		fromTheNodeProgress = 1; /* display issue */
		
		setStatus(true, false, false);

		notifyChange();
		
		UnlockWaiter uw = new UnlockWaiter(this, duplicatedQueryManager.getConnection(), dir);

		final Thread fork = new ThawThread(uw,
						   "Unlock waiter",
						   this);
		uw.setThread(fork);

		fork.start();

		return true;
	}

	public synchronized boolean continueSaveFileTo(final String dir) {

		Logger.info(this, "Asking file '"+filename+"' to the node...");

		destinationDir = dir;

		status = "Requesting file";
		fromTheNodeProgress = 1; /* display issue */
		setStatus(true, false, false);

		notifyChange();
		
		if(destinationDir == null) {
			Logger.warning(this, "saveFileTo() : Wtf ?");
		}

		final FCPMessage getRequestStatus = new FCPMessage();

		getRequestStatus.setMessageName("GetRequestStatus");
		getRequestStatus.setValue("Identifier", getIdentifier());
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
				setStatus(false, true, false);
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
					setStatus(false, true, false);
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
				
				if (fromTheNodeProgress <= 0) /* display issue */
					fromTheNodeProgress = 1;

				notifyChange();

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

		if(getIdentifier() == null) {
			Logger.notice(this, "Can't remove non-started queries");
			return true;
		}

		stopMessage.setMessageName("RemovePersistentRequest");

		if(globalQueue)
			stopMessage.setValue("Global", "true");
		else
			stopMessage.setValue("Global", "false");

		stopMessage.setValue("Identifier", getIdentifier());

		queueManager.getQueryManager().writeMessage(stopMessage);

		setStatus(false, isFinished(), isSuccessful());

		return true;
	}

	public boolean pause(final FCPQueueManager queryManager) {
		/* TODO ? : Reduce the priority
		   instead of stopping */

		Logger.info(this, "Pausing fetching of the key : "+getFileKey());

		removeRequest();
		
		setStatus(false, false, false);

		status = "Delayed";

		notifyChange();

		return true;

	}
	
	public boolean stop(final FCPQueueManager queueManager) {
		return stop(queueManager, true);
	}

	public boolean stop(final FCPQueueManager queueManager, boolean notify) {
		Logger.info(this, "Stop fetching of the key : "+getFileKey());
		
		if(isPersistent() && !removeRequest())
			return false;
		
		queueManager.getQueryManager().deleteObserver(this);

		boolean wasFinished = isFinished();
		
		setStatus(false, true, wasFinished && isSuccessful());

		fatal = true;
		status = "Stopped";

		if (!restartIfFailed && !wasFinished && notify) {
			notifyChange();
		}

		return true;
	}


	public void updatePersistentRequest(final boolean clientToken) {
		if(!isPersistent())
			return;

		final FCPMessage msg = new FCPMessage();

		msg.setMessageName("ModifyPersistentRequest");
		msg.setValue("Global", Boolean.toString(globalQueue));
		msg.setValue("Identifier", getIdentifier());
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

		notifyChange();
	}

	public int getQueryType() {
		return 1;
	}

	public String getStatus() {
		return status;
	}

	public String getFileKey() {
		// TODO : It's a fix due to Frost
		//        => to remove when it will become unneeded

		if (filename != null && key != null
		    && key.startsWith("CHK@")
		    && key.indexOf('/') < 0) {
			return key + "/" + filename;
		}

		return key;
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
		if (filename != null)
			return filename.replaceAll("\\|", "-");
		return key;
	}

	public int getAttempt() {
		return attempt;
	}

	public void setAttempt(final int x) {
		attempt = x;

		if(x == 0) {
			/* We suppose it's a restart */
			setBlockNumbers(-1, -1, -1, false);
		}
	}

	public int getMaxAttempt() {
		return maxRetries;
	}

	public boolean isWritingSuccessful() {
		return writingSuccessful;
	}

	public boolean isFatallyFailed() {
		return ((!isSuccessful()) && fatal);
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

       	result.put("Identifier", getIdentifier());
		result.put("FileSize", Long.toString(fileSize));
		result.put("Running", Boolean.toString(isRunning()));
		result.put("Successful", Boolean.toString(isSuccessful()));
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
		setIdentifier(   (String)parameters.get("Identifier"));

		Logger.info(this, "Resuming id : "+getIdentifier());
		fileSize       = Long.parseLong((String)parameters.get("FileSize"));
		boolean running        = Boolean.valueOf((String)parameters.get("Running")).booleanValue();
		boolean successful     = Boolean.valueOf((String)parameters.get("Successful")).booleanValue();
		maxRetries     = Integer.parseInt((String)parameters.get("MaxRetries"));
		
		setStatus(running, !running, successful);

		if((persistence == PERSISTENCE_UNTIL_DISCONNECT) && !isFinished()) {
			setStatus(false, false, false);
			setBlockNumbers(-1, -1, -1, false);
			status = "Waiting";
		}

		return true;
	}


	public boolean isPersistent() {
		return (persistence < PERSISTENCE_UNTIL_DISCONNECT);
	}

	public boolean isGlobal() {
		return globalQueue;
	}

	public int getTransferWithTheNodeProgression() {
		return fromTheNodeProgress;
	}

	public int getGetFailedCode() {
		return getFailedCode;
	}

	public int getProtocolErrorCode() {
		return protocolErrorCode;
	}

}
