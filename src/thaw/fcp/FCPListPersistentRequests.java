package thaw.fcp;

public class FCPListPersistentRequests implements FCPQuery {


	public FCPListPersistentRequests() { }


	public boolean start(FCPQueueManager queueManager) {
		FCPMessage newMessage = new FCPMessage();
		
		newMessage.setMessageName("ListPersistentRequests");

		queueManager.getQueryManager().writeMessage(newMessage);

		return true;
	}

	public boolean stop(FCPQueueManager queueManager) {

		return false;
	}

	public int getQueryType() {
		return 0;
	}
	


}
