package thaw.fcp;


public class FCPRemovePeer implements FCPQuery {
	private String name;

	/**
	 * Ref can be a real ref, or URL=http://where.to-get-the-ref-on-the.net/
	 */
	public FCPRemovePeer(String name) {
		this.name = name;
	}


	public boolean start(FCPQueueManager queueManager) {
		FCPMessage msg = new FCPMessage();

		msg.setMessageName("RemovePeer");

		msg.setValue("NodeIdentifier", name);

		return queueManager.getQueryManager().writeMessage(msg);
	}


	public boolean stop(FCPQueueManager queueManager) {
		/* can't stop */
		return false;
	}


	public int getQueryType() {
		return 0;
	}
}
