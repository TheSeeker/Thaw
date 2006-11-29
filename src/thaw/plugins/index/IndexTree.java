package thaw.plugins.index;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JTree;
import javax.swing.JMenu;
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

import java.util.Vector;
import java.util.Iterator;

import java.sql.*;


import thaw.plugins.Hsqldb;
import thaw.core.*;
import thaw.fcp.*;

import thaw.gui.JDragTree;

/**
 * Manages the index tree and its menu (right-click).
 */
public class IndexTree extends java.util.Observable implements MouseListener, ActionListener, java.util.Observer, javax.swing.event.TreeSelectionListener {

	public final static Color SELECTION_COLOR = new Color(190, 190, 190);
	public final static Color LOADING_COLOR = new Color(230, 230, 230);
	public final static Color LOADING_SELECTION_COLOR = new Color(150, 150, 150);

	private JPanel panel;

	private JTree tree;
	private IndexCategory root;

	private JPopupMenu indexCategoryMenu;
	private Vector indexCategoryActions; /* IndexManagementHelper.MenuAction */
	// downloadIndexes
	// createIndex
	// addIndex
	// addCategory
	// renameCategory
	// deleteCategory
	// copyKeys


	private JPopupMenu indexAndFileMenu; /* hem ... and links ... */
	private Vector indexAndFileActions; /* hem ... and links ... */ /* IndexManagementHelper.MenuAction */
	private JMenu indexMenu;
	// download
	// insert
	// renameIndex
	// delete
	// change keys
	// copy public key
	// copy private key

	private JMenu fileMenu;
	// addFileAndInsert
	// addFileWithoutInserting
	// addAKey

	private JMenu linkMenu;
	// addALink

	private boolean selectionOnly;

	private IndexTreeNode selectedNode = null;

	private DefaultTreeModel treeModel;

	private Hsqldb db;
	private FCPQueueManager queueManager;


	/**
	 * @param queueManager Not used if selectionOnly is set to true
	 */
	public IndexTree(String rootName,
			 boolean selectionOnly,
			 FCPQueueManager queueManager,
			 Hsqldb db) {
		this.queueManager = queueManager;

		this.db = db;
		this.selectionOnly = selectionOnly;

		panel = new JPanel();
		panel.setLayout(new BorderLayout(10, 10));


		root = new IndexCategory(db, queueManager, -1, null, rootName);
		root.loadChildren();

		root.addObserver(this);

		treeModel = new DefaultTreeModel(root);

		if (!selectionOnly) {
			tree = new JDragTree(treeModel);
			tree.addMouseListener(this);
		} else {
			tree = new JTree(treeModel);
			//tree.addMouseListener(this);
		}

		IndexTreeRenderer treeRenderer = new IndexTreeRenderer();
		treeRenderer.setLeafIcon(IconBox.minIndex);

		tree.setCellRenderer(treeRenderer);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);


		// Menus :

		JMenuItem item;


		indexCategoryMenu = new JPopupMenu(I18n.getMessage("thaw.plugin.index.category"));
		indexCategoryActions = new Vector();

		indexAndFileMenu = new JPopupMenu();
		indexAndFileActions = new Vector();
		indexMenu = new JMenu(I18n.getMessage("thaw.plugin.index.index"));
		fileMenu = new JMenu(I18n.getMessage("thaw.common.files"));
		linkMenu = new JMenu(I18n.getMessage("thaw.plugin.index.links"));


