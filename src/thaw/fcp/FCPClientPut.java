package thaw.fcp;

import java.util.HashMap;
import java.io.File;
import java.util.Observer;
import java.util.Observable;
import java.io.FileInputStream;

import thaw.core.Logger;

/**
 * Allow to insert a simple file.
 */
public class FCPClientPut extends Observable implements FCPTransferQuery, Observer {
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

	private int progress = 0;
	private int toTheNodeProgress = 0;
	private String status = null;

	private int attempt = 0;

	private String identifier = null;

	private boolean running = false;
	private boolean finished = false;
	private boolean successful = false;
	private boolean sending = false;
	
	private FCPGenerateSSK sskGenerator = null;


	private final static int PACKET_SIZE = 1024;

	/**
	 * To resume query from file. (see core.QueueKeeper)
	 */
	public FCPClientPut(FCPQueueManager queueManager, HashMap parameters) {
		this.queueManager = queueManager;
		setParameters(parameters);
	}

	/**
	 * To start a new insert.
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
		this.localFile = file;
		fileSize = file.length();

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

	}

	/**
	 * Used for resuming.
	 * @param publicKey : Complete key (with filename, etc)
	 */
	public FCPClientPut(String identifier, String publicKey,
			    int priority, int persistence, boolean global,
			    String srcFile, String status, int progress,
			    FCPQueueManager queueManager) {
		toTheNodeProgress = 100;
		
		this.queueManager = queueManager;
		this.identifier = identifier;
		
		if(publicKey.startsWith("CHK"))
			keyType = 0;
		if(publicKey.startsWith("KSK"))
			keyType = 1;
		if(publicKey.startsWith("SSK"))
			keyType = 2;
		if(publicKey.startsWith("USK"))
			keyType = 2;


		this.publicKey = publicKey;

		if(srcFile != null) {
			String[] plop = srcFile.split(File.separator);
			this.name = plop[plop.length-1];
		} else {
			String[] plop = publicKey.split(File.separator);
			this.name = plop[plop.length-1];

			if(keyType != 0) {
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
		}
		
		this.publicKey = null;

		this.priority = priority;
		this.global = global;

		this.persistence = persistence;

		this.progress = progress;
		
		this.status = status;
		running = true;
		finished = false;
		successful = false;
	}


	public boolean start(FCPQueueManager queueManager) {
		this.queueManager = queueManager;
		
		queueManager.getQueryManager().addObserver(this);
		
		progress = 0;
		finished = false;
		successful = false;
		running = false;
		
		if(keyType == 2 && privateKey == null) {
			generateSSK();
		}
		
		if( (keyType == 2 && privateKey != null) || keyType != 2) {
			startInsert();
		}
		
		setChanged();
		notifyObservers();		

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



	private class UnlockWaiter implements Runnable {
		FCPClientPut clientPut;
		FCPConnection connection;

		public UnlockWaiter(FCPClientPut clientPut, FCPConnection connection) {
			this.clientPut = clientPut;
			this.connection = connection;
		}

		public void run() {
			while(true) {
				if(!connection.isWritingLocked())
					break;

				try {
					Thread.sleep(200);
				} catch(java.lang.InterruptedException e) {

				}

			}

			if(!connection.lockWriting()) {
				/* Ah ben oué mais non ... */
				run();
			}

			clientPut.continueInsert();
			return;
		}
	}



	public boolean startInsert() {
		FCPConnection connection = queueManager.getQueryManager().getConnection();

		toTheNodeProgress= 0;

		status = "Waiting socket availability";
		
		Logger.info(this, "Another file is being uploaded ... waiting ...");
		
		Thread fork = new Thread(new UnlockWaiter(this, connection));
		
		fork.start();
		
		return true;
	}

	public boolean continueInsert() {
		running = true; /* Here we are really running */
		sending = true;

		FCPConnection connection = queueManager.getQueryManager().getConnection();

		status = "Sending to the node";

		identifier = queueManager.getAnID() + "-"+ localFile.getName();

		setChanged();
		notifyObservers();

		FCPMessage msg = new FCPMessage();

		msg.setMessageName("ClientPut");
		msg.setValue("URI", getInsertionKey());
		msg.setValue("Identifier", identifier);
		msg.setValue("Verbosity", "512");
		msg.setValue("MaxRetries", "-1");
		msg.setValue("PriorityClass", (new Integer(priority)).toString());
		msg.setValue("GetCHKOnly", "false");
		if(global)
			msg.setValue("Global", "true");
		else
			msg.setValue("Global", "false");
		msg.setValue("ClientToken", localFile.getPath());
		
		switch(persistence) {
		case(0): msg.setValue("Persistence", "forever"); break;
		case(1): msg.setValue("Persistence", "reboot"); break;
		case(2): msg.setValue("Persistence", "connection"); break;
		default: Logger.notice(this, "Unknow persistence !?"); break;
		}

		msg.setValue("UploadFrom", "direct");

		msg.setAmountOfDataWaiting(localFile.length());
		Logger.info(this, "Sending "+(new Long(localFile.length()))+" bytes on socket ...");
		
		queueManager.getQueryManager().writeMessage(msg, false);

		
		Logger.info(this, "Sending file to the node");
		boolean ret = sendFile();
		Logger.info(this, "File sent (or not)");

		connection.unlockWriting();
		sending = false;

		if(ret == true) {
			successful = false;
			finished = false;
			progress = 0;
			running = true;

			status = "Inserting";
			
			setChanged();
			notifyObservers();

			return true;
		} else {
			successful = false;
			finished = true;
			progress = 0;
			running = false;

			status = "Unable to send the file to the node";
			setChanged();
			notifyObservers();

			return false;
		}

	}


	private boolean sendFile() {
		FCPConnection connection = queueManager.getQueryManager().getConnection();

		long remaining = localFile.length();
		byte[] data = null;

		FileInputStream in = null;

		try {
			in = new FileInputStream(localFile);
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
				toTheNodeProgress = (int) (((origSize - remaining) * 100) / origSize);
				setChanged();
				notifyObservers();
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
		} catch(java.io.IOException e) {
			/* we will suppose its ok ... */
			Logger.notice(this, "available() die (hu ?)");
		}
		
		return true;
	}

	public boolean stop(FCPQueueManager queueManager) {
		if(removeRequest()) {
			status = "Stopped";
			finished = false;
			successful = false;
			running = false;
			
			setChanged();
			notifyObservers();

			return true;
		}

		return false;
	}

	public void update(Observable o, Object param) {
		if(o == sskGenerator) {
			privateKey = sskGenerator.getPrivateKey();
			publicKey = sskGenerator.getPublicKey() + "/" + name;
			
			setChanged();
			notifyObservers();

			startInsert();
			return;
		}

		if(o == queueManager.getQueryManager()) {
			FCPMessage msg = (FCPMessage)param;

			if(msg == null
			   || msg.getValue("Identifier") == null
			   || !msg.getValue("Identifier").equals(identifier))
				return;

			if(msg.getMessageName().equals("URIGenerated")) {
				running = true;
				finished = false;
				successful = false;

				status = "Inserting";

				publicKey = msg.getValue("URI");
				publicKey = publicKey.replaceAll("freenet:", "");

				
				if(keyType == 0)
					publicKey = publicKey + "/" +name;

				/*
				if(keyType > 0)
					publicKey = publicKey + "/" + name + "-" + (new Integer(rev)).toString();
				*/
				

				setChanged();
				notifyObservers();
				return;
			}

			if(msg.getMessageName().equals("PutSuccessful")) {
				successful = true;
				finished = true;
				running = false;

				publicKey = msg.getValue("URI");
				publicKey = publicKey.replaceAll("freenet:", "");

				if(keyType == 0)
					publicKey = publicKey + "/" + name;
				if(keyType == 1)
					publicKey = "KSK@"+name+"-" + (new Integer(rev)).toString();
				if(keyType == 2)
					publicKey = publicKey + "/" + name + "-" + (new Integer(rev)).toString();
				

				status = "Finished";

				progress = 100;

				setChanged();
				notifyObservers();
				return;
			}

			if(msg.getMessageName().equals("PutFailed")) {

				successful = false;
				running = false;
				finished = true;

				status = "Failed ("+msg.getValue("CodeDescription")+")";

				if(msg.getValue("Fatal") != null &&
				   msg.getValue("Fatal").equals("false"))
					status = status + " (non-fatal)";

				setChanged();
				notifyObservers();
				return;
			}

			if(msg.getMessageName().equals("ProtocolError")) {

				successful = false;
				running = false;
				finished = true;

				status = "Protocol error ("+msg.getValue("CodeDescription")+")";
				if(msg.getValue("Fatal") != null && 
				   msg.getValue("Fatal").equals("false"))
					status = status + " (non-fatal)";

				setChanged();
				notifyObservers();

				return;
			}

			if(msg.getMessageName().equals("IdentifierCollision")) {
				status = "Identifier collision";
				start(queueManager); /* et hop ca repart :) */
				return;
			}


			if(msg.getMessageName().equals("PersistentPut")) {
				status = "Inserting";
				//publicKey = msg.getValue("URI");
				return;
			}

			if(msg.getMessageName().equals("StartedCompression")) {
				status = "Compressing";
				
				setChanged();
				notifyObservers();

				return;
			}

			if(msg.getMessageName().equals("FinishedCompression")) {
				status = "Inserting";

				if(msg.getValue("OrigSize") == null
				   || msg.getValue("CompressedSize") == null) {
					setChanged();
					notifyObservers();
					return;
				}

				int rate = (int)( ((new Long(msg.getValue("OrigSize"))).longValue() * 100) / (new Long(msg.getValue("CompressedSize"))).longValue() );

				Logger.info(this, "Compression: "+ (new Integer(rate)).intValue());

				setChanged();
				notifyObservers();

				return;
			}

			if(msg.getMessageName().equals("SimpleProgress")) {

				if(msg.getValue("Total") != null
				   && msg.getValue("Succeeded") != null) {

					long total = (new Long(msg.getValue("Total"))).longValue();
					//long required = (new Long(msg.getValue("Required"))).longValue();
					long succeeded = (new Long(msg.getValue("Succeeded"))).longValue();
					
					progress = (int)((succeeded * 99) / total);
				
					running = true;
					finished = false;
					successful = false;

					//if(fileSize == 0)
					//	fileSize = BLOCK_SIZE * required; // NOT RELIABLE

					status = "Inserting";
					
					setChanged();
					notifyObservers();
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
		progress = 0;
		status = "Delayed";

		running = false;
		successful = false;
		finished = false;

		removeRequest();
		return false;
	}


	public boolean removeRequest() {
		setChanged();
		notifyObservers();

		if(sending) {
			Logger.notice(this, "Can't interrupt while sending to the node ...");
			status = status + " (can't interrupt while sending to the node)";
			setChanged();
			notifyObservers();
			return false;
		}

		if(isRunning() || isFinished()) {
			FCPMessage msg = new FCPMessage();
			msg.setMessageName("RemovePersistentRequest");
			msg.setValue("Identifier", identifier);
			
			if(global)
				msg.setValue("Global", "true");
			else
				msg.setValue("Global", "false");
			
			queueManager.getQueryManager().writeMessage(msg);
			
			running = false;
			
			queueManager.getQueryManager().deleteObserver(this);
		} else {
			Logger.notice(this, "Nothing to remove");
		}


		return true;
	}

	public int getThawPriority() {
		return priority;
	}
	
	public String getStatus() {
		return status;
	}

	public int getProgression() {
		return progress;
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

		if(keyType == 0 && publicKey != null)
			key = publicKey;
		if(keyType == 0 && publicKey == null)
			key = "CHK@coinCoin/"+name;
		if(keyType == 1)
			key = "KSK@" + name + "-"+((new Integer(rev)).intValue());
		if(keyType == 2)
			key = privateKey + name+"-"+rev;

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

	public void setAttempt(int x) {
		attempt = x;
	}

	public int getMaxAttempt() {
		return -1;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isFinished() {
		return finished;
	}

	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * not tested : TODO : to test it
	 */
	public HashMap getParameters() {
		HashMap result = new HashMap();

		result.put("localFile", localFile.getPath());
		result.put("keyType", ((new Integer(keyType)).toString()));
		result.put("Revision", ((new Integer(rev)).toString()));
		result.put("Name", name);
		if(privateKey != null)
			result.put("privateKey", privateKey);
		if(publicKey != null)
			result.put("publicKey", publicKey);
		result.put("priority", ((new Integer(priority)).toString()));
		result.put("global", ((new Boolean(global)).toString()));
		result.put("persistence", (new Integer(persistence)).toString());
		
		result.put("progress", (new Integer(progress)).toString());
		
		result.put("status", status);

		result.put("attempt", (new Integer(attempt)).toString());
		if(identifier != null)
			result.put("identifier", identifier);
		result.put("running", ((new Boolean(running)).toString()));
		result.put("successful", ((new Boolean(successful)).toString()));
		result.put("finished", ((new Boolean(finished)).toString()));

		return result;
	}

	/**
	 * not tested ; TODO : to test it
	 */
	public boolean setParameters(HashMap parameters) {
				
		localFile = new File((String)parameters.get("localFile"));
		keyType = (new Integer((String)parameters.get("keyType"))).intValue();
		rev = (new Integer((String)parameters.get("Revision"))).intValue();
		name = (String)parameters.get("name");

		privateKey = (String)parameters.get("privateKey");
		if(privateKey == null || privateKey.equals(""))
			privateKey = null;

		publicKey = (String)parameters.get("publicKey");
		if(privateKey == null || publicKey.equals(""))
			publicKey = null;

		priority = ((new Integer((String)parameters.get("priority"))).intValue());
		global = ((new Boolean((String)parameters.get("global"))).booleanValue());
		persistence = ((new Integer((String)parameters.get("persistence"))).intValue());
		progress = ((new Integer((String)parameters.get("progress"))).intValue());
		status = (String)parameters.get("status");
		attempt = ((new Integer((String)parameters.get("attempt"))).intValue());

		identifier = (String)parameters.get("identifier");
		if(identifier == null || identifier.equals(""))
			identifier = null;

		running = ((new Boolean((String)parameters.get("running"))).booleanValue());
		successful = ((new Boolean((String)parameters.get("successful"))).booleanValue());
		finished = ((new Boolean((String)parameters.get("finished"))).booleanValue());

		if(persistence == 2 && !isFinished()) {
			progress = 0;
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

	/**
	 * Do nothing.
	 */
	public boolean saveFileTo(String dir) {
		return false;
	}

	public int getTransferWithTheNodeProgression() {
		return toTheNodeProgress;
	}
}
