package thaw.plugins;

import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.gui.IconBox;
import thaw.core.Logger;
import thaw.core.Plugin;
import thaw.plugins.index.AutoRefresh;
import thaw.plugins.index.DatabaseManager;
import thaw.plugins.index.IndexBrowserPanel;
import thaw.plugins.index.IndexConfigPanel;
import thaw.plugins.index.IndexManagementHelper;


public class IndexBrowser extends ToolbarModifier implements Plugin, ChangeListener {

	public static final String DEFAULT_INDEX = "USK@p-uFAWUomLm37MCQLu3r67-B8e6yF1kS4q2v0liM1Vk,h0MWqM~lF0Bec-AIv445PLn06ams9-RFbnwO6Cm2Snc,AQACAAE/Thaw/2/Thaw.frdx";

	private Core core;
	private Hsqldb hsqldb;
	private Signatures signatures;


	private IndexBrowserPanel browserPanel;
	private Vector toolbarActions;

	private IndexConfigPanel configPanel;

	private AutoRefresh autoRefresh = null;

	public IndexBrowser() {

	}

	public boolean run(final Core core) {
		this.core = core;


		if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
			Logger.info(this, "Loading Hsqldb plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
				Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
				return false;
			}
		}

		hsqldb = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");
		hsqldb.registerChild(this);


		if(core.getPluginManager().getPlugin("thaw.plugins.Signatures") == null) {
			Logger.info(this, "Loading signatures plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.Signatures") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.Signatures")) {
				Logger.error(this, "Unable to load thaw.plugins.Signatures !");
				return false;
			}
		}

		signatures = (Signatures)core.getPluginManager().getPlugin("thaw.plugins.Signatures");
		signatures.registerChild(this);


		boolean newDb;

		newDb = DatabaseManager.init(hsqldb, core.getConfig(), core.getSplashScreen());

		browserPanel = new IndexBrowserPanel(hsqldb, core);
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


		configPanel = new IndexConfigPanel(core.getConfigWindow(), core.getConfig(), browserPanel);
		configPanel.addTab();

		autoRefresh = null;

		if (core.getConfig().getValue("indexAutoRefreshActivated") != null) {
			if (Boolean.valueOf(core.getConfig().getValue("indexAutoRefreshActivated")).booleanValue()) {
				autoRefresh = new AutoRefresh(hsqldb, browserPanel, core.getQueueManager(), core.getConfig());
			}
		} else {
			if (AutoRefresh.DEFAULT_ACTIVATED) {
				autoRefresh = new AutoRefresh(hsqldb, browserPanel, core.getQueueManager(), core.getConfig());
			}
		}

		thaw.plugins.index.File.resumeTransfers(core.getQueueManager(), hsqldb);

		if (autoRefresh != null)
			autoRefresh.start();

		return true;
	}

	public IndexBrowserPanel getIndexBrowserPanel() {
		return browserPanel;
	}

	public boolean stop() {
		if (autoRefresh != null)
			autoRefresh.stop();

		browserPanel.getCommentTab().hideTab();
		browserPanel.getBlackList().hidePanel();

		browserPanel.stopAllThreads();

		core.getMainWindow().getTabbedPane().removeChangeListener(this);

		if (browserPanel != null) {
			core.getMainWindow().removeTab(browserPanel.getPanel());
			browserPanel.saveState();
		}

		hsqldb.unregisterChild(this);
		signatures.unregisterChild(this);

		if (configPanel != null)
			configPanel.removeTab();

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

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.indexBrowser;
	}
}
