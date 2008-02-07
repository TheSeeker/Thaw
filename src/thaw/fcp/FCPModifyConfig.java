package thaw.fcp;

public class FCPModifyConfig implements FCPQuery {
	private String name, value;
	
	public FCPModifyConfig(String name, String newValue) {
		this.name = name;
		this.value = newValue;
	}

	public int getQueryType() {
		return 0;
	}

	public boolean start(FCPQueueManager queueManager) {
		FCPMessage msg = new FCPMessage();
		msg.setMessageName("ModifyConfig");
		msg.setValue(name, value);
		
		queueManager.getQueryManager().writeMessage(msg);
		
		return true;
	}

	public boolean stop(FCPQueueManager queueManager) {
		return false;
	}

}
