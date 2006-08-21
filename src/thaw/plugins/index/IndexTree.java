package thaw.plugins.index;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JTree;
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

import thaw.plugins.Hsqldb;
import thaw.core.*;
import thaw.fcp.*;

import thaw.gui.JDragTree;

/**
 * Manages the index tree and its menu (right-click).
 */
public class IndexTree implements MouseListener, ActionListener, java.util.Observer {
	
	private JPanel panel;

	private JDragTree tree;
	private IndexCategory root;

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
	private JMenuItem copyKeys;
	private JMenuItem copyKey;
       
	private boolean modifiables;

	private IndexTreeNode selectedNode;

	private DefaultTreeModel treeModel;

	private Hsqldb db;
	private FCPQueueManager queueManager;

	/**
	 * Menu is defined according to the 'modifiables' parameters.
	 * @param modifiables If set to true, then only indexes having private keys will
	 *                    be displayed else only indexes not having private keys will
	 *                    be displayed.
	 */
	public IndexTree(String name,
			 boolean modifiables,
			 FCPQueueManager queueManager,
			 Hsqldb db) {
		this.queueManager = queueManager;

		this.db = db;
		this.modifiables = modifiables;

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

		copyKey = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKey"));
		copyKeys = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKey"));

		indexCategoryMenu.add(updateIndexCategory);
		indexCategoryMenu.add(addIndex);
		indexCategoryMenu.add(addIndexCategory);
		indexCategoryMenu.add(copyKeys);
		indexCategoryMenu.add(updateIndex);
		indexCategoryMenu.add(renameIndexCategory);
		indexCategoryMenu.add(deleteIndexCategory);

		indexMenu.add(updateIndex);
		indexMenu.add(copyKey);
		indexMenu.add(renameIndex);
		indexMenu.add(deleteIndex);

		addIndex.addActionListener(this);
		addIndexCategory.addActionListener(this);
		renameIndexCategory.addActionListener(this);
		deleteIndexCategory.addActionListener(this);
		updateIndexCategory.addActionListener(this);

		updateIndex.addActionListener(this);
		copyKey.addActionListener(this);
		copyKeys.addActionListener(this);
		renameIndex.addActionListener(this);
		deleteIndex.addActionListener(this);
		

		root = new IndexCategory(db, queueManager, -1, null, name, modifiables);
		root.loadChildren();

		root.addObserver(this);

		treeModel = new DefaultTreeModel(root);
		tree = new JDragTree(treeModel);
		tree.addMouseListener(this);

		IndexTreeRenderer treeRenderer = new IndexTreeRenderer();
		treeRenderer.setLeafIcon(IconBox.minIndex);

		tree.setCellRenderer(treeRenderer);

		panel.add(new JScrollPane(tree));
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


	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	public void mousePressed(MouseEvent e) {
		showPopupMenu(e);
	}

	public void mouseReleased(MouseEvent e) {
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

	public void actionPerformed(ActionEvent e) {
		if(selectedNode == null)
			return;

		if(e.getSource() == addIndex) {
			String name = null;

			String publicKey = null;

			if(!modifiables) {
				publicKey = askAName(I18n.getMessage("thaw.plugin.index.indexKey"), "USK@");

				try {
					publicKey = java.net.URLDecoder.decode(publicKey, "UTF-8");
				} catch(java.io.UnsupportedEncodingException exc) {
					Logger.warning(this, "UnsupportedEncodingException (UTF-8): "+exc.toString());
				}

				try {
					String[] cutcut = publicKey.split("/");
					name = cutcut[cutcut.length-2];
				} catch(Exception exc) {
					Logger.warning(this, "Error while parsing index key: "+publicKey+" because: "+exc.toString() );
					name = publicKey;
				}
			} else
				name = askAName(I18n.getMessage("thaw.plugin.index.indexName"),
					       I18n.getMessage("thaw.plugin.index.newIndex"));

			if(name == null)
				return;

			IndexCategory parent = (IndexCategory)selectedNode;

			Index index = new Index(db, queueManager, -2, parent, name, name, publicKey, null, 0, modifiables);

			if(modifiables)
				index.generateKeys(queueManager);

			index.create();
			parent.insert(index.getTreeNode(), 0);

			treeModel.reload(parent);
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

		if(e.getSource() == copyKey
		   || e.getSource() == copyKeys) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			StringSelection st = new StringSelection(selectedNode.getKey());
			Clipboard cp = tk.getSystemClipboard();
			cp.setContents(st, null);
		}

	}

	public String askAName(String prompt, String defVal) {
		return JOptionPane.showInputDialog(prompt, defVal);
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

}
