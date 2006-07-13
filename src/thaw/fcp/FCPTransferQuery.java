package thaw.fcp;

import java.util.HashMap;

/**
 * Transfer query == fetch / insert query. These queries must be able to
 * give more informations than the other.
 * Functions returning status of the request may be call frequently, so try to make them fast.
 *
 */
public interface FCPTransferQuery extends FCPQuery {

	/**
	 * Stop the transfer, but don't consider it as failed.
	 * @param queueManager QueueManager gives access to QueryManager;
	 */
	public boolean pause(FCPQueueManager queueManager);


	/**
	 * Only if persistent. Remove it from the queue.
	 */
	public boolean removeRequest();

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
	 * Informal.
	 * Human readable string describring the
	 * status of the query.
	 * @return can be null (== "Waiting")
	 */
	public String getStatus();


	/**
	 * For persistent request only.
	 * @param dir Directory
	 */
	public boolean saveFileTo(String dir);

	/**
	 * Informal.
	 * In pourcents.
	 */
	public int getProgression();

	/**
	 * Informal.
	 * Gives *public* final key only.
	 * @return can be null
	 */
	public String getFileKey();

	/**
	 * Informal. In bytes.
	 * @return can be -1
	 */
	public long getFileSize();

	/**
	 * Where is the file on the disk.
	 */
	public String getPath();

	/**
	 * @return can return -1
	 */
	public int getAttempt();

	public void setAttempt(int x);

	/**
	 * @return can return -1
	 */
	public int getMaxAttempt();

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
	public boolean isGlobal();

	/**
	 * @return can be null (if non active, or meaningless).
	 */
	public String getIdentifier();


}
