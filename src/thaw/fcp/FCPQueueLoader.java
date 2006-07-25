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
			queueManager.getQueryManager().getConnection().lockWriting();

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

		if(msg.getMessageName().equals("PersistentGet")) {
			Logger.info(this, "Resuming from PersistentGet");
			
			int persistence = 0;

			if(msg.getValue("PersistenceType").equals("reboot"))
				persistence = 1;

			boolean global = true;

			if(msg.getValue("Global").equals("false"))
				global = false;

			String destinationDir = null;

			if(msg.getValue("Identifier").startsWith(thawId))
				destinationDir = msg.getValue("ClientToken");

			int priority = Integer.parseInt(msg.getValue("PriorityClass"));


			FCPClientGet clientGet = new FCPClientGet(msg.getValue("Identifier"),
								  msg.getValue("URI"), // key
								  priority, persistence, global,
								  destinationDir, "Fetching", 0,
								  queueManager);
								  
								  
			if(queueManager.addQueryToTheRunningQueue(clientGet, false))
				queueManager.getQueryManager().addObserver(clientGet);
			else
				Logger.info(this, "Already in the running queue");

		}


		if(msg.getMessageName().equals("PersistentPut")) {
			Logger.info(this, "Resuming from PersistentPut");
			
			int persistence = 2;

			/* TOFIX : Node doesn't return PersistenceType */
			/*
			if(msg.getValue("PersistenceType").equals("reboot"))
				persistence = 1;
			*/

			boolean global = true;

			if(msg.getValue("Global").equals("false"))
				global = false;

			int priority = Integer.parseInt(msg.getValue("PriorityClass"));

			long fileSize = 0;

			if(msg.getValue("DataLength") != null)
				fileSize = Long.parseLong(msg.getValue("DataLength"));

			String filePath=null;

			if(msg.getValue("Identifier").startsWith(thawId))
				filePath = msg.getValue("ClientToken");

			FCPClientPut clientPut = new FCPClientPut(msg.getValue("Identifier"),
								  msg.getValue("URI"), // key
								  priority, persistence, global,
								  filePath,
								  "Inserting", 0, fileSize,
								  queueManager);
								  
								  
			if(queueManager.addQueryToTheRunningQueue(clientPut, false))
				queueManager.getQueryManager().addObserver(clientPut);
			else
				Logger.info(this, "Already in the running queue");
			

			return;
		}

		if(msg.getMessageName().equals("EndListPersistentRequests")) {
			Logger.info(this, "End Of ListPersistentRequests.");
			queueManager.getQueryManager().getConnection().unlockWriting();

			return;
		}
	}
}
