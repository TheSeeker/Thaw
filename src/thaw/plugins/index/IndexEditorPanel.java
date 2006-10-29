package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import javax.swing.tree.DefaultMutableTreeNode;

import javax.swing.JToolBar;
import javax.swing.JButton;

import javax.swing.JFileChooser;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Vector;
import java.util.Iterator;


import thaw.core.*;
import thaw.fcp.*;

import thaw.plugins.Hsqldb;


public class IndexEditorPanel implements java.util.Observer, javax.swing.event.TreeSelectionListener, ActionListener {
	public final static int DEFAULT_INSERTION_PRIORITY = 4;

	private IndexTree indexTree;

	private JSplitPane split;

	private JPanel listAndDetails;
	private Tables tables;
	private FileDetailsEditor fileDetails;

	private JToolBar toolBar;
	private JButton addButton;
	private JButton insertAndAddButton;
	private JButton linkButton;

	private FileList fileList = null; /* Index or SearchResult object */
	private LinkList linkList = null;

	private Hsqldb db;
	private FCPQueueManager queueManager;
	private Config config;

	public IndexEditorPanel(Hsqldb db, FCPQueueManager queueManager, Config config) {
		this.db = db;
		this.queueManager = queueManager;
		this.config = config;

		this.indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.yourIndexes"), true, false, queueManager, db);

		this.toolBar = new JToolBar();
		this.toolBar.setFloatable(false);

		this.addButton = new JButton(IconBox.addToIndexAction);
		this.addButton.setToolTipText(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));
		this.insertAndAddButton = new JButton(IconBox.insertAndAddToIndexAction);
		this.insertAndAddButton.setToolTipText(I18n.getMessage("thaw.plugin.index.addFilesWithInserting"));
		this.linkButton = new JButton(IconBox.makeALinkAction);
		this.linkButton.setToolTipText(I18n.getMessage("thaw.plugin.index.addLink"));

		this.addButton.addActionListener(this);
		this.insertAndAddButton.addActionListener(this);
		this.linkButton.addActionListener(this);

		this.buttonsEnabled(false);

		this.toolBar.add(this.insertAndAddButton);
		this.toolBar.add(this.addButton);
		this.toolBar.addSeparator();
		this.toolBar.add(this.linkButton);

		this.tables = new Tables(true, db, queueManager, this.indexTree, config);
		this.fileDetails = new FileDetailsEditor(true);

		this.listAndDetails = new JPanel();
		this.listAndDetails.setLayout(new BorderLayout(0, 0));

		this.listAndDetails.add(this.toolBar, BorderLayout.NORTH);
		this.listAndDetails.add(this.tables.getPanel(), BorderLayout.CENTER);
		this.listAndDetails.add(this.fileDetails.getPanel(), BorderLayout.SOUTH);

		this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				       this.indexTree.getPanel(),
				       this.listAndDetails);

		this.indexTree.addTreeSelectionListener(this);

	}

	public void restoreState() {
		if (this.config.getValue("indexEditorPanelSplitPosition") != null)
			this.split.setDividerLocation(Integer.parseInt(this.config.getValue("indexEditorPanelSplitPosition")));
		this.tables.restoreState();
	}

	public JSplitPane getPanel() {
		return this.split;
	}

	public void saveState() {
		this.indexTree.save();
		this.config.setValue("indexEditorPanelSplitPosition", Integer.toString(this.split.getDividerLocation()));
		this.tables.saveState();
	}

	public void buttonsEnabled(boolean a) {
		this.addButton.setEnabled(a);
		this.insertAndAddButton.setEnabled(a);
		this.linkButton.setEnabled(a);
	}

	protected void setList(FileAndLinkList l) {
		this.setLinkList(l);
		this.setFileList(l);
	}

	protected void setLinkList(LinkList l) {
		this.buttonsEnabled(l != null && l instanceof Index);
		this.linkList = l;
		this.tables.setLinkList(l);
	}

	protected void setFileList(FileList l) {
		this.buttonsEnabled(l != null && l instanceof Index);

		this.fileList = l;
		this.tables.setFileList(l);
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

		if(node instanceof FileList) {
			Logger.info(this, "FileList !");
			this.setFileList((FileList)node);
		}

		if(node instanceof LinkList) {
			Logger.info(this, "LinkList !");
			this.setLinkList((LinkList)node);
		}

	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.linkButton) {
			Thread linkMakerThread = new Thread(new LinkMaker());
			linkMakerThread.start();
		}

		if(e.getSource() == this.addButton
		   || e.getSource() == this.insertAndAddButton) {
			FileChooser fileChooser = new FileChooser();

			if(e.getSource() == this.addButton)
				fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithInserting"));
			if(e.getSource() == this.insertAndAddButton)
				fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));

			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

			Vector files = fileChooser.askManyFiles();

			if(files == null) {
				Logger.info(this, "add[andInsert]Button : Cancelled");
				return;
			}

			String category = FileCategory.promptForACategory();

			for(Iterator it = files.iterator();
			    it.hasNext();) {

				java.io.File ioFile = (java.io.File)it.next();

				FCPTransferQuery insertion = null;

				if(e.getSource() == this.insertAndAddButton) {
					insertion = new FCPClientPut(ioFile, 0, 0, null,
								     null, DEFAULT_INSERTION_PRIORITY,
								     true, 0, false);
					((FCPClientPut)insertion).addObserver(this);
					this.queueManager.addQueryToThePendingQueue(insertion);
				} else {
					insertion = new FCPClientPut(ioFile, 0, 0, null,
								     null, DEFAULT_INSERTION_PRIORITY,
								     true, 2, true); /* getCHKOnly */
					insertion.start(this.queueManager);
				}


				thaw.plugins.index.File file = new thaw.plugins.index.File(this.db, ioFile.getPath(),
											   category, (Index)this.fileList,
											   insertion);

				((Index)this.fileList).addFile(file);
			}
		}
	}


	public class LinkMaker implements Runnable {
		public LinkMaker() { }

		public void run() {
			IndexSelecter indexSelecter = new IndexSelecter();
			String indexKey = indexSelecter.askForAnIndexURI(IndexEditorPanel.this.db);
			if (indexKey != null) {
				Link newLink = new Link(IndexEditorPanel.this.db, indexKey, (Index)IndexEditorPanel.this.linkList);
				((Index)IndexEditorPanel.this.linkList).addLink(newLink);
			}
		}
	}

	public void update(java.util.Observable o, Object param) {
		if(o instanceof FCPClientPut) {
			FCPClientPut clientPut = (FCPClientPut)o;
			if(clientPut.isFinished()) {
				this.queueManager.remove(clientPut);
			}
		}
	}
}
