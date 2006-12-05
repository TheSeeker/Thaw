package thaw.fcp;

import java.util.HashMap;
import java.io.File;
import java.util.Observer;
import java.util.Observable;
import java.io.FileInputStream;

import java.util.Iterator;

import thaw.core.Logger;

/**
 * Allow to insert a simple file.
 */
public class FCPClientPut extends Observable implements FCPTransferQuery, Observer {
	public final static int DEFAULT_INSERTION_PRIORITY = 4;

	private FCPQueueManager queueManager = null;
	private final static int BLOCK_SIZE = 32768;

	private File localFile = null;
	private long fileSize = 0;
	private int keyType = 0;
	private int rev = 0;
	private String name = null;
	private String privateKey = null; /* must finish by '/' (cf SSKKeypair) */
	private String publicKey = null; /* publicKey contains the filename etc */
	private int priority = -1;
	private boolean global = true;
	private int persistence = 2;
	private boolean getCHKOnly = false;

	private int progress = 0;
	private int toTheNodeProgress = 0;
	private String status = null;

	private int attempt = 0;

	private String identifier = null;

	private boolean running = false;
	private boolean finished = false;
	private boolean successful = false;
	private boolean fatal = true;
	private boolean sending = false;

	private FCPGenerateSSK sskGenerator = null;
	private boolean lockOwner = false;

	private HashMap metadatas = null;

	private final static int PACKET_SIZE = 1024;

	/**
	 * To resume query from file. (see core.QueueKeeper)
	 */
	public FCPClientPut(FCPQueueManager queueManager, HashMap parameters) {
		this.queueManager = queueManager;
		this.setParameters(parameters);
	}

	/**
	 * To start a new insertion.
	 * @param keyType : 0 = CHK ; 1 = KSK ; 2 = SSK
	 * @param rev  : ignored if key == CHK
	 * @param name : ignored if key == CHK
	 * @param privateKey : ignored if key == CHK/KSK ; can be null if it has to be generated ; USK@[...]/
	 * @param persistence 0 = Forever ; 1 = Until node reboot ; 2 = Until the app disconnect
	 */
	public FCPClientPut(File file, int keyType,
			    int rev, String name,
			    String privateKey, int priority,
			    boolean global, int persistence) {
		this(file, keyType, rev, name, privateKey, priority, global, persistence,
		     false);
	}

