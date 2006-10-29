package thaw.fcp;

import java.util.HashMap;

/**
 * Transfer query == fetch / insert query. These queries must be able to
 * give more informations than the other.
 * Functions returning status of the request may be call frequently, so try to make them fast.
 * Some methods are only useful for downloads, and some for insertions, so check getQueryType() before calling them.
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
	 * Currently the same than Thaw priority.
	 */
	public int getFCPPriority();

	/**
	 * call updatePersistentRequest() after to apply the change (Please note that the change
	 * will be visible even if you don't call it).
	 */
	public void setFCPPriority(int prio);

	/**
	 * you can call it after saveFileTo() to update the clientToken.
	 * @param clientToken tell if the clientToken must be updated or just the priority
	 */
	public void updatePersistentRequest(boolean clientToken);

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
	 * Is about the transfer on the network.
	 * In pourcents.
	 */
	public int getProgression();

	public boolean isProgressionReliable();

	/**
	 * Is about the transfer between the node and thaw.
	 */
	public int getTransferWithTheNodeProgression();


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
	public boolean isFatallyFailed();

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

	public String getFilename();
}
