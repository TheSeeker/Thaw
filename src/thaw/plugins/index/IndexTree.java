package thaw.plugins.index;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
//import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import javax.swing.JOptionPane;

import javax.swing.JScrollPane;

import javax.swing.Icon;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;

import java.util.Enumeration;

import java.awt.Color;

import javax.swing.JFrame;

import javax.swing.JToolBar;
import javax.swing.JButton;

import javax.swing.JTextField;
import javax.swing.JLabel;

import thaw.plugins.Hsqldb;
import thaw.core.*;
import thaw.fcp.*;

import thaw.gui.JDragTree;

/**
 * Manages the index tree and its menu (right-click).
 */
public class IndexTree extends java.util.Observable implements MouseListener, ActionListener, java.util.Observer {
	
	private JPanel panel;

	//private JDragTree tree;
	private JTree tree;
	private IndexCategory root;

	private JToolBar toolBar;
	private JButton newIndex;
	private JButton reuseIndex;
	private JButton refreshAll;


	private JPopupMenu indexCategoryMenu;
	private JPopupMenu indexMenu;

	private JMenuItem addIndex;
	private JMenuItem addIndexCategory;
	private JMenuItem renameIndexCategory;
	private JMenuItem renameIndex;
	private JMenuItem deleteIndexCategory;
	private JMenuItem deleteIndex;
	private JMenuItem updateIndexCategory;
	private JMenuItem updateIndex;
	private JMenuItem copyPublicKeys;
	private JMenuItem copyPublicKey;
	private JMenuItem copyPrivateKeys;
	private JMenuItem copyPrivateKey;
	private JMenuItem reloadFromFreenet;
	
       
	private boolean modifiables;
	private boolean selectionOnly;

	private IndexTreeNode selectedNode;

	private DefaultTreeModel treeModel;

	private Hsqldb db;
	private FCPQueueManager queueManager;


	/** used for a special form ***/
	private int formState;
	private JButton okButton = null;
	private JButton cancelButton = null;
	

