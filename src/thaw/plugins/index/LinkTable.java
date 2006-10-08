package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JFileChooser;
import javax.swing.JProgressBar;
import javax.swing.JLabel;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;

import java.util.Vector;
import java.util.Iterator;

import java.util.Observable;
import java.util.Observer;

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

		linkListModel = new LinkListModel();
		table = new JTable(linkListModel);
		table.setShowGrid(true);

		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.linkList")), BorderLayout.NORTH);
		panel.add(new JScrollPane(table));
		
		rightClickMenu = new JPopupMenu();
		removeLinks = new JMenuItem(I18n.getMessage("thaw.common.remove"));
		addThisIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.addIndexFromLink"));
		copyKey = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKey"));

		removeLinks.addActionListener(this);
		addThisIndex.addActionListener(this);
		copyKey.addActionListener(this);

		if (modifiables)
			rightClickMenu.add(removeLinks);
		rightClickMenu.add(addThisIndex);
		rightClickMenu.add(copyKey);

		table.addMouseListener(this);

		indexTree = tree;
	}

	public JPanel getPanel() {
		return panel;
	}

	public void setLinkList(LinkList linkList) {
		if(this.linkList != null) {
			this.linkList.unloadLinks();
		}
		
		if(linkList != null) {
			linkList.loadLinks(null, true);
		}

		this.linkList = linkList;

		linkListModel.reloadLinkList(linkList);
	}

	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON3
		   && linkList != null) {
			removeLinks.setEnabled(linkList instanceof Index);
			selectedRows = table.getSelectedRows();
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

		if (linkList == null)
			return;

		links = linkList.getLinkList();

		for (int i = 0 ; i < selectedRows.length;  i++) {
			
			if (e.getSource() == removeLinks) {
				Link link = (Link)links.get(selectedRows[i]);
				((Index)linkList).removeLink(link);
			}

			if (e.getSource() == addThisIndex) {
				Link link = (Link)links.get(selectedRows[i]);
				Index index = new Index(db, queueManager, -2, null, Index.getNameFromKey(link.getKey()),
							Index.getNameFromKey(link.getKey()), link.getKey(), null,
							0, null, false);
				index.create();
				indexTree.addToRoot(index);
			}

			if (e.getSource() == copyKey) {
				Link link = (Link)links.get(selectedRows[i]);
				if (link.getKey() != null)
					keyList = keyList + link.getKey() + "\n";
			}
		}

		if(e.getSource() == copyKey) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			StringSelection st = new StringSelection(keyList);
			Clipboard cp = tk.getSystemClipboard();
			cp.setContents(st, null);
		}

	}



	public class LinkListModel extends javax.swing.table.AbstractTableModel implements java.util.Observer {
		public Vector columnNames;

		public Vector links = null; /* thaw.plugins.index.Link Vector */

		public LinkList linkList;

		public LinkListModel() {
			super();

			columnNames = new Vector();

			columnNames.add(I18n.getMessage("thaw.plugin.index.index"));
			columnNames.add(I18n.getMessage("thaw.common.key"));
		}

		public void reloadLinkList(LinkList newLinkList) {
			if (linkList != null && (linkList instanceof Observable)) {
				((Observable)linkList).deleteObserver(this);
			}

			if (newLinkList != null && (newLinkList instanceof Observable)) {
				((Observable)newLinkList).addObserver(this);
			}

			linkList = newLinkList;


			if(links != null) {
				for(Iterator it = links.iterator();
				    it.hasNext(); ) {
					thaw.plugins.index.Link link = (thaw.plugins.index.Link)it.next();
					link.deleteObserver(this);
				}
			}

			links = null;
			
			if(linkList != null) {
				links = linkList.getLinkList();
			}

			if(links != null) {
				for(Iterator it = links.iterator();
				    it.hasNext(); ) {
					thaw.plugins.index.Link link = (thaw.plugins.index.Link)it.next();
					link.addObserver(this);
				}
			}

			refresh();

		}

		public int getRowCount() {
			if (links == null)
				return 0;

			return links.size();
		}
		

		public int getColumnCount() {
			return columnNames.size();
		}

		public String getColumnName(int column) {
			return (String)columnNames.get(column);
		}

		public Object getValueAt(int row, int column) {
			thaw.plugins.index.Link link = (thaw.plugins.index.Link)links.get(row);

			switch(column) {
			case(0): return link.getIndexName();
			case(1): return link.getKey();
			default: return null;
			}
		}

		public void refresh() {
			if(linkList != null) {				
				links = linkList.getLinkList();
			}

			TableModelEvent event = new TableModelEvent(this);
			refresh(event);
		}

		public void refresh(int row) {
			TableModelEvent event = new TableModelEvent(this, row);
			refresh(event);
		}

		public void refresh(TableModelEvent e) {
			fireTableChanged(e);
		}

		public void update(java.util.Observable o, Object param) {
			if(param instanceof thaw.plugins.index.Link) {

				/* TODO : It can be a remove ... to check ... */

				thaw.plugins.index.Link link = (thaw.plugins.index.Link)param;
				
				link.deleteObserver(this);
				link.addObserver(this);
			}

			refresh(); /* TODO : Do it more nicely ... :) */
		}
	}

}

