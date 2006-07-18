package thaw.fcp;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.Logger;

/**
 * Manage a running and a pending queue of FCPTransferQuery.
 * Please notice that runningQueue contains too finished queries.
 */
public class FCPQueueManager extends java.util.Observable implements Runnable {

	private final static int PRIORITY_MIN = 6; /* So 0 to 6 */

	private FCPQueryManager queryManager;
	private int maxDownloads, maxInsertions;

	/* offset in the array == priority */
	/* Vector contains FCPQuery */
	private Vector[] pendingQueries = new Vector[PRIORITY_MIN+1];
	private Vector runningQueries;

	private Thread scheduler;
	private boolean stopThread = false;

	private int lastId;
	private String thawId;


	/**
	 * Calls setQueryManager() and then resetQueue().
	 */
	public FCPQueueManager(FCPQueryManager queryManager,
			       String thawId,
			       int maxDownloads, int maxInsertions) {
		lastId = 0;
		this.thawId = thawId;
		setMaxDownloads(maxDownloads);
		setMaxInsertions(maxInsertions);

		setQueryManager(queryManager);
		resetQueue();
	}

	/**
	 * Use it if you want to bypass the queue.
	 */
	public FCPQueryManager getQueryManager() {
		return queryManager;
	}

	public void setMaxDownloads(int maxDownloads) {
		this.maxDownloads = maxDownloads;
	}

	public void setMaxInsertions(int maxInsertions) {
		this.maxInsertions = maxInsertions;
	}

	/**
	 * You should call resetQueue() after calling this function.
	 */
	public void setQueryManager(FCPQueryManager queryManager) {
		this.queryManager = queryManager;
	}


	/**
	 * Will purge the current known queue.
	 */
	public void resetQueue() {
		runningQueries = new Vector();
		for(int i = 0; i <= PRIORITY_MIN ; i++)
			pendingQueries[i] = new Vector();
	}

	/**
	 * Take care: Can change while you're using it.
	 */
	public Vector[] getPendingQueues() {
		return pendingQueries;
	}

	/**
	 * Take care: Can change while you're using it.
	 */
	public Vector getRunningQueue() {
		return runningQueries;
	}

	/**
	 * @return false if already added.
	 */
	public boolean addQueryToThePendingQueue(FCPTransferQuery query) {
		if(query.getThawPriority() < 0) {
			return addQueryToTheRunningQueue(query);
		}

		if(isAlreadyPresent(query)) {
			Logger.notice(this, "Key was already in one of the queues : "+query.getFilename());
			return false;
		}

		Logger.debug(this, "Adding query to the pending queue ...");
		
		pendingQueries[query.getThawPriority()].add(query);

		setChanged();
		notifyObservers(query);

		Logger.debug(this, "Adding done");
		return true;
	}

	/**
	 * will call start() function of the query.
	 * @return false if already added
	 */
	public boolean addQueryToTheRunningQueue(FCPTransferQuery query) {
		return addQueryToTheRunningQueue(query, true);
	}

	public boolean addQueryToTheRunningQueue(FCPTransferQuery query, boolean callStart) {
		Logger.debug(this, "Adding query to the running queue ...");

		if(isAlreadyPresent(query)) {
			Logger.notice(this, "Key was already in one of the queues");
			return false;
		}

		if(!callStart && query.getIdentifier().startsWith(thawId)) {
			/* It's a resumed query => We to adapt the next Id 
			 * to avoid collisions.
			 */

			/* FIXME (not urgent) : Find a cleaner / safer way. */
			try {
				String[] subId = query.getIdentifier().split("-");
				subId = subId[0].split("_");
				int id = ((new Integer(subId[subId.length-1])).intValue());
				
				if(id > lastId) {
					lastId = id;
				}
			} catch(Exception e) {
				Logger.notice(this, "Exception while parsing previous Id. Not really a problem");
			}
		}

		runningQueries.add(query);

		setChanged();
		notifyObservers(query);

		if(callStart)
			query.start(this);
		
		Logger.debug(this, "Adding done");

		return true;
	}


	
	/**
	 * *Doesn't* call stop() from the query.
	 */
	public void moveFromRunningToPendingQueue(FCPTransferQuery query) {
		remove(query);
		addQueryToThePendingQueue(query);
	}


	/**
	 * Restart non-persistent and non-finished queries being in the runninQueue.
	 * Usefull to restart these query when thaw just start.
	 */
	public  void restartNonPersistent() {
		Logger.info(this, "Restarting non persistent query");

		for(Iterator queryIt = getRunningQueue().iterator() ;
		    queryIt.hasNext();) {
			FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

			if(!query.isPersistent() && !query.isFinished())
				query.start(this);
		}

		Logger.info(this, "Restart done.");
		    
	}
	
