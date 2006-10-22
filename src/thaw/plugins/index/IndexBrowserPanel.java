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
	public final static int DEFAULT_INSERTION_PRIORITY = 4;

	private IndexTree indexTree;

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

		indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.indexes"), false, false, queueManager, db);

		listAndDetails = new JPanel();
		listAndDetails.setLayout(new BorderLayout(10, 10));

		tables = new Tables(false, db, queueManager, indexTree, config);
		fileDetails = new FileDetailsEditor(false);

		listAndDetails.add(tables.getPanel(), BorderLayout.CENTER);
		listAndDetails.add(fileDetails.getPanel(), BorderLayout.SOUTH);

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				       indexTree.getPanel(),
				       listAndDetails);

		indexTree.addTreeSelectionListener(this);
	}

	public void restoreState() {
		if (config.getValue("indexBrowserPanelSplitPosition") != null)
			split.setDividerLocation(Integer.parseInt(config.getValue("indexBrowserPanelSplitPosition")));
		tables.restoreState();
	}

	public JSplitPane getPanel() {
		return split;
	}

	public void saveState() {
		indexTree.save();
		config.setValue("indexBrowserPanelSplitPosition", Integer.toString(split.getDividerLocation()));
		tables.saveState();
	}


	protected void setList(FileAndLinkList l) {
		tables.setList(l);
	}

	protected void setFileList(FileList l) {
		tables.setFileList(l);
	}

	protected void setLinkList(LinkList l) {
		tables.setLinkList(l);
	}
	

	public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
		javax.swing.tree.TreePath path = e.getPath();

		setList(null);

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
			setFileList((FileList)node);
		}

		if (node instanceof LinkList) {
			Logger.info(this, "LinkList !");
			setLinkList((LinkList)node);
		}

	}


	public void actionPerformed(ActionEvent e) {

	}
}
