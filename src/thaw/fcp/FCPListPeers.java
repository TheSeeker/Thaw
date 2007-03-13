package thaw.fcp;

import java.util.Observer;
import java.util.Observable;

import java.util.Vector;
import java.util.Hashtable;

public class FCPListPeers extends Observable implements FCPQuery, Observer {

	private boolean withMetadata;
	private boolean withVolatile;

	private FCPQueueManager queueManager;

	private Hashtable peers; /* key : peer name -> hashtable : key : parameter name -> parameter value */


	public FCPListPeers(boolean withMetadata, boolean withVolatile) {
		this.withMetadata = withMetadata;
		this.withVolatile = withVolatile;
		peers = new Hashtable();
	}


	public boolean start(FCPQueueManager queueManager) {
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

			if (msg.getMessageName() == null
			    || !msg.getMessageName().equals("Peer"))
				return;

			peers.put(msg.getValue("identity"), msg.getValues());

			setChanged();
			notifyObservers(this);
		}
	}


	public Hashtable getPeers() {
		return peers;
	}


	public int getQueryType() {
		return 0;
	}
}
