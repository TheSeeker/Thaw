package thaw.plugins;

import thaw.core.*;
import thaw.plugins.fetchPlugin.*;

import thaw.fcp.*;

public class FetchPlugin implements thaw.core.Plugin {
	private Core core;

	private FetchPanel fetchPanel = null;
	

	public FetchPlugin() {

	}


	public boolean run(Core core) {
		this.core = core;
		
		Logger.info(this, "Starting plugin \"FetchPlugin\" ...");

		fetchPanel = new FetchPanel(core, this);

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.download"), 
					    IconBox.minDownloads,
					    fetchPanel.getPanel());

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"FetchPlugin\" ...");

		core.getMainWindow().removeTab(fetchPanel.getPanel());

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.common.download");
	}


	public void fetchFiles(String[] keys, int priority,
			       int persistence, boolean globalQueue,
			       String destination) {

		for(int i = 0 ; i < keys.length ; i++) {
			if(keys[i].length() < 10)
				continue;

			String[] subKey = keys[i].split("\\?"); /* Because of VolodyA :p */

			String key = FreenetURIHelper.cleanURI(subKey[0]);

			core.getQueueManager().addQueryToThePendingQueue(new FCPClientGet(key,
											  priority,
											  persistence,
											  globalQueue, -1,
											  destination));
		}

	}

}
