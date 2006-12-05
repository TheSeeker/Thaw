package thaw.plugins;

import javax.swing.JButton;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.*;

import thaw.plugins.index.*;

public class IndexBrowser extends ToolbarModifier implements Plugin, ChangeListener, java.util.Observer {

	public static final String DEFAULT_INDEX = "USK@G-ofLp2KlhHBNPezx~GDWDKThJ-QUxJK8c2xiF~-jwE,-55vLnqo3U1H5qmKA1LLADoYGQdk-Y3hSLxyKeUyHNc,AQABAAE/Thaw/2/Thaw.xml";

	private Core core;
	private Hsqldb hsqldb;

	private IndexBrowserPanel browserPanel;
	private Vector toolbarActions;

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

		boolean newDb;

		newDb = false;

		if (core.getConfig().getValue("indexDatabaseVersion") == null) {
			TableCreator.createTables(hsqldb);
			newDb = true;
			core.getConfig().setValue("indexDatabaseVersion", "1");
		}

		browserPanel = new IndexBrowserPanel(hsqldb, core.getQueueManager(), core.getConfig(), core.getMainWindow());
		browserPanel.getIndexTree().addObserver(this);

		setMainWindow(core.getMainWindow());
		core.getMainWindow().getTabbedPane().addChangeListener(this);

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.index.indexes"),
					    IconBox.minIndexBrowser,
					    browserPanel.getPanel());

		browserPanel.restoreState();

		JButton button;
		toolbarActions = new Vector();
		IndexManagementHelper.IndexAction action;

		button = new JButton(IconBox.refreshAction);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.downloadIndexes"));
		action = new IndexManagementHelper.IndexDownloader(button);
		action.setTarget(browserPanel.getIndexTree().getRoot()); /* TODO : Listen to tree to only refresh the selected node */
		addButtonToTheToolbar(button);
		toolbarActions.add(action);

		button = new JButton(IconBox.indexReuse);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.addAlreadyExistingIndex"));
		action = new IndexManagementHelper.IndexReuser(hsqldb, core.getQueueManager(), browserPanel.getUnknownIndexList(), browserPanel.getIndexTree(), core.getMainWindow(), button);
		action.setTarget(browserPanel.getIndexTree().getRoot());
		addButtonToTheToolbar(button);
		toolbarActions.add(action);

		button = new JButton(IconBox.indexNew);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.createIndex"));
		action = new IndexManagementHelper.IndexCreator(hsqldb, core.getQueueManager(), browserPanel.getUnknownIndexList(), browserPanel.getIndexTree(), button);
		action.setTarget(browserPanel.getIndexTree().getRoot());
		addButtonToTheToolbar(button);
		toolbarActions.add(action);

		if (newDb) {
			IndexManagementHelper.addIndex(hsqldb, core.getQueueManager(), browserPanel.getUnknownIndexList(), browserPanel.getIndexTree(),
						       browserPanel.getIndexTree().getRoot(), DEFAULT_INDEX);
		}

		stateChanged(null);

		return true;
	}

	public boolean stop() {
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
	public void stateChanged(ChangeEvent e) {
		int tabId;

		tabId = core.getMainWindow().getTabbedPane().indexOfTab(I18n.getMessage("thaw.plugin.index.indexes"));

		if (tabId < 0) {
			Logger.warning(this, "Unable to find the tab !");
			return;
		}

		if (core.getMainWindow().getTabbedPane().getSelectedIndex() == tabId) {
			displayButtonsInTheToolbar();
		} else {
			hideButtonsInTheToolbar();
		}
	}

	public void update (java.util.Observable o, Object arg) {
		if (o == browserPanel.getIndexTree()
		    && arg instanceof IndexTreeNode) {

			for (Iterator it = toolbarActions.iterator();
			     it.hasNext(); ) {
				IndexManagementHelper.IndexAction action = (IndexManagementHelper.IndexAction)it.next();
				action.setTarget((IndexTreeNode)arg);
			}

		}
	}
}
