package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.tree.DefaultMutableTreeNode;

import thaw.core.Config;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.MainWindow;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;


public class IndexBrowserPanel implements javax.swing.event.TreeSelectionListener {
	private IndexTree indexTree;
	private Tables tables;
	private DetailPanel detailPanel;
	private UnknownIndexList unknownList;
	private IndexProgressBar indexProgressBar;

	private JSplitPane split;

	private JPanel listAndDetails;
	private JSplitPane leftSplit;

	private JPanel globalPanel;

	private Hsqldb db;
	private FCPQueueManager queueManager;
	private Config config;
	private MainWindow mainWindow;


	public IndexBrowserPanel(final Hsqldb db, final FCPQueueManager queueManager, final Config config, final MainWindow mainWindow) {
		this.db = db;
		this.queueManager = queueManager;
		this.config = config;
		this.mainWindow = mainWindow;

		unknownList = new UnknownIndexList(queueManager, this);

		indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.indexes"),
					  false, queueManager, this, config);

		leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					   indexTree.getPanel(),
					   unknownList.getPanel());

		listAndDetails = new JPanel();
		listAndDetails.setLayout(new BorderLayout(10, 10));

		tables = new Tables(false, queueManager, this, config);
		detailPanel = new DetailPanel();

		listAndDetails.add(tables.getPanel(), BorderLayout.CENTER);
		listAndDetails.add(detailPanel.getPanel(), BorderLayout.SOUTH);

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				       leftSplit, listAndDetails);

		indexTree.addTreeSelectionListener(this);

		indexProgressBar = new IndexProgressBar();

		globalPanel = new JPanel(new BorderLayout());
		globalPanel.add(split, BorderLayout.CENTER);
		globalPanel.add(indexProgressBar.getProgressBar(), BorderLayout.SOUTH);
	}


	public void restoreState() {
		if (config.getValue("indexBrowserPanelSplitPosition") != null)
			split.setDividerLocation(Integer.parseInt(config.getValue("indexBrowserPanelSplitPosition")));

		leftSplit.setSize(150, MainWindow.DEFAULT_SIZE_Y - 150);
		leftSplit.setResizeWeight(0.5);

		if (config.getValue("indexTreeUnknownListSplitLocation") == null) {
			leftSplit.setDividerLocation((0.5));
		} else {
			try {
				leftSplit.setDividerLocation(Integer.parseInt(config.getValue("indexTreeUnknownListSplitLocation")));
			} catch(java.lang.IllegalArgumentException e) { /* TODO: Find why it happens */
				Logger.error(this, "Exception while setting indexTree split location");
			}
		}

		leftSplit.setResizeWeight(0.5);

		tables.restoreState();
	}

	public Hsqldb getDb() {
		return db;
	}

	public Tables getTables() {
		return tables;
	}

	public IndexTree getIndexTree() {
		return indexTree;
	}

	public UnknownIndexList getUnknownIndexList() {
		return unknownList;
	}

	public DetailPanel getDetailPanel() {
		return detailPanel;
	}

	public IndexProgressBar getIndexProgressBar() {
		return indexProgressBar;
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}


	public JPanel getPanel() {
		return globalPanel;
	}

	public void stopAllThreads() {
		tables.stopRefresh();
	}

	public void saveState() {
		config.setValue("indexBrowserPanelSplitPosition", Integer.toString(split.getDividerLocation()));
		int splitLocation;

		splitLocation = leftSplit.getDividerLocation();

		config.setValue("indexTreeUnknownListSplitLocation",
				Integer.toString(splitLocation));

		tables.saveState();
	}


	protected void setList(final FileAndLinkList l) {
		tables.setList(l);
	}

	protected void setFileList(final FileList l) {
		tables.setFileList(l);
	}

	protected void setLinkList(final LinkList l) {
		tables.setLinkList(l);
	}

	public void valueChanged(final javax.swing.event.TreeSelectionEvent e) {
		final javax.swing.tree.TreePath path = e.getPath();

		setList(null);

		if(path == null) {
			Logger.notice(this, "Path null ?");
			return;
		}

		final IndexTreeNode node = (IndexTreeNode)(path.getLastPathComponent());

		if(node == null) {
			Logger.notice(this, "Node null ?");
			return;
		}

		if (node instanceof FileList) {
			Logger.debug(this, "FileList !");
			setFileList((FileList)node);
		}

		if (node instanceof LinkList) {
			Logger.debug(this, "LinkList !");
			setLinkList((LinkList)node);
		}

	}


	/**
	 * Called by IndexBrowser when the panel become visible
	 */
	public void isVisible(boolean visibility) {
		if (visibility) {
			indexTree.getToolbarModifier().displayButtonsInTheToolbar();
		} else {
			// one of these foor may be the buttons owner ?
			indexTree.getToolbarModifier().hideButtonsInTheToolbar();
			tables.getLinkTable().getToolbarModifier().hideButtonsInTheToolbar();
			tables.getFileTable().getToolbarModifier().hideButtonsInTheToolbar();
			unknownList.getToolbarModifier().hideButtonsInTheToolbar();
		}
	}


}
