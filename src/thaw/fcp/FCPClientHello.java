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
		this.setID(id);
		this.queryManager = queryManager;
	}


	public void setID(String id) {
		this.id = id;
	}

	public String getNodeFCPVersion() {
		return this.nodeFCPVersion;
	}

	public String getNodeVersion() {
		return this.nodeVersion;
	}

	public String getNodeName() {
		return this.nodeName;
	}

	public boolean isOnTestnet() {
		return this.testnet;
	}

	public int getNmbCompressionCodecs() {
		return this.nmbCompressionCodecs;
	}


	/**
	 * Warning: This query is blocking (only this one) !
	 */
	public boolean start(FCPQueueManager queueManager) {

		FCPMessage message = new FCPMessage();

		message.setMessageName("ClientHello");
		message.setValue("Name", this.id);
		message.setValue("ExpectedVersion", FCP_EXPECTED_VERSION);

		this.queryManager.addObserver(this);

		if(!this.queryManager.writeMessage(message)) {
			Logger.warning(this, "Unable to say hello ... ;(");
			return false;
		}

		while(!this.receiveAnswer) {
			try {
				Thread.sleep(500);
			} catch(java.lang.InterruptedException e) {
				/* Dodo j'ai dis ! */
			}
		}

		if(this.nodeName != null) {
			Logger.info(this, "Hello "+this.nodeName+", I'm Thaw :)");
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

		if(o == this.queryManager) {

			if("NodeHello".equals( answer.getMessageName() )) {
				this.successful = true;
				Logger.info(this, "Received a nodeHello");

				this.nodeFCPVersion = answer.getValue("FCPVersion");
				this.nodeVersion = answer.getValue("Version");
				this.nodeName = answer.getValue("Node");
				this.testnet = Boolean.valueOf(answer.getValue("Testnet")).booleanValue();
				this.nmbCompressionCodecs = Integer.parseInt(answer.getValue("CompressionCodecs"));

				this.queryManager.deleteObserver(this);

				this.receiveAnswer = true;
			}

			if("CloseConnectionDuplicateClientName".equals( answer.getMessageName() )) {
				/* Damn ... ! */
				Logger.warning(this, "According to the node, Thaw ID is already used. Please change it in the configuration");
				this.queryManager.deleteObserver(this);
				this.queryManager.getConnection().disconnect();
				this.receiveAnswer = true;
			}
		}

		if(!this.receiveAnswer) {
			Logger.warning(this, "This message wasn't for us ?! : "+answer.getMessageName());
		}
	}

	/**
	 * Not used.
	 */
	public boolean stop(FCPQueueManager queueManager) {
		return true;
	}


	public int getQueryType() {
		return 0;
	}

}

