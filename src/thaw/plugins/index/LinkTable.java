package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;

import thaw.core.I18n;
import thaw.gui.IconBox;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.ToolbarModifier;


public class LinkTable implements MouseListener, KeyListener, ActionListener {

	private JPanel panel;
	private JTable table;

	private LinkListModel linkListModel = null;
	private LinkList      linkList = null;

	private FCPQueueManager queueManager;

	private IndexBrowserPanel indexBrowser;

	private JPopupMenu rightClickMenu;
	private Vector rightClickActions;
	private JMenuItem gotoItem;

	private ToolbarModifier toolbarModifier;
	private Vector toolbarActions;

	private int[] selectedRows;

	public LinkTable (final FCPQueueManager queueManager, IndexBrowserPanel indexBrowser) {
		this.indexBrowser = indexBrowser;

		linkListModel = new LinkListModel();
		table = new JTable(linkListModel);
		table.setShowGrid(false);
		table.setDefaultRenderer(table.getColumnClass(0), new LinkRenderer());

		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.linkList")), BorderLayout.NORTH);
		panel.add(new JScrollPane(table));

		table.addMouseListener(this);

		rightClickMenu = new JPopupMenu();
		rightClickActions = new Vector();

		toolbarModifier = new ToolbarModifier(indexBrowser.getMainWindow());
		toolbarActions = new Vector();

		JMenuItem item;
		JButton button;

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.addIndexesFromLink"));
		button = new JButton(IconBox.indexReuse);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.addIndexesFromLink"));
		toolbarActions.add(new LinkManagementHelper.IndexAdder(button, queueManager,
								       indexBrowser, true));

		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new LinkManagementHelper.IndexAdder(item, queueManager,
									  indexBrowser, true));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.copyKeys"));
		rightClickMenu.add(item);
		rightClickActions.add(new LinkManagementHelper.PublicKeyCopier(item));

