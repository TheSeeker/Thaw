package thaw.fcp;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Observable;
import java.util.Observer;

import thaw.core.Logger;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;


/**
 * Allow to insert a simple file.
 */
public class FCPClientPut extends FCPTransferQuery implements Observer {

	private FCPQueueManager queueManager;

	private File localFile;
	private long fileSize = 0;
	private int keyType = KEY_TYPE_CHK;
	private int rev = 0;
	private String name;
	private String privateKey; /* must finish by '/' (cf SSKKeypair) */
	private String publicKey; /* publicKey contains the filename etc */
	private int priority = DEFAULT_PRIORITY;
	private boolean global = true;
	private int persistence = PERSISTENCE_FOREVER;
	private boolean getCHKOnly = false;

	private int toTheNodeProgress = 0;
	private String status;

	private int attempt = 0;
	private String identifier;

	private boolean fatal = true;
	private boolean sending = false;

	private FCPGenerateSSK sskGenerator;
	private boolean lockOwner = false;

	private final HashMap metadatas = new LinkedHashMap();

	private final static int PACKET_SIZE = 1024;

	private SHA256Computer sha;

	private int putFailedCode = -1;

	/**
	 * To resume query from file. (see core.QueueKeeper)
	 */
	public FCPClientPut(final FCPQueueManager queueManager, final HashMap parameters) {
		super(true);

		this.queueManager = queueManager;
		setParameters(parameters);
	}

	/**
	 * To start a new insertion.
	 * @param keyType : KEY_TYPE_CHK ; KEY_TYPE_KSK ; KEY_TYPE_SSK
	 * @param rev  : ignored if key == CHK || rev == -1
	 * @param name : ignored if key == CHK
	 * @param privateKey : ignored if key == CHK/KSK ; can be null if it has to be generated ; USK@[...]/ (must ends with a '/'
	 * @param persistence PERSISTENCE_FOREVER ; PERSISTENCE_UNTIL_NODE_REBOOT ; PERSISTENCE_UNTIL_DISCONNEC
	 */
	public FCPClientPut(final File file, final int keyType,
			    final int rev, final String name,
			    final String privateKey, final int priority,
			    final boolean global, final int persistence) {
		this(file, keyType, rev, name, privateKey, priority, global, persistence, false);
	}

	/**
	 * To start a new insertion.
	 */
	public FCPClientPut(final File file, final int keyType,
			    final int rev, final String name,
			    final String privateKey, final int priority,
			    final boolean global, final int persistence,
			    final boolean getCHKOnly) {
		super(true);

		this.getCHKOnly = getCHKOnly;
		localFile = file;

		if (file != null) {
			this.name = file.getName();
			fileSize = file.length();
		} else {
			this.name = name;
			fileSize = 0;
		}

		this.keyType = keyType;
		this.rev = rev;

		if(keyType == 0) {
			this.name = file.getName();
			this.privateKey = null;
		} else {
			this.name = name;
			this.privateKey = privateKey;
		}

		publicKey = null;

		this.priority = priority;
		this.global = global;
		this.persistence = persistence;
		status = "Waiting";
		setStatus(false, false, false);
		setBlockNumbers(-1, -1, -1, true);
		attempt = 0;

		identifier = null;
		fatal = true;

	}

	/**
	 * Used for resuming from a PersistentPut.
	 * @param publicKey : Complete key (with filename, etc)
	 */
	protected FCPClientPut(final String identifier, final String publicKey,
			    final int priority, final int persistence, final boolean global,
			    final String filePath, final String fileName, final String status, final int progress,
			    final long fileSize, final FCPQueueManager queueManager) {
		super(true);

		if(fileSize > 0)
			this.fileSize = fileSize;

		toTheNodeProgress = 100;

		this.queueManager = queueManager;
		this.identifier = identifier;

		if(publicKey.startsWith("CHK"))
			keyType = KEY_TYPE_CHK;
		else if(publicKey.startsWith("KSK"))
			keyType = KEY_TYPE_KSK;
		else if(publicKey.startsWith("SSK"))
			keyType = KEY_TYPE_SSK;
		else if(publicKey.startsWith("USK"))
			keyType = KEY_TYPE_SSK;


		this.publicKey = publicKey;

		if (fileName != null)
			name = fileName;

		this.publicKey = null;

		this.priority = priority;
		this.global = global;

		this.persistence = persistence;
		
		setBlockNumbers(-1, -1, -1, true);
		setStatus(true, false, false);
		
		this.status = status;
		fatal = true;
	}


