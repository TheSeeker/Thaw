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

import thaw.core.*;
import thaw.fcp.*;

import thaw.plugins.index.*;

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


	public IndexEditorPanel(Hsqldb db, FCPQueueManager queueManager) {
		this.db = db;
		this.queueManager = queueManager;

		indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.yourIndexes"), true, false, queueManager, db);

		toolBar = new JToolBar();
		toolBar.setFloatable(false);

		addButton = new JButton(IconBox.addToIndexAction);
		addButton.setToolTipText(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));
		insertAndAddButton = new JButton(IconBox.insertAndAddToIndexAction);
		insertAndAddButton.setToolTipText(I18n.getMessage("thaw.plugin.index.addFilesWithInserting"));
		linkButton = new JButton(IconBox.makeALinkAction);
		linkButton.setToolTipText(I18n.getMessage("thaw.plugin.index.addLink"));

		addButton.addActionListener(this);
		insertAndAddButton.addActionListener(this);
		linkButton.addActionListener(this);

		buttonsEnabled(false);

		toolBar.add(addButton);
		toolBar.add(insertAndAddButton);
		toolBar.addSeparator();
		toolBar.add(linkButton);

		tables = new Tables(true, db, queueManager, indexTree);
		fileDetails = new FileDetailsEditor(true);

		listAndDetails = new JPanel();
		listAndDetails.setLayout(new BorderLayout(0, 0));

		listAndDetails.add(toolBar, BorderLayout.NORTH);
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

	public void buttonsEnabled(boolean a) {
		addButton.setEnabled(a);
		insertAndAddButton.setEnabled(a);
		linkButton.setEnabled(a);
	}

	protected void setList(FileAndLinkList l) {
		setLinkList(l);
		setFileList(l);
	}

	protected void setLinkList(LinkList l) {
		buttonsEnabled(l != null && l instanceof Index);
		this.linkList = l;
		tables.setLinkList(l);
	}

	protected void setFileList(FileList l) {
		buttonsEnabled(l != null && l instanceof Index);

		this.fileList = l;
		tables.setFileList(l);		
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

		if(node instanceof FileList) {
			Logger.info(this, "FileList !");
			setFileList((FileList)node);
		}

		if(node instanceof LinkList) {
			Logger.info(this, "LinkList !");
			setLinkList((LinkList)node);
		}

	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == linkButton) {
			Thread linkMakerThread = new Thread(new LinkMaker());
			linkMakerThread.start();
		}

		if(e.getSource() == addButton
		   || e.getSource() == insertAndAddButton) {
			FileChooser fileChooser = new FileChooser();
			
			if(e.getSource() == addButton)
				fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithInserting"));
			if(e.getSource() == insertAndAddButton)
				fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));

			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			
			java.io.File[] files = fileChooser.askManyFiles();

			if(files == null) {
				Logger.info(this, "add[andInsert]Button : Cancelled");
				return;
			}

			String category = FileCategory.promptForACategory();

			for(int i = 0 ; i < files.length ; i++) {
				FCPTransferQuery insertion = null;

				if(e.getSource() == insertAndAddButton) {
					insertion = new FCPClientPut(files[i], 0, 0, null,
								     null, DEFAULT_INSERTION_PRIORITY,
								     true, 0, false);
					((FCPClientPut)insertion).addObserver(this);
					queueManager.addQueryToThePendingQueue(insertion);
				} else {
					insertion = new FCPClientPut(files[i], 0, 0, null,
								     null, DEFAULT_INSERTION_PRIORITY,
								     true, 2, true); /* getCHKOnly */
					insertion.start(queueManager);
				}


				thaw.plugins.index.File file = new thaw.plugins.index.File(db, files[i].getPath(),
											   category, (Index)fileList,
											   insertion);
				
				((Index)fileList).addFile(file);
			}
		}
	}


	public class LinkMaker implements Runnable {
		public LinkMaker() { }

		public void run() {
			IndexSelecter indexSelecter = new IndexSelecter();
			String indexKey = indexSelecter.askForAnIndexURI(db);
			if (indexKey != null) {
				Link newLink = new Link(db, indexKey, (Index)linkList);
				((Index)linkList).addLink(newLink);
			}
		}
	}

	public void update(java.util.Observable o, Object param) {
		if(o instanceof FCPClientPut) {
			FCPClientPut clientPut = (FCPClientPut)o;
			if(clientPut.isFinished()) {
				queueManager.remove(clientPut);				
			}
		}
	}
}
