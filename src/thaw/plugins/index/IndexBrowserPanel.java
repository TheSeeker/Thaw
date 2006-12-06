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


public class IndexBrowserPanel implements javax.swing.event.TreeSelectionListener, ActionListener {
	private IndexTree indexTree;
	private JSplitPane leftSplit;
	private UnknownIndexList unknownList;

	private JSplitPane split;

	private JPanel listAndDetails;
	private Tables tables;
	private FileDetailsEditor fileDetails;

	private Hsqldb db;
	private FCPQueueManager queueManager;
	private Config config;



	public IndexBrowserPanel(final Hsqldb db, final FCPQueueManager queueManager, final Config config, final MainWindow mainWindow) {
		this.db = db;
		this.queueManager = queueManager;
		this.config = config;

		unknownList = new UnknownIndexList(db, queueManager);
		indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.indexes"), false, queueManager, unknownList, mainWindow, db);
		unknownList.setIndexTree(indexTree); /* TODO: dirty => find a better way */

		leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					   indexTree.getPanel(),
					   unknownList.getPanel());

		listAndDetails = new JPanel();
		listAndDetails.setLayout(new BorderLayout(10, 10));

		tables = new Tables(false, db, queueManager, unknownList, indexTree, config);
		fileDetails = new FileDetailsEditor(false);

		listAndDetails.add(tables.getPanel(), BorderLayout.CENTER);
		listAndDetails.add(fileDetails.getPanel(), BorderLayout.SOUTH);

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					    leftSplit, listAndDetails);

		indexTree.addTreeSelectionListener(this);
	}

	public void restoreState() {
		if (config.getValue("indexBrowserPanelSplitPosition") != null)
			split.setDividerLocation(Integer.parseInt(config.getValue("indexBrowserPanelSplitPosition")));

		leftSplit.setSize(150, MainWindow.DEFAULT_SIZE_Y - 150);
		leftSplit.setResizeWeight(0.5);

		if (config.getValue("indexTreeUnknownListSplitLocation") == null) {
			leftSplit.setDividerLocation((0.5));
		} else {
			leftSplit.setDividerLocation(Double.parseDouble(config.getValue("indexTreeUnknownListSplitLocation")));
		}

		leftSplit.setResizeWeight(0.5);

		tables.restoreState();
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

	public JSplitPane getPanel() {
		return split;
	}

	public void saveState() {
		indexTree.save();
		config.setValue("indexBrowserPanelSplitPosition", Integer.toString(split.getDividerLocation()));
		double splitLocation;

		splitLocation = ((double)leftSplit.getDividerLocation() - ((double)leftSplit.getMinimumDividerLocation())) / (((double)leftSplit.getMaximumDividerLocation()) - ((double)leftSplit.getMinimumDividerLocation()));

		config.setValue("indexTreeUnknownListSplitLocation",
				Double.toString(splitLocation));

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

		final IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();

		if(node == null) {
			Logger.notice(this, "Node null ?");
			return;
		}

		if (node instanceof FileList) {
			Logger.info(this, "FileList !");
			setFileList((FileList)node);
		}

		if (node instanceof LinkList) {
			Logger.info(this, "LinkList !");
			setLinkList((LinkList)node);
		}

	}


	public void actionPerformed(final ActionEvent e) {

	}
}
