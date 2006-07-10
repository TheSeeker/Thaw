package thaw.fcp;

import java.util.HashMap;

public class FCPWatchGlobal implements FCPQuery {
	private boolean watch;


	public FCPWatchGlobal(boolean v) {
		watch = v;
	}

	public boolean start(FCPQueueManager queueManager) {
		FCPMessage message = new FCPMessage();

		message.setMessageName("WatchGlobal");

		if(watch)
			message.setValue("Enabled", "true");
		else
			message.setValue("Enabled", "false");

		message.setValue("VerbosityMask", "1");

		queueManager.getQueryManager().writeMessage(message);

		return true;
	}

	public boolean stop(FCPQueueManager queueManager) {
		return true;
	}

	public int getThawPriority() {
		return -1;
	}

	public int getQueryType() {
		return 0;
	}
	
	public String getStatus() {
		return null;
	}

	public int getProgression() {
		return 100;
	}

	public String getFileKey() {
		return null;
	}

	public long getFileSize() {
		return 0;
	}

	public String getPath() {
		return null;
	}

	public int getAttempt() {
		return 0;
	}

	public boolean isFinished() {
		return true;
	}

	public boolean isSuccessful() {
		return true;
	}

	public boolean isRunning() {
		return false;
	}

	public HashMap getParameters() {
		return null;
	}

	public boolean setParameters(HashMap parameters) {
		return true;
	}


	public boolean isPersistent() {
		return false;
	}

	public String getIdentifier() {
		return null;
	}
}
