package thaw.plugins.signatures;


import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import java.awt.BorderLayout;
import java.awt.GridLayout;


import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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


			dialog.setSize(500, 500);
			dialog.setVisible(true);
		}


		public void updateList() {

		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == addIdentity) {
				String nick = JOptionPane.showInputDialog(dialog,
									  I18n.getMessage("thaw.plugin.signature.enterNick"));
				Identity id = Identity.generate(nick);
				id.insert();
				updateList();
			}

			if (e.getSource() == removeIdentity) {

			}

			if (e.getSource() == closeWindow) {
				dialog.setVisible(false);
			}
		}
	}


	protected class OtherIdentitiesPanel implements ActionListener {
		public  OtherIdentitiesPanel() {

		}

		public void actionPerformed(ActionEvent e) {

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