	public boolean start(final FCPQueueManager queueManager) {
		this.queueManager = queueManager;
		putFailedCode = -1;
		identifier = null;

		if((localFile != null) && (localFile.length() <= 0)) {
			Logger.warning(this, "Empty or unreachable file:"+localFile.getPath());

			status = "EMPTY OR UNREACHABLE FILE";
			
			setStatus(false, true, false);

			fatal = true;

			notifyChange();
			
			return false;
		}


		queueManager.getQueryManager().addObserver(this);
		
		setBlockNumbers(-1, -1, -1, false);
		setStatus(true, false, false);

		sha = null;

		if (queueManager.getQueryManager().getConnection().isLocalSocket() && localFile != null) {
			status = "Computing hash to get approval from the node ...";

			identifier = queueManager.getAnID() + "-"+ localFile.getName();

			String salt = queueManager.getQueryManager().getConnection().getClientHello().getConnectionId()
				+"-"+ identifier
				+"-";
			Logger.info(this, "Salt used for this transfer: ~" + salt+ "~");

			sha = new SHA256Computer(salt, localFile.getPath());
			sha.addObserver(this);

			Thread th = new ThawThread(sha, "Hash computer", this);
			th.start();
		} else {
			return startProcess();
		}

		return true;
	}


	public boolean startProcess() {
		if((keyType == KEY_TYPE_SSK) && (privateKey == null)) {
			generateSSK();
		}

		if( ((keyType == KEY_TYPE_SSK) && (privateKey != null)) || (keyType != KEY_TYPE_SSK)) {
			startInsert();
		}

		setChanged();
		this.notifyObservers();

		return true;
	}

	/**
	 * doesn't set running = true. startInsert() will.
	 */
	public void generateSSK() {
		status = "Generating keys";

		sskGenerator = new FCPGenerateSSK();

		sskGenerator.addObserver(this);
		sskGenerator.start(queueManager);
	}



	private class UnlockWaiter implements ThawRunnable {
		FCPClientPut clientPut;
		FCPConnection c;

