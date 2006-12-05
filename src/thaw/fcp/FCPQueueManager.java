package thaw.fcp;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.Logger;

/**
 * Manage a running and a pending queue of FCPTransferQuery.
 * Please notice that runningQueue contains too finished queries.
 * Notify when: a query is added and when a query change to one queue to another.
 */
public class FCPQueueManager extends java.util.Observable implements Runnable, java.util.Observer {

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

	private boolean queueCompleted;

	/**
	 * Calls setQueryManager() and then resetQueues().
	 */
	public FCPQueueManager(FCPQueryManager queryManager,
			       String thawId,
			       int maxDownloads, int maxInsertions) {
		this.lastId = 0;
		this.queueCompleted = false;

		this.setThawId(thawId);
		this.setMaxDownloads(maxDownloads);
		this.setMaxInsertions(maxInsertions);

		this.setQueryManager(queryManager);
		this.resetQueues();

		queryManager.getConnection().addObserver(this);
	}

	public boolean isQueueCompletlyLoaded() {
		return this.queueCompleted;
	}

	public void setQueueCompleted() {
		this.queueCompleted = true;
	}

	/**
	 * Use it if you want to bypass the queue.
	 */
	public FCPQueryManager getQueryManager() {
		return this.queryManager;
	}

	public void setThawId(String thawId) {
		this.thawId = thawId;
	}

	public void setMaxDownloads(int maxDownloads) {
		this.maxDownloads = maxDownloads;
	}

	public void setMaxInsertions(int maxInsertions) {
		this.maxInsertions = maxInsertions;
	}

	/**
	 * You should call resetQueues() after calling this function.
	 */
	public void setQueryManager(FCPQueryManager queryManager) {
		this.queryManager = queryManager;
	}


	/**
	 * Will purge the current known queue.
	 */
	public void resetQueues() {
		this.runningQueries = new Vector();

		for(int i = 0; i <= PRIORITY_MIN ; i++)
			this.pendingQueries[i] = new Vector();
	}

	/**
	 * Take care: Can change while you're using it.
	 */
	public Vector[] getPendingQueues() {
		return this.pendingQueries;
	}

	/**
	 * Take care: Can change while you're using it.
	 * The running queue contains running request, but also finished/failed ones.
	 */
	public Vector getRunningQueue() {
		return this.runningQueries;
	}

	/**
	 * @return < 0 if no limit
	 */
	public int getMaxDownloads() {
		return this.maxDownloads;
	}

	/**
	 * @return < 0 if no limite
	 */
	public int getMaxInsertions() {
		return this.maxInsertions;
	}

	/**
	 * @return false if already added.
	 */
	public boolean addQueryToThePendingQueue(FCPTransferQuery query) {
		if(query.getThawPriority() < 0) {
			return this.addQueryToTheRunningQueue(query);
		}

		if(this.isAlreadyPresent(query)) {
			Logger.notice(this, "Key was already in one of the queues : "+query.getFilename());
			return false;
		}

		Logger.notice(this, "Adding query to the pending queue ...");

		this.pendingQueries[query.getThawPriority()].add(query);

		this.setChanged();
		this.notifyObservers(query);

		Logger.notice(this, "Adding done");
		return true;
	}

	/**
	 * will call start() function of the query.
	 * @return false if already added
	 */
	public boolean addQueryToTheRunningQueue(FCPTransferQuery query) {
		return this.addQueryToTheRunningQueue(query, true);
	}

