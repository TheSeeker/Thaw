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

		identifier = queueManager.getAnID();

		FCPMessage msg = new FCPMessage();
		msg.setMessageName("GenerateSSK");
		msg.setValue("Identifier", identifier);

		queueManager.getQueryManager().writeMessage(msg);
	
		return true;
	}

	
	public void update (Observable o, Object param) {
		FCPMessage msg = (FCPMessage)param;

		if(msg.getValue("Identifier") == null
		   || !msg.getValue("Identifier").equals(identifier))
			return;

		if(msg.getMessageName().equals("SSKKeypair")) {
			Logger.debug(this, "SSKKeypair !");

			privateKey = msg.getValue("InsertURI");
			publicKey = msg.getValue("RequestURI");

			privateKey = privateKey.replaceFirst("freenet:", "");
			publicKey = publicKey.replaceFirst("freenet:", "");

			setChanged();
			notifyObservers();

			stop(queueManager);

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
		return privateKey;
	}

	/**
	 * @return publicKey without the "freenet:" prefix.
	 */
	public String getPublicKey() {
		return publicKey;
	}

}
