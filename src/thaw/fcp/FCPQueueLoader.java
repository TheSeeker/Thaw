package thaw.fcp;

import java.util.Observer;
import java.util.Observable;

import thaw.core.Logger;

/**
 * Reload the queue from the queue node.
 * Send himself the ListPersistentRequests.
 * It remains active to receive and add the persistentGet/Put receive during the execution
 */
public class FCPQueueLoader implements FCPQuery, Observer {
	private FCPQueueManager queueManager;
	private String thawId;

	public FCPQueueLoader(String thawId) {
		this.thawId = thawId;
	}

	public boolean start(FCPQueueManager queueManager) {
		this.queueManager = queueManager;

		queueManager.getQueryManager().addObserver(this);


		FCPListPersistentRequests listPersistent = new FCPListPersistentRequests();
		boolean ret = listPersistent.start(queueManager);

		if(ret)
			queueManager.getQueryManager().getConnection().addToWriterQueue();

		return ret;
	}


	public boolean stop(FCPQueueManager queueManager) {
		queueManager.getQueryManager().deleteObserver(this);
		return true;
	}

	public int getQueryType() {
		return 0;
	}


	public void update(Observable o, Object param) {
		FCPMessage msg = (FCPMessage)param;

		if("PersistentGet".equals( msg.getMessageName() )) {
			Logger.info(this, "Resuming from PersistentGet");

			int persistence = 0;

			if("reboot".equals( msg.getValue("PersistenceType") ))
				persistence = 1;

			boolean global = true;

			if("false".equals( msg.getValue("Global") ))
				global = false;

			String destinationDir = null;

			if(msg.getValue("Identifier").startsWith(this.thawId))
				destinationDir = msg.getValue("ClientToken");

			int priority = Integer.parseInt(msg.getValue("PriorityClass"));


			FCPClientGet clientGet = new FCPClientGet(msg.getValue("Identifier"),
								  msg.getValue("URI"), // key
								  priority, persistence, global,
								  destinationDir, "Fetching", 0,
								  -1, this.queueManager);

			if(this.queueManager.addQueryToTheRunningQueue(clientGet, false))
				this.queueManager.getQueryManager().addObserver(clientGet);
			else
				Logger.info(this, "Already in the running queue");

		}


		if("PersistentPut".equals( msg.getMessageName() )) {
			Logger.info(this, "Resuming from PersistentPut");

			int persistence = 0;

			if(msg.getValue("PersistenceType") != null
			   && msg.getValue("PersistenceType").equals("reboot"))
				persistence = 1;

			boolean global = true;

			if("false".equals( msg.getValue("Global") ))
				global = false;

			int priority = Integer.parseInt(msg.getValue("PriorityClass"));

			long fileSize = 0;

			if(msg.getValue("DataLength") != null)
				fileSize = Long.parseLong(msg.getValue("DataLength"));

			String filePath=null;

			if(msg.getValue("Identifier").startsWith(this.thawId))
				filePath = msg.getValue("ClientToken");

			FCPClientPut clientPut = new FCPClientPut(msg.getValue("Identifier"),
								  msg.getValue("URI"), // key
								  priority, persistence, global,
								  filePath, msg.getValue("TargetFilename"),
								  "Inserting", 0, fileSize,
								  this.queueManager);


			if(this.queueManager.addQueryToTheRunningQueue(clientPut, false))
				this.queueManager.getQueryManager().addObserver(clientPut);
			else
				Logger.info(this, "Already in the running queue");

			return;
		}

		if("EndListPersistentRequests".equals( msg.getMessageName() )) {
			Logger.info(this, "End Of ListPersistentRequests.");
			this.queueManager.getQueryManager().getConnection().removeFromWriterQueue();
			this.queueManager.setQueueCompleted();
			return;
		}
	}
}