	public boolean addQueryToTheRunningQueue(FCPTransferQuery query, boolean callStart) {
		Logger.debug(this, "Adding query to the running queue ...");

		if(this.isAlreadyPresent(query)) {
			Logger.notice(this, "Key was already in one of the queues");
			return false;
		}

		if(!callStart && query.getIdentifier().startsWith(this.thawId)) {
			/* It's a resumed query => We to adapt the next Id
			 * to avoid collisions.
			 */

			/* FIXME (not urgent) : Find a cleaner / safer way. */
			try {
				String[] subId = query.getIdentifier().split("-");
				subId = subId[0].split("_");
				int id = Integer.parseInt(subId[subId.length-1]);

				if(id > this.lastId) {
					this.lastId = id;
				}
			} catch(Exception e) {
				Logger.notice(this, "Exception while parsing previous Id (Not really a problem)");
			}
		}

		this.runningQueries.add(query);

		this.setChanged();
		this.notifyObservers(query);

		if(callStart)
			query.start(this);

		Logger.debug(this, "Adding done");

		return true;
	}



	/**
	 * *Doesn't* call stop() from the query.
	 */
	public void moveFromRunningToPendingQueue(FCPTransferQuery query) {
		this.remove(query);
		this.addQueryToThePendingQueue(query);
	}


	/**
	 * Restart non-persistent and non-finished queries being in the runninQueue.
	 * Usefull to restart these query when thaw just start.
	 */
	public  void restartNonPersistent() {
		Logger.info(this, "Restarting non persistent query");

		for(Iterator queryIt = this.getRunningQueue().iterator() ;
		    queryIt.hasNext();) {
			FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

			if(!query.isPersistent() && !query.isFinished())
				query.start(this);
		}

		Logger.info(this, "Restart done.");
	}

	public void remove(FCPTransferQuery query) {
		this.runningQueries.remove(query);

		for(int i = 0 ; i <= PRIORITY_MIN ; i++)
			this.pendingQueries[i].remove(query);

		this.setChanged();
		this.notifyObservers(query);
	}


