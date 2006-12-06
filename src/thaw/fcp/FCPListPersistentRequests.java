package thaw.fcp;

public class FCPListPersistentRequests implements FCPQuery {


	public FCPListPersistentRequests() { }


	public boolean start(final FCPQueueManager queueManager) {
		final FCPMessage newMessage = new FCPMessage();

		newMessage.setMessageName("ListPersistentRequests");

		queueManager.getQueryManager().writeMessage(newMessage);

		return true;
	}

	public boolean stop(final FCPQueueManager queueManager) {
		return true;
	}

	public int getQueryType() {
		return 0;
	}

}
