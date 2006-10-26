package thaw.plugins.index;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import javax.swing.JScrollPane;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import java.awt.Color;

import javax.swing.JFrame;

import javax.swing.JToolBar;
import javax.swing.JButton;

import javax.swing.JTextField;
import javax.swing.JLabel;


import java.sql.*;


import thaw.plugins.Hsqldb;
import thaw.core.*;
import thaw.fcp.*;

import thaw.gui.JDragTree;

/**
 * Manages the index tree and its menu (right-click).
 */
public class IndexTree extends java.util.Observable implements MouseListener, ActionListener, java.util.Observer {

	public final static Color SELECTION_COLOR = new Color(190, 190, 190);
	public final static Color LOADING_COLOR = new Color(230, 230, 230);
	public final static Color LOADING_SELECTION_COLOR = new Color(150, 150, 150);
	
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

		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout(10, 10));
		
		this.indexCategoryMenu = new JPopupMenu();
		this.indexMenu = new JPopupMenu();

		if(modifiables)
			this.addIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.createIndex"));
		else
			this.addIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.addIndex"));

		this.addIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.addCategory"));
		this.renameIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.rename"));
		this.renameIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.rename"));
		this.deleteIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.delete"));
		this.deleteIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.delete"));
		
		if(modifiables) {
			this.updateIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.insertIndex"));
			this.updateIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.insertIndexes"));
		} else {
			this.updateIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.downloadIndex"));
			this.updateIndexCategory = new JMenuItem(I18n.getMessage("thaw.plugin.index.downloadIndexes"));
		}

		this.copyPublicKey = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKey"));
		this.copyPublicKeys = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKeys"));

		if (modifiables) {
			this.copyPrivateKey = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyPrivateKey"));
			this.copyPrivateKeys = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyPrivateKey"));
			this.copyPrivateKey.addActionListener(this);
			this.copyPrivateKeys.addActionListener(this);

			this.reloadFromFreenet = new JMenuItem(I18n.getMessage("thaw.plugin.index.reloadFromFreenet"));
			this.reloadFromFreenet.addActionListener(this);
		}

		this.indexCategoryMenu.add(this.updateIndexCategory);
		this.indexCategoryMenu.add(this.addIndex);
		this.indexCategoryMenu.add(this.addIndexCategory);
		this.indexCategoryMenu.add(this.copyPublicKeys);
		this.indexCategoryMenu.add(this.updateIndex);
		this.indexCategoryMenu.add(this.renameIndexCategory);
		this.indexCategoryMenu.add(this.deleteIndexCategory);
		if (modifiables) {
			this.indexCategoryMenu.add(this.copyPrivateKeys);
		}

		this.indexMenu.add(this.updateIndex);
		this.indexMenu.add(this.copyPublicKey);
		this.indexMenu.add(this.renameIndex);
		this.indexMenu.add(this.deleteIndex);
		if (modifiables) {
			this.indexMenu.add(this.copyPrivateKey);
			this.indexMenu.add(this.reloadFromFreenet);
		}

		this.addIndex.addActionListener(this);
		this.addIndexCategory.addActionListener(this);
		this.renameIndexCategory.addActionListener(this);
		this.deleteIndexCategory.addActionListener(this);
		this.updateIndexCategory.addActionListener(this);

		this.updateIndex.addActionListener(this);
		this.copyPublicKey.addActionListener(this);
		this.copyPublicKeys.addActionListener(this);
		this.renameIndex.addActionListener(this);
		this.deleteIndex.addActionListener(this);
		

		this.root = new IndexCategory(db, queueManager, -1, null, rootName, modifiables);
		this.root.loadChildren();

		this.root.addObserver(this);

		this.treeModel = new DefaultTreeModel(this.root);

		if (!selectionOnly) {
			this.tree = new JDragTree(this.treeModel);
			this.tree.addMouseListener(this);
		} else {
			this.tree = new JTree(this.treeModel);
			this.tree.addMouseListener(this);
		}

		IndexTreeRenderer treeRenderer = new IndexTreeRenderer();
		treeRenderer.setLeafIcon(IconBox.minIndex);

		this.tree.setCellRenderer(treeRenderer);

		//if (selectionOnly)
		this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		this.toolBar = new JToolBar();

		this.newIndex   = new JButton(IconBox.indexNew);
		if (!modifiables)
			this.newIndex.setToolTipText(I18n.getMessage("thaw.plugin.index.addIndex"));
		else
			this.newIndex.setToolTipText(I18n.getMessage("thaw.plugin.index.createIndex"));
		this.newIndex.addActionListener(this);


		if (!modifiables) {
			this.refreshAll = new JButton(IconBox.refreshAction);
			this.refreshAll.setToolTipText(I18n.getMessage("thaw.plugin.index.downloadIndexes"));
			this.refreshAll.addActionListener(this);
		} else {
			this.reuseIndex = new JButton(IconBox.indexReuse);
			this.reuseIndex.setToolTipText(I18n.getMessage("thaw.plugin.index.addAlreadyExisting"));
			this.reuseIndex.addActionListener(this);
		}


		this.toolBar.add(this.newIndex);
		if (!modifiables)
			this.toolBar.add(this.refreshAll);
		else
			this.toolBar.add(this.reuseIndex);

		if (!selectionOnly)
			this.panel.add(this.toolBar, BorderLayout.NORTH);
		this.panel.add(new JScrollPane(this.tree), BorderLayout.CENTER);
	}


	public javax.swing.JComponent getPanel() {
		return this.panel;
	}

	public void addTreeSelectionListener(javax.swing.event.TreeSelectionListener tsl) {
		this.tree.addTreeSelectionListener(tsl);
	}


	public JTree getTree() {
		return this.tree;
	}

	public IndexCategory getRoot() {
		return this.root;
	}

	public void mouseClicked(MouseEvent e) {
		this.notifySelection(e);
	}

	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	public void mousePressed(MouseEvent e) {
		if (!this.selectionOnly)
			this.showPopupMenu(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (!this.selectionOnly)
			this.showPopupMenu(e);
	}

	protected void showPopupMenu(MouseEvent e) {
		if(e.isPopupTrigger()) {
			TreePath path = this.tree.getPathForLocation(e.getX(), e.getY());
			
			if(path == null)
				return;

			this.selectedNode = (IndexTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();

			if(this.selectedNode == null)
				return;

			if(this.selectedNode instanceof IndexCategory)
				this.indexCategoryMenu.show(e.getComponent(), e.getX(), e.getY());
			
			if(this.selectedNode instanceof Index)
				this.indexMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void notifySelection(MouseEvent e) {
		TreePath path = this.tree.getPathForLocation(e.getX(), e.getY());
			
		if(path == null)
			return;

		this.selectedNode = (IndexTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();

		this.setChanged();
		this.notifyObservers(this.selectedNode);
	}

	public IndexTreeNode getSelectedNode() {
		Object obj = this.tree.getLastSelectedPathComponent();

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
		if(this.selectedNode == null)
			this.selectedNode = this.root;

		if(e.getSource() == this.addIndex
		   || e.getSource() == this.newIndex) {
			String name = null;

			String publicKey = null;

			if(!this.modifiables) {
				publicKey = this.askAName(I18n.getMessage("thaw.plugin.index.indexKey"), "USK@");

				if (publicKey == null)
					return;

				publicKey = FreenetURIHelper.cleanURI(publicKey);

				name = Index.getNameFromKey(publicKey);

			} else {
				name = this.askAName(I18n.getMessage("thaw.plugin.index.indexName"),
						I18n.getMessage("thaw.plugin.index.newIndex"));				
			}

			if(name == null)
				return;

			IndexCategory parent;

			if (this.selectedNode != null && (this.selectedNode instanceof IndexCategory))
				parent = (IndexCategory)this.selectedNode;
			else
				parent = this.root;

			Index index = new Index(this.db, this.queueManager, -2, parent, name, name, publicKey, null, 0, null, this.modifiables);

			if(this.modifiables)
				index.generateKeys(this.queueManager);

			index.create();

			if (!this.modifiables)
				index.update();

			parent.insert(index.getTreeNode(), 0);

			this.treeModel.reload(parent);
		}


		if (e.getSource() == this.reuseIndex) {
			Thread newThread = new Thread(new IndexReuser());

			newThread.start();
		}



		if(e.getSource() == this.addIndexCategory) {
			String name = this.askAName(I18n.getMessage("thaw.plugin.index.categoryName"), 
					       I18n.getMessage("thaw.plugin.index.newCategory"));

			if(name == null)
				return;

			IndexCategory parent = (IndexCategory)this.selectedNode;

			/* the id will be defined when created */
			IndexCategory cat = new IndexCategory(this.db, this.queueManager, -2, parent,
							      name, this.modifiables);
			cat.create();
			parent.insert(cat, 0);

			this.treeModel.reload(parent);
		}


		if(e.getSource() == this.renameIndexCategory
		   || e.getSource() == this.renameIndex) {
			
			String newName;

			if(e.getSource() == this.renameIndexCategory)
				newName = this.askAName(I18n.getMessage("thaw.plugin.index.categoryName"), 
						   this.selectedNode.toString());
			else
				newName = this.askAName(I18n.getMessage("thaw.plugin.index.indexName"), 
						   this.selectedNode.toString());

			if(newName == null)
				return;

			this.selectedNode.rename(newName);

			this.treeModel.reload(this.selectedNode.getTreeNode());
		}

		if(e.getSource() == this.deleteIndexCategory
		   || e.getSource() == this.deleteIndex) {

			MutableTreeNode parent = (MutableTreeNode)this.selectedNode.getTreeNode().getParent();

			if(parent != null)
				parent.remove(this.selectedNode.getTreeNode());

			this.selectedNode.delete();

			if(parent != null)
				this.treeModel.reload(parent);
			else
				this.treeModel.reload();
		}

		if(e.getSource() == this.updateIndex
		   || e.getSource() == this.updateIndexCategory) {
			this.selectedNode.update();
		}

		if (e.getSource() == this.reloadFromFreenet) {
			this.selectedNode.updateFromFreenet(-1);
		}
		
		if (e.getSource() == this.refreshAll) {
			this.root.update();
		}

		if(e.getSource() == this.copyPublicKey
		   || e.getSource() == this.copyPublicKeys) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			StringSelection st = new StringSelection(this.selectedNode.getPublicKey());
			Clipboard cp = tk.getSystemClipboard();
			cp.setContents(st, null);
		}

		
		if(e.getSource() == this.copyPrivateKey
		   || e.getSource() == this.copyPrivateKeys) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			StringSelection st = new StringSelection(this.selectedNode.getPrivateKey());
			Clipboard cp = tk.getSystemClipboard();
			cp.setContents(st, null);
		}

		if (e.getSource() == this.okButton) {
			this.formState = 1;
		}

		if (e.getSource() == this.cancelButton) {
			this.formState = 2;
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

			keys = IndexTree.this.askKeys(true);
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

			if (IndexTree.this.selectedNode != null && IndexTree.this.selectedNode instanceof IndexCategory)
				parent = (IndexCategory)IndexTree.this.selectedNode;
			else
				parent = IndexTree.this.root;

			Index index = new Index(IndexTree.this.db, IndexTree.this.queueManager, -2, parent, name, name, publicKey, privateKey, 0, null, IndexTree.this.modifiables);

			index.create();
			
			index.updateFromFreenet(-1);

			parent.insert(index.getTreeNode(), 0);
			
			IndexTree.this.treeModel.reload(parent);
		}
	}


	public String[] askKeys(boolean askPrivateKey) {
		//I18n.getMessage("thaw.plugin.index.indexKey")
		this.formState = 0;

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

		this.cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
		this.okButton = new JButton(I18n.getMessage("thaw.common.ok"));

		this.cancelButton.addActionListener(this);
		this.okButton.addActionListener(this);

		frame.getContentPane().add(this.cancelButton);
		frame.getContentPane().add(this.okButton);

		frame.setSize(500, 100);
		frame.setVisible(true);
		
		while(this.formState == 0) {
			try {
				Thread.sleep(500);
			} catch(InterruptedException e) {
				/* \_o< */
			}
		}

		frame.setVisible(false);

		if (this.formState == 2)
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
		this.root.save();
	}


	public void update(java.util.Observable o, Object param) {
		if( (o instanceof Index)
		    && (param == null) ) {
			Index index = (Index)o;

			this.treeModel.nodeChanged(index.getTreeNode());
			if(index.getTreeNode().getParent() != null)
				this.treeModel.nodeChanged(index.getTreeNode().getParent());

		}
	}


	public class IndexTreeRenderer extends DefaultTreeCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

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
			this.setBackgroundNonSelectionColor(Color.WHITE);
			this.setBackgroundSelectionColor(SELECTION_COLOR);

			if(value instanceof DefaultMutableTreeNode) {
				Object o = ((DefaultMutableTreeNode)value).getUserObject();

				if(o instanceof Index) {
					Index index = (Index)o;

					if(index.isUpdating()) {
						this.setBackgroundNonSelectionColor(LOADING_COLOR);
						this.setBackgroundSelectionColor(LOADING_SELECTION_COLOR);
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


	public boolean addToRoot(IndexTreeNode node) {
		if (this.alreadyExistingIndex(node.getPublicKey())) {
			Logger.notice(this, "Index already added");
			return false;
		}

		node.setParent(this.root);
		this.root.insert(node.getTreeNode(), this.root.getChildCount());
		this.reloadModel(this.root);

		return true;
	}

	

	public boolean alreadyExistingIndex(String key) {
		String realKey = key.substring(0, 60).toLowerCase();

		try {
			Connection c = this.db.getConnection();
			PreparedStatement st;

			String query;

			query = "SELECT id FROM indexes WHERE LOWER(publicKey) LIKE ?";

			
			if (this.modifiables)
				query = query + " AND privateKey IS NOT NULL;";
			else
				query = query + " AND privateKey IS NULL;";
			

			Logger.info(this, query + " : " + realKey+"%");

			st = c.prepareStatement(query);

			st.setString(1, realKey+"%");

			if (st.execute()) {
				ResultSet results = st.getResultSet();

				if (results.next()) {
					return true;
				}
			}


		} catch(java.sql.SQLException e) {
			Logger.warning(this, "Exception while trying to check if '"+key+"' is already know: '"+e.toString()+"'");
		}

		return false;
	}



	/**
	 * @param node can be null
	 */
	public void reloadModel(DefaultMutableTreeNode node) {
		this.treeModel.reload(node);
	}

}
