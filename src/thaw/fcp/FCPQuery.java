package thaw.fcp;

interface FCPQuery {

	public boolean start(FCPQueryManager queryManager);
	public boolean stop(FCPQueryManager queryManager);

	/**
	 * Used by the QueueManager only.
	 * Currently these priority are the same
	 * as FCP priority, but it can change in the
	 * future.
	 * -1 = No priority
	 */
	public int getThawPriority();

}
