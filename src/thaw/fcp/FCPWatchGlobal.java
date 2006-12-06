package thaw.fcp;

public class FCPWatchGlobal implements FCPQuery {
	private boolean watch;


	public FCPWatchGlobal(final boolean v) {
		watch = v;
	}

	public boolean start(final FCPQueueManager queueManager) {
		final FCPMessage message = new FCPMessage();

		message.setMessageName("WatchGlobal");

		if(watch)
			message.setValue("Enabled", "true");
		else
			message.setValue("Enabled", "false");

		message.setValue("VerbosityMask", "1");

		queueManager.getQueryManager().writeMessage(message);

		return true;
	}

	public boolean stop(final FCPQueueManager queueManager) {
		return true;
	}

	public int getQueryType() {
		return 0;
	}

}