	/**
	 * Menu is defined according to the 'modifiables' parameters.
	 * @param modifiables If set to true, then only indexes having private keys will
	 *                    be displayed else only indexes not having private keys will
	 *                    be displayed.
	 * @param queueManager Not used if selectionOnly is set to true
	 */
	public IndexTree(String rootName,
			 boolean modifiables,
			 boolean selectionOnly,
			 FCPQueueManager queueManager,
			 Hsqldb db) {
		this.queueManager = queueManager;

		this.db = db;
		this.modifiables = modifiables;
		this.selectionOnly = selectionOnly;

		panel = new JPanel();
		panel.setLayout(new BorderLayout(10, 10));
		
		indexCategoryMenu = new JPopupMenu();
		indexMenu = new JPopupMenu();

		if(modifiables)
			addIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.createIndex"));
		else
			addIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.addIndex"));

		addIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.addCategory"));
		renameIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.rename"));
		renameIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.rename"));
		deleteIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.delete"));
		deleteIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.delete"));
		
		if(modifiables) {
			updateIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.insertIndex"));
			updateIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.insertIndexes"));
		} else {
			updateIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.downloadIndex"));
			updateIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.downloadIndexes"));
		}

		copyPublicKey = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKey"));
		copyPublicKeys = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKeys"));

		if (modifiables) {
			copyPrivateKey = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyPrivateKey"));
			copyPrivateKeys = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyPrivateKey"));
			copyPrivateKey.addActionListener(this);
			copyPrivateKeys.addActionListener(this);

			reloadFromFreenet = new JMenuItem(I18n.getMessage("thaw.plugin.index.reloadFromFreenet"));
			reloadFromFreenet.addActionListener(this);
		}

		indexCategoryMenu.add(updateIndexCategory);
		indexCategoryMenu.add(addIndex);
		indexCategoryMenu.add(addIndexCategory);
		indexCategoryMenu.add(copyPublicKeys);
		indexCategoryMenu.add(updateIndex);
		indexCategoryMenu.add(renameIndexCategory);
		indexCategoryMenu.add(deleteIndexCategory);
		if (modifiables) {
			indexCategoryMenu.add(copyPrivateKeys);
		}

		indexMenu.add(updateIndex);
		indexMenu.add(copyPublicKey);
		indexMenu.add(renameIndex);
		indexMenu.add(deleteIndex);
		if (modifiables) {
			indexMenu.add(copyPrivateKey);
			indexMenu.add(reloadFromFreenet);
		}

		addIndex.addActionListener(this);
		addIndexCategory.addActionListener(this);
		renameIndexCategory.addActionListener(this);
		deleteIndexCategory.addActionListener(this);
		updateIndexCategory.addActionListener(this);

		updateIndex.addActionListener(this);
		copyPublicKey.addActionListener(this);
		copyPublicKeys.addActionListener(this);
		renameIndex.addActionListener(this);
		deleteIndex.addActionListener(this);
		

		root = new IndexCategory(db, queueManager, -1, null, rootName, modifiables);
		root.loadChildren();

		root.addObserver(this);

		treeModel = new DefaultTreeModel(root);

		if (!selectionOnly) {
			tree = new JDragTree(treeModel);
			tree.addMouseListener(this);
		} else {
			tree = new JTree(treeModel);
			tree.addMouseListener(this);
		}

		IndexTreeRenderer treeRenderer = new IndexTreeRenderer();
		treeRenderer.setLeafIcon(IconBox.minIndex);

		tree.setCellRenderer(treeRenderer);

		if (selectionOnly)
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		toolBar = new JToolBar();

		newIndex   = new JButton(IconBox.indexNew);
		if (!modifiables)
			newIndex.setToolTipText(I18n.getMessage("thaw.plugin.index.addIndex"));
		else
			newIndex.setToolTipText(I18n.getMessage("thaw.plugin.index.createIndex"));
		newIndex.addActionListener(this);


		if (!modifiables) {
			refreshAll = new JButton(IconBox.refreshAction);
			refreshAll.setToolTipText(I18n.getMessage("thaw.plugin.index.downloadIndexes"));
			refreshAll.addActionListener(this);
		} else {
			reuseIndex = new JButton(IconBox.indexReuse);
			reuseIndex.setToolTipText(I18n.getMessage("thaw.plugin.index.addAlreadyExisting"));
			reuseIndex.addActionListener(this);
		}


		toolBar.add(newIndex);
		if (!modifiables)
			toolBar.add(refreshAll);
		else
			toolBar.add(reuseIndex);

		if (!selectionOnly)
			panel.add(toolBar, BorderLayout.NORTH);
		panel.add(new JScrollPane(tree), BorderLayout.CENTER);
	}


	public javax.swing.JComponent getPanel() {
		return panel;
	}

	public void addTreeSelectionListener(javax.swing.event.TreeSelectionListener tsl) {
		tree.addTreeSelectionListener(tsl);
	}


	public JTree getTree() {
		return tree;
	}

	public IndexCategory getRoot() {
		return root;
	}

