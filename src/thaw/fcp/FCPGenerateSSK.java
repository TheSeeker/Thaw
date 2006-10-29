package thaw.fcp;

import java.util.Observer;
import java.util.Observable;

import thaw.core.Logger;

public class FCPGenerateSSK extends Observable implements FCPQuery, Observer {
	private String identifier = null;
	private String privateKey = null;
	private String publicKey = null;

	private FCPQueueManager queueManager = null;


	public FCPGenerateSSK() {

	}


	public boolean start(FCPQueueManager queueManager) {
		this.queueManager = queueManager;

		queueManager.getQueryManager().addObserver(this);

		this.identifier = queueManager.getAnID();

		FCPMessage msg = new FCPMessage();
		msg.setMessageName("GenerateSSK");
		msg.setValue("Identifier", this.identifier);

		queueManager.getQueryManager().writeMessage(msg);

		return true;
	}


	public void update (Observable o, Object param) {
		FCPMessage msg = (FCPMessage)param;

		if(msg.getValue("Identifier") == null
		   || !msg.getValue("Identifier").equals(this.identifier))
			return;

		if("SSKKeypair".equals( msg.getMessageName() )) {
			Logger.debug(this, "SSKKeypair !");

			this.privateKey = msg.getValue("InsertURI");
			this.publicKey = msg.getValue("RequestURI");

			this.privateKey = this.privateKey.replaceFirst("freenet:", "");
			this.publicKey = this.publicKey.replaceFirst("freenet:", "");

			this.setChanged();
			this.notifyObservers();

			this.stop(this.queueManager);

			return;
		}


	}


	public boolean stop(FCPQueueManager queueManager) {
		queueManager.getQueryManager().deleteObserver(this);

		return true;
	}

	public int getQueryType() {
		return 0;
	}


	/**
	 * @return privateKey without the "freenet:" prefix.
	 */
	public String getPrivateKey() {
		return this.privateKey;
	}

	/**
	 * @return publicKey without the "freenet:" prefix.
	 */
	public String getPublicKey() {
		return this.publicKey;
	}

}
