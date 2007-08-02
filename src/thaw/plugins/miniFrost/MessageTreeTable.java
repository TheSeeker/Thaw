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

import java.awt.Color;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.DefaultCellEditor;
import javax.swing.event.TableModelEvent;


import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;

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
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import java.util.HashMap;


import thaw.gui.Table;
import thaw.gui.IconBox;
import thaw.core.I18n;
import thaw.core.Logger;

import thaw.plugins.signatures.Identity;

import thaw.plugins.miniFrost.interfaces.Author;
import thaw.plugins.miniFrost.interfaces.Board;
import thaw.plugins.miniFrost.interfaces.BoardFactory;
import thaw.plugins.miniFrost.interfaces.Message;
import thaw.plugins.miniFrost.interfaces.Draft;


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
		I18n.getMessage("thaw.plugin.miniFrost.unarchivate"),
		I18n.getMessage("thaw.plugin.miniFrost.newMessage")
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

	private JCheckBox seeTree;
	private JCheckBox seeUnsigned;
	private JComboBox minTrustLevel;
	private int minTrustLevelInt;



	/** for the thread tree **/
	private MessageNodeTree messageNodeTree;
	private HashMap messageNodeHashMap;


	public MessageTreeTable(MiniFrostPanel mainPanel) {
		this.mainPanel = mainPanel;

		orderBy = Board.ORDER_DATE;
		desc = true;

		panel = new JPanel(new BorderLayout(5, 5));


		/* Actions */

		JPanel northPanel = new JPanel(new BorderLayout(20, 20));

		searchField = new JTextField("");
		everywhereBox = new JCheckBox(I18n.getMessage("thaw.plugin.miniFrost.onAllBoards"));
		searchButton = new JButton(IconBox.minSearch);
		searchButton.setToolTipText(I18n.getMessage("thaw.common.search"));

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

		panel.add(northPanel, BorderLayout.NORTH);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);


		/** some filters **/

		/* archived */

		seeArchived = new JCheckBox(I18n.getMessage("thaw.plugin.miniFrost.seeArchived"));
		seeArchived.setSelected(false);
		seeArchived.addActionListener(this);

		/* trust level */

		String minTrustLvlStr = mainPanel.getConfig().getValue("minTrustLevel");
		minTrustLevelInt = thaw.plugins.Signatures.DEFAULT_MIN_TRUST_LEVEL;

		if (minTrustLvlStr != null)
			minTrustLevelInt = Integer.parseInt(minTrustLvlStr);


		JPanel minTrustLevelPanel = new JPanel(new BorderLayout(5, 5));
		minTrustLevelPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.hideStatusBelow")), BorderLayout.WEST);
		minTrustLevel = new JComboBox(Identity.trustLevelUserStr);
		minTrustLevel.setSelectedItem(Identity.getTrustLevelStr(minTrustLevelInt));
		minTrustLevel.addActionListener(this);
		minTrustLevelPanel.add(minTrustLevel, BorderLayout.CENTER);

		seeUnsigned = new JCheckBox(I18n.getMessage("thaw.plugin.miniFrost.seeUnsigned"), true);
		seeUnsigned.addActionListener(this);

		seeTree = new JCheckBox(I18n.getMessage("thaw.plugin.miniFrost.seeTree"));
		seeTree.setSelected(true);
		seeTree.addActionListener(this);

		JPanel southWestPanel = new JPanel(new GridLayout(2, 1));
		southWestPanel.add(new JLabel(""));
		southWestPanel.add(seeTree);


		JPanel southEastPanel = new JPanel(new GridLayout(2, 1));

		JPanel southEastPanelTop = new JPanel(new GridLayout(1, 2, 10, 10));
		southEastPanelTop.add(seeUnsigned);
		southEastPanelTop.add(seeArchived);

		southEastPanel.add(southEastPanelTop);
		southEastPanel.add(minTrustLevelPanel);

		JPanel southPanel = new JPanel(new BorderLayout(5, 5));

		southPanel.add(southWestPanel, BorderLayout.WEST);
		southPanel.add(new JLabel(""), BorderLayout.CENTER);
		southPanel.add(southEastPanel, BorderLayout.EAST);

		panel.add(southPanel, BorderLayout.SOUTH);

		mainPanel.getBoardTree().addObserver(this);

		refresh();
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




	protected class MessageNodeTree extends JTree {

		public MessageNodeTree(TreeNode root) {
			super(root);

			DefaultTreeCellRenderer cellRenderer = (DefaultTreeCellRenderer)getCellRenderer();
			cellRenderer.setOpenIcon(cellRenderer.getDefaultLeafIcon());
			cellRenderer.setClosedIcon(cellRenderer.getDefaultLeafIcon());
		}

		protected int visibleRow;
		protected int rowHeight;

		public void setBounds(int x, int y, int w, int h) {
			super.setBounds(x, 0, w, table.getHeight());
		}

		public void paint(java.awt.Graphics g) {
			g.translate(0, (-(visibleRow+1) * rowHeight));
			super.paint(g);
		}

		public Component getTableCellRendererComponent(JTable table,
							       Object value,
							       boolean isSelected,
							       boolean hasFocus,
							       int row, int column) {
			if (isSelected)
				setSelectionRow(row+1); /* don't forget the root :) */

			setRowHeight(table.getRowHeight());
			rowHeight = table.getRowHeight();

			Message msg = ((MessageNode)value).getMessage();

			int mod = Font.PLAIN;

			if (msg != null) {

				if (!msg.isRead()) {
					mod = Font.BOLD;
				}

				if (msg.isArchived()) {
					if (mod == Font.BOLD)
						mod = Font.ITALIC | Font.BOLD;
					else
						mod = Font.ITALIC;
				}

			} else
				mod = Font.ITALIC;

			setFont(getFont().deriveFont(mod));

			if (msg != null && msg.getSender().getIdentity() != null)
				setForeground(msg.getSender().getIdentity().getTrustLevelColor());

			visibleRow = row;
			return this;
		}
	}


	protected static class RootMessageNode implements TreeNode {
		private Vector children;

		public RootMessageNode(Vector nodes) {
			this.children = nodes;
		}

		public Enumeration children() {
			return children.elements();
		}

		public boolean getAllowsChildren() {
			return true;
		}

		public TreeNode getChildAt(int childIndex) {
			return (MessageNode)children.get(childIndex);
		}

		public int getChildCount() {
			return children.size();
		}

		public int getIndex(TreeNode node) {
			return children.indexOf(node);
		}

		public TreeNode getParent() {
			return null;
		}

		public boolean isLeaf() {
			return (children.size() == 0);
		}
	}


	protected static class MessageNode implements TreeNode {
		private Vector children;

		private boolean hasParent;
		private TreeNode parent;

		private Message msg;

		public MessageNode(Message msg) {
			this.parent = null;
			this.msg = msg;
			children = new Vector(0);
		}

		public Message getMessage() {
			return msg;
		}

		public void setParent(TreeNode node) {
			parent = node;
		}

		/**
		 * will register
		 */
		public void setParent(HashMap messageNodes) {
			String inReplyTo;

			if (msg != null && (inReplyTo = msg.getInReplyToId()) != null) {
				hasParent = true;

				MessageNode node = (MessageNode)messageNodes.get(inReplyTo);

				if (node != null) {
					setParent(node);
					node.registerChild(this);
				}
			}
		}

		public boolean hasParent() {
			return hasParent;
		}

		public void registerChild(MessageNode node) {
			children.add(node);
		}

		public java.util.Enumeration children() {
			return children.elements();
		}

		public boolean getAllowsChildren() {
			return true;
		}

		public TreeNode getChildAt(int childIndex) {
			return (MessageNode)children.get(childIndex);
		}

		public int getChildCount() {
			return children.size();
		}

		public int getIndex(TreeNode node) {
			return children.indexOf(node);
		}

		public TreeNode getParent() {
			return parent;
		}

		public boolean isLeaf() {
			return (children.size() == 0);
		}

		public String toString() {
			if (msg != null)
				return msg.getSubject();
			else
				return "(?)";
		}
	}



	protected class MessageTableRenderer extends Table.DefaultRenderer {
		private JCheckBox checkBoxRenderer;

		public MessageTableRenderer() {
			super();
			this.checkBoxRenderer = new JCheckBox();
		}

		public Component getTableCellRendererComponent(final JTable table, Object value,
							       final boolean isSelected, final boolean hasFocus,
							       final int row, final int column) {
			Component c;

			Message msg = model.getMsg(row);
			Author author = (msg != null ? msg.getSender() : null);

			if (value instanceof Boolean) {

				checkBoxRenderer.setEnabled(msg != null);

				if (msg != null)
					checkBoxRenderer.setSelected(((Boolean)value).booleanValue());
				else
					checkBoxRenderer.setSelected(false);

				return checkBoxRenderer;
			}

			if (value instanceof MessageNode) {
				return messageNodeTree.getTableCellRendererComponent(table,
										     value,
										     isSelected,
										     hasFocus,
										     row,
										     column);
			}

			Color color = Color.BLACK;


			if (column == 2) {
				value = ((author != null) ? author.toString() : "(?)");
			} else if (column == 3) {
				if (author != null && author.getIdentity() != null)
					value = author.getIdentity().getTrustLevelStr();
				else
					value = I18n.getMessage("thaw.plugin.signature.trustLevel.none");
			}

			if (author != null && author.getIdentity() != null)
				color = author.getIdentity().getTrustLevelColor();

			c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
								row, column);

			c.setForeground(color);

			int mod = Font.PLAIN;

			if (msg != null) {
				if (!msg.isRead()) {
					mod = Font.BOLD;
				}

				if (msg.isArchived()) {
					if (mod == Font.BOLD)
						mod = Font.ITALIC | Font.BOLD;
					else
						mod = Font.ITALIC;
				}
			}
			else
				mod = Font.ITALIC;

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
			return ((MessageNode)msgs.get(row)).getMessage();
		}

		public Object getValueAt(int row, int column) {
			if (column == 0) {
				return new Boolean(selection[row]);
			}

			if (column == 1) {
				return (MessageNode)msgs.get(row);
			}

			Message msg = ((MessageNode)msgs.get(row)).getMessage();

			if (msg == null)
				return "(?)";

			if (column == 2) {
				return msg.getSender();
			}

			if (column == 3) {
				return msg.getSender();
			}

			if (column == 4) {
				return msg.getDate();
			}

			if (column == 5) {
				return msg; //.getMessage();
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
			for (int i = 0 ; i < selection.length ; i++) {
				Message msg = ((MessageNode)msgs.get(i)).getMessage();

				if (msg != null)
					selection[i] = s;
			}
		}


		public boolean[] getSelection() {
			return selection;
		}


		public void switchSelection(int row) {
			Message msg = ((MessageNode)msgs.get(row)).getMessage();

			if (msg != null)
				selection[row] = !selection[row];
			else
				selection[row] = false;
		}

		public Vector getMessages(Vector msgs) {
			return msgs;
		}

		public void refresh(Message msg) {
			/* quick and dirty */
			refresh();
		}

		public void refresh(MessageNode msgNode) {
			refresh(msgs.indexOf(msgNode));
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


	private boolean rebuildMsgList(Vector msgs, TreeNode node, int proof) {
		if (node instanceof MessageNode)
			msgs.add(node);

		if (proof >= 20) {
			Logger.notice(this, "Too proof, sorry");
			return false;
		}

		for(Enumeration e = node.children();
		    e.hasMoreElements();) {
			TreeNode sub = (TreeNode)e.nextElement();
			if (!rebuildMsgList(msgs, sub, proof+1))
				return false;
		}

		return true;
	}



	public void refresh(String[] keywords, int orderBy, boolean desc, boolean allBoards) {
		Vector msgs = null;

		if ((!allBoards) && targetBoard != null) {
			Vector rawMsgs = targetBoard.getMessages(keywords, orderBy,
								 desc, seeArchived.isSelected(),
								 seeUnsigned.isSelected(),
								 minTrustLevelInt);

			msgs = new Vector(rawMsgs.size());

			for(Iterator it = rawMsgs.iterator();
			    it.hasNext();) {
				msgs.add(new MessageNode((Message)it.next()));
			}
		}

		if (allBoards) {
			msgs = new Vector();

			BoardFactory[] factories = mainPanel.getPluginCore().getFactories();

			for (int i = 0 ; i < factories.length ; i++) {
				Vector boardMsgs = factories[i].getAllMessages(keywords, orderBy, desc,
									       seeArchived.isSelected(),
									       seeUnsigned.isSelected(),
									       minTrustLevelInt);
				for (Iterator it = boardMsgs.iterator();
				     it.hasNext();) {
					msgs.add(new MessageNode((Message)it.next()));
				}
			}
		}

		if (msgs == null) {
			msgs = new Vector();
		}

		Logger.info(this, "Nmb msgs in the tree : "+Integer.toString(msgs.size()));

		Vector rootNodes;

		if (seeTree.isSelected()) {

			/** Filling in messageNodeHashMap **/
			messageNodeHashMap = new HashMap(msgs.size());

			for (Iterator it = msgs.iterator();
			     it.hasNext();) {
				MessageNode node = (MessageNode)it.next();
				messageNodeHashMap.put(node.getMessage().getMsgId(), node);
			}


			/** Building the tree **/
			for (Iterator it = msgs.iterator();
			     it.hasNext();) {
				((MessageNode)it.next()).setParent(messageNodeHashMap);
			}

			/** we search the nodes who should have a parent but haven't **/
			/* we don't use an iterator to avoid the collisions */
			for (int i = 0 ; i < msgs.size(); i++) {
				MessageNode node = (MessageNode)msgs.get(i);

				if (node.getParent() == null && node.hasParent()) {
					MessageNode newEmptyNode = new MessageNode(null);
					node.setParent(newEmptyNode);
					newEmptyNode.registerChild(node);

					/* we replace */
					msgs.set(i, newEmptyNode);
					/* and readd the other node at the end of the vector */
					msgs.add(node);
				}
			}

			rootNodes = new Vector();

			/* Building the root tree */

			for (Iterator it = msgs.iterator();
			     it.hasNext();) {
				MessageNode node = (MessageNode)it.next();

				if (node.getParent() == null)
					rootNodes.add(node);
			}
		} else
			rootNodes = msgs;


		/* we add all the message without any parent to the root */
		RootMessageNode rootNode = new RootMessageNode(rootNodes);

		for (Iterator it = rootNodes.iterator();
		     it.hasNext();) {
			((MessageNode)it.next()).setParent(rootNode);
		}


		/** and to finish, the tree itself **/
		messageNodeTree = new MessageNodeTree(rootNode);

		for (int i = 0 ; i < messageNodeTree.getRowCount() ; i++) {
			messageNodeTree.expandRow(i);
		}

		/** next we check we have the same order in the tree and in the table **/

		msgs = new Vector();

		rebuildMsgList(msgs, rootNode, 0);

		Logger.info(this, "Nmb msgs in the tree : "+Integer.toString(msgs.size()));

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

	public boolean nextUnread() {

		if (targetBoard == null) {
			Logger.warning(this, "No message selected atm ; can't get the next unread message");
			return false;
		}

		Message newMsg = targetBoard.getNextUnreadMessage(seeUnsigned.isSelected(),
								  minTrustLevelInt);

		if (newMsg != null) {
			mainPanel.getMessagePanel().setMessage(newMsg);
			newMsg.setRead(true);
			refresh();
			mainPanel.getBoardTree().refresh(targetBoard);
			mainPanel.displayMessage();

			return true;
		}

		return false;
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == seeUnsigned
		    || e.getSource() == minTrustLevel
		    || e.getSource() == seeArchived
		    || e.getSource() == seeTree) {

			minTrustLevelInt = Identity.getTrustLevel((String)(minTrustLevel.getSelectedItem()));
			refresh();

		} else if (e.getSource() == searchButton
		    || e.getSource() == searchField) {

			keywords = searchField.getText().split(" ");

			Logger.info(this, "Searching ...");

			refresh(keywords, orderBy, desc, everywhereBox.isSelected());

		} else if (e.getSource() == nextUnread) {

			nextUnread();

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
			} else if (sel == 7) { /* new message */
				Draft draft = targetBoard.getDraft(null);
				mainPanel.getDraftPanel().setDraft(draft);
				mainPanel.displayDraftPanel();
			}

			actions.setSelectedIndex(0);
		}
	}



	public void mouseClicked(MouseEvent e)  {
		int row    = table.rowAtPoint(e.getPoint());
		int column = table.columnAtPoint(e.getPoint());

		Logger.info(this, "Mouse clicked");

		if (column == 0) {
			model.switchSelection(row);
			refresh(row);
		} else {
			Message msg = model.getMsg(row);
			if (msg != null) {
				mainPanel.getMessagePanel().setMessage(msg);
				mainPanel.displayMessage();
			}
		}
	}

	public void mouseEntered(MouseEvent e)  { }
	public void mouseExited(MouseEvent e)   { }
	public void mousePressed(MouseEvent e)  { }
	public void mouseReleased(MouseEvent e) { }
}
