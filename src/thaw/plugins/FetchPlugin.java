package thaw.plugins;

import javax.swing.JPanel;

import thaw.core.*;
import thaw.i18n.I18n;
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

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.download"), fetchPanel.getPanel());

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
			String[] subKey = keys[i].split("\\?"); /* Because of VolodyA :p */

			String key = subKey[0].replace("http://127.0.0.1:8888/", "");

			core.getQueueManager().addQueryToThePendingQueue(new FCPClientGet(key,
											  priority,
											  persistence,
											  globalQueue,
											  destination));
		}

	}

}
