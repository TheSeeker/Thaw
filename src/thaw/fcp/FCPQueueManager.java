package thaw.fcp;

import thaw.core.Logger;


public class FCPQueueManager {

	private FCPQueryManager queryManager;
	private int maxDownloads, maxInsertions;


	/**
	 * Calls setQueryManager() and then resetQueue().
	 */
	public FCPQueueManager(FCPQueryManager queryManager,
			       int maxDownloads, int maxInsertions) {
		setMaxDownloads(maxDownloads);
		setMaxInsertions(maxInsertions);

		setQueryManager(queryManager);
		resetQueue();
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
		/* TODO */
	}
	

}

