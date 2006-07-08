package thaw.fcp;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.Logger;


public class FCPQueueManager extends java.util.Observable implements Runnable {

	private final static int PRIORITY_MIN = 6; /* So 0 to 6 */

	private FCPQueryManager queryManager;
	private int maxDownloads, maxInsertions;

	/* offset in the array == priority */
	/* Vector contains FCPQuery */
	private Vector[] pendingQueries = new Vector[PRIORITY_MIN+1];
	private Vector runningQueries;

	private Thread scheduler;

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
	 * Will purge the current known queue, and reload the queue according to the node.
	 * Assume you have already called FCPConnection.connect().
	 */
	public void resetQueue() {
		runningQueries = new Vector();
		for(int i = 0; i <= PRIORITY_MIN ; i++)
			pendingQueries[i] = new Vector();
		
		/* TODO */
	}


	public Vector[] getPendingQueues() {
		return pendingQueries;
	}

	public Vector getRunningQueue() {
		return runningQueries;
	}

	public void addQueryToThePendingQueue(FCPQuery query) {
		if(query.getThawPriority() < 0) {
			addQueryToTheRunningQueue(query);
			return;
		}

		if(isAlreadyPresent(query)) {
			Logger.notice(this, "Key was already in one of the queues");
			return;
		}

		Logger.debug(this, "Adding query to the pending queue ...");
		
		pendingQueries[query.getThawPriority()].add(query);

		setChanged();
		notifyObservers(query);

		Logger.debug(this, "Adding done");
	}


	public void addQueryToTheRunningQueue(FCPQuery query) {
		Logger.debug(this, "Adding query to the running queue ...");

		runningQueries.add(query);

		setChanged();
		notifyObservers(query);

		query.start(this);
		
		Logger.debug(this, "Adding done");
	}

	
	public void remove(FCPQuery query) {
		runningQueries.remove(query);

		for(int i = 0 ; i <= PRIORITY_MIN ; i++)
			pendingQueries[i].remove(query);

		setChanged();
		notifyObservers(query);
	}

	/**
	 * Compare using the key.
	 */
	public boolean isAlreadyPresent(FCPQuery query) {
		Iterator it;

		for(it = runningQueries.iterator();
		    it.hasNext(); )
			{
				FCPQuery plop = (FCPQuery)it.next();
				if(plop.getFileKey().equals(query.getFileKey()))
					return true;
			}

		for(int i = 0 ; i <= PRIORITY_MIN ; i++) {
			for(it = pendingQueries[i].iterator();
			    it.hasNext(); )
				{
					FCPQuery plop = (FCPQuery)it.next();
					if(plop.getFileKey().equals(query.getFileKey()))
						return true;
				}
		}

		return false;
	}


	public synchronized void ordonnance() {
		
			/* We count the running query to see if there is an empty slot */

			int runningInsertions = 0;
			int runningDownloads = 0;

			for(Iterator it = runningQueries.iterator(); it.hasNext(); ) {
				FCPQuery query = (FCPQuery)it.next();

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
				    && (runningInsertions < maxInsertions
					|| runningDownloads < maxDownloads) ;
			    priority++)	{

				for(Iterator it = pendingQueries[priority].iterator();
				    it.hasNext()
					    && (runningInsertions < maxInsertions
						|| runningDownloads < maxDownloads); ) {
					
					FCPQuery query = (FCPQuery)it.next();
					
					if( (query.getQueryType() == 1
					     && runningDownloads < maxDownloads)
					    || (query.getQueryType() == 2
						&& runningInsertions < maxInsertions) ) {
						
						Logger.debug(this, "Scheduler : Moving a query from pendingQueue to the runningQueue");
						pendingQueries[priority].remove(query);
						
						it = pendingQueries[priority].iterator(); /* We reset iterator */

						addQueryToTheRunningQueue(query);

						if(query.getQueryType() == 1)
							runningDownloads++;

						if(query.getQueryType() == 2)
							runningInsertions++;
					}

					

				}

			}

	}


	public void run() {

		while(true) {
			try {
				Thread.sleep(500);
			} catch(java.lang.InterruptedException e) {
				/* We don't care */
			}

			ordonnance();
		}

	}

	public void startScheduler() {
		scheduler = new Thread(this);
		scheduler.start();
	}

	public void stopScheduler() {
		scheduler.stop(); /* I should find a safer way */
	}


	public String getAnID() {
		lastId++;

		if(lastId >= 65535) {
			lastId = 0;
		}

		return (thawId+"-"+(new Integer(lastId)).toString());
	}

}

