package thaw.plugins;

import java.util.Iterator;
import java.util.Vector;

import java.awt.BorderLayout;
import javax.swing.JPanel;

import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.IconBox;
import thaw.core.Logger;
import thaw.core.Plugin;
import thaw.plugins.index.IndexBrowserPanel;
import thaw.plugins.index.IndexManagementHelper;
import thaw.plugins.index.IndexTreeNode;
import thaw.plugins.index.DatabaseManager;

public class IndexBrowser extends ToolbarModifier implements Plugin, ChangeListener {

	public static final String DEFAULT_INDEX = "USK@G-ofLp2KlhHBNPezx~GDWDKThJ-QUxJK8c2xiF~-jwE,-55vLnqo3U1H5qmKA1LLADoYGQdk-Y3hSLxyKeUyHNc,AQABAAE/Thaw/2/Thaw.xml";

	private Core core;
	private Hsqldb hsqldb;

	private IndexBrowserPanel browserPanel;
	private Vector toolbarActions;

	public IndexBrowser() {

	}

	public boolean run(final Core core) {
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

		boolean newDb;

		newDb = false;

		if (core.getConfig().getValue("indexDatabaseVersion") == null) {
			DatabaseManager.createTables(hsqldb);
			newDb = true;
			core.getConfig().setValue("indexDatabaseVersion", "1");
		}

		browserPanel = new IndexBrowserPanel(hsqldb, core.getQueueManager(), core.getConfig(), core.getMainWindow());
		setMainWindow(core.getMainWindow());
		core.getMainWindow().getTabbedPane().addChangeListener(this);

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.index.indexes"),
					    IconBox.minIndexBrowser,
					    browserPanel.getPanel());

		browserPanel.restoreState();

		if (newDb) {
			IndexManagementHelper.addIndex(core.getQueueManager(), browserPanel, null, IndexBrowser.DEFAULT_INDEX);
		}

		stateChanged(null);

		return true;
	}

	public boolean stop() {
		core.getMainWindow().getTabbedPane().removeChangeListener(this);

		if (browserPanel != null) {
			core.getMainWindow().removeTab(browserPanel.getPanel());
			browserPanel.saveState();
		}

		hsqldb.unregisterChild(this);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.index.browser");
	}


       	/**
	 * Called when the JTabbedPane changed (ie change in the selected tab, etc)
	 * @param e can be null.
	 */
	public void stateChanged(final ChangeEvent e) {
		int tabId;

		tabId = core.getMainWindow().getTabbedPane().indexOfTab(I18n.getMessage("thaw.plugin.index.indexes"));

		if (tabId < 0) {
			Logger.warning(this, "Unable to find the tab !");
			return;
		}

		browserPanel.isVisible(core.getMainWindow().getTabbedPane().getSelectedIndex() == tabId);
	}

}