	private boolean isTheSame(FCPTransferQuery queryA,
				  FCPTransferQuery queryB) {
		if(queryA.getQueryType() != queryB.getQueryType())
			return false;

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
	 * Compare only the refs.
	 */
	public boolean isInTheQueues(FCPTransferQuery query) {
		if(this.runningQueries.contains(query))
			return true;

		for(int i = 0 ; i < this.pendingQueries.length ; i++) {
			if(this.pendingQueries[i].contains(query))
				return true;
		}

		return false;
	}


	/**
	 * Not very reliable ?
	 */
	public FCPTransferQuery getTransfer(String key) {
		boolean interrupted=true;
		boolean isAKey = true;
		Iterator it;

		if (key == null)
			return null;

		if (key.startsWith("SSK@") || key.startsWith("USK@")
		    || key.startsWith("KSK@") || key.startsWith("CHK@"))
			isAKey = true;
		else
			isAKey = false;

		while(interrupted) {
			interrupted = false;

			try {
				for(it = this.runningQueries.iterator();
				    it.hasNext(); )
					{
						FCPTransferQuery plop = (FCPTransferQuery)it.next();
						if (isAKey) {
							if (plop.getFileKey() == key
							    || key.equals(plop.getFileKey()))
								return plop;
						} else {
							if (plop.getFilename() == key
							    || key.equals(plop.getFilename()))
								return plop;
						}
					}

				for(int i = 0 ; i <= PRIORITY_MIN ; i++) {
					for(it = this.pendingQueries[i].iterator();
					    it.hasNext(); )
						{
							FCPTransferQuery plop = (FCPTransferQuery)it.next();
							if (isAKey) {
								if (plop.getFileKey() == key
								    || key.equals(plop.getFileKey()))
									return plop;
							} else {
								if (plop.getFilename() == key
								    || key.equals(plop.getFilename()))
									return plop;
							}
						}

				}
			} catch(java.util.ConcurrentModificationException e) {
				Logger.notice(this, "getTransfer(): Collission. Reitering");
				interrupted = true;
			}

		}

		return null;
	}


	/**
	 * Not reliable
	 */
	public FCPTransferQuery getTransferByFilename(String name) {
		boolean interrupted=true;

		Iterator it;

		if (name == null)
			return null;

		while(interrupted) {
			interrupted = false;

			try {
				for(it = this.runningQueries.iterator();
				    it.hasNext(); )
					{
						FCPTransferQuery plop = (FCPTransferQuery)it.next();

						if (plop.getFilename() == name
						    || name.equals(plop.getFilename()))
							return plop;
					}

				for(int i = 0 ; i <= PRIORITY_MIN ; i++) {
					for(it = this.pendingQueries[i].iterator();
					    it.hasNext(); )
						{
							FCPTransferQuery plop = (FCPTransferQuery)it.next();

							if (plop.getFilename() == name
							    || name.equals(plop.getFilename()))
								return plop;
						}

				}
			} catch(java.util.ConcurrentModificationException e) {
				Logger.notice(this, "getTransferByFilename(): Collission. Reitering");
				interrupted = true;
			}

		}

		return null;
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
				for(it = this.runningQueries.iterator();
				    it.hasNext(); )
					{
						FCPTransferQuery plop = (FCPTransferQuery)it.next();
						if(this.isTheSame(plop, query))
							return true;
					}

				for(int i = 0 ; i <= PRIORITY_MIN ; i++) {
					for(it = this.pendingQueries[i].iterator();
					    it.hasNext(); )
						{
							FCPTransferQuery plop = (FCPTransferQuery)it.next();
							if(this.isTheSame(plop, query))
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


	public void schedule() {
			/* We count the running query to see if there is an empty slot */

			int runningInsertions = 0;
			int runningDownloads = 0;

			for(Iterator it = this.runningQueries.iterator(); it.hasNext(); ) {
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
				    && ( (this.maxInsertions <= -1 || runningInsertions < this.maxInsertions)
					|| (this.maxDownloads <= -1 || runningDownloads < this.maxDownloads) ) ;
			    priority++)	{

				try {
					for(Iterator it = this.pendingQueries[priority].iterator();
					    it.hasNext()
						    && ( (this.maxInsertions <= -1 || runningInsertions < this.maxInsertions)
							|| (this.maxDownloads <= -1 || runningDownloads < this.maxDownloads) ); ) {

						FCPTransferQuery query = (FCPTransferQuery)it.next();

						if( (query.getQueryType() == 1
						     && (this.maxDownloads <= -1 || runningDownloads < this.maxDownloads) )
						    || (query.getQueryType() == 2
							&& (this.maxInsertions <= -1 || runningInsertions < this.maxInsertions)) ) {

							Logger.debug(this, "Scheduler : Moving a query from pendingQueue to the runningQueue");
							this.pendingQueries[priority].remove(query);

							it = this.pendingQueries[priority].iterator(); /* We reset iterator */

							this.addQueryToTheRunningQueue(query);

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

			if(this.stopThread)
				return;

			try {
				if(queryManager.getConnection().isConnected()
				   && !queryManager.getConnection().isWriting()
				   && queueCompleted) {

					this.schedule();

				}
			} catch(java.util.ConcurrentModificationException e) {
				Logger.notice(this, "Ordonnancor: Collision !");
			} catch(Exception e) {
				Logger.error(this, "EXCEPTION FROM ORDONNANCOR : "+e.toString());
				Logger.error(this, "ERROR : "+e.getMessage());
				e.printStackTrace();
			}
		}

	}

	public void startScheduler() {
		this.scheduler = new Thread(this);
		this.stopThread = false;
		this.scheduler.start();
	}

	public void stopScheduler() {
		this.stopThread = true;
	}


	public String getAnID() {
		this.lastId++;

		if(this.lastId >= 65535) {
			this.lastId = 0;
		}

		return (this.thawId+"_"+ Integer.toString(this.lastId));
	}

	public void update(java.util.Observable o, Object arg) {
		if(o == this.queryManager.getConnection()
		   && !this.queryManager.getConnection().isConnected()) {

			/* Only the running queue ...
			 * pending query are specifics to Thaw
			 */
			this.runningQueries = new Vector();

			this.setChanged();
			this.notifyObservers();
		}
	}
}

