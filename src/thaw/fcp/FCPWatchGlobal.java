package thaw.fcp;

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

	public int getQueryType() {
		return 0;
	}
	
}
