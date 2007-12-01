package thaw.fcp;

import java.util.Observable;
import java.util.Observer;

import java.util.Hashtable;


public class FCPGetNode extends Observable implements FCPQuery, Observer {
	public final static String[] refElements = {
		"identity",
		"location",
		"testnet",
		"myName",
		"lastGoodVersion",
		"sig",
		"version",
		"dsaPubKey.y",
		"physical.udp",
		"dsaGroup.g",
		"dsaGroup.q",
		"dsaGroup.p",
		"ark.pubURI",
		"ark.number",
		"auth.negTypes"
	};

	private String ref;

	public final static String maxMemElement = "volatile.maximumJavaMemory";
	private long maxMem  = 134217728;

	public final static String usedMemElement = "volatile.usedJavaMemory";
	private long usedMem = 0;

	public final static String nmbThreadsElement = "volatile.runningThreadCount";
	private int nmbThreads = 0;


	private boolean withPrivate;
	private boolean withVolatile;

	private Hashtable allParameters;


	public FCPGetNode(boolean withPrivate, boolean withVolatile) {
		this.withPrivate = withPrivate;
		this.withVolatile = withVolatile;
	}

	public boolean start(FCPQueueManager queueManager) {
		FCPMessage msg = new FCPMessage();

		msg.setMessageName("GetNode");
		msg.setValue("WithPrivate", Boolean.toString(withPrivate));
		msg.setValue("WithVolatile", Boolean.toString(withVolatile));

		queueManager.getQueryManager().addObserver(this);

		return queueManager.getQueryManager().writeMessage(msg);
	}

	public boolean stop(FCPQueueManager queueManager) {
		queueManager.getQueryManager().deleteObserver(this);
		return true;
	}

	public int getQueryType() {
		return 0;
	}




	public void update(Observable o, Object param) {

		if (o instanceof FCPQueryManager) {
			final FCPMessage msg = (FCPMessage)param;

			if (msg.getMessageName() == null
			    || !msg.getMessageName().equals("NodeData"))
				return;

			ref = "";

			for (int i = 0 ; i < refElements.length ; i++) {
				if (msg.getValue(refElements[i]) != null)
					ref += refElements[i] + "=" +
						msg.getValue(refElements[i])
					+ "\n";
			}

			ref += "End\n";

			if (withVolatile) {
				if (msg.getValue(maxMemElement) != null)
					maxMem = Long.parseLong(msg.getValue(maxMemElement));

				if (msg.getValue(usedMemElement) != null)
					usedMem = Long.parseLong(msg.getValue(usedMemElement));

				if (msg.getValue(nmbThreadsElement) != null)
					nmbThreads = Integer.parseInt(msg.getValue(nmbThreadsElement));
			}

			allParameters = msg.getValues();

			setChanged();
			notifyObservers(this);
		}
	}


	/**
	 * Darknet ref.
	 * Not really maintained anymore. Please report if outdated
	 */
	public String getRef() {
		return ref;
	}

	public long getMaxJavaMemory() {
		return maxMem;
	}

	public long getUsedJavaMemory() {
		return usedMem;
	}

	public int getNmbThreads() {
		return nmbThreads;
	}

	public Hashtable getAllParameters() {
		return allParameters;
	}
}
