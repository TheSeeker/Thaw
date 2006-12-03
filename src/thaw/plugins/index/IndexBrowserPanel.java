package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import thaw.core.*;
import thaw.fcp.*;

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



	public IndexBrowserPanel(Hsqldb db, FCPQueueManager queueManager, Config config) {
		this.db = db;
		this.queueManager = queueManager;
		this.config = config;

		unknownList = new UnknownIndexList(db, queueManager);
		indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.indexes"), false, queueManager, unknownList, db);
		unknownList.setIndexTree(indexTree); /* TODO: dirty => find a better way */

		leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					   indexTree.getPanel(),
					   unknownList.getPanel());

		this.listAndDetails = new JPanel();
		this.listAndDetails.setLayout(new BorderLayout(10, 10));

		this.tables = new Tables(false, db, queueManager, unknownList, this.indexTree, config);
		this.fileDetails = new FileDetailsEditor(false);

		this.listAndDetails.add(this.tables.getPanel(), BorderLayout.CENTER);
		this.listAndDetails.add(this.fileDetails.getPanel(), BorderLayout.SOUTH);

		this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					    leftSplit, listAndDetails);

		this.indexTree.addTreeSelectionListener(this);
	}

	public void restoreState() {
		if (config.getValue("indexBrowserPanelSplitPosition") != null)
			split.setDividerLocation(Integer.parseInt(this.config.getValue("indexBrowserPanelSplitPosition")));

		leftSplit.setSize(150, MainWindow.DEFAULT_SIZE_Y - 150);
		leftSplit.setResizeWeight(0.5);

		if (config.getValue("indexTreeUnknownListSplitLocation") == null) {
			leftSplit.setDividerLocation(((double)0.5));
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
		return this.split;
	}

	public void saveState() {
		this.indexTree.save();
		this.config.setValue("indexBrowserPanelSplitPosition", Integer.toString(this.split.getDividerLocation()));
		double splitLocation;

		splitLocation = ((double)leftSplit.getDividerLocation() - ((double)leftSplit.getMinimumDividerLocation())) / (((double)leftSplit.getMaximumDividerLocation()) - ((double)leftSplit.getMinimumDividerLocation()));

		config.setValue("indexTreeUnknownListSplitLocation",
				Double.toString(splitLocation));

		this.tables.saveState();
	}


	protected void setList(FileAndLinkList l) {
		this.tables.setList(l);
	}

	protected void setFileList(FileList l) {
		this.tables.setFileList(l);
	}

	protected void setLinkList(LinkList l) {
		this.tables.setLinkList(l);
	}

	public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
		javax.swing.tree.TreePath path = e.getPath();

		this.setList(null);

		if(path == null) {
			Logger.notice(this, "Path null ?");
			return;
		}

		IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();

		if(node == null) {
			Logger.notice(this, "Node null ?");
			return;
		}

		if (node instanceof FileList) {
			Logger.info(this, "FileList !");
			this.setFileList((FileList)node);
		}

		if (node instanceof LinkList) {
			Logger.info(this, "LinkList !");
			this.setLinkList((LinkList)node);
		}

	}


	public void actionPerformed(ActionEvent e) {

	}
}
