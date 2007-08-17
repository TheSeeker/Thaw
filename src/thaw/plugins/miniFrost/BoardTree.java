package thaw.plugins.miniFrost;

import javax.swing.JPanel;
import java.util.Observable;
import java.util.Observer;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import java.util.Vector;
import java.util.Iterator;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


import java.awt.Color;

import java.awt.Font;

import thaw.gui.JDragTree;
import thaw.gui.IconBox;
import thaw.core.I18n;

import thaw.plugins.miniFrost.interfaces.BoardFactory;
import thaw.plugins.miniFrost.interfaces.Board;

/**
 * It's called BoardTree, but in fact it's just a list.<br/>
 * Notify each time the selection is changed (board is given in argument)
 */
public class BoardTree extends Observable
	implements javax.swing.event.ListSelectionListener,
		   MouseListener {

	/* X and Y are replaced */
	public final static String DRAFTS_STR = "W: X / U : Y";

	private JPanel panel;

	private BoardListModel model;
	private JList list;

	private JPopupMenu rightClickMenu;
	private Vector actions;

	private MiniFrostPanel mainPanel;

	public final static Color SELECTION_COLOR         = new Color(190, 190, 190);
	public final static Color LOADING_COLOR           = new Color(230, 230, 230);
	public final static Color LOADING_SELECTION_COLOR = new Color(150, 150, 150);

	private JLabel draftsState;


	public BoardTree(MiniFrostPanel mainPanel) {
		this.mainPanel = mainPanel;

		/* label */

		panel = new JPanel(new BorderLayout());

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.boards")),
			  BorderLayout.NORTH);


		/* board list */

		model = new BoardListModel();
		refresh();

		list = new JList(model);
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new BoardListRenderer());
		list.addListSelectionListener(this);
		//list.setPreferredSize(new java.awt.Dimension(100, 100));
		list.addMouseListener(this);

		JScrollPane scroll = new JScrollPane(list);
		scroll.setPreferredSize(new java.awt.Dimension(100, 100));
		panel.add(scroll, BorderLayout.CENTER);

		actions = new Vector();

		/* right click menu */

		JMenuItem item;
		rightClickMenu = new JPopupMenu();

		item = new JMenuItem("");
		rightClickMenu.add(item);
		actions.add(new BoardManagementHelper.BoardNameDisplayer(item));

		rightClickMenu.addSeparator();

		item = new JMenuItem(I18n.getMessage("thaw.common.add"),
				     IconBox.minAdd);
		rightClickMenu.add(item);
		actions.add(new BoardManagementHelper.BoardAdder(mainPanel, item));

		item = new JMenuItem(I18n.getMessage("thaw.common.remove"),
				     IconBox.minDelete);
		rightClickMenu.add(item);
		actions.add(new BoardManagementHelper.BoardRemover(mainPanel, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.loadNewMessages"),
				     IconBox.minRefreshAction);
		rightClickMenu.add(item);
		actions.add(new BoardManagementHelper.BoardRefresher(mainPanel, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.markAsRead"),
				     IconBox.minMarkAsRead);
		rightClickMenu.add(item);
		actions.add(new BoardManagementHelper.MarkAllAsRead(mainPanel, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.newMessage"),
				     IconBox.minMsgNew);
		rightClickMenu.add(item);
		actions.add(new BoardManagementHelper.NewMessage(mainPanel, item));

		/* buttons */

		JPanel southPanel = new JPanel(new BorderLayout());

		JPanel buttonPanel = new JPanel(new GridLayout(2, 3));

		JButton button;

		button = new JButton(IconBox.minAdd);
		button.setToolTipText(I18n.getMessage("thaw.common.add"));
		actions.add(new BoardManagementHelper.BoardAdder(mainPanel, button));
		buttonPanel.add(button);

		button = new JButton(IconBox.minDelete);
		button.setToolTipText(I18n.getMessage("thaw.common.remove"));
		actions.add(new BoardManagementHelper.BoardRemover(mainPanel, button));
		buttonPanel.add(button);

		button = new JButton(IconBox.minRefreshAction);
		button.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.loadNewMessages"));
		actions.add(new BoardManagementHelper.BoardRefresher(mainPanel, button));
		buttonPanel.add(button);

		button = new JButton(IconBox.minMarkAsRead);
		button.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.markAsRead"));
		actions.add(new BoardManagementHelper.MarkAllAsRead(mainPanel, button));
		buttonPanel.add(button);

		button = new JButton(IconBox.minMsgNew);
		button.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.newMessage"));
		actions.add(new BoardManagementHelper.NewMessage(mainPanel, button));
		buttonPanel.add(button);

		/* drafts state */

		draftsState = new JLabel("");
		updateDraftValues(0, 0);

		southPanel.add(new JLabel(""), BorderLayout.CENTER);
		southPanel.add(buttonPanel, BorderLayout.WEST);
		southPanel.add(draftsState, BorderLayout.SOUTH);

		panel.add(southPanel, BorderLayout.SOUTH);
	}


	protected class BoardListRenderer extends DefaultListCellRenderer {
		private MessageTreeTable messageTreeTable;

		public BoardListRenderer() {
			messageTreeTable = mainPanel.getMessageTreeTable();
		}

		public java.awt.Component getListCellRendererComponent(JList list, Object value,
								       int index, boolean isSelected,
								       boolean cellHasFocus) {
			Board board = (Board)value;

			String str = board.toString();

			int unread = 0;

			if ( (unread = board.getNewMessageNumber(messageTreeTable.seeUnsigned(),
								 messageTreeTable.seeArchived(),
								 messageTreeTable.getMinTrustLevel())) > 0)
				str += " ("+Integer.toString(unread)+")";

			java.awt.Component c = super.getListCellRendererComponent(list, str,
										  index, isSelected,
										  cellHasFocus);

			c.setFont(c.getFont().deriveFont((float)13.5));

			if (unread > 0)
				c.setFont(c.getFont().deriveFont(Font.BOLD));
			else
				c.setFont(c.getFont().deriveFont(Font.PLAIN));

			if (board.isRefreshing()) {
				if (isSelected) {
					c.setBackground(LOADING_SELECTION_COLOR);
				} else {
					c.setBackground(LOADING_COLOR);
				}
			} else if (isSelected) {
				c.setBackground(SELECTION_COLOR);
			}

			return c;
		}
	}


	protected class BoardListModel extends AbstractListModel {
		private Vector boardList;

		public BoardListModel() {
			super();
			boardList = null;
		}

		public Vector getBoardList() {
			synchronized(boardList) {
				return boardList;
			}
		}

		public void setBoardList(Vector l) {
			int oldSize = 0;

			if (boardList != null) {
				oldSize = boardList.size();
			}

			boardList = l;

			synchronized(boardList) {
				if (boardList.size() < oldSize)
					fireIntervalRemoved(this, boardList.size(), oldSize);

				if (boardList.size() > oldSize)
					fireIntervalAdded(this, oldSize, boardList.size());

				fireContentsChanged(this, 0, boardList.size());
			}
		}

		public void refresh(Board board) {
			synchronized(boardList) {
				refresh(boardList.indexOf(board));
			}
		}

		public void refresh(int row) {
			fireContentsChanged(this, row, row);
		}

		public Object getElementAt(int index) {
			if (boardList == null)
				return null;

			synchronized(boardList) {
				return boardList.get(index);
			}
		}

		public int getSize() {
			if (boardList == null)
				return 0;

			synchronized(boardList) {
				return boardList.size();
			}
		}
	}


	public Vector getBoards() {
		return model.getBoardList();
	}



	public void refresh() {
		Vector boards = new Vector();

		BoardFactory[] factories = mainPanel.getPluginCore().getFactories();

		if (factories != null) {
			for (int i = 0 ; i < factories.length; i++) {
				Vector v = factories[i].getBoards();

				if (v != null) {
					for (Iterator it = v.iterator();
					     it.hasNext();) {
						boards.add(it.next());
					}

				}
			}
		}

		java.util.Collections.sort(boards);

		model.setBoardList(boards);
	}

	public void refresh(Board board) {
		model.refresh(board);
	}

	public void refresh(int row) {
		model.refresh(row);
	}


	public void loadState() {

	}

	public void saveState() {

	}

	public JPanel getPanel() {
		return panel;
	}

	public void updateDraftValues(int waitings, int postings) {
		String str;

		str = DRAFTS_STR.replaceAll("X", Integer.toString(waitings));
		str = str.replaceAll(       "Y", Integer.toString(postings));

		draftsState.setText(str);
	}

	public void valueChanged(javax.swing.event.ListSelectionEvent e) {
		Board b = ((Board)list.getSelectedValue());

		for (Iterator it = actions.iterator();
		     it.hasNext();) {
			((BoardManagementHelper.BoardAction)it.next()).setTarget(b);
		}

		setChanged();
		notifyObservers(b);

		list.requestFocus();
	}


	public void mouseClicked(final MouseEvent e) {

	}

	public void mouseEntered(final MouseEvent e) { }
	public void mouseExited(final MouseEvent e) { }

	public void mousePressed(final MouseEvent e) {
		showPopupMenu(e);
	}

	public void mouseReleased(final MouseEvent e) {
		showPopupMenu(e);
	}

	protected void showPopupMenu(final MouseEvent e) {
		if(e.isPopupTrigger()) {
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
}
