package thaw.fcp;


/**
 * This interface was designed for file query (insertions / downloads)
 * but it's used sometimes for other things.
 */
public interface FCPQuery {

	/**
	 * @param queueManager QueueManager gives access to QueryManager.
	 */
	public boolean start(FCPQueueManager queueManager);

	/**
	 * Definitive stop. Transfer is considered as failed.
	 * @return false if really it *cannot* stop the query.
	 * @param queueManger QueueManager gives access to QueryManager.
	 */
	public boolean stop(FCPQueueManager queueManager);

	/**
	 * Tell if the query is a download query or an upload query.
	 * If >= 1 then *must* be Observable and implements FCPTransfertQuery.
	 * @return 0 : Meaningless ; 1 : Download ; 2 : Upload ; >= 2 : ?
	 */
	public int getQueryType();
	
}