	/**
	 * To start a new insertion.
	 */
	public FCPClientPut(File file, int keyType,
			    int rev, String name,
			    String privateKey, int priority,
			    boolean global, int persistence,
			    boolean getCHKOnly) {
		this.getCHKOnly = getCHKOnly;
		this.localFile = file;

		if (file != null) {
			this.name = file.getName();
			this.fileSize = file.length();
		} else {
			this.name = name;
			this.fileSize = 0;
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

		this.publicKey = null;

		this.priority = priority;
		this.global = global;
		this.persistence = persistence;
		this.status = "Waiting";
		this.progress = 0;
		this.attempt = 0;

		this.identifier = null;

		this.running = false;
		this.finished = false;
		this.successful = false;
		this.fatal = true;

	}

	/**
	 * Used for resuming from a PersistentPut.
	 * @param publicKey : Complete key (with filename, etc)
	 */
	public FCPClientPut(String identifier, String publicKey,
			    int priority, int persistence, boolean global,
			    String filePath, String fileName, String status, int progress,
			    long fileSize, FCPQueueManager queueManager) {


		if(fileSize > 0)
			this.fileSize = fileSize;

		this.toTheNodeProgress = 100;

		this.queueManager = queueManager;
		this.identifier = identifier;

		if(publicKey.startsWith("CHK"))
			this.keyType = 0;
		if(publicKey.startsWith("KSK"))
			this.keyType = 1;
		if(publicKey.startsWith("SSK"))
			this.keyType = 2;
		if(publicKey.startsWith("USK"))
			this.keyType = 2;


		this.publicKey = publicKey;

		if(filePath != null && filePath.startsWith("thaw")) {

			String[] plop = filePath.split(File.separator.replaceAll("\\\\", "\\\\\\\\"));
			this.name = plop[plop.length-1];

			this.localFile = new File(filePath);
			if(this.localFile.length() > 0)
				this.fileSize = this.localFile.length();

		} else {


			String[] plop = publicKey.split("/");
			this.name = plop[plop.length-1];

			if(this.keyType != 0) {
				plop = this.name.split("\\-");
				if(plop.length >= 2) {
					this.name = "";
					for(int i = 0 ; i < plop.length-1; i++) {
						this.name = this.name + plop[i];
						if(i < plop.length-2)
							this.name = this.name +"-";
					}
				}
			}


			if("null".equals( this.name ) || "CHK@".equals( this.name )) {
				Logger.warning(this, "The node returns \"null\" as filename. Using id !");
				Logger.warning(this, "( URI="+publicKey +" )");
				Logger.warning(this, "( Identifier="+identifier+" )");

				plop = this.identifier.split("\\-");

				if(plop.length >= 2) {
					this.name = "";
					for(int i = 1 ; i < plop.length; i++) {
						if(i != 1)
							this.name = this.name +"-";
						this.name = this.name + plop[i];

					}
				}


			}

		}

		if (fileName != null) { /* We want to force the filename */ /* See PersistentPut.TargetFilename */
			this.name = fileName;
		}

		this.publicKey = null;

		this.priority = priority;
		this.global = global;

		this.persistence = persistence;

		this.progress = progress;

		this.status = status;
		this.running = true;
		this.finished = false;
		this.successful = false;
		this.fatal = true;
	}


	public boolean start(FCPQueueManager queueManager) {
		this.queueManager = queueManager;

		if(this.localFile != null && this.localFile.length() <= 0) {
			Logger.warning(this, "Empty or unreachable file:"+this.localFile.getPath());

			this.status = "EMPTY OR UNREACHABLE FILE";

			this.successful = false;
			this.fatal = true;
			this.finished = true;
			this.running = false;

			this.setChanged();
			this.notifyObservers();

			return false;
		}


		queueManager.getQueryManager().addObserver(this);

		this.progress = 0;
		this.finished = false;
		this.successful = false;
		this.running = false;

		if(this.keyType == 2 && this.privateKey == null) {
			this.generateSSK();
		}

		if( (this.keyType == 2 && this.privateKey != null) || this.keyType != 2) {
			this.startInsert();
		}

		this.setChanged();
		this.notifyObservers();

		return true;
	}

	/**
	 * doesn't set running = true. startInsert() will.
	 */
	public void generateSSK() {
		this.status = "Generating keys";

		this.sskGenerator = new FCPGenerateSSK();

		this.sskGenerator.addObserver(this);
		this.sskGenerator.start(this.queueManager);
	}



	private class UnlockWaiter implements Runnable {
		FCPClientPut clientPut;
		FCPConnection connection;

		public UnlockWaiter(FCPClientPut clientPut, FCPConnection connection) {
			this.clientPut = clientPut;
			this.connection = connection;
		}

		public void run() {
			connection.addToWriterQueue();

			FCPClientPut.this.lockOwner = true;

			this.clientPut.continueInsert();
			return;
		}
	}



	public boolean startInsert() {
		FCPConnection connection = this.queueManager.getQueryManager().getConnection();

		this.toTheNodeProgress= 0;

		this.status = "Waiting for socket availability";

		Logger.info(this, "Another file is being uploaded ... waiting ...");

		Thread fork = new Thread(new UnlockWaiter(this, connection));

		fork.start();

		return true;
	}

	public boolean continueInsert() {
		this.running = true; /* Here we are really running */
		this.sending = true;

		FCPConnection connection = this.queueManager.getQueryManager().getConnection();

		this.status = "Sending to the node";

		if (this.localFile != null)
			this.identifier = this.queueManager.getAnID() + "-"+ this.localFile.getName();
		else
			this.identifier = this.queueManager.getAnID();

		this.setChanged();
		this.notifyObservers();

		FCPMessage msg = new FCPMessage();

		msg.setMessageName("ClientPut");
		msg.setValue("URI", this.getInsertionKey());

		if(this.metadatas != null) {
			for(Iterator keyIt = this.metadatas.keySet().iterator();
			    keyIt.hasNext();) {
				String key = (String)keyIt.next();
				String value = (String)this.metadatas.get(key);
				msg.setValue("Metadata."+key, value);
			}
		}

		msg.setValue("Identifier", this.identifier);
		msg.setValue("MaxRetries", "-1");
		msg.setValue("PriorityClass", Integer.toString(this.priority));

		if(this.getCHKOnly) {
			msg.setValue("GetCHKOnly", "true");
			//msg.setValue("Verbosity", "0");
		} else {
			msg.setValue("GetCHKOnly", "false");
			//msg.setValue("Verbosity", "512");
		}
		msg.setValue("Verbosity", "512");

		if(this.global)
			msg.setValue("Global", "true");
		else
			msg.setValue("Global", "false");

		if (this.localFile != null)
			msg.setValue("ClientToken", this.localFile.getPath());

		switch(this.persistence) {
		case(0): msg.setValue("Persistence", "forever"); break;
		case(1): msg.setValue("Persistence", "reboot"); break;
		case(2): msg.setValue("Persistence", "connection"); break;
		default: Logger.notice(this, "Unknow persistence !?"); break;
		}

		if (this.localFile != null)
			msg.setValue("TargetFilename", this.localFile.getName());
		else
			msg.setValue("TargetFilename", this.name);

		msg.setValue("UploadFrom", "direct");

		msg.setAmountOfDataWaiting(this.fileSize);
		Logger.info(this, "Sending "+Long.toString(this.fileSize)+" bytes on socket ...");

		this.queueManager.getQueryManager().writeMessage(msg, false);


		Logger.info(this, "Sending file to the node");
		boolean ret = this.sendFile();
		Logger.info(this, "File sent (or not :p)");

		connection.removeFromWriterQueue();
		this.lockOwner = false;
		this.sending = false;

		if(ret == true) {
			this.successful = false;
			this.fatal = true;
			this.finished = false;
			this.progress = 0;
			this.running = true;

			if (!this.getCHKOnly)
				this.status = "Inserting";
			else
				this.status = "Computing";

			this.setChanged();
			this.notifyObservers();

			return true;
		} else {
			this.successful = false;
			this.finished = true;
			this.progress = 0;
			this.running = false;

			this.status = "Unable to send the file to the node";
			this.setChanged();
			this.notifyObservers();

			return false;
		}

	}


	private boolean sendFile() {
		FCPConnection connection = this.queueManager.getQueryManager().getConnection();

		long remaining = this.fileSize;
		byte[] data = null;

		FileInputStream in = null;

		if (this.localFile == null) {
			this.toTheNodeProgress = 100;
			return true;
		}

		try {
			in = new FileInputStream(this.localFile);
		} catch(java.io.FileNotFoundException e) {
			Logger.error(this, "FileNotFoundException ?! ohoh, problems ...");
			return false;
		}

		long startTime = System.currentTimeMillis();
		long origSize = remaining;

		while(remaining > 0) {
			int to_read = PACKET_SIZE;

			if(remaining < to_read)
				to_read = (int)remaining;

			data = new byte[to_read];

			try {
				if(in.read(data) < 0) {
					Logger.error(this, "Error while reading file ?!");
					return false;
				}
			} catch(java.io.IOException e) {
				Logger.error(this, "IOException while reading file! proobleeem");
				return false;
			}

			if(!connection.rawWrite(data)) {
				Logger.error(this, "Error while writing file on socket ! Disconnected ?");
				return false;
			}

			remaining = remaining - to_read;

			if( System.currentTimeMillis() >= (startTime+3000) ) {
				this.toTheNodeProgress = (int) (((origSize - remaining) * 100) / origSize);
				this.setChanged();
				this.notifyObservers();
				startTime = System.currentTimeMillis();
			}

			//Logger.verbose(this, "Remaining: "+(new Long(remaining)).toString());
		}


		this.toTheNodeProgress = 100;

		try {
			if(in.available() > 0) {
				Logger.error(this, "File not send completly ?!");
				return false;
			}
		} catch(java.io.IOException e) {
			/* we will suppose its ok ... */
			Logger.notice(this, "available() die (hu ?)");
		}

		return true;
	}

	public boolean stop(FCPQueueManager queueManager) {
		if(this.removeRequest()) {
			this.status = "Stopped";
			this.finished = true;
			this.successful = false;
			this.fatal= true;
			this.running = false;

			this.setChanged();
			this.notifyObservers();

			return true;
		}

		return false;
	}

	public void update(Observable o, Object param) {
		if(o == sskGenerator) {
			privateKey = sskGenerator.getPrivateKey();
			publicKey = sskGenerator.getPublicKey() + "/" + this.name;

			setChanged();
			notifyObservers();

			startInsert();
			return;
		}

		if(o == queueManager.getQueryManager()) {
			FCPMessage msg = (FCPMessage)param;

			if(msg == null
			   || msg.getValue("Identifier") == null
			   || !msg.getValue("Identifier").equals(this.identifier))
				return;

			if("URIGenerated".equals( msg.getMessageName() )
			   || "PutFetchable".equals( msg.getMessageName() )) {
				this.running = true;
				this.finished = false;
				this.successful = false;

				this.publicKey = msg.getValue("URI");

				this.publicKey = this.publicKey.replaceAll("freenet:", "");

				Logger.info(this, msg.getMessageName()+": "+this.publicKey);

				if(this.getCHKOnly) {
					this.status = "CHK";

					this.progress = 100;
					this.toTheNodeProgress = 100;
					this.running = false;
					this.finished = true;
					this.successful = true;
					this.fatal = false;
					this.sending = false;

					this.setChanged();
					this.notifyObservers();
					this.queueManager.getQueryManager().deleteObserver(this);
					return;
				}

				this.status = "Inserting";

				this.setChanged();
				this.notifyObservers();
				return;
			}

			if("PutSuccessful".equals( msg.getMessageName() )) {
				this.successful = true;
				this.finished = true;
				this.running = false;

				this.publicKey = msg.getValue("URI");

				if (this.publicKey == null) {
					this.status = "[Warning]";
					Logger.warning(this, "PutSuccessful message without URI field ?!");
					this.setChanged();
					this.notifyObservers();
					return;
				}

				this.publicKey = this.publicKey.replaceAll("freenet:", "");

				if(this.keyType == 1)
					this.publicKey = "KSK@"+this.name+"-" + Integer.toString(this.rev);
				//if(keyType == 2)
				//	publicKey = publicKey + "/" + name + "-" + Integer.toString(rev);


				this.status = "Finished";

				this.progress = 100;

				this.setChanged();
				this.notifyObservers();
				return;
			}

			if("PutFailed".equals( msg.getMessageName() )) {

				this.successful = false;
				this.running = false;
				this.finished = true;
				this.fatal = true;

				this.status = "Failed ("+msg.getValue("CodeDescription")+")";

				if(msg.getValue("Fatal") != null &&
				   msg.getValue("Fatal").equals("false")) {
					this.status = this.status + " (non-fatal)";
					this.fatal = false;
				}

				Logger.warning(this, "==== PUT FAILED ===");
				Logger.warning(this, msg.toString());

				this.setChanged();
				this.notifyObservers();
				return;
			}

			if("ProtocolError".equals( msg.getMessageName() )) {

				this.successful = false;
				this.running = false;
				this.fatal = true;
				this.finished = true;

				if(this.lockOwner) {
					this.lockOwner = false;
					this.queueManager.getQueryManager().getConnection().removeFromWriterQueue();
				}

				this.status = "Protocol error ("+msg.getValue("CodeDescription")+")";

				if(msg.getValue("Fatal") != null &&
				   msg.getValue("Fatal").equals("false")) {
					this.status = this.status + " (non-fatal)";
					this.fatal = false;
				}

				this.setChanged();
				this.notifyObservers();

				return;
			}

			if(msg.getMessageName().equals("IdentifierCollision")) {
				this.status = "Identifier collision";
				this.start(this.queueManager); /* et hop ca repart :) */
				return;
			}


			if(msg.getMessageName().equals("PersistentPut")) {
				this.status = "Inserting";
				//publicKey = msg.getValue("URI");
				return;
			}

			if(msg.getMessageName().equals("StartedCompression")) {
				this.status = "Compressing";

				this.setChanged();
				this.notifyObservers();

				return;
			}

			if(msg.getMessageName().equals("FinishedCompression")) {
				this.status = "Inserting";

				if(msg.getValue("OrigSize") == null
				   || msg.getValue("CompressedSize") == null) {
					this.setChanged();
					this.notifyObservers();
					return;
				}

				int rate = (int)( ((new Long(msg.getValue("OrigSize"))).longValue() * 100) / (new Long(msg.getValue("CompressedSize"))).longValue() );

				Logger.info(this, "Compression: "+ Integer.toString(rate));

				this.setChanged();
				this.notifyObservers();

				return;
			}

			if(msg.getMessageName().equals("SimpleProgress")) {

				if(msg.getValue("Total") != null
				   && msg.getValue("Succeeded") != null) {

					long total = (new Long(msg.getValue("Total"))).longValue();
					//long required = (new Long(msg.getValue("Required"))).longValue();
					long succeeded = (new Long(msg.getValue("Succeeded"))).longValue();

					this.progress = (int)((succeeded * 99) / total);

					this.running = true;
					this.finished = false;
					this.successful = false;

					//if(fileSize == 0)
					//	fileSize = BLOCK_SIZE * required; // NOT RELIABLE

					if (!this.getCHKOnly)
						this.status = "Inserting";
					else
						this.status = "Computing";

					this.setChanged();
					this.notifyObservers();
				}

				return;
			}


			Logger.notice(this, "Unknow message.");

			return;
		}

	}


	public int getQueryType() {
		return 2;
	}

	public boolean pause(FCPQueueManager queueManager) {
		this.progress = 0;
		this.status = "Delayed";

		this.running = false;
		this.successful = false;
		this.finished = false;
		this.fatal = true;

		this.removeRequest();
		return false;
	}


	public boolean removeRequest() {
		this.setChanged();
		this.notifyObservers();

		if(this.sending) {
			Logger.notice(this, "Can't interrupt while sending to the node ...");
			this.status = this.status + " (can't interrupt while sending to the node)";
			this.setChanged();
			this.notifyObservers();
			return false;
		}

		if(this.isRunning() || this.isFinished()) {
			FCPMessage msg = new FCPMessage();
			msg.setMessageName("RemovePersistentRequest");
			msg.setValue("Identifier", this.identifier);

			if(this.global)
				msg.setValue("Global", "true");
			else
				msg.setValue("Global", "false");

			this.queueManager.getQueryManager().writeMessage(msg);

			this.running = false;

			this.queueManager.getQueryManager().deleteObserver(this);
		} else {
			Logger.notice(this, "Nothing to remove");
		}


		return true;
	}

	public void updatePersistentRequest(boolean clientToken) {
		//if(!isPersistent())
		//	return;

		FCPMessage msg = new FCPMessage();

		msg.setMessageName("ModifyPersistentRequest");
		msg.setValue("Global", Boolean.toString(this.global));
		msg.setValue("Identifier", this.identifier);
		msg.setValue("PriorityClass", Integer.toString(this.priority));

		if(clientToken && this.getPath() != null)
			msg.setValue("ClientToken", this.getPath());

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



	public String getStatus() {
		return this.status;
	}

	public int getProgression() {
		return this.progress;
	}

	public boolean isProgressionReliable() {
		return true;
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

		if(this.keyType == 0 && this.publicKey != null)
			key = this.publicKey;
		if(this.keyType == 0 && this.publicKey == null)
			key = "CHK@";
		if(this.keyType == 1)
			key = "KSK@" + this.name + "-"+ Integer.toString(this.rev);
		if(this.keyType == 2)
			key = this.privateKey + this.name+"-"+this.rev;

		return key;
	}

	public long getFileSize() {
		return this.fileSize; /* a "file length" ? why not "file size" as usual ? */
	}

	public String getPath() {
		if(this.localFile != null)
			return this.localFile.getPath();
		else
			return null;
	}

	public String getFilename() {
		if(this.localFile != null)
			return this.localFile.getName();
		else
			return this.name;
	}

	public int getAttempt() {
		return this.attempt;
	}

	public void setAttempt(int x) {
		this.attempt = x;
	}

	public int getMaxAttempt() {
		return -1;
	}

	public boolean isRunning() {
		return this.running;
	}

	public boolean isFinished() {
		return this.finished;
	}

	public boolean isSuccessful() {
		return this.successful;
	}

	public boolean isFatallyFailed() {
		return ((!this.successful) && this.fatal);
	}

	public HashMap getParameters() {
		HashMap result = new HashMap();

		result.put("localFile", this.localFile.getPath());
		result.put("keyType", Integer.toString(this.keyType));
		result.put("Revision", Integer.toString(this.rev));
		result.put("Name", this.name);
		if(this.privateKey != null)
			result.put("privateKey", this.privateKey);
		if(this.publicKey != null)
			result.put("publicKey", this.publicKey);
		result.put("priority", Integer.toString(this.priority));
		result.put("global", Boolean.toString(this.global));
		result.put("persistence", Integer.toString(this.persistence));

		result.put("progress", Integer.toString(this.progress));

		result.put("status", this.status);

		result.put("attempt", Integer.toString(this.attempt));
		if(this.identifier != null)
			result.put("identifier", this.identifier);
		result.put("running", Boolean.toString(this.running));
		result.put("successful", Boolean.toString(this.successful));
		result.put("finished", Boolean.toString(this.finished));

		return result;
	}


	public boolean setParameters(HashMap parameters) {

		this.localFile = new File((String)parameters.get("localFile"));

		this.fileSize = this.localFile.length();

		this.keyType = Integer.parseInt((String)parameters.get("keyType"));
		this.rev = Integer.parseInt((String)parameters.get("Revision"));
		this.name = (String)parameters.get("name");

		this.privateKey = (String)parameters.get("privateKey");
		if(this.privateKey == null || this.privateKey.equals(""))
			this.privateKey = null;

		this.publicKey = (String)parameters.get("publicKey");
		if(this.privateKey == null || this.publicKey == null || this.publicKey.equals(""))
			this.publicKey = null;

		this.priority = Integer.parseInt((String)parameters.get("priority"));

		this.global = Boolean.valueOf((String)parameters.get("global")).booleanValue();

		this.persistence = Integer.parseInt((String)parameters.get("persistence"));
		this.progress = Integer.parseInt(((String)parameters.get("progress")));
		this.status = (String)parameters.get("status");
		this.attempt = Integer.parseInt((String)parameters.get("attempt"));

		this.identifier = (String)parameters.get("identifier");
		if(this.identifier == null || this.identifier.equals(""))
			this.identifier = null;

		this.running = Boolean.valueOf((String)parameters.get("running")).booleanValue();
		this.successful = Boolean.valueOf((String)parameters.get("successful")).booleanValue();
		this.finished = Boolean.valueOf((String)parameters.get("finished")).booleanValue();

		if(this.persistence == 2 && !this.isFinished()) {
			this.progress = 0;
			this.status = "Waiting";
		}

		return true;
	}


	public boolean isPersistent() {
		return this.persistence < 2;
	}

	public boolean isGlobal() {
		return this.global;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public String getPrivateKey() {
		return this.privateKey;
	}

	public String getPublicKey() {
		return this.publicKey;
	}

	/**
	 * Do nothing.
	 */
	public boolean saveFileTo(String dir) {
		return false;
	}

	public int getTransferWithTheNodeProgression() {
		return this.toTheNodeProgress;
	}

	public HashMap getMetadatas() {
		return this.metadatas;
	}

	public void setMetadata(String name, String val) {
		if(this.metadatas == null)
			this.metadatas = new HashMap();

		if(val == null)
			this.metadatas.remove(name);
		else
			this.metadatas.put(name, val);
	}

	public String getMetadata(String name) {
		return (String)this.metadatas.get(name);
	}

}