		public UnlockWaiter(final FCPClientPut clientPut, final FCPConnection c) {
			this.clientPut = clientPut;
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

			lockOwner = true;

			clientPut.continueInsert();
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



	public boolean startInsert() {
		final FCPConnection connection = queueManager.getQueryManager().getConnection();

		toTheNodeProgress= 0;

		status = "Waiting for socket availability";

		Logger.info(this, "Waiting for socket availability ...");

		UnlockWaiter uw = new UnlockWaiter(this, connection);

		final Thread fork = new ThawThread(uw,
						   "Unlock waiter",
						   this);
		uw.setThread(fork);
		fork.start();

		return true;
	}

	public boolean continueInsert() {
		setStatus(true, false, false);

		sending = true;

		final FCPConnection connection = queueManager.getQueryManager().getConnection();

		status = "Sending to the node";

		if(identifier == null) { /* see start() ; when computing hash */
			if (localFile != null)
				identifier = queueManager.getAnID() + "-"+ localFile.getName();
			else
				identifier = queueManager.getAnID();
		}

		setChanged();
		this.notifyObservers();

		final FCPMessage msg = new FCPMessage();

		msg.setMessageName("ClientPut");
		msg.setValue("URI", getInsertionKey());

		if(!metadatas.isEmpty()) {
			for(final Iterator keyIt = metadatas.keySet().iterator();
			    keyIt.hasNext();) {
				final String key = (String)keyIt.next();
				final String value = (String)metadatas.get(key);
				msg.setValue("Metadata."+key, value);
			}
		}

		msg.setValue("Identifier", identifier);
		msg.setValue("MaxRetries", "-1");
		msg.setValue("PriorityClass", Integer.toString(priority));

		if(getCHKOnly) {
			msg.setValue("GetCHKOnly", "true");
		} else {
			msg.setValue("GetCHKOnly", "false");
		}
		msg.setValue("Verbosity", "512");

		if(global)
			msg.setValue("Global", "true");
		else
			msg.setValue("Global", "false");

		if (localFile != null)
			msg.setValue("ClientToken", localFile.getPath());

		switch(persistence) {
		case(PERSISTENCE_FOREVER): msg.setValue("Persistence", "forever"); break;
		case(PERSISTENCE_UNTIL_NODE_REBOOT): msg.setValue("Persistence", "reboot"); break;
		case(PERSISTENCE_UNTIL_DISCONNECT): msg.setValue("Persistence", "connection"); break;
		default: Logger.error(this, "Unknow persistence !?"); break;
		}

		if (localFile != null)
			msg.setValue("TargetFilename", localFile.getName());
		else
			msg.setValue("TargetFilename", name);

		if (!connection.isLocalSocket()) {
			msg.setValue("UploadFrom", "direct");
			msg.setAmountOfDataWaiting(fileSize);
		} else {
			msg.setValue("UploadFrom", "disk");
			msg.setValue("Filename", localFile.getPath());

			if (sha != null)
				msg.setValue("FileHash", sha.getHash());
		}

		Logger.info(this, "Sending "+Long.toString(fileSize)+" bytes on socket ...");

		queueManager.getQueryManager().writeMessage(msg, false);

		boolean ret = true;

		if (!connection.isLocalSocket()) {
			Logger.info(this, "Sending file to the node");
			ret = sendFile();
			Logger.info(this, "File sent (or not :p)");
		}

		connection.removeFromWriterQueue();
		lockOwner = false;
		sending = false;
		
		setBlockNumbers(-1, -1, -1, true);
		
		if(ret == true) {
			setStatus(true, false, false);
			fatal = true;

			if (!getCHKOnly)
				status = "Inserting";
			else
				status = "Computing";

			notifyChange();

			return true;
		} else {
			setStatus(false, true, false);

			status = "Unable to send the file to the node";

			notifyChange();

			return false;
		}

	}


	private boolean sendFile() {
		final FCPConnection connection = queueManager.getQueryManager().getConnection();

		long remaining = fileSize;
		byte[] data = null;

		FileInputStream in = null;

		if (localFile == null) {
			toTheNodeProgress = 100;
			return true;
		}

		try {
			in = new FileInputStream(localFile);
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(this, "FileNotFoundException ?! ohoh, problems ...");
			return false;
		}

		long startTime = System.currentTimeMillis();
		final long origSize = remaining;

		while(remaining > 0) {
			int to_read = FCPClientPut.PACKET_SIZE;

			if(remaining < to_read)
				to_read = (int)remaining;

			data = new byte[to_read];

			try {
				if(in.read(data) < 0) {
					Logger.error(this, "Error while reading file ?!");
					return false;
				}
			} catch(final java.io.IOException e) {
				Logger.error(this, "IOException while reading file! proobleeem");
				return false;
			}

			if(!connection.rawWrite(data)) {
				Logger.error(this, "Error while writing file on socket ! Disconnected ?");
				return false;
			}

			remaining = remaining - to_read;

			if( System.currentTimeMillis() >= (startTime+3000) ) {
				toTheNodeProgress = (int) (((origSize - remaining) * 100) / origSize);
				setChanged();
				this.notifyObservers();
				startTime = System.currentTimeMillis();
			}

			//Logger.verbose(this, "Remaining: "+(new Long(remaining)).toString());
		}


		toTheNodeProgress = 100;

		try {
			if(in.available() > 0) {
				Logger.error(this, "File not send completly ?!");
				return false;
			}
		} catch(final java.io.IOException e) {
			/* we will suppose its ok ... */
			Logger.notice(this, "available() IOException (hu ?)");
		}

		return true;
	}

	public boolean stop(final FCPQueueManager queueManager) {
		if(removeRequest()) {
			boolean wasFinished = isFinished();

			status = "Stopped";

			if (wasFinished)
				setStatus(false, true, false);
			else
				setStatus(false, true, isSuccessful());

			fatal= true;

			if (!wasFinished) {
				notifyChange();
			}

			return true;
		}

		return false;
	}

	public void update(final Observable o, final Object param) {
		if (o == sha) {
			if(sha.isFinished())
				startProcess();
			else {
				status = "Computing hash";
				setBlockNumbers(100, 100, sha.getProgression(), true);
			}
			
			notifyChange();
			
			return;
		}

		if(o == sskGenerator) {
			privateKey = sskGenerator.getPrivateKey();
			publicKey = sskGenerator.getPublicKey() + "/" + name;

			notifyChange();

			startInsert();
			return;
		}

		if(o == queueManager.getQueryManager()) {
			final FCPMessage msg = (FCPMessage)param;

			if((msg == null)
			   || (msg.getValue("Identifier") == null)
			   || !msg.getValue("Identifier").equals(identifier))
				return;

			if("URIGenerated".equals( msg.getMessageName() )
			   || "PutFetchable".equals( msg.getMessageName() )) {
				setStatus(true, false, false);

				publicKey = msg.getValue("URI");

				publicKey = publicKey.replaceAll("freenet:", "");

				Logger.info(this, msg.getMessageName()+": "+publicKey);

				if(getCHKOnly) {
					status = "CHK";
					
					setStatus(false, true, true);

					toTheNodeProgress = 100;
					fatal = false;
					sending = false;

					notifyChange();
					queueManager.getQueryManager().deleteObserver(this);
					return;
				}

				status = "Inserting";

				setChanged();
				this.notifyObservers();
				return;
			}

			if("PutSuccessful".equals(msg.getMessageName())) {
				setStatus(false, true, true);

				setStartupTime(Long.valueOf(msg.getValue("StartupTime")).longValue());
				setCompletionTime(Long.valueOf(msg.getValue("CompletionTime")).longValue());
				publicKey = msg.getValue("URI");

				if (publicKey == null) {
					status = "[Warning]";
					Logger.warning(this, "PutSuccessful message without URI field ?!");
					setChanged();
					this.notifyObservers();
					return;
				}

				publicKey = publicKey.replaceAll("freenet:", "");

				if(keyType == KEY_TYPE_KSK) {
					if (rev >= 0)
						publicKey = "KSK@"+name+"-" + Integer.toString(rev);
					else
						publicKey = "KSK@"+name;
				}
				//if(keyType == KEY_TYPE_SSK)
				//	publicKey = publicKey + "/" + name + "-" + Integer.toString(rev);


				status = "Finished";

				setChanged();
				this.notifyObservers();
				return;
			}

			if ("PersistentRequestModified".equals(msg.getMessageName())) {
				if (msg.getValue("PriorityClass") == null) {
					Logger.warning(this, "No priority specified ?! Message ignored.");
				} else {
					priority = Integer.parseInt(msg.getValue("PriorityClass"));
				}
				return;
			}

			if ("PersistentRequestRemoved".equals(msg.getMessageName())) {
				if (!isFinished()) {
					setStatus(false, true, false);
					fatal = true;
					status = "Removed";
				}

				Logger.info(this, "PersistentRequestRemoved >> Removing from the queue");
				queueManager.getQueryManager().deleteObserver(this);
				queueManager.remove(this);

				notifyChange();
				return;
			}


			if ("PutFailed".equals( msg.getMessageName() )) {
				setStatus(false, true, false);
				fatal = true;

				putFailedCode = Integer.parseInt(msg.getValue("Code"));

				status = "Failed ("+msg.getValue("CodeDescription")+")";

				if((msg.getValue("Fatal") != null) &&
				   msg.getValue("Fatal").equals("false")) {
					status = status + " (non-fatal)";
					fatal = false;
				}

				if (putFailedCode != 9)
					Logger.warning(this, "Insertion failed");
				else
					Logger.warning(this, "Insertion error : collision");
				Logger.notice(this, msg.toString());

				notifyChange();
				return;
			}

			if("ProtocolError".equals( msg.getMessageName() )) {
				setStatus(false, true, false);
				fatal = true;

				if(lockOwner) {
					lockOwner = false;
					queueManager.getQueryManager().getConnection().removeFromWriterQueue();
				}

				status = "Protocol error ("+msg.getValue("CodeDescription")+")";

				if((msg.getValue("Fatal") != null) &&
				   msg.getValue("Fatal").equals("false")) {
					status = status + " (non-fatal)";
					fatal = false;
				}

				notifyChange();

				return;
			}

			if("IdentifierCollision".equals(msg.getMessageName())) {
				status = "Identifier collision";
				start(queueManager); /* et hop ca repart :) */
				return;
			}


			if("PersistentPut".equals(msg.getMessageName())) {
				status = "Inserting";
				//publicKey = msg.getValue("URI");
				return;
			}

			if("StartedCompression".equals(msg.getMessageName())) {
				status = "Compressing";
				
				notifyChange();

				return;
			}

			if("FinishedCompression".equals(msg.getMessageName())) {
				status = "Inserting";

				if((msg.getValue("OrigSize") == null)
				   || (msg.getValue("CompressedSize") == null)) {
					notifyChange();
					return;
				}

				final int rate = (int)( ((new Long(msg.getValue("OrigSize"))).longValue() * 100) / (new Long(msg.getValue("CompressedSize"))).longValue() );

				Logger.notice(this, "Compression rate: "+ Integer.toString(rate)+" %");

				notifyChange();

				return;
			}

			if("SimpleProgress".equals(msg.getMessageName())) {

				if((msg.getValue("Total") != null)
				   && (msg.getValue("Succeeded") != null)) {

					final long total = (new Long(msg.getValue("Total"))).longValue();
					long required = (new Long(msg.getValue("Required"))).longValue();
					final long succeeded = (new Long(msg.getValue("Succeeded"))).longValue();
					
					setBlockNumbers(required, total, succeeded, true);
					setStatus(true, false, false);

					if (!getCHKOnly)
						status = "Inserting";
					else
						status = "Computing";

					notifyChange();
				}

				return;
			}


			if (msg.getMessageName() == null)
				Logger.notice(this, "Unknow message (name == null)");
			else
				Logger.notice(this, "Unkwown message: "+msg.getMessageName());			

			return;
		}

	}


	public int getQueryType() {
		return 2;
	}

	public boolean pause(final FCPQueueManager queueManager) {
		/*
		 * TODO : lower priority ?
		 */
		
		setStatus(false, false, false);
		status = "Delayed";

		fatal = true;

		removeRequest();
		notifyChange();
		
		return false;
	}


	public boolean removeRequest() {
		if(sending) {
			Logger.notice(this, "Can't interrupt while sending to the node ...");
			status = status + " (can't interrupt while sending to the node)";
			return false;
		}

		if(isRunning() || isFinished()) {
			final FCPMessage msg = new FCPMessage();
			msg.setMessageName("RemovePersistentRequest");
			msg.setValue("Identifier", identifier);

			if(global)
				msg.setValue("Global", "true");
			else
				msg.setValue("Global", "false");

			queueManager.getQueryManager().writeMessage(msg);

			setStatus(false, false, false);

			queueManager.getQueryManager().deleteObserver(this);
		} else {
			Logger.notice(this, "Nothing to remove");
		}


		return true;
	}

	public void updatePersistentRequest(final boolean clientToken) {
		//if(!isPersistent())
		//	return;

		final FCPMessage msg = new FCPMessage();

		msg.setMessageName("ModifyPersistentRequest");
		msg.setValue("Global", Boolean.toString(global));
		msg.setValue("Identifier", identifier);
		msg.setValue("PriorityClass", Integer.toString(priority));

		if(clientToken && (getPath() != null))
			msg.setValue("ClientToken", getPath());

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



	public String getStatus() {
		return status;
	}

	/**
	 * @return public key
	 */
	public String getFileKey() {
		return publicKey;
	}

	public int getKeyType() {
		return keyType;
	}

	public String getInsertionKey() {
		String key = null;

		if ((keyType == KEY_TYPE_CHK) && (publicKey != null))
			key = publicKey;

		else if ((keyType == KEY_TYPE_CHK) && (publicKey == null))
			key = "CHK@";

		else if (keyType == KEY_TYPE_KSK) {
			if (rev >= 0)
				key = "KSK@" + name + "-"+ Integer.toString(rev);
			else
				key = "KSK@" + name;
		}
		else if (keyType == KEY_TYPE_SSK && privateKey.startsWith("SSK")) {
			if (rev >= 0)
				key = privateKey + name+"-"+rev;
			else
				key = privateKey + name;
		}
		else if (keyType == KEY_TYPE_SSK && privateKey.startsWith("USK")) {
			if (rev >= 0)
				key = privateKey + name + "/" + rev;
			else
				key = privateKey + name;
		}

		if (key == null) {
			Logger.warning(this, "Unknown key type ?! May result in a strange behavior !");
			return privateKey;
		}

		return key;
	}

	public long getFileSize() {
		return fileSize; /* a "file length" ? why not "file size" as usual ? */
	}

	public String getPath() {
		if(localFile != null)
			return localFile.getPath();
		else
			return null;
	}

	public String getFilename() {
		if(localFile != null)
			return localFile.getName();
		else
			return name;
	}

	public int getAttempt() {
		return attempt;
	}

	public void setAttempt(final int x) {
		attempt = x;
	}

	public int getMaxAttempt() {
		return -1;
	}

	public boolean isFatallyFailed() {
		return ((!isSuccessful()) && fatal);
	}

	public HashMap getParameters() {
		final HashMap result = new HashMap();

		result.put("localFile", localFile.getPath());
		result.put("keyType", Integer.toString(keyType));
		result.put("Revision", Integer.toString(rev));
		result.put("Name", name);
		if(privateKey != null)
			result.put("privateKey", privateKey);
		if(publicKey != null)
			result.put("publicKey", publicKey);
		result.put("priority", Integer.toString(priority));
		result.put("global", Boolean.toString(global));
		result.put("persistence", Integer.toString(persistence));

		result.put("status", status);

		result.put("attempt", Integer.toString(attempt));
		if(identifier != null)
			result.put("identifier", identifier);
		result.put("running", Boolean.toString(isRunning()));
		result.put("successful", Boolean.toString(isSuccessful()));
		result.put("finished", Boolean.toString(isFinished()));

		return result;
	}


	public boolean setParameters(final HashMap parameters) {

		localFile = new File((String)parameters.get("localFile"));

		fileSize = localFile.length();

		keyType = Integer.parseInt((String)parameters.get("keyType"));
		rev = Integer.parseInt((String)parameters.get("Revision"));
		name = (String)parameters.get("name");

		privateKey = (String)parameters.get("privateKey");
		if((privateKey == null) || privateKey.equals(""))
			privateKey = null;

		publicKey = (String)parameters.get("publicKey");
		if((privateKey == null) || (publicKey == null) || publicKey.equals(""))
			publicKey = null;

		priority = Integer.parseInt((String)parameters.get("priority"));

		global = Boolean.valueOf((String)parameters.get("global")).booleanValue();

		persistence = Integer.parseInt((String)parameters.get("persistence"));
		status = (String)parameters.get("status");
		attempt = Integer.parseInt((String)parameters.get("attempt"));

		identifier = (String)parameters.get("identifier");
		if((identifier == null) || identifier.equals(""))
			identifier = null;

		boolean running = Boolean.valueOf((String)parameters.get("running")).booleanValue();
		boolean successful = Boolean.valueOf((String)parameters.get("successful")).booleanValue();
		boolean finished = Boolean.valueOf((String)parameters.get("finished")).booleanValue();
		
		setStatus(running, finished, successful);

		if ((persistence == PERSISTENCE_UNTIL_DISCONNECT) && !isFinished()) {
			setStatus(false, false, false);
			status = "Waiting";
		}

		return true;
	}


	public boolean isPersistent() {
		return persistence < 2;
	}

	public boolean isGlobal() {
		return global;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public int getRevision() {
		return rev;
	}

	/**
	 * Do nothing.
	 */
	public boolean saveFileTo(final String dir) {
		return false;
	}

	public int getTransferWithTheNodeProgression() {
		return toTheNodeProgress;
	}

	public HashMap getMetadatas() {
		return metadatas;
	}

	public void setMetadata(final String name, final String val) {
		if(val == null)
			metadatas.remove(name);
		else
			metadatas.put(name, val);
	}

	public String getMetadata(final String name) {
		return (String)metadatas.get(name);
	}


	/**
	 * @return -1 if none
	 */
	public int getPutFailedCode() {
		return putFailedCode;
	}

}