		item = new JMenuItem(I18n.getMessage("thaw.common.remove"));
		button = new JButton(IconBox.delete);
		button.setToolTipText(I18n.getMessage("thaw.common.remove"));
		toolbarActions.add(new LinkManagementHelper.LinkRemover(indexBrowser, button));
		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new LinkManagementHelper.LinkRemover(indexBrowser, item));

		gotoItem = new JMenuItem(I18n.getMessage("thaw.plugin.index.gotoIndex"));
		rightClickMenu.add(gotoItem);
		gotoItem.addActionListener(this);

		updateRightClickMenu(null);
	}

	public ToolbarModifier getToolbarModifier() {
		return toolbarModifier;
	}

	public JPanel getPanel() {
		return panel;
	}

	protected void updateRightClickMenu(final Vector selectedLinks) {
		LinkManagementHelper.LinkAction action;

		for (final Iterator it = rightClickActions.iterator();
		     it.hasNext(); ) {
			action = (LinkManagementHelper.LinkAction)it.next();
			action.setTarget(selectedLinks);
		}

		gotoItem.setEnabled((linkList != null) && !(linkList instanceof Index));
	}

	protected void updateToolbar(final Vector selectedLinks) {
		LinkManagementHelper.LinkAction action;

		for (final Iterator it = toolbarActions.iterator();
		     it.hasNext(); ) {
			action = (LinkManagementHelper.LinkAction)it.next();
			action.setTarget(selectedLinks);
		}
	}

	protected Vector getSelectedLinks(final int[] selectedRows) {
		final Vector srcList = linkList.getLinkList(null, false);
		final Vector links = new Vector();

		for(int i = 0 ; i < selectedRows.length ; i++) {
			final Link link = (Link)srcList.get(selectedRows[i]);
			links.add(link);
		}

		return links;
	}

	public void setLinkList(final LinkList linkList) {
		this.linkList = linkList;
		linkListModel.reloadLinkList(linkList);

		if (linkList != null)
			indexBrowser.getUnknownIndexList().addLinks(linkList);
	}

	public LinkList getLinkList() {
		return linkList;
	}

	public void mouseClicked(final MouseEvent e) {
		Vector selection;

		if (linkList == null) {
			selectedRows = null;
			return;
		}

		selectedRows = table.getSelectedRows();
		selection = getSelectedLinks(selectedRows);

		if ((e.getButton() == MouseEvent.BUTTON1) && linkList != null) {
			updateToolbar(selection);
			toolbarModifier.displayButtonsInTheToolbar();
		}

		if ((e.getButton() == MouseEvent.BUTTON3)
		   && (linkList != null)) {
			updateRightClickMenu(selection);
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void mouseEntered(final MouseEvent e) { }

	public void mouseExited(final MouseEvent e) { }

	public void mousePressed(final MouseEvent e) { }

	public void mouseReleased(final MouseEvent e) { }

	public void keyPressed(final KeyEvent e) { }

	public void keyReleased(final KeyEvent e) { }

	public void keyTyped(final KeyEvent e) { }

	public void actionPerformed(final ActionEvent e) {
		final Vector links;
		final String keyList = "";

		if (linkList == null) // don't forget that linkList == Index most of the time
			return;

		if (e.getSource() == gotoItem) {
			if (selectedRows.length <= 0)
				return;

			/* TODO */

			return;
		}
	}

	public void refresh() {
		linkListModel.refresh();
	}


	public class LinkListModel extends javax.swing.table.AbstractTableModel implements java.util.Observer {
		private static final long serialVersionUID = 1L;

		public Vector columnNames;

		public Vector links = null; /* thaw.plugins.index.Link Vector */

		public LinkList linkList;

		public LinkListModel() {
			super();

			columnNames = new Vector();

			columnNames.add(I18n.getMessage("thaw.plugin.index.index"));
			//columnNames.add(I18n.getMessage("thaw.common.key"));
		}

		public void reloadLinkList(final LinkList newLinkList) {

			linkList = newLinkList;
			links = null;

			this.refresh();

		}

		public int getRowCount() {
			if (links == null)
				return 0;

			return links.size();
		}

		public int getColumnCount() {
			return columnNames.size();
		}

		public String getColumnName(final int column) {
			return (String)columnNames.get(column);
		}

		public Object getValueAt(final int row, final int column) {
			final thaw.plugins.index.Link link = (thaw.plugins.index.Link)links.get(row);

			switch(column) {
			case(0): return link.getIndexName();
				//case(1): return link.getPublicKey();
			default: return null;
			}
		}

		public void refresh() {
			if(linkList != null) {
				links = linkList.getLinkList(null, false);
			} else
				links = null;

			final TableModelEvent event = new TableModelEvent(this);
			this.refresh(event);
		}

		public void refresh(final int row) {
			final TableModelEvent event = new TableModelEvent(this, row);
			this.refresh(event);
		}

		public void refresh(final TableModelEvent e) {
			fireTableChanged(e);
		}

		public void update(final java.util.Observable o, final Object param) {
			if(param instanceof thaw.plugins.index.Link) {

				//link.deleteObserver(this);
				//link.addObserver(this);
			}

			this.refresh(); /* TODO : Do it more nicely ... :) */
		}
	}


	private class LinkRenderer extends DefaultTableCellRenderer {
		private final static long serialVersionUID = 20060821;

		private Color softGray;

		public LinkRenderer() {
			softGray = new Color(240,240,240);
		}

		public Component getTableCellRendererComponent(final JTable table, final Object value,
							       final boolean isSelected, final boolean hasFocus,
							       final int row, final int column) {

			if(value == null)
				return super.getTableCellRendererComponent(table, "",
									   isSelected, hasFocus, row, column);

			Component cell;

			if(value instanceof Long)
				cell = super.getTableCellRendererComponent(table,
									   thaw.gui.GUIHelper.getPrintableSize(((Long)value).longValue()),
									   isSelected, hasFocus, row, column);
			else
				cell = super.getTableCellRendererComponent(table, value,
									   isSelected, hasFocus,
									   row, column);
			if (!isSelected) {
				if (row % 2 == 0)
					cell.setBackground(Color.WHITE);
				else
					cell.setBackground(softGray);
			}
			return cell;
		}

	}

}

