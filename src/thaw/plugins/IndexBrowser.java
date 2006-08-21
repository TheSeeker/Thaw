package thaw.plugins;

import thaw.core.*;

import thaw.plugins.index.*;

public class IndexBrowser implements Plugin {
	private Core core;
	private Hsqldb hsqldb;

	private IndexBrowserPanel browserPanel;

	public IndexBrowser() {

	}

	public boolean run(Core core) {
		this.core = core;

		if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
			Logger.info(this, "Loading Hsqldb plugin");

			if(!core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb")
			   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
				Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
				return false;
			}
		}

		hsqldb = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");

		hsqldb.registerChild(this);

		TableCreator.createTables(hsqldb);

		browserPanel = new IndexBrowserPanel(hsqldb, core.getQueueManager());

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.index.browser"),
					    IconBox.minIndexBrowser,
					    browserPanel.getPanel());

		return true;
	}

	public boolean stop() {
		core.getMainWindow().removeTab(browserPanel.getPanel());

		browserPanel.save();

		hsqldb.unregisterChild(this);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.index.browser");
	}


}