		// Category menu

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.downloadIndexes"));
		indexCategoryMenu.add(item);
		indexCategoryActions.add(new IndexManagementHelper.IndexDownloader(item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addAlreadyExistingIndex"));
		indexCategoryMenu.add(item);
		indexCategoryActions.add(new IndexManagementHelper.IndexReuser(db, queueManager, this, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addCategory"));
		indexCategoryMenu.add(item);
		indexCategoryActions.add(new IndexManagementHelper.IndexCategoryAdder(db, queueManager, this, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.createIndex"));
		indexCategoryMenu.add(item);
		indexCategoryActions.add(new IndexManagementHelper.IndexCreator(db, queueManager, this, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.rename"));
		indexCategoryMenu.add(item);
		indexCategoryActions.add(new IndexManagementHelper.IndexRenamer(this, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.delete"));
		indexCategoryMenu.add(item);
		indexCategoryActions.add(new IndexManagementHelper.IndexDeleter(this, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKeys"));
		indexCategoryMenu.add(item);
		indexCategoryActions.add(new IndexManagementHelper.PublicKeyCopier(item));


		// Index menu
		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.downloadIndex"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexDownloader(item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.insertIndex"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexUploader(item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.rename"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexRenamer(this, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.delete"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexDeleter(this, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKey"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.PublicKeyCopier(item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyPrivateKey"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.PrivateKeyCopier(item));


		// File menu

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addFilesWithInserting"));
		fileMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.FileInserterAndAdder(db, queueManager, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));
		fileMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.FileAdder(db, queueManager, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addKeys"));
		fileMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.KeyAdder(db, item));

		// Link menu
		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addLink"));
		linkMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.LinkAdder(db, item));

		indexAndFileMenu.add(indexMenu);
		indexAndFileMenu.add(fileMenu);
		indexAndFileMenu.add(linkMenu);

		updateMenuState(null);

		addTreeSelectionListener(this);

		panel.add(new JScrollPane(tree), BorderLayout.CENTER);
	}



	public javax.swing.JComponent getPanel() {
		return this.panel;
	}

	public void addTreeSelectionListener(javax.swing.event.TreeSelectionListener tsl) {
		tree.addTreeSelectionListener(tsl);
	}

	public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
		TreePath path = e.getPath();

		if(path == null)
			return;

		selectedNode = (IndexTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();

		setChanged();
		notifyObservers(selectedNode);
	}


	public void updateMenuState(IndexTreeNode node) {
		IndexManagementHelper.IndexAction action;

		for(Iterator it = indexCategoryActions.iterator();
		    it.hasNext();) {
			action = (IndexManagementHelper.IndexAction)it.next();
			action.setTarget(node);
		}

		for(Iterator it = indexAndFileActions.iterator();
		    it.hasNext();) {
			action = (IndexManagementHelper.IndexAction)it.next();
			action.setTarget(node);
		}
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
			if(selectedNode == null)
				return;

			if(selectedNode instanceof IndexCategory) {
				updateMenuState(selectedNode);
				indexCategoryMenu.show(e.getComponent(), e.getX(), e.getY());
			}

			if(selectedNode instanceof Index) {
				updateMenuState(selectedNode);
				indexAndFileMenu.show(e.getComponent(), e.getX(), e.getY());
			}
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
		if(selectedNode == null)
			selectedNode = root;
	}

	public void save() {
		root.save();
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
		return addToIndexCategory(root, node);
	}

	public boolean addToIndexCategory(IndexCategory target, IndexTreeNode node) {
		if ((node instanceof Index) && alreadyExistingIndex(node.getPublicKey())) {
			Logger.notice(this, "Index already added");
			return false;
		}

		node.setParent(target);
		target.getTreeNode().insert(node.getTreeNode(), target.getTreeNode().getChildCount());
		treeModel.reload(target);

		return true;
	}


	public boolean alreadyExistingIndex(String key) {
		int maxLength = 0;

		if (key == null || key.length() <= 10)
			return false;

		if (key.length() <= 60)
			maxLength = key.length();
		else
			maxLength = 60;

		String realKey = key.substring(0, maxLength).toLowerCase();

		try {
			Connection c = this.db.getConnection();
			PreparedStatement st;

			String query;

			query = "SELECT id FROM indexes WHERE LOWER(publicKey) LIKE ?";


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

	public void reloadModel() {
		this.treeModel.reload();
	}

}
