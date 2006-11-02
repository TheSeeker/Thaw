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

	private LinkListModel linkListModel;
	private LinkList      linkList;

	private FCPQueueManager queueManager;
	private boolean modifiables;

	private JPopupMenu rightClickMenu;
	private JMenuItem removeLinks;
	private JMenuItem addThisIndex;
	private JMenuItem copyKey;
	private IndexTree indexTree;

	private Hsqldb db;

	private int[] selectedRows;

	public LinkTable (boolean modifiables, Hsqldb db, FCPQueueManager queueManager, IndexTree tree) {
		this.modifiables = modifiables;
		this.queueManager = queueManager;
		this.db = db;

		this.linkListModel = new LinkListModel();
		this.table = new JTable(this.linkListModel);
		this.table.setShowGrid(true);

		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout());

		this.panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.linkList")), BorderLayout.NORTH);
		this.panel.add(new JScrollPane(this.table));

		this.rightClickMenu = new JPopupMenu();
		this.removeLinks = new JMenuItem(I18n.getMessage("thaw.common.remove"));
		this.addThisIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.addIndexFromLink"));
		this.copyKey = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKey"));

		this.removeLinks.addActionListener(this);
		this.addThisIndex.addActionListener(this);
		this.copyKey.addActionListener(this);

		if (modifiables) {
			this.rightClickMenu.add(this.removeLinks);
		}
		else {
			this.rightClickMenu.add(this.addThisIndex);
		}

		this.rightClickMenu.add(this.copyKey);

		this.table.addMouseListener(this);

		this.indexTree = tree;
	}

	public JPanel getPanel() {
		return this.panel;
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
		if(e.getButton() == MouseEvent.BUTTON3
		   && this.linkList != null) {
			this.removeLinks.setEnabled(this.linkList instanceof Index);
			this.selectedRows = this.table.getSelectedRows();
			this.rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
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

		if (this.linkList == null)
			return;

		links = this.linkList.getLinkList();

		for (int i = 0 ; i < this.selectedRows.length;  i++) {
			if (e.getSource() == this.removeLinks) {
				Link link = (Link)links.get(this.selectedRows[i]);
				((Index)this.linkList).removeLink(link);
			}

			if (e.getSource() == this.addThisIndex) {
				Link link = (Link)links.get(this.selectedRows[i]);
				Index index = new Index(this.db, this.queueManager, -2, null, Index.getNameFromKey(link.getKey()),
							Index.getNameFromKey(link.getKey()), link.getKey(), null,
							0, null, false);
				if (this.indexTree.addToRoot(index))
					index.create();
			}

			if (e.getSource() == this.copyKey) {
				Link link = (Link)links.get(this.selectedRows[i]);
				if (link.getKey() != null)
					keyList = keyList + link.getKey() + "\n";
			}
		}

		if(e.getSource() == this.copyKey) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			StringSelection st = new StringSelection(keyList);
			Clipboard cp = tk.getSystemClipboard();
			cp.setContents(st, null);
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
			case(1): return link.getKey();
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

