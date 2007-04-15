package thaw.fcp;


public class FCPAddPeer implements FCPQuery {
	private String ref;

	/**
	 * Ref can be a real ref, or URL=http://where.to-get-the-ref-on-the.net/
	 */
	public FCPAddPeer(String ref) {
		this.ref = ref;
	}


	public boolean start(FCPQueueManager queueManager) {
		FCPMessage msg = new FCPMessage();

		msg.setMessageName("AddPeer");

		String[] lines = ref.split("\n");

		for (int i = 0 ; i < lines.length ; i++) {
			String[] elements = lines[i].split("=");

			if (elements.length < 2) /* may happen for the word 'end' at the end of the ref */
				continue;

			String optName = elements[0];
			String optValue = "";

			for (int j = 1; j < elements.length ; j++)
				optValue += elements[j];

			msg.setValue(optName, optValue);
		}

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
