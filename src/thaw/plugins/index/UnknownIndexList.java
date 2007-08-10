package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;

import java.awt.event.MouseAdapter;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import javax.swing.JComponent;

import thaw.gui.CheckBox;

import thaw.core.I18n;
import thaw.core.Logger;
import thaw.gui.IconBox;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.ToolbarModifier;


public class UnknownIndexList implements MouseListener, ActionListener {
	public final static int MAX_INDEXES = 50;

	private int offset;

	private Link[] linkList;
	private boolean full;
	private Vector vList; /* only when < MAX_INDEXES */

	private JPanel panel;
	private JList list;

	private JScrollPane scrollPane;

	private CheckBox autoSorting;

	private JPopupMenu rightClickMenu = null;
	private Vector rightClickActions = null;

	private JMenuItem sortItem = null;

	private ToolbarModifier toolbarModifier;
	private Vector toolbarActions;

	private FCPQueueManager queueManager;
	private IndexBrowserPanel indexBrowser;


	public UnknownIndexList(final FCPQueueManager queueManager, IndexBrowserPanel indexBrowser) {
		this.queueManager = queueManager;
		this.indexBrowser = indexBrowser;

		offset = 0;
		full = false;
		vList = new Vector();
		linkList = new Link[UnknownIndexList.MAX_INDEXES+1];

		for(int i = 0 ; i < linkList.length ; i++)
			linkList[i] = null;

		list = new JList(vList);

		list.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.unknownIndexes")),
			  BorderLayout.NORTH);

		scrollPane = new JScrollPane(list);
		panel.add(scrollPane, BorderLayout.CENTER);

		autoSorting = new CheckBox(indexBrowser.getConfig(),
					   "autoSorting",
					   I18n.getMessage("thaw.plugin.index.autoSorting"),
					   true);

		autoSorting.addActionListener(this);
		panel.add(autoSorting, BorderLayout.SOUTH);

		JButton button;

		toolbarModifier = new ToolbarModifier(indexBrowser.getMainWindow());
		toolbarActions = new Vector();

		button = new JButton(IconBox.indexReuse);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.addIndexesFromLink"));
		toolbarActions.add(new LinkManagementHelper.IndexAdder(button, queueManager,
								       indexBrowser, false));
		toolbarModifier.addButtonToTheToolbar(button);

		list.addMouseListener(this);

