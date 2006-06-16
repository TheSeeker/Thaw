package thaw.plugins;

import javax.swing.JPanel;

import thaw.core.*;
import thaw.i18n.I18n;


public class FetchPlugin implements thaw.core.Plugin {
	private Core core;

	private JPanel mainPanel;
	

	public FetchPlugin() {

	}


	public boolean run(Core core) {
		this.core = core;
		
		Logger.info(this, "Starting plugin \"FetchPlugin\" ...");

		mainPanel = new JPanel();

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.download"), mainPanel);

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"FetchPlugin\" ...");

		core.getMainWindow().removeTab(mainPanel);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.common.download");
	}

}
