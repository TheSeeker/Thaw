package thaw.fcp;

import java.util.HashMap;

/**
 * This interface was designed for file query (insertions / downloads)
 * but it's used sometimes for other things.
 * TODO : Simplify this interface and create an interface FCPTransferQuery extending this one.
 */
public interface FCPQuery {

	/**
	 * @param queueManager QueueManager gives access to QueryManager.
	 */
	public boolean start(FCPQueueManager queueManager);

	/**
	 * @param queueManger QueueManager gives access to QueryManager.
	 */
	public boolean stop(FCPQueueManager queueManager);

	/**
	 * Used by the QueueManager only.
	 * Currently these priority are the same
	 * as FCP priority, but it can change in the
	 * future.
	 * -1 = No priority
	 * Always between -1 and 6.
	 */
	public int getThawPriority();

	/**
	 * Tell if the query is a download query or an upload query.
	 * If >= 1 then *must* be Observable.
	 * @return 0 : Meaningless ; 1 : Download ; 2 : Upload
	 */
	public int getQueryType();
	

	/**
	 * Informal.
	 * Human readable string describring the
	 * status of the query.
	 * @return can be null (== "Waiting")
	 */
	public String getStatus();

	/**
	 * Informal.
	 * In pourcents.
	 */
	public int getProgression();

	/**
	 * Informal.
	 * @return can be null
	 */
	public String getFileKey();

	/**
	 * Informal. In bytes.
	 * @return can be -1
	 */
	public long getFileSize();

	/**
	 * Informal.
	 * @return can return null
	 */
	public String getPath();

	/**
	 * Informal.
	 * @return can return -1
	 */
	public int getAttempt();

	public boolean isRunning();

	public boolean isFinished();

	/**
	 * If unknow, return false.
	 * Query is considered as a failure is isFinished() && !isSuccesful()
	 */
	public boolean isSuccessful();

	/**
	 * Use to save the query in an XML file / a database / whatever.
	 * @return A HashMap : String (parameter name) -> String (parameter value) or null.
	 */
	public HashMap getParameters();

	/**
	 * Opposite of getParameters().
	 * @return true if successful (or ignored) ; false if not.
	 */
	public boolean setParameters(HashMap parameters);


	public boolean isPersistent();

	/**
	 * @return can be null (if non active, or meaningless).
	 */
	public String getIdentifier();
}