		applyAutoSortingSetting();
	}


	public ToolbarModifier getToolbarModifier() {
		return toolbarModifier;
	}

	public JPanel getPanel() {
		return panel;
	}

	public boolean isInList(final Link l) {
		if (l == null)
			return false;

		for (int i = 0 ; i < linkList.length ; i++) {
			if (linkList[i] == null)
				continue;

			if (l.compare(linkList[i]))
				return true;
		}

		return false;
	}

	public void erase(int i) {
		linkList[linkList.length-1] = null;
		for (int j = i ; j < linkList.length-1; j++) {
			linkList[j] = linkList[j+1];
		}
	}

	public boolean removeLink(final Index index) {
		boolean ret = false;

		for (int i = 0 ; i < linkList.length ; i++) {
			if ((linkList[i] != null) && linkList[i].compare(index)) {
				if (!full)
					vList.remove(linkList[i]);
				erase(i);
				ret = true;
			}
		}

		refresh();

		return ret;
	}


	public boolean removeLink(final Link link) {
		boolean ret = false;

		for (int i = 0 ; i < linkList.length ; i++) {
			if ((linkList[i] != null) && linkList[i].compare(link)) {
				if (!full)
					vList.remove(linkList[i]);
				erase(i);
				ret = true;
			}
		}

		refresh();

		return ret;
	}


	public void makePlace(int i) {
		int j;
		for (j = linkList.length - 1; j > i ; j--) {
			linkList[j] = linkList[j-1];
		}
		linkList[j] = null;
	}

	/**
	 * will check that the link link to an unknown index before adding
	 */
	public boolean addLink(final Link link) {
		if ((link == null)
		    || link.isBlackListed()
		    || Index.isAlreadyKnown(indexBrowser.getDb(), link.getPublicKey()) >= 0
		    || isInList(link))
			return false;

		makePlace(0);
		linkList[0] = link;

		if (!full) {
			vList.add(0, link);
			list.setListData(vList);
		} else {
			list.setListData(linkList);
		}

		offset++;

		if (offset >= UnknownIndexList.MAX_INDEXES) {
			offset = 0;
			full = true;
		}

		refresh();

		return true;
	}

	/**
	 * will add the link from that index (if links link to unknown indexes)
	 */
	public boolean addLinks(final LinkList index) {
		boolean ret = false;

		if (index == null)
			return false;

		final Vector ll = index.getLinkList(null, false);

		if ((ll == null) || (ll.size() == 0))
			return false;

		for (final Iterator it = ll.iterator();
		     it.hasNext();) {
			if (addLink(((Link)it.next())))
				ret = true;
		}

		return ret;
	}


	protected void updateRightClickMenu(Vector links) {
		if (rightClickMenu == null) {
			/* first time */
			/* I don't remember why it's done here .... */

			rightClickMenu = new JPopupMenu();
			rightClickActions = new Vector();
			JMenuItem item;

			item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addIndexesFromLink"), IconBox.minAdd);
			rightClickMenu.add(item);
			rightClickActions.add(new LinkManagementHelper.IndexAdder(item, queueManager,
										  indexBrowser, false));

			sortItem = new JMenuItem(I18n.getMessage("thaw.plugin.index.sortAlphabetically"));
			rightClickMenu.add(sortItem);
			sortItem.addActionListener(this);


			item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKeys"));
			rightClickMenu.add(item);
			rightClickActions.add(new LinkManagementHelper.PublicKeyCopier(item));

			item = new JMenuItem(I18n.getMessage("thaw.plugin.index.editBlackList"));
			rightClickMenu.add(item);
			rightClickActions.add(new LinkManagementHelper.BlackListDisplayer(item, indexBrowser.getBlackList()));

			item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addToBlackList"), IconBox.minStop);
			rightClickMenu.add(item);
			rightClickActions.add(new LinkManagementHelper.ToBlackListAdder(item, indexBrowser));
		}

		LinkManagementHelper.LinkAction action;

		for(final Iterator it = rightClickActions.iterator();
		     it.hasNext(); ) {
			action = (LinkManagementHelper.LinkAction)it.next();
			action.setTarget(links);
		}
	}

	public void updateToolbar(Vector links) {
		LinkManagementHelper.LinkAction action;

		for(final Iterator it = toolbarActions.iterator();
		     it.hasNext(); ) {
			action = (LinkManagementHelper.LinkAction)it.next();
			action.setTarget(links);
		}
	}

	public Vector getSelectedLinks() {
		final Object[] sLink;

		try {
			sLink = list.getSelectedValues();
		} catch(ArrayIndexOutOfBoundsException e) {
			return null;
		}

		final Vector vLink = new Vector();

		for (int i = 0; i < sLink.length ; i++) {
			vLink.add(sLink[i]);
		}

		return vLink;
	}


	public void refresh() {
		list.revalidate();
		list.repaint();
	}


	public void applyAutoSortingSetting() {
		LinkManagementHelper.LinkAction action;

		if (rightClickActions != null) {
			for (Iterator it = rightClickActions.iterator();
			     it.hasNext();) {
				action = (LinkManagementHelper.LinkAction)it.next();
				if (action instanceof LinkManagementHelper.IndexAdder)
					((LinkManagementHelper.IndexAdder)action).setAutoSorting(autoSorting.isSelected());
			}
		}

		for(final Iterator it = toolbarActions.iterator();
		    it.hasNext(); ) {
			action = (LinkManagementHelper.LinkAction)it.next();
			if (action instanceof LinkManagementHelper.IndexAdder)
				((LinkManagementHelper.IndexAdder)action).setAutoSorting(autoSorting.isSelected());
		}

	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == sortItem) {

			Vector v;

			if (full) {
				v = new Vector(linkList.length);

				for (int i = 0 ; i < linkList.length ; i++) {
					if (linkList[i] != null)
						v.add(linkList[i]);
				}
			} else
				v = vList;

			java.util.Collections.sort(v);

			if (full) {
				for (int i = 0 ; i < linkList.length ; i++) {
					linkList[i] = null;
				}

				v.toArray(linkList);
			} else
				vList = v;

			refresh();

		} else if (e.getSource() == autoSorting) {

			applyAutoSortingSetting();

		}
	}


	public void mouseClicked(final MouseEvent e) {
		Vector selection;

		if (linkList == null) {
			selection = null;
			return;
		}

		selection = getSelectedLinks();

		if (selection == null)
			return;

		if (e.getButton() == MouseEvent.BUTTON1) {
			updateToolbar(selection);
			toolbarModifier.displayButtonsInTheToolbar();
		}

		if (e.getButton() == MouseEvent.BUTTON3) {
			updateRightClickMenu(selection);
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void mouseEntered(final MouseEvent e) { }

	public void mouseExited(final MouseEvent e) { }

	public void mousePressed(final MouseEvent e) { }

	public void mouseReleased(final MouseEvent e) { }
}
