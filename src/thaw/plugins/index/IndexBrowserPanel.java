package thaw.plugins;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import javax.swing.tree.DefaultMutableTreeNode;

import javax.swing.JToolBar;
import javax.swing.JButton;

import javax.swing.JFileChooser;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import thaw.core.*;
import thaw.fcp.*;

import thaw.plugins.index.*;


public class IndexBrowserPanel implements javax.swing.event.TreeSelectionListener, ActionListener {
	public final static int DEFAULT_INSERTION_PRIORITY = 4;

	private IndexTree indexTree;

	private JSplitPane split;

	private JPanel listAndDetails;
	private Tables tables;
	private FileDetailsEditor fileDetails;

	private Hsqldb db;
	private FCPQueueManager queueManager;


	public IndexBrowserPanel(Hsqldb db, FCPQueueManager queueManager) {
		this.db = db;
		this.queueManager = queueManager;

		indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.indexes"), false, false, queueManager, db);

		listAndDetails = new JPanel();
		listAndDetails.setLayout(new BorderLayout(10, 10));

		tables = new Tables(false, queueManager);
		fileDetails = new FileDetailsEditor(false);

		listAndDetails.add(tables.getPanel(), BorderLayout.CENTER);
		listAndDetails.add(fileDetails.getPanel(), BorderLayout.SOUTH);

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				       indexTree.getPanel(),
				       listAndDetails);

		indexTree.addTreeSelectionListener(this);

	}

	public JSplitPane getPanel() {
		return split;
	}

	public void save() {
		indexTree.save();
	}
	
	protected void setFileList(FileList l) {
		tables.getFileTable().setFileList(l);		
	}
	

	public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
		javax.swing.tree.TreePath path = e.getPath();
		
		if(path == null) {
			Logger.notice(this, "Path null ?");
			setFileList(null);
			return;
		}
		
		IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();

		if(node == null) {
			Logger.notice(this, "Node null ?");
			setFileList(null);
			return;
		}

		if(node instanceof FileList) {
			Logger.info(this, "FileList !");
			setFileList((FileList)node);
			return;
		}
		
		setFileList(null);
	}


	public void actionPerformed(ActionEvent e) {

	}
}
