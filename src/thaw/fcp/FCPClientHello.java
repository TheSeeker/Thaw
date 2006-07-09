package thaw.fcp;

import java.util.Observer;
import java.util.Observable;

import thaw.core.Logger;

/**
 * http://wiki.freenetproject.org/FreenetFCPSpec2Point0
 * See "ClientHello" and "NodeHello".
 * Note: This query disconnect you if node answer CloseConnectionDuplicateClientName
 *       and start() returns false.
 */
public class FCPClientHello implements FCPQuery, Observer {

	private final static String FCP_EXPECTED_VERSION = "2.0";
	private String id = null;

	private String nodeFCPVersion = null;
	private String nodeVersion = null;
	private String nodeName = null;
	private boolean testnet = false; /* Hmm, in fact, we shouldn't have to bother about this one */
	private int nmbCompressionCodecs = -1;

	private boolean receiveAnswer = false;
	private boolean successful = false;

	private FCPQueryManager queryManager = null;


	/**
	 * Need to know the id of the application (see FCP specs).
	 */
	public FCPClientHello(FCPQueryManager queryManager, String id) {
		setID(id);
		this.queryManager = queryManager;
	}


	public void setID(String id) {
		this.id = id;
	}

	public int getThawPriority() {
		return -1;
	}

	public String getNodeFCPVersion() {
		return nodeFCPVersion;
	}

	public String getNodeVersion() {
		return nodeVersion;
	}

	public String getNodeName() {
		return nodeName;
	}

	public boolean isOnTestnet() {
		return testnet;
	}

	public int getNmbCompressionCodecs() {
		return nmbCompressionCodecs;
	}


	/**
	 * Warning: This query is blocking (only this one) !
	 */
	public boolean start(FCPQueueManager queueManager) {

		FCPMessage message = new FCPMessage();

		message.setMessageName("ClientHello");
		message.setValue("Name", id);
		message.setValue("ExpectedVersion", FCP_EXPECTED_VERSION);

		queryManager.addObserver(this);

		if(!queryManager.writeMessage(message)) {
			Logger.warning(this, "Unable to say hello ... ;(");
			return false;
		}

		while(!receiveAnswer) {
			try {
				Thread.sleep(500);
			} catch(java.lang.InterruptedException e) {
				/* Dodo j'ai dis ! */
			}
		}

		if(nodeName != null) {
			Logger.info(this, "Hello "+nodeName+", I'm Thaw :)");
		} else {
			Logger.warning(this, "Unable to connect, ID is probably already taken");
			return false;
		}

		return true;
	}


	public void update(Observable o, Object arg) {
		if(arg == null)
			return;

		FCPMessage answer = (FCPMessage)arg;

		if(o == queryManager) {
			
			if(answer.getMessageName().equals("NodeHello")) {
				successful = true;
				Logger.info(this, "Received a nodeHello");

				nodeFCPVersion = answer.getValue("FCPVersion");
				nodeVersion = answer.getValue("Version");
				nodeName = answer.getValue("Node");
				testnet = (new Boolean(answer.getValue("Testnet"))).booleanValue();
				nmbCompressionCodecs = (new Integer(answer.getValue("CompressionCodecs"))).intValue();

				queryManager.deleteObserver(this);

				receiveAnswer = true;
			}

			if(answer.getMessageName().equals("CloseConnectionDuplicateClientName")) {
				/* Damn ... ! */
				Logger.warning(this, "According to the node, Thaw ID is already used. Please change it in the configuration");
				queryManager.deleteObserver(this);
				queryManager.getConnection().disconnect();
				receiveAnswer = true;
			}
		}

		if(!receiveAnswer) {
			Logger.warning(this, "This message wasn't for us ?! : "+answer.getMessageName());
		}
	}

	/**
	 * Not used.
	 */
	public boolean stop(FCPQueueManager queueManager) {
		return false;
	}


	public int getQueryType() {
		return 0;
	}

	public String getStatus() {
		return null;
	}

	public int getProgression() {
		return 0;
	}

	public String getFileKey() {
		return null;
	}

	public long getFileSize() {
		return 0;
	}

	public boolean isRunning() {
		return false;
	}

	public boolean isFinished() {
		return true;
	}

	public String getPath() {
		return null;
	}

	public int getAttempt() {
		return -1;
	}

	public boolean isSuccessful() {
		return successful;
	}

}

