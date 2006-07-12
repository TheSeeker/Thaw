package thaw.fcp;

import java.util.HashMap;

/**
 * TODO
 */
public class FCPClientPut implements FCPTransferQuery {

	/**
	 * To resume query from file. (see core.QueueKeeper)
	 */
	public FCPClientPut(FCPQueueManager queueManager, HashMap parameters) {

	}


	public boolean start(FCPQueueManager queueManager) {
		return false;
	}

	public boolean stop(FCPQueueManager queueManager) {
		return false;
	}

	public int getQueryType() {
		return 2;
	}

	public boolean pause(FCPQueueManager queueManager) {
		return false;
	}


	public boolean removeRequest() {
		return false;
	}

	public int getThawPriority() {
		return -1;
	}
	
	public String getStatus() {
		return "Tulip";
	}

	public int getProgression() {
		return 0;
	}

	public String getFileKey() {
		return "Tulip";
	}

	public long getFileSize() {
		return 0;
	}

	public String getPath() {
		return "Tulip";
	}

	public int getAttempt() {
		return 0;
	}

	public void setAttempt(int x) {
		return;
	}

	public int getMaxAttempt() {
		return 0;
	}

	public boolean isRunning() {
		return false;
	}

	public boolean isFinished() {
		return false;
	}

	public boolean isSuccessful() {
		return false;
	}

	public HashMap getParameters() {
		return null;
	}

	public boolean setParameters(HashMap parameters) {
		return false;
	}


	public boolean isPersistent() {
		return true;
	}

	public boolean isGlobal() {
		return true;
	}

	public String getIdentifier() {
		return "Tulip";
	}

	/**
	 * Copy simply the file ... a little bit useless ...
	 */
	public boolean saveFileTo(String dir) {
		return false;
	}
}
