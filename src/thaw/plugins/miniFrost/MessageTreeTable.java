package thaw.plugins.miniFrost;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.util.Observer;
import java.util.Observable;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JButton;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.DefaultCellEditor;
import javax.swing.event.TableModelEvent;

import java.util.Vector;
import java.awt.Component;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.table.TableCellEditor;
import javax.swing.event.CellEditorListener;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import java.util.EventObject;

import javax.swing.JComboBox;
import javax.swing.JCheckBox;

import java.awt.Font;

import java.awt.event.KeyEvent;


import thaw.gui.Table;
import thaw.gui.IconBox;
import thaw.core.I18n;
import thaw.core.Logger;

import thaw.plugins.miniFrost.interfaces.Board;
import thaw.plugins.miniFrost.interfaces.BoardFactory;
import thaw.plugins.miniFrost.interfaces.Message;


public class MessageTreeTable implements Observer,
					 MouseListener,
					 ActionListener
{

	public final static String[] COLUMNS = {
		"", /* checkboxes */
		I18n.getMessage("thaw.plugin.miniFrost.subject"),
		I18n.getMessage("thaw.plugin.miniFrost.author"),
		I18n.getMessage("thaw.plugin.miniFrost.status"), /* author status */
		I18n.getMessage("thaw.plugin.miniFrost.date"),
	};


	public final static String[] ACTIONS = new String[] {
		"",
		I18n.getMessage("thaw.plugin.miniFrost.selectAll"),
		I18n.getMessage("thaw.plugin.miniFrost.selectNone"),
		I18n.getMessage("thaw.plugin.miniFrost.markAsRead"),
		I18n.getMessage("thaw.plugin.miniFrost.markAsNonRead"),
		I18n.getMessage("thaw.plugin.miniFrost.archivate"),
		I18n.getMessage("thaw.plugin.miniFrost.unarchivate")
	};


	public final static int FIRST_COLUMN_SIZE = 25;
	public final static int DEFAULT_ROW_HEIGHT = 20;


	private MiniFrostPanel mainPanel;
	private JPanel panel;

	private Board targetBoard;


	private MessageTableModel model;
	private Table table;


	private JTextField searchField;
	private JCheckBox everywhereBox;
	private JButton searchButton;
	private JButton nextUnread;

	private JCheckBox seeArchived;

	private JComboBox actions;

	private String[] keywords;
	private int orderBy;
	private boolean desc;


	public MessageTreeTable(MiniFrostPanel mainPanel) {
		this.mainPanel = mainPanel;

		orderBy = Board.ORDER_DATE;
		desc = true;

		panel = new JPanel(new BorderLayout(5, 5));


		/* Actions */

		JPanel northPanel = new JPanel(new BorderLayout(20, 20));

		searchField = new JTextField("");
		everywhereBox = new JCheckBox(I18n.getMessage("thaw.plugin.miniFrost.onAllBoards"));
		searchButton = new JButton(I18n.getMessage("thaw.common.search"),
					   IconBox.minSearch);

		searchButton.addActionListener(this);
		searchField.addActionListener(this);


		nextUnread = new JButton("", IconBox.minNextUnread);
		nextUnread.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.nextUnread"));
		nextUnread.addActionListener(this);

		JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
		searchPanel.add(searchField, BorderLayout.CENTER);

		JPanel boxAndButtonPanel = new JPanel(new BorderLayout(5, 5));
		boxAndButtonPanel.add(everywhereBox, BorderLayout.CENTER);
		boxAndButtonPanel.add(searchButton, BorderLayout.EAST);

		searchPanel.add(nextUnread, BorderLayout.WEST);
		searchPanel.add(boxAndButtonPanel, BorderLayout.EAST);


		northPanel.add(searchPanel, BorderLayout.CENTER);

		actions = new JComboBox(ACTIONS);
		actions.addActionListener(this);

		northPanel.add(actions, BorderLayout.EAST);


		/* Table */

		model = new MessageTableModel();
		table = new Table(mainPanel.getConfig(),
				  "table_minifrost_message_table",
				  model);
		table.setDefaultRenderer(table.getColumnClass(0), new MessageTableRenderer());
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

		table.getColumnModel().getColumn(0).setPreferredWidth(FIRST_COLUMN_SIZE);
		table.getColumnModel().getColumn(0).setResizable(false);
		table.getColumnModel().getColumn(0).setPreferredWidth(FIRST_COLUMN_SIZE);
		table.getColumnModel().getColumn(0).setMinWidth(FIRST_COLUMN_SIZE);
		table.getColumnModel().getColumn(0).setMaxWidth(FIRST_COLUMN_SIZE);

		table.setRowHeight(DEFAULT_ROW_HEIGHT);

		table.addMouseListener(this);

		setBoard(null);
		refresh();


		panel.add(northPanel, BorderLayout.NORTH);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);


		/* some filters */

		seeArchived = new JCheckBox(I18n.getMessage("thaw.plugin.miniFrost.seeArchived"));
		seeArchived.setSelected(false);
		seeArchived.addActionListener(this);

		JPanel southPanel = new JPanel(new BorderLayout(5, 5));
		southPanel.add(new JLabel(""), BorderLayout.CENTER);
		southPanel.add(seeArchived, BorderLayout.EAST);

		panel.add(southPanel, BorderLayout.SOUTH);

		mainPanel.getBoardTree().addObserver(this);
	}


	public void hided() {
		nextUnread.setMnemonic(KeyEvent.VK_Z);
	}

	public void redisplayed() {
		/**
		 * due to a swing bug ?
		 */
		table.getColumnModel().getColumn(0).setPreferredWidth(FIRST_COLUMN_SIZE);
		table.getColumnModel().getColumn(0).setResizable(false);
		table.getColumnModel().getColumn(0).setPreferredWidth(FIRST_COLUMN_SIZE);
		table.getColumnModel().getColumn(0).setMinWidth(FIRST_COLUMN_SIZE);
		table.getColumnModel().getColumn(0).setMaxWidth(FIRST_COLUMN_SIZE);

		nextUnread.setMnemonic(KeyEvent.VK_N);
		nextUnread.requestFocus();
	}

	public JPanel getPanel() {
		return panel;
	}

	protected class MessageTableRenderer extends Table.DefaultRenderer {
		public MessageTableRenderer() {
			super();
		}

		public Component getTableCellRendererComponent(final JTable table, Object value,
							       final boolean isSelected, final boolean hasFocus,
							       final int row, final int column) {
			Component c;

			if (value instanceof Boolean) {
				JCheckBox box = new JCheckBox();

				box.setSelected(((Boolean)value).booleanValue());

				return box;
			}

			if (column == 3) {
				value = "NOT CHECKED";
			}

			c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
								row, column);

			Message msg = model.getMsg(row);

			int mod = Font.PLAIN;

			if (!msg.isRead()) {
				mod = Font.BOLD;
			}

			if (msg.isArchived()) {
				if (mod == Font.BOLD)
					mod = Font.ITALIC | Font.BOLD;
				else
					mod = Font.ITALIC;
			}

			c.setFont(c.getFont().deriveFont(mod));

			return c;
		}
	}


	protected class MessageTableModel
		extends javax.swing.table.AbstractTableModel {


		private Vector msgs;
		private boolean[] selection;

		public MessageTableModel() {
			super();
			this.msgs = null;
		}

		public int getRowCount() {
			if (msgs == null) return 0;
			return msgs.size();
		}

		public int getColumnCount() {
			return COLUMNS.length;
		}

		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		public Message getMsg(int row) {
			return (Message)msgs.get(row);
		}

		public Object getValueAt(int row, int column) {
			if (column == 0) {
				return new Boolean(selection[row]);
			}

			if (column == 1) {
				Message msg = (Message)msgs.get(row);
				return "("+Integer.toString(msg.getRev()) + ") "+
					msg.getSubject();
			}

			if (column == 2) {
				return ((Message)msgs.get(row)).getSender().toString();
			}

			if (column == 3) {
				return ((Message)msgs.get(row)).getSender();
			}

			if (column == 4) {
				return ((Message)msgs.get(row)).getDate();
			}

			if (column == 5) {
				return ((Message)msgs.get(row));
			}

			return null;
		}

		public void setMessages(Vector msgs) {
			this.msgs = msgs;

			int lng = msgs.size();

			selection = new boolean[lng];

			for (int i = 0 ; i < lng ; i++)
				selection[i] = false;
		}


		public void setSelectedAll(boolean s) {
			for (int i = 0 ; i < selection.length ; i++)
				selection[i] = s;
		}


		public boolean[] getSelection() {
			return selection;
		}


		public void switchSelection(int row) {
			selection[row] = !selection[row];
		}

		public Vector getMessages(Vector msgs) {
			return msgs;
		}

		public void refresh(Message msg) {
			refresh(msgs.indexOf(msg));
		}

		public void refresh(int row) {
			if (row == -1) {
				Logger.error(this, "Message not found in the list ?!");
				return;
			}

			fireTableChanged(new TableModelEvent(this, row));
		}

		public void refresh() {
			fireTableChanged(new TableModelEvent(this));
		}
	}

	public void setBoard(Board board) {
		this.targetBoard = board;

		searchField.setText("");
		everywhereBox.setSelected(false);
		keywords = null;
	}

	public Board getBoard() {
		return targetBoard;
	}

	public void refresh() {
		refresh(keywords, orderBy, desc, everywhereBox.isSelected());
	}

	public void refresh(String[] keywords, int orderBy, boolean desc, boolean allBoards) {
		Vector msgs = null;

		if ((!allBoards) && targetBoard != null) {
			msgs = targetBoard.getMessages(keywords, orderBy,
						       desc, seeArchived.isSelected());
		}

		if (allBoards) {
			msgs = new Vector();

			BoardFactory[] factories = mainPanel.getPluginCore().getFactories();

			for (int i = 0 ; i < factories.length ; i++) {
				Vector boardMsgs = factories[i].getAllMessages(keywords, orderBy, desc,
									       seeArchived.isSelected());
				msgs.addAll(boardMsgs);
			}
		}

		if (msgs != null)
			model.setMessages(msgs);

		model.refresh();
	}

	public void refresh(Message msg) {
		model.refresh(msg);
	}

	public void refresh(int row) {
		model.refresh(row);
	}


	public void update(Observable o, Object param) {
		if (o == mainPanel.getBoardTree()) {
			setBoard((Board)param);
			refresh();
		}
	}



	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == seeArchived) {

			refresh();

		} else if (e.getSource() == searchButton
		    || e.getSource() == searchField) {

			keywords = searchField.getText().split(" ");

			Logger.info(this, "Searching ...");

			refresh(keywords, orderBy, desc, everywhereBox.isSelected());

		} else if (e.getSource() == nextUnread) {

			if (targetBoard == null) {
				Logger.warning(this, "No message selected atm ; can't get the next unread message");
				return;
			}

			Message newMsg = targetBoard.getNextUnreadMessage();

			if (newMsg != null) {
				mainPanel.getMessagePanel().setMessage(newMsg);
				newMsg.setRead(true);
				refresh();
				mainPanel.getBoardTree().refresh(targetBoard);
				mainPanel.displayMessage();
			}

		} else if (e.getSource() == actions) {
			int sel = actions.getSelectedIndex();
			boolean[] selected = model.getSelection();

			Logger.info(this, "Applying action : "+Integer.toString(sel));

			if (sel <= 0)
				return;

			if (sel == 3 || sel == 4) { /* mark as (non-)read */
				boolean markAsRead = (sel == 3);

				for (int i = 0 ; i < selected.length ; i++) {
					if (selected[i]) {
						model.getMsg(i).setRead(markAsRead);
						model.refresh(i);
					}
				}

				mainPanel.getBoardTree().refresh(targetBoard);
			} else if (sel == 5 || sel == 6) { /* (un)archive */
				boolean archive = (sel == 5);

				for (int i = 0 ; i < selected.length ; i++) {
					if (selected[i])
						model.getMsg(i).setArchived(archive);
				}
				refresh();

				mainPanel.getBoardTree().refresh(targetBoard);
			} else if (sel == 1 || sel == 2) { /* (un)select all */
				boolean select = (sel == 1);
				model.setSelectedAll(select);
				model.refresh();
			}

			actions.setSelectedIndex(0);
		}
	}



	public void mouseClicked(MouseEvent e)  {
		int row    = table.rowAtPoint(e.getPoint());
		int column = table.columnAtPoint(e.getPoint());

		if (column == 0) {
			model.switchSelection(row);
			refresh(row);
		} else {
			mainPanel.getMessagePanel().setMessage(model.getMsg(row));
			mainPanel.displayMessage();
		}
	}

	public void mouseEntered(MouseEvent e)  { }
	public void mouseExited(MouseEvent e)   { }
	public void mousePressed(MouseEvent e)  { }
	public void mouseReleased(MouseEvent e) { }
}
