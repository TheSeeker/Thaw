package thaw.plugins;

import thaw.core.*;
import thaw.plugins.index.*;

public class IndexEditor implements Plugin {
	private Core core;
	private Hsqldb hsqldb;

	private IndexEditorPanel editorPanel;

	public IndexEditor() {

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

		this.hsqldb = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");

		this.hsqldb.registerChild(this);

		TableCreator.createTables(this.hsqldb);

		
		this.editorPanel = new IndexEditorPanel(this.hsqldb, core.getQueueManager(), core.getConfig());


		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.index.editor"),
					    IconBox.minIndexEditor,
					    this.editorPanel.getPanel());
		
		this.editorPanel.restoreState();

		return true;
	}


	public boolean stop() {
		this.core.getMainWindow().removeTab(this.editorPanel.getPanel());

		this.editorPanel.saveState();

		this.hsqldb.unregisterChild(this);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.index.editor");
	}
}