	public void mouseClicked(MouseEvent e) {
		notifySelection(e);
	}

	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	public void mousePressed(MouseEvent e) {
		if (!selectionOnly)
			showPopupMenu(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (!selectionOnly)
			showPopupMenu(e);
	}

	protected void showPopupMenu(MouseEvent e) {
		if(e.isPopupTrigger()) {
			TreePath path = tree.getPathForLocation(e.getX(), e.getY());
			
			if(path == null)
				return;

			selectedNode = (IndexTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();

			if(selectedNode == null)
				return;

			if(selectedNode instanceof IndexCategory)
				indexCategoryMenu.show(e.getComponent(), e.getX(), e.getY());
			
			if(selectedNode instanceof Index)
				indexMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void notifySelection(MouseEvent e) {
		TreePath path = tree.getPathForLocation(e.getX(), e.getY());
			
		if(path == null)
			return;

		selectedNode = (IndexTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();

		setChanged();
		notifyObservers(selectedNode);
	}

	public IndexTreeNode getSelectedNode() {
		Object obj = tree.getLastSelectedPathComponent();

		if (obj == null)
			return null;

		if (obj instanceof IndexTreeNode)
			return (IndexTreeNode)obj;

		if (obj instanceof DefaultMutableTreeNode)
			return ((IndexTreeNode)(((DefaultMutableTreeNode)obj).getUserObject()));

		Logger.notice(this, "getSelectedNode(): Unknow kind of node ?!");

		return null;
	}

	public void actionPerformed(ActionEvent e) {
		if(selectedNode == null)
			selectedNode = root;

		if(e.getSource() == addIndex
		   || e.getSource() == newIndex) {
			String name = null;

			String keys[] = null;
			String publicKey = null;

			if(!modifiables) {
				publicKey = askAName(I18n.getMessage("thaw.plugin.index.indexKey"), "USK@");

				if (publicKey == null)
					return;

				publicKey = FreenetURIHelper.cleanURI(publicKey);

				name = Index.getNameFromKey(publicKey);

			} else {
				name = askAName(I18n.getMessage("thaw.plugin.index.indexName"),
						I18n.getMessage("thaw.plugin.index.newIndex"));				
			}

			if(name == null)
				return;

			IndexCategory parent;

			if (selectedNode != null)
				parent = (IndexCategory)selectedNode;
			else
				parent = root;

			Index index = new Index(db, queueManager, -2, parent, name, name, publicKey, null, 0, null, modifiables);

			if(modifiables)
				index.generateKeys(queueManager);

			index.create();
			parent.insert(index.getTreeNode(), 0);

			treeModel.reload(parent);
		}


		if (e.getSource() == reuseIndex) {
			Thread newThread = new Thread(new IndexReuser());

			newThread.start();
		}



		if(e.getSource() == addIndexCategory) {
			String name = askAName(I18n.getMessage("thaw.plugin.index.categoryName"), 
					       I18n.getMessage("thaw.plugin.index.newCategory"));

			if(name == null)
				return;

			IndexCategory parent = (IndexCategory)selectedNode;

			/* the id will be defined when created */
			IndexCategory cat = new IndexCategory(db, queueManager, -2, parent,
							      name, modifiables);
			cat.create();
			parent.insert(cat, 0);

			treeModel.reload(parent);
		}

		if(e.getSource() == renameIndexCategory
		   || e.getSource() == renameIndex) {
			
			String newName;

			if(e.getSource() == renameIndexCategory)
				newName = askAName(I18n.getMessage("thaw.plugin.index.categoryName"), 
						   selectedNode.toString());
			else
				newName = askAName(I18n.getMessage("thaw.plugin.index.indexName"), 
						   selectedNode.toString());

			if(newName == null)
				return;

			selectedNode.rename(newName);

			treeModel.reload(selectedNode.getTreeNode());
		}

		if(e.getSource() == deleteIndexCategory
		   || e.getSource() == deleteIndex) {

			MutableTreeNode parent = (MutableTreeNode)selectedNode.getTreeNode().getParent();

			if(parent != null)
				parent.remove(selectedNode.getTreeNode());

			selectedNode.delete();

			if(parent != null)
				treeModel.reload(parent);
			else
				treeModel.reload();
		}

		if(e.getSource() == updateIndex
		   || e.getSource() == updateIndexCategory) {
			selectedNode.update();
		}

		if (e.getSource() == reloadFromFreenet) {
			selectedNode.updateFromFreenet(-1);
		}
		
		if (e.getSource() == refreshAll) {
			root.update();
		}

		if(e.getSource() == copyPublicKey
		   || e.getSource() == copyPublicKeys) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			StringSelection st = new StringSelection(selectedNode.getPublicKey());
			Clipboard cp = tk.getSystemClipboard();
			cp.setContents(st, null);
		}

		
		if(e.getSource() == copyPrivateKey
		   || e.getSource() == copyPrivateKeys) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			StringSelection st = new StringSelection(selectedNode.getPrivateKey());
			Clipboard cp = tk.getSystemClipboard();
			cp.setContents(st, null);
		}

		if (e.getSource() == okButton) {
			formState = 1;
		}

		if (e.getSource() == cancelButton) {
			formState = 2;
		}
	}

	public String askAName(String prompt, String defVal) {
		return JOptionPane.showInputDialog(prompt, defVal);
	}


	/* It's gruiiicckk */
	private class IndexReuser implements Runnable {
		public IndexReuser() {

		}

		public void run() {
			String keys[];
			String publicKey = null;
			String privateKey = null;

			keys = askKeys(true);
			if (keys == null)
				return;

			publicKey = keys[0];
			privateKey = keys[1];

			try {
				publicKey = java.net.URLDecoder.decode(publicKey, "UTF-8");
			} catch(java.io.UnsupportedEncodingException exc) {
				Logger.warning(this, "UnsupportedEncodingException (UTF-8): "+exc.toString());
			}

			String name = Index.getNameFromKey(publicKey);
			
			IndexCategory parent;

			if (selectedNode != null && selectedNode instanceof IndexCategory)
				parent = (IndexCategory)selectedNode;
			else
				parent = root;

			Index index = new Index(db, queueManager, -2, parent, name, name, publicKey, privateKey, 0, null, modifiables);

			index.create();
			parent.insert(index.getTreeNode(), 0);
			
			treeModel.reload(parent);
		}
	}


	public String[] askKeys(boolean askPrivateKey) {
		//I18n.getMessage("thaw.plugin.index.indexKey")
		formState = 0;

		JFrame frame = new JFrame(I18n.getMessage("thaw.plugin.index.indexKey"));

		frame.getContentPane().setLayout(new GridLayout(askPrivateKey ? 3 : 2, 2));

		JTextField publicKeyField = new JTextField("USK@");
		JTextField privateKeyField = new JTextField("SSK@");

		frame.getContentPane().add(new JLabel(I18n.getMessage("thaw.plugin.index.indexKey")));
		frame.getContentPane().add(publicKeyField);

		if (askPrivateKey) {
			frame.getContentPane().add(new JLabel(I18n.getMessage("thaw.plugin.index.indexPrivateKey")));
			frame.getContentPane().add(privateKeyField);
		}

		cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
		okButton = new JButton(I18n.getMessage("thaw.common.ok"));

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);

		frame.getContentPane().add(cancelButton);
		frame.getContentPane().add(okButton);

		frame.setSize(500, 100);
		frame.setVisible(true);
		
		while(formState == 0) {
			try {
				Thread.sleep(500);
			} catch(InterruptedException e) {
				/* \_o< */
			}
		}

		frame.setVisible(false);

		if (formState == 2)
			return null;

		String[] keys = new String[2];

		keys[0] = publicKeyField.getText();
		if (askPrivateKey)
			keys[1] = privateKeyField.getText();
		else
			keys[1] = null;

		return keys;
	}

	public void save() {
		root.save();
	}


	public void update(java.util.Observable o, Object param) {
		if( (o instanceof Index)
		    && (param == null) ) {
			Index index = (Index)o;

			treeModel.nodeChanged(index.getTreeNode());
			if(index.getTreeNode().getParent() != null)
				treeModel.nodeChanged(index.getTreeNode().getParent());

		}
	}


	public class IndexTreeRenderer extends DefaultTreeCellRenderer {

		public IndexTreeRenderer() {
			super();
		}

		public java.awt.Component getTreeCellRendererComponent(JTree tree,
								       Object value,
								       boolean selected,
								       boolean expanded,
								       boolean leaf,
								       int row,
								       boolean hasFocus) {
			setBackgroundNonSelectionColor(Color.WHITE);

			if(value instanceof DefaultMutableTreeNode) {
				Object o = ((DefaultMutableTreeNode)value).getUserObject();

				if(o instanceof Index) {
					Index index = (Index)o;

					if(index.isUpdating()) {
						setBackgroundNonSelectionColor(Color.LIGHT_GRAY);
					}
				}
			}
			

			return super.getTreeCellRendererComponent(tree,
								  value,
								  selected,
								  expanded,
								  leaf,
								  row,
								  hasFocus);

		}
	}


	void addToRoot(IndexTreeNode node) {
		node.setParent(root);
		root.insert(node.getTreeNode(), root.getChildCount());
		reloadModel(root);
	}

	/**
	 * @param node can be null
	 */
	void reloadModel(DefaultMutableTreeNode node) {
		treeModel.reload(node);
	}

}
