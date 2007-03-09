package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import thaw.core.Config;
import thaw.core.FreenetURIHelper;
import thaw.core.I18n;
import thaw.gui.IconBox;
import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;
import thaw.gui.JDragTree;
import thaw.plugins.ToolbarModifier;

/**
 * Manages the index tree and its menu (right-click).
 */
public class IndexTree extends java.util.Observable implements MouseListener, ActionListener, javax.swing.event.TreeSelectionListener {

	public final static Color SELECTION_COLOR = new Color(190, 190, 190);
	public final static Color LOADING_COLOR = new Color(230, 230, 230);
	public final static Color LOADING_SELECTION_COLOR = new Color(150, 150, 150);

	private JPanel panel;

	private JTree tree;
	private IndexRoot root;

	private JPopupMenu indexFolderMenu;
	private Vector indexFolderActions; /* IndexManagementHelper.MenuAction */
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

	private FCPQueueManager queueManager;
	private IndexBrowserPanel indexBrowser;

	private ToolbarModifier toolbarModifier;
	private Vector toolbarActions;


	private Vector updatingIndexes;


	/**
	 * @param queueManager Not used if selectionOnly is set to true
	 * @param config Not used if selectionOnly is set to true (used for lastDestinationDirectory and lastSourceDirectory)
	 */
	public IndexTree(final String rootName, boolean selectionOnly,
			 final FCPQueueManager queueManager,
			 final IndexBrowserPanel indexBrowser,
			 final Config config) {
		updatingIndexes = new Vector();

		this.queueManager = queueManager;
		this.selectionOnly = selectionOnly;

		panel = new JPanel();
		panel.setLayout(new BorderLayout(10, 10));

		boolean loadOnTheFly = false;

		if (config.getValue("loadIndexTreeOnTheFly") != null)
			loadOnTheFly = Boolean.valueOf(config.getValue("loadIndexTreeOnTheFly")).booleanValue();

		root = new IndexRoot(queueManager, indexBrowser, rootName, loadOnTheFly);

		treeModel = new DefaultTreeModel(root);

		if (!selectionOnly) {
			tree = new JDragTree(treeModel);
			tree.addMouseListener(this);
		} else {
			tree = new JTree(treeModel);
			//tree.addMouseListener(this);
		}

		final IndexTreeRenderer treeRenderer = new IndexTreeRenderer();
		treeRenderer.setLeafIcon(IconBox.minIndexReadOnly);

		tree.setCellRenderer(treeRenderer);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setExpandsSelectedPaths(true);

		// Menus :

		JMenuItem item;


		indexFolderMenu = new JPopupMenu(I18n.getMessage("thaw.plugin.index.category"));
		indexFolderActions = new Vector();

		indexAndFileMenu = new JPopupMenu();
		indexAndFileActions = new Vector();
		indexMenu = new JMenu(I18n.getMessage("thaw.plugin.index.index"));
		fileMenu = new JMenu(I18n.getMessage("thaw.common.files"));
		linkMenu = new JMenu(I18n.getMessage("thaw.plugin.index.links"));


		// Folder menu

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.downloadIndexes"));
		indexFolderMenu.add(item);
		indexFolderActions.add(new IndexManagementHelper.IndexDownloader(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.markAllAsSeen"));
		indexFolderMenu.add(item);
		indexFolderActions.add(new IndexManagementHelper.IndexHasChangedFlagReseter(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.sortAlphabetically"));
		indexFolderMenu.add(item);
		indexFolderActions.add(new IndexManagementHelper.IndexFolderReorderer(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addAlreadyExistingIndex"));
		indexFolderMenu.add(item);
		indexFolderActions.add(new IndexManagementHelper.IndexReuser(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addCategory"));
		indexFolderMenu.add(item);
		indexFolderActions.add(new IndexManagementHelper.IndexFolderAdder(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.createIndex"));
		indexFolderMenu.add(item);
		indexFolderActions.add(new IndexManagementHelper.IndexCreator(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.rename"));
		indexFolderMenu.add(item);
		indexFolderActions.add(new IndexManagementHelper.IndexRenamer(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.delete"));
		indexFolderMenu.add(item);
		indexFolderActions.add(new IndexManagementHelper.IndexDeleter(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKeys"));
		indexFolderMenu.add(item);
		indexFolderActions.add(new IndexManagementHelper.PublicKeyCopier(item));


		// Index menu
		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.downloadIndex"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexDownloader(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.insertIndex"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexUploader(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.rename"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexRenamer(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.exportIndex"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexExporter(item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.importIndex"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexImporter(item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.delete"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexDeleter(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.changeIndexKeys"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.IndexKeyModifier(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyPrivateKey"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.PrivateKeyCopier(item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKey"));
		indexMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.PublicKeyCopier(item));


		// File menu

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addFilesWithInserting"));
		fileMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.FileInserterAndAdder(config, queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));
		fileMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.FileAdder(config, queueManager, indexBrowser, item));


		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addKeys"));
		fileMenu.add(item);
		IndexManagementHelper.IndexAction ac = new IndexManagementHelper.KeyAdder(indexBrowser, item);
		indexAndFileActions.add(ac);


		// Link menu
		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addLink"));
		linkMenu.add(item);
		indexAndFileActions.add(new IndexManagementHelper.LinkAdder(indexBrowser, item));

		indexAndFileMenu.add(indexMenu);
		indexAndFileMenu.add(fileMenu);
		indexAndFileMenu.add(linkMenu);

		updateMenuState(null);

		addTreeSelectionListener(this);

		panel.add(new JScrollPane(tree), BorderLayout.CENTER);


		// Toolbar
		JButton button;
		IndexManagementHelper.IndexAction action;
		toolbarActions = new Vector();

		toolbarModifier = new ToolbarModifier(indexBrowser.getMainWindow());

		button = new JButton(IconBox.refreshAction);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.downloadIndexes"));
		action = new IndexManagementHelper.IndexDownloader(queueManager, indexBrowser, button);
		action.setTarget(getRoot());
		toolbarModifier.addButtonToTheToolbar(button);
		toolbarActions.add(action);

		button = new JButton(IconBox.folderNew);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.addCategory"));
		action = new IndexManagementHelper.IndexFolderAdder(indexBrowser, button);
		action.setTarget(getRoot());
		toolbarModifier.addButtonToTheToolbar(button);
		toolbarActions.add(action);

		button = new JButton(IconBox.indexReuse);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.addAlreadyExistingIndex"));
		action = new IndexManagementHelper.IndexReuser(queueManager, indexBrowser, button);
		action.setTarget(getRoot());
		toolbarModifier.addButtonToTheToolbar(button);
		toolbarActions.add(action);

		button = new JButton(IconBox.indexNew);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.createIndex"));
		action = new IndexManagementHelper.IndexCreator(queueManager, indexBrowser, button);
		action.setTarget(getRoot());
		toolbarModifier.addButtonToTheToolbar(button);
		toolbarActions.add(action);

		button = new JButton(IconBox.key);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.changeIndexKeys"));
		action = new IndexManagementHelper.IndexKeyModifier(indexBrowser, button);
		action.setTarget(getRoot());
		toolbarModifier.addButtonToTheToolbar(button);
		toolbarActions.add(action);

		button = new JButton(IconBox.delete);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.delete"));
		action = new IndexManagementHelper.IndexDeleter(indexBrowser, button);
		action.setTarget(getRoot());
		toolbarModifier.addButtonToTheToolbar(button);
		toolbarActions.add(action);


		toolbarModifier.addButtonToTheToolbar(null);

		button = new JButton(IconBox.addToIndexAction);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));
		action = new IndexManagementHelper.FileAdder(config, queueManager, indexBrowser, button);
		action.setTarget(null);
		toolbarModifier.addButtonToTheToolbar(button);
		toolbarActions.add(action);

		button = new JButton(IconBox.makeALinkAction);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.addLink"));
		action = new IndexManagementHelper.LinkAdder(indexBrowser, button);
		action.setTarget(getRoot());
		toolbarModifier.addButtonToTheToolbar(button);
		toolbarActions.add(action);
	}



	/**
	 * Used by IndexBrowserPanel when the visibility changed
	 */
	public ToolbarModifier getToolbarModifier() {
		return toolbarModifier;
	}


	public javax.swing.JComponent getPanel() {
		return panel;
	}

	public void addTreeSelectionListener(final javax.swing.event.TreeSelectionListener tsl) {
		tree.addTreeSelectionListener(tsl);
	}

	public void valueChanged(final javax.swing.event.TreeSelectionEvent e) {
		final TreePath path = e.getPath();

		if(path == null)
			return;

		selectedNode = (IndexTreeNode)(path.getLastPathComponent());


		// Update toolbar
		for (final Iterator it = toolbarActions.iterator();
		     it.hasNext(); ) {
			final IndexManagementHelper.IndexAction action = (IndexManagementHelper.IndexAction)it.next();
			action.setTarget(selectedNode);
		}


		// Notify observers

		setChanged();
		notifyObservers(selectedNode); /* will make the toolbar visible */
	}


	public void updateMenuState(final IndexTreeNode node) {
		IndexManagementHelper.IndexAction action;

		for(final Iterator it = indexFolderActions.iterator();
		    it.hasNext();) {
			action = (IndexManagementHelper.IndexAction)it.next();
			action.setTarget(node);
		}

		for(final Iterator it = indexAndFileActions.iterator();
		    it.hasNext();) {
			action = (IndexManagementHelper.IndexAction)it.next();
			action.setTarget(node);
		}
	}


	public JTree getTree() {
		return tree;
	}

	public IndexRoot getRoot() {
		return root;
	}

	public void mouseClicked(final MouseEvent e) {
		notifySelection(e);
	}

	public void mouseEntered(final MouseEvent e) { }
	public void mouseExited(final MouseEvent e) { }

	public void mousePressed(final MouseEvent e) {
		if (!selectionOnly)
			showPopupMenu(e);
	}

	public void mouseReleased(final MouseEvent e) {
		if (!selectionOnly)
			showPopupMenu(e);
	}

	protected void showPopupMenu(final MouseEvent e) {
		if(e.isPopupTrigger()) {
			if(selectedNode == null)
				return;

			if(selectedNode instanceof IndexFolder) {
				updateMenuState(selectedNode);
				indexFolderMenu.show(e.getComponent(), e.getX(), e.getY());
			}

			if(selectedNode instanceof Index) {
				updateMenuState(selectedNode);
				indexAndFileMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	public void notifySelection(final MouseEvent e) {
		final TreePath path = tree.getPathForLocation(e.getX(), e.getY());

		if(path == null)
			return;

		selectedNode = (IndexTreeNode)(path.getLastPathComponent());

		if (selectedNode != null) {
			if ((indexBrowser != null) && (selectedNode instanceof Index)) {
				indexBrowser.getUnknownIndexList().addLinks(((Index)selectedNode));
			}

			if (selectedNode instanceof Index) {
				if (((Index)selectedNode).hasChanged()) {
					((Index)selectedNode).setHasChangedFlag(false);
					redraw(path);
				}
			}

		}

		toolbarModifier.displayButtonsInTheToolbar();

		setChanged();
		notifyObservers(selectedNode);
	}

	public IndexTreeNode getSelectedNode() {
		final Object obj = tree.getLastSelectedPathComponent();

		if (obj == null)
			return null;

		if (obj instanceof IndexTreeNode)
			return (IndexTreeNode)obj;

		if (obj instanceof DefaultMutableTreeNode)
			return ((IndexTreeNode)(((DefaultMutableTreeNode)obj).getUserObject()));

		Logger.notice(this, "getSelectedNode(): Unknow kind of node ?!");

		return null;
	}


	public void actionPerformed(final ActionEvent e) {
		if(selectedNode == null)
			selectedNode = root;
	}


	private boolean forceHasChangedFlagReload = false;


	public void refresh() {
		refresh(((IndexTreeNode)null));
	}


	public void refresh(IndexTreeNode node) {
		forceHasChangedFlagReload = true;

		if (treeModel != null) {
			if (node != null && node.isInTree())
				treeModel.reload(node.getTreeNode());
			else
				treeModel.reload(getRoot().getTreeNode());
		}

		forceHasChangedFlagReload = false;
	}


	public void refresh(TreePath path) {
		refresh();
	}


	public void redraw() {
		redraw((IndexTreeNode)null);
	}

	public void redraw(IndexTreeNode node, boolean parents) {
		if (!parents)
			redraw(node);
		else {
			while (node != null) {
				redraw(node);
				node = ((IndexTreeNode)(((MutableTreeNode)node).getParent()));
			}
		}
	}

	public void redraw(IndexTreeNode node) {
		//refresh(node);
		forceHasChangedFlagReload = true;
		if (treeModel != null) {
			if (node != null && node.isInTree())
				treeModel.nodeChanged(node.getTreeNode());
			else
				treeModel.nodeChanged(getRoot().getTreeNode());
		}
		forceHasChangedFlagReload = false;
	}

	public void redraw(TreePath path) {
		Object[] nodes = (path.getPath());

		for (int i = 0 ; i < nodes.length ; i++) {
			IndexTreeNode node = (IndexTreeNode)nodes[i];
			redraw(node);
		}
	}

	public class IndexTreeRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = 1L;

		public IndexTreeRenderer() {
			super();
		}

		public java.awt.Component getTreeCellRendererComponent(final JTree tree,
								       final Object value,
								       final boolean selected,
								       final boolean expanded,
								       final boolean leaf,
								       final int row,
								       final boolean hasFocus) {
			setBackgroundNonSelectionColor(Color.WHITE);
			setBackgroundSelectionColor(IndexTree.SELECTION_COLOR);

			if(value instanceof DefaultMutableTreeNode || value instanceof IndexTreeNode) {
				Object o;

				if (value instanceof DefaultMutableTreeNode)
					o = ((DefaultMutableTreeNode)value).getUserObject();
				else
					o = value;

				if(o instanceof Index) {
					final Index index = (Index)o;

					if (isIndexUpdating(index)) {
						setBackgroundNonSelectionColor(IndexTree.LOADING_COLOR);
						setBackgroundSelectionColor(IndexTree.LOADING_SELECTION_COLOR);
					}

					if (index.isModifiable()) {
						setLeafIcon(IconBox.minIndex);
					} else {
						setLeafIcon(IconBox.minIndexReadOnly);
					}

				}

				if (o instanceof IndexTreeNode) {
					/* Remember that for the index category,
					   this kind of query is recursive */
					boolean modifiable = ((IndexTreeNode)o).isModifiable();

					if (forceHasChangedFlagReload)
						((IndexTreeNode)o).forceHasChangedReload();

					boolean hasChanged = ((IndexTreeNode)o).hasChanged();

					int style = 0;

					if (modifiable)
						style |= Font.ITALIC;
					if (hasChanged)
						style |= Font.BOLD;

					if (style == 0)
						style = Font.PLAIN;

					setFont(new Font("Dialog", style, 12));
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


	public boolean addToRoot(final IndexTreeNode node) {
		return addToIndexFolder(root, node);
	}

	public boolean addToIndexFolder(final IndexFolder target, final IndexTreeNode node) {
		if ((node instanceof Index) && alreadyExistingIndex(node.getPublicKey())) {
			Logger.notice(this, "Index already added");
			return false;
		}

		node.setParent(target.getId());
		target.getTreeNode().insert(node.getTreeNode(), target.getTreeNode().getChildCount());
		treeModel.reload(target);

		return true;
	}


	public boolean alreadyExistingIndex(final String key) {
		if ((key == null) || (key.length() <= 10))
			return false;

		String realKey = FreenetURIHelper.getComparablePart(key);

		try {
			final Connection c = indexBrowser.getDb().getConnection();
			PreparedStatement st;

			String query;

			query = "SELECT id FROM indexes WHERE LOWER(publicKey) LIKE ?";


			Logger.info(this, query + " : " + realKey+"%");

			st = c.prepareStatement(query);

			st.setString(1, realKey+"%");

			if (st.execute()) {
				final ResultSet results = st.getResultSet();

				if (results.next())
					return true;
			}


		} catch(final java.sql.SQLException e) {
			Logger.warning(this, "Exception while trying to check if '"+key+"' is already know: '"+e.toString()+"'");
		}

		return false;
	}



	/**
	 * @param node can be null
	 */
	public void reloadModel(final MutableTreeNode node) {
		treeModel.reload(node);
	}


	public void reloadModel() {
		treeModel.reload();
	}


	/* TODO : Improve this ; quite ugly */


	public void addUpdatingIndex(Index index) {
		updatingIndexes.add(new Integer(index.getId()));
	}

	public void removeUpdatingIndex(Index index) {
		updatingIndexes.remove(new Integer(index.getId()));
	}

	public boolean isIndexUpdating(Index index) {
		return (updatingIndexes.indexOf(new Integer(index.getId())) >= 0);
	}
}
