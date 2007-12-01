package thaw.fcp;

import java.util.Observer;
import java.util.Observable;

import java.util.Hashtable;


public class FCPListPeers extends Observable implements FCPQuery, Observer {

	private boolean withMetadata;
	private boolean withVolatile;

	private Hashtable peers; /* key : peer name -> hashtable : key : parameter name -> parameter value */

	private boolean endList;

	public FCPListPeers(boolean withMetadata, boolean withVolatile) {
		endList = true;

		this.withMetadata = withMetadata;
		this.withVolatile = withVolatile;
		peers = new Hashtable();
	}


	public boolean start(FCPQueueManager queueManager) {
		endList = false;
		peers = new Hashtable();

		FCPMessage msg = new FCPMessage();

		msg.setMessageName("ListPeers");
		msg.setValue("WithMetadata", Boolean.toString(withMetadata));
		msg.setValue("WithVolatile", Boolean.toString(withVolatile));

		queueManager.getQueryManager().addObserver(this);

		return queueManager.getQueryManager().writeMessage(msg);
	}


	public boolean stop(FCPQueueManager queueManager) {
		queueManager.getQueryManager().deleteObserver(this);
		return true;
	}

	public void update(Observable o, Object param) {
		if (o instanceof FCPQueryManager) {
			final FCPMessage msg = (FCPMessage)param;

			if (msg.getMessageName() == null)
				return;

			if (msg.getMessageName().equals("Peer")) {
				peers.put(msg.getValue("identity"), msg.getValues());
			}

			if (msg.getMessageName().equals("EndListPeers")) {
				endList = true;
				setChanged();
				notifyObservers(this);
			}
		}
	}

	public boolean hasEnded() {
		return endList;
	}


	public Hashtable getPeers() {
		return peers;
	}


	public int getQueryType() {
		return 0;
	}
}
