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
import thaw.gui.CheckBox;

import thaw.core.I18n;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;
import thaw.gui.IconBox;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FreenetURIHelper;
import thaw.plugins.ToolbarModifier;


public class UnknownIndexList implements MouseListener, ActionListener {
	private Vector linkList;

	private JPanel panel;
	private JList list;

	private JScrollPane scrollPane;

	private CheckBox autoSorting;

	private JPopupMenu rightClickMenu = null;
	private Vector rightClickActions = null;

	private ToolbarModifier toolbarModifier;
	private Vector toolbarActions;

	private FCPQueueManager queueManager;
	private IndexBrowserPanel indexBrowser;


	public UnknownIndexList(final FCPQueueManager queueManager, IndexBrowserPanel indexBrowser) {
		this.queueManager = queueManager;
		this.indexBrowser = indexBrowser;

		linkList = new Vector();

		list = new JList(linkList);

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
		panel.add(new JScrollPane(autoSorting, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), BorderLayout.SOUTH);

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
		return (linkList.indexOf(l) >= 0);
	}


	public boolean removeLink(final Index index) {
		boolean ret = false;

		/* to avoid iterator collisions */

		for (int i = 0 ; i < linkList.size() ; i++) {
			Link l = (Link)linkList.get(i);
			if (l.compare(index)) {
				ret = true;
				linkList.remove(l);
			}
		}

		refresh();

		return ret;
	}


	public boolean removeLink(final Link link) {
		boolean ret = false;

		while (linkList.remove(link)) {
			ret = true;
		}

		refresh();

		return ret;
	}


	public boolean addLink(Link link) {
		return addLink(link, true);
	}

	/**
	 * will check that the link link to an unknown index before adding
	 */
	public boolean addLink(final Link link, boolean refresh) {
		if ((link == null)
		    || link.isBlackListed()
		    || Index.isAlreadyKnown(indexBrowser.getDb(), link.getPublicKey()) >= 0
		    || isInList(link)
		    || FreenetURIHelper.isObsolete(link.getPublicKey()))
			return false;

		linkList.add(link);

		if (refresh)
			refresh();

		return true;
	}

	private class LinkAdder implements ThawRunnable {
		private LinkList index;
		private boolean running;

		public LinkAdder(LinkList index) {
			this.index = index;
			this.running = true;
		}

		public void run() {
			boolean ret = false;

			final Link[] ll = index.getLinkList(null, false);

			if ((ll == null) || (ll.length == 0))
				return;

			for (int i = 0 ; i < ll.length && running ; i++) {
				if (addLink(ll[i], false))
					ret = true;
			}

			if (ret)
				refresh();

			return;
		}

		public void stop() {
			running = false;
		}
	}

	private LinkAdder lastLinkAdder = null;


	/**
	 * will add the link from that index (if links link to unknown indexes)
	 */
	public void addLinks(final LinkList index) {
		if (index == null)
			return;

		if (lastLinkAdder != null)
			lastLinkAdder.stop();

		lastLinkAdder = new LinkAdder(index);
		Thread th = new ThawThread(lastLinkAdder, "Unknown index list computer", this);
		th.start();
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


			item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKeys"));
			rightClickMenu.add(item);
			rightClickActions.add(new LinkManagementHelper.PublicKeyCopier(item));

			item = new JMenuItem(I18n.getMessage("thaw.plugin.index.editBlackList"));
			rightClickMenu.add(item);
			rightClickActions.add(new LinkManagementHelper.BlackListDisplayer(item, indexBrowser.getBlackList()));

			item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addToBlackList"), IconBox.minStop);
			rightClickMenu.add(item);
			rightClickActions.add(new LinkManagementHelper.ToBlackListAdder(item, indexBrowser));

			applyAutoSortingSetting();
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
		java.util.Collections.sort(linkList);

		list.setListData(linkList);

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
		if (e.getSource() == autoSorting) {

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