	public void remove(FCPTransferQuery query) {
		runningQueries.remove(query);

		for(int i = 0 ; i <= PRIORITY_MIN ; i++)
			pendingQueries[i].remove(query);

		setChanged();
		notifyObservers(query);
	}

	
	private boolean isTheSame(FCPTransferQuery queryA,
				  FCPTransferQuery queryB) {
		if(queryA.getIdentifier() != null && queryB.getIdentifier() != null) {
			if(queryA.getIdentifier().equals(queryB.getIdentifier())) {
				Logger.debug(this, "isTheSame(): Identifier");
				return true;
			}
			return false;
		}

		if(queryA.getFileKey() != null && queryB.getFileKey() != null) {
			if(queryA.getFileKey().equals(queryB.getFileKey())) {
				Logger.debug(this, "isTheSame(): FileKey");
				return true;
			}
			return false;
		}
		
		if(queryA.getFilename().equals(queryB.getFilename())) {
			Logger.debug(this, "isTheSame(): Filename");
			return true;
		}
		return false;
	}


	/**
	 * Compare using the key.
	 */
	public boolean isAlreadyPresent(FCPTransferQuery query) {
		boolean interrupted=true;

		Iterator it;

		while(interrupted) {
			interrupted = false;

			try {
				for(it = runningQueries.iterator();
				    it.hasNext(); )
					{
						FCPTransferQuery plop = (FCPTransferQuery)it.next();
						if(isTheSame(plop, query))
							return true;
					}
				
				for(int i = 0 ; i <= PRIORITY_MIN ; i++) {
					for(it = pendingQueries[i].iterator();
					    it.hasNext(); )
						{
							FCPTransferQuery plop = (FCPTransferQuery)it.next();
							if(isTheSame(plop, query))
								return true;
						}
					
				}
			} catch(java.util.ConcurrentModificationException e) {
				Logger.notice(this, "isAlreadyPresent(): Collission. Reitering");
				interrupted = true;
			}

		}


		return false;
	}


	public void ordonnance() {
		
			/* We count the running query to see if there is an empty slot */

			int runningInsertions = 0;
			int runningDownloads = 0;

			for(Iterator it = runningQueries.iterator(); it.hasNext(); ) {
				FCPTransferQuery query = (FCPTransferQuery)it.next();

				if(query.getQueryType() == 1 /* Download */
				   && !query.isFinished())
					runningDownloads++;

				if(query.getQueryType() == 2 /* Insertion */
				   && !query.isFinished())
					runningInsertions++;
			}


			/* We move queries from the pendingQueue to the runningQueue until we got our quota */
			for(int priority = 0;
			    priority <= PRIORITY_MIN
				    && ( (maxInsertions <= -1 || runningInsertions < maxInsertions)
					|| (maxDownloads <= -1 || runningDownloads < maxDownloads) ) ;
			    priority++)	{

				try {
					for(Iterator it = pendingQueries[priority].iterator();
					    it.hasNext()
						    && ( (maxInsertions <= -1 || runningInsertions < maxInsertions)
							|| (maxDownloads <= -1 || runningDownloads < maxDownloads) ); ) {
						
						FCPTransferQuery query = (FCPTransferQuery)it.next();
						
						if( (query.getQueryType() == 1
						     && (maxDownloads <= -1 || runningDownloads < maxDownloads) )
						    || (query.getQueryType() == 2
							&& (maxInsertions <= -1 || runningInsertions < maxInsertions)) ) {
							
							Logger.debug(this, "Scheduler : Moving a query from pendingQueue to the runningQueue");
							pendingQueries[priority].remove(query);
							
							it = pendingQueries[priority].iterator(); /* We reset iterator */
							
							addQueryToTheRunningQueue(query);
							
							if(query.getQueryType() == 1)
								runningDownloads++;
							
							if(query.getQueryType() == 2)
								runningInsertions++;

							try {
								Thread.sleep(300);
							} catch(java.lang.InterruptedException e) { }
						}

						
						
					}
				} catch(java.util.ConcurrentModificationException e) {
					Logger.notice(this, "Collision.");
					priority--;
				}

			}

	}


	public void run() {
		try {
			Thread.sleep(5000);
		} catch(java.lang.InterruptedException e) {
			// I'm stupid. I'm stupid. I'm stupid. (r9654)
		}

		while(true) {
			try {
				Thread.sleep(500);
			} catch(java.lang.InterruptedException e) {
				/* We don't care */
			}

			if(stopThread)
				return;

			ordonnance();
		}

	}

	public void startScheduler() {
		scheduler = new Thread(this);
		stopThread = false;
		scheduler.start();
	}

	public void stopScheduler() {
		stopThread = true;
	}


	public String getAnID() {
		lastId++;

		if(lastId >= 65535) {
			lastId = 0;
		}

		return (thawId+"_"+(new Integer(lastId)).toString());
	}

}

