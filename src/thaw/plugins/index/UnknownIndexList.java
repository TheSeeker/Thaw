package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.*;
import thaw.plugins.Hsqldb;
import thaw.fcp.FCPQueueManager;

public class UnknownIndexList implements MouseListener {
	public final static int MAX_INDEXES = 50;

	private int offset;

	private Link[] linkList;
	private boolean full;
	private Vector vList; /* only when < 50 */

	private JPanel panel;
	private JList list;

	private JScrollPane scrollPane;

	private JPopupMenu rightClickMenu = null;
	private Vector rightClickActions = null;

	private Hsqldb db;
	private FCPQueueManager queueManager;
	private IndexTree indexTree;

	public UnknownIndexList(Hsqldb db, FCPQueueManager queueManager) {
		this.db = db;
		this.queueManager = queueManager;

		offset = 0;
		full = false;
		vList = new Vector();
		linkList = new Link[MAX_INDEXES];

		for(int i = 0 ; i < linkList.length ; i++)
			linkList[i] = null;

		list = new JList(vList);

		list.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.unknownIndexes")),
			  BorderLayout.NORTH);
		scrollPane = new JScrollPane(list);
		panel.add(scrollPane);

		list.addMouseListener(this);
	}

	public void setIndexTree(IndexTree tree) {
		this.indexTree = tree;
	}

	public JPanel getPanel() {
		return panel;
	}

	public boolean isInList(Link l) {
		if (l == null)
			return false;

		for (int i = 0 ; i < linkList.length ; i++) {
			if (l.compare(linkList[i]))
				return true;
		}

		return false;
	}

	public boolean removeLink(Index index) {
		boolean ret = false;

		for (int i = 0 ; i < linkList.length ; i++) {
			if (linkList[i] != null && linkList[i].compare(index)) {
				if (!full)
					vList.remove(linkList[i]);
				linkList[i] = null;
				ret = true;
			}
		}

		return ret;
	}

	/**
	 * will check that the link link to an unknown index before adding
	 */
	public boolean addLink(Link link) {
		if (link == null || link.isIndexAlreadyKnown() || isInList(link))
			return false;

		linkList[linkList.length - 1 - offset] = link;

		if (!full) {
			vList.add(0, link);
			list.setListData(vList);
		} else {
			list.setListData(linkList);
		}

		offset++;

		if (offset >= MAX_INDEXES) {
			offset = 0;
			full = true;
		}

		return true;
	}

	/**
	 * will add the link from that index (if links link to unknown indexes)
	 */
	public boolean addLinks(Index index) {
		boolean ret = false;

		Vector ll = index.getLinkList();

		if (ll == null || ll.size() == 0)
			return false;

		for (Iterator it = ll.iterator();
		     it.hasNext();) {
			if (addLink(((Link)it.next())))
				ret = true;
		}

		return ret;
	}


	protected void updateRightClickMenu() {
		if (rightClickMenu == null) {
			rightClickMenu = new JPopupMenu();
			rightClickActions = new Vector();
			JMenuItem item;

			item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addIndexesFromLink"));
			rightClickMenu.add(item);
			rightClickActions.add(new LinkManagementHelper.IndexAdder(item, db, queueManager, this, indexTree));

			item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKeys"));
			rightClickMenu.add(item);
			rightClickActions.add(new LinkManagementHelper.PublicKeyCopier(item));
		}

		Object[] sLink = list.getSelectedValues();
		Vector vLink = new Vector();

		for (int i = 0; i < sLink.length ; i++)
			vLink.add(sLink[i]);

		LinkManagementHelper.LinkAction action;

		for(Iterator it = rightClickActions.iterator();
		     it.hasNext(); ) {
			action = (LinkManagementHelper.LinkAction)it.next();
			action.setTarget(vLink);
		}
	}


	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON3
		   && this.linkList != null) {
			updateRightClickMenu();
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void mouseEntered(MouseEvent e) { }

	public void mouseExited(MouseEvent e) { }

	public void mousePressed(MouseEvent e) { }

	public void mouseReleased(MouseEvent e) { }
}
