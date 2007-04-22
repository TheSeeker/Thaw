package thaw.plugins.signatures;


import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.BorderLayout;
import java.awt.GridLayout;


import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Vector;

import thaw.core.I18n;
import thaw.core.ConfigWindow;

import thaw.gui.IconBox;

import thaw.plugins.Hsqldb;



public class SigConfigTab implements ActionListener {
	private Hsqldb db;
	private ConfigWindow configWindow;


	private JPanel configPanel;

	private JButton yourIdentitiesButton;
	private JButton otherIdentitiesButton;


	public SigConfigTab(ConfigWindow configWin, Hsqldb db) {
		this.db = db;
		this.configWindow = configWin;

		configPanel = new JPanel();

		yourIdentitiesButton  = new JButton(I18n.getMessage("thaw.plugin.signature.yourIdentities"));
		otherIdentitiesButton = new JButton(I18n.getMessage("thaw.plugin.signature.otherIdentities"));

		yourIdentitiesButton.addActionListener(this);
		otherIdentitiesButton.addActionListener(this);


		configPanel.add(yourIdentitiesButton);
		configPanel.add(otherIdentitiesButton);
	}

	public JPanel getPanel() {
		return configPanel;
	}



	protected class YourIdentitiesPanel implements ActionListener {
		private JDialog dialog;

		private JList list;

		private JButton addIdentity;
		private JButton removeIdentity;
		private JButton closeWindow;


		public YourIdentitiesPanel() {
			dialog = new JDialog(configWindow.getFrame(),
					     I18n.getMessage("thaw.plugin.signature.dialogTitle.yourIdentities"));

			dialog.getContentPane().setLayout(new BorderLayout(5, 5));

			dialog.getContentPane().add(new JLabel(I18n.getMessage("thaw.plugin.signature.dialogTitle.yourIdentities")+" :"),
						    BorderLayout.NORTH);

			list = new JList();

			updateList();

			dialog.getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);

			JPanel southPanel = new JPanel(new BorderLayout());
			southPanel.add(new JLabel(""), BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel(new GridLayout(1, 3));

			addIdentity = new JButton(IconBox.minAdd);
			removeIdentity = new JButton(IconBox.minRemove);
			closeWindow = new JButton(IconBox.minClose);

			addIdentity.setToolTipText(I18n.getMessage("thaw.plugin.signature.addIdentity"));
			removeIdentity.setToolTipText(I18n.getMessage("thaw.plugin.signature.removeIdentity"));
			closeWindow.setToolTipText(I18n.getMessage("thaw.common.closeWin"));

			addIdentity.addActionListener(this);
			removeIdentity.addActionListener(this);
			closeWindow.addActionListener(this);

			buttonPanel.add(addIdentity);
			buttonPanel.add(removeIdentity);
			buttonPanel.add(closeWindow);

			southPanel.add(buttonPanel, BorderLayout.EAST);

			dialog.getContentPane().add(southPanel, BorderLayout.SOUTH);


			dialog.setSize(500, 300);
			dialog.setVisible(true);
		}


		public void updateList() {
			list.setListData(Identity.getYourIdentities(db));
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == addIdentity) {
				String nick = JOptionPane.showInputDialog(dialog,
									  I18n.getMessage("thaw.plugin.signature.enterNick"),
									  I18n.getMessage("thaw.plugin.signature.enterNick"),
									  JOptionPane.QUESTION_MESSAGE);

				if (nick != null) {
					Identity id = Identity.generate(db, nick);
					id.insert();
					updateList();
				}
			}

			if (e.getSource() == removeIdentity) {
				Identity i = (Identity)list.getSelectedValue();
				if (i != null) {
					i.delete();
					updateList();
				}
			}

			if (e.getSource() == closeWindow) {
				dialog.setVisible(false);
			}
		}
	}


	protected class IdentityModel extends javax.swing.table.AbstractTableModel {
		public String[] columnNames = {
			I18n.getMessage("thaw.plugin.signature.nickname"),
			I18n.getMessage("thaw.plugin.signature.trustLevel")
		};

		private Vector identities;

		public IdentityModel() {

		}

		public void setIdentities(Vector i) {
			identities = i;

			final TableModelEvent event = new TableModelEvent(this);
			fireTableChanged(event);
		}

		public int getRowCount() {
			if (identities == null)
				return 0;

			return identities.size();
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(final int column) {
			return columnNames[column];
		}

		public Object getValueAt(int row, int column) {
			if (identities == null)
				return null;

			if (column == 0)
				return ((Identity)identities.get(row)).toString();

			if (column == 1)
				return I18n.getMessage(((Identity)identities.get(row)).getTrustLevelStr());

			return null;
		}

		public Identity getIdentity(int line) {
			return (Identity)identities.get(line);
		}
	}


	protected class OtherIdentitiesPanel implements ActionListener {
		private JDialog dialog;
		private IdentityModel model;

		private JTable table;

		private JButton close;

		private Vector statusButtons;


		public  OtherIdentitiesPanel() {
			dialog = new JDialog(configWindow.getFrame(),
					     I18n.getMessage("thaw.plugin.signature.dialogTitle.yourIdentities"));

			dialog.getContentPane().setLayout(new BorderLayout(5, 5));

			model = new IdentityModel();

			table = new JTable(model);

			dialog.getContentPane().add(new JScrollPane(table),
						    BorderLayout.CENTER);

			JPanel eastPanel = new JPanel(new BorderLayout());

			JPanel statusButtonsPanel = new JPanel(new GridLayout(Identity.trustLevelInt.length -1, 1));

			statusButtons = new Vector();

			for (int i = 0 ; i < Identity.trustLevelInt.length ; i++) {
				if (Identity.trustLevelInt[i] < 100) {
					JButton button = new JButton(I18n.getMessage(Identity.trustLevelStr[i]));
					statusButtonsPanel.add(button);
					statusButtons.add(button);
					button.addActionListener(this);
				}
			}

			JPanel eastTopPanel = new JPanel();

			eastTopPanel.add(statusButtonsPanel);
			eastPanel.add(eastTopPanel, BorderLayout.CENTER);

			JPanel eastBottomPanel = new JPanel();

			close = new JButton(IconBox.minClose);
			close.addActionListener(this);

			eastBottomPanel.add(close);

			eastPanel.add(eastBottomPanel, BorderLayout.SOUTH);

			dialog.getContentPane().add(eastPanel, BorderLayout.EAST);

			updateList();

			dialog.setSize(500, 300);
			dialog.setVisible(true);
		}

		public void updateList() {
			model.setIdentities(Identity.getOtherIdentities(db));
		}

		public void actionPerformed(ActionEvent e) {

			if (e.getSource() == close) {
				dialog.setVisible(false);
			}

			int row = table.getSelectedRow();

			if (row < 0)
				return;

			Identity target = model.getIdentity(row);

			if (target == null)
				return;

			if (e.getSource() instanceof JButton) {
				JButton bt = (JButton)e.getSource();

				int i;

				for (i = 0 ; i < Identity.trustLevelStr.length ; i++) {
					if (I18n.getMessage(Identity.trustLevelStr[i]).equals(bt.getText()))
						break;
				}

				if (i >= Identity.trustLevelStr.length)
					return;

				target.setTrustLevel(Identity.trustLevelInt[i]);

				updateList();
			}

		}
	}



	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == yourIdentitiesButton) {
			new YourIdentitiesPanel();
		}

		if (e.getSource() == otherIdentitiesButton) {
			new OtherIdentitiesPanel();
		}
	}
}

