package thaw.plugins;

import javax.swing.JPanel;

import thaw.core.*;
import thaw.i18n.I18n;
import thaw.plugins.fetchPlugin.*;


public class FetchPlugin implements thaw.core.Plugin {
	private Core core;

	private FetchPanel fetchPanel = null;
	

	public FetchPlugin() {

	}


	public boolean run(Core core) {
		this.core = core;
		
		Logger.info(this, "Starting plugin \"FetchPlugin\" ...");

		fetchPanel = new FetchPanel();

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

}
