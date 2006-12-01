package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import javax.swing.event.TableModelEvent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JLabel;

import javax.swing.tree.TreePath;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import java.util.Vector;
import java.util.Iterator;

import java.util.Observable;
import thaw.core.*;
import thaw.fcp.*;

import thaw.plugins.Hsqldb;


public class LinkTable implements MouseListener, KeyListener, ActionListener {

	private JPanel panel;
	private JTable table;

	private LinkListModel linkListModel = null;
	private LinkList      linkList = null;

	private FCPQueueManager queueManager;

	private IndexTree indexTree;
	private Hsqldb db;
	private Tables tables;

	private JPopupMenu rightClickMenu;
	private Vector rightClickActions;
	private JMenuItem gotoItem;

	private int[] selectedRows;

	public LinkTable (Hsqldb db, FCPQueueManager queueManager, IndexTree tree, Tables tables) {
		this.queueManager = queueManager;
		this.db = db;

		this.linkListModel = new LinkListModel();
		this.table = new JTable(this.linkListModel);
		this.table.setShowGrid(true);

		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout());

		this.panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.linkList")), BorderLayout.NORTH);
		this.panel.add(new JScrollPane(this.table));

		this.table.addMouseListener(this);

		this.indexTree = tree;
		this.tables = tables;

		rightClickMenu = new JPopupMenu();
		rightClickActions = new Vector();

		JMenuItem item;

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addIndexesFromLink"));
		rightClickMenu.add(item);
		rightClickActions.add(new LinkManagementHelper.IndexAdder(item, db, queueManager, tree));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKeys"));
		rightClickMenu.add(item);
		rightClickActions.add(new LinkManagementHelper.PublicKeyCopier(item));

		item = new JMenuItem(I18n.getMessage("thaw.common.remove"));
		rightClickMenu.add(item);
		rightClickActions.add(new LinkManagementHelper.LinkRemover(item));

		gotoItem = new JMenuItem(I18n.getMessage("thaw.plugin.index.gotoIndex"));
		rightClickMenu.add(gotoItem);
		gotoItem.addActionListener(this);

		updateRightClickMenu(null);
	}

	public JPanel getPanel() {
		return this.panel;
	}

	protected void updateRightClickMenu(Vector selectedLinks) {
		LinkManagementHelper.LinkAction action;

		for (Iterator it = rightClickActions.iterator();
		     it.hasNext(); ) {
			action = (LinkManagementHelper.LinkAction)it.next();
			action.setTarget(selectedLinks);
		}

		gotoItem.setEnabled(linkList != null && !(linkList instanceof Index));
	}

	protected Vector getSelectedLinks(int[] selectedRows) {
		Vector srcList = linkList.getLinkList();
		Vector links = new Vector();

		for(int i = 0 ; i < selectedRows.length ; i++) {
			Link link = (Link)srcList.get(selectedRows[i]);
			links.add(link);
		}

		return links;
	}

	public void setLinkList(LinkList linkList) {
		if(this.linkList != null) {
			this.linkList.unloadLinks();
		}

		if(linkList != null) {
			linkList.loadLinks(null, true);
		}

		this.linkList = linkList;

		this.linkListModel.reloadLinkList(linkList);
	}

	public void mouseClicked(MouseEvent e) {
		if (linkList instanceof Index)
			((Index)linkList).setChanged(false);

		if(e.getButton() == MouseEvent.BUTTON3
		   && this.linkList != null) {
			selectedRows = table.getSelectedRows();
			updateRightClickMenu(getSelectedLinks(selectedRows));
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void mouseEntered(MouseEvent e) { }

	public void mouseExited(MouseEvent e) { }

	public void mousePressed(MouseEvent e) { }

	public void mouseReleased(MouseEvent e) { }

	public void keyPressed(KeyEvent e) { }

	public void keyReleased(KeyEvent e) { }

	public void keyTyped(KeyEvent e) { }

	public void actionPerformed(ActionEvent e) {
		Vector links;
		String keyList = "";

		if (linkList == null) // don't forget that linkList == Index most of the time
			return;

		if (e.getSource() == gotoItem) {
			if (selectedRows.length <= 0)
				return;

			Link link = (Link)linkList.getLinkList().get(selectedRows[0]);

			if (link.getParentId() == -1) {
				Logger.notice(this, "No parent ? Abnormal !");
				return;
			}

			Index parent;
			if (link.getParent() == null)
				parent = indexTree.getRoot().getIndex(link.getParentId());
			else
				parent = link.getParent();

			if (parent == null) {
				Logger.notice(this, "Cannot find again the parent ?! Id: "+Integer.toString(link.getParentId()));
				return;
			}

			indexTree.getTree().setSelectionPath(new TreePath(parent.getTreeNode().getPath()));

			tables.setList(parent);

			return;
		}
	}



	public class LinkListModel extends javax.swing.table.AbstractTableModel implements java.util.Observer {
		private static final long serialVersionUID = 1L;

		public Vector columnNames;

		public Vector links = null; /* thaw.plugins.index.Link Vector */

		public LinkList linkList;

		public LinkListModel() {
			super();

			this.columnNames = new Vector();

			this.columnNames.add(I18n.getMessage("thaw.plugin.index.index"));
			this.columnNames.add(I18n.getMessage("thaw.common.key"));
		}

		public void reloadLinkList(LinkList newLinkList) {
			if (this.linkList != null && (this.linkList instanceof Observable)) {
				((Observable)this.linkList).deleteObserver(this);
			}

			if (newLinkList != null && (newLinkList instanceof Observable)) {
				((Observable)newLinkList).addObserver(this);
			}

			this.linkList = newLinkList;


			if(this.links != null) {
				for(Iterator it = this.links.iterator();
				    it.hasNext(); ) {
					thaw.plugins.index.Link link = (thaw.plugins.index.Link)it.next();
					link.deleteObserver(this);
				}
			}

			this.links = null;

			if(this.linkList != null) {
				this.links = this.linkList.getLinkList();
			}

			this.refresh();

		}

		public int getRowCount() {
			if (this.links == null)
				return 0;

			return this.links.size();
		}

		public int getColumnCount() {
			return this.columnNames.size();
		}

		public String getColumnName(int column) {
			return (String)this.columnNames.get(column);
		}

		public Object getValueAt(int row, int column) {
			thaw.plugins.index.Link link = (thaw.plugins.index.Link)this.links.get(row);

			switch(column) {
			case(0): return link.getIndexName();
			case(1): return link.getPublicKey();
			default: return null;
			}
		}

		public void refresh() {
			if(this.linkList != null) {
				this.links = this.linkList.getLinkList();
			}

			TableModelEvent event = new TableModelEvent(this);
			this.refresh(event);
		}

		public void refresh(int row) {
			TableModelEvent event = new TableModelEvent(this, row);
			this.refresh(event);
		}

		public void refresh(TableModelEvent e) {
			this.fireTableChanged(e);
		}

		public void update(java.util.Observable o, Object param) {
			if(param instanceof thaw.plugins.index.Link) {

				//link.deleteObserver(this);
				//link.addObserver(this);
			}

			this.refresh(); /* TODO : Do it more nicely ... :) */
		}
	}

}

