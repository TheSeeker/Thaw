package thaw.plugins.signatures;


import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.JComboBox;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Vector;

import java.io.File;


import thaw.core.I18n;
import thaw.core.ConfigWindow;
import thaw.core.Config;
import thaw.core.Logger;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;

import thaw.gui.IconBox;
import thaw.gui.FileChooser;
import java.util.Observer;
import java.util.Observable;


import thaw.plugins.Hsqldb;



public class SigConfigTab implements ActionListener, Observer {
	private Hsqldb db;
	private ConfigWindow configWindow;
	private Config config;

	private JPanel configPanel;

	private JButton yourIdentitiesButton;
	private JButton otherIdentitiesButton;

	private JComboBox minLevel;


	public SigConfigTab(Config config, ConfigWindow configWin, Hsqldb db) {
		this.db = db;
		this.configWindow = configWin;
		this.config = config;


		configPanel = new JPanel(new BorderLayout(5, 5));

		JPanel topPanel = new JPanel();

		yourIdentitiesButton  = new JButton(I18n.getMessage("thaw.plugin.signature.yourIdentities"));
		otherIdentitiesButton = new JButton(I18n.getMessage("thaw.plugin.signature.otherIdentities"));

		yourIdentitiesButton.addActionListener(this);
		otherIdentitiesButton.addActionListener(this);


		topPanel.add(yourIdentitiesButton);
		topPanel.add(otherIdentitiesButton);

		configPanel.add(topPanel, BorderLayout.NORTH);

		JPanel middlePanel = new JPanel();

		JLabel l = new JLabel(I18n.getMessage("thaw.plugin.signature.ignoreLowerThan")+" : ");
		Vector possibleLevels = new Vector();

		for (int i = 0 ; i < Identity.trustLevelInt.length ; i++) {
			if (Identity.trustLevelInt[i] < 100)
				possibleLevels.add(Identity.trustLevelStr[i]);
		}

		minLevel = new JComboBox(possibleLevels);

		reset();

		middlePanel.add(l);
		middlePanel.add(minLevel);

		configPanel.add(middlePanel, BorderLayout.CENTER);

		configWindow.addObserver(this);
	}

	public void destroy() {
		configWindow.deleteObserver(this);
	}

	public JPanel getPanel() {
		return configPanel;
	}

	public void apply() {
		int i ;

		String val = (String)minLevel.getSelectedItem();

		if (val == null) {
			Logger.error(this, "no value selected ?!");
			return;
		}

		for (i = 0 ; i < Identity.trustLevelStr.length ; i++) {
			if (Identity.trustLevelStr[i].equals(val))
				break;
		}

		if (i >= Identity.trustLevelStr.length)
			return;

		Logger.info(this, "Setting min trust level to : "+Integer.toString(Identity.trustLevelInt[i]));

		config.setValue("minTrustLevel", Integer.toString(Identity.trustLevelInt[i]));
	}

	public void reset() {
		int i;

		int min = Integer.parseInt(config.getValue("minTrustLevel"));

		for (i = 0 ; i < Identity.trustLevelInt.length ; i++) {
			if (Identity.trustLevelInt[i] == min)
				break;
		}

		if (i >= Identity.trustLevelInt.length)
			return;

		minLevel.setSelectedItem(Identity.trustLevelStr[i]);
	}


	public void update(Observable o, Object param) {
		if (param == configWindow.getOkButton())
			apply();
		else if (param == configWindow.getCancelButton())
			reset();
	}
	
	
	/************************ YOUR IDENTITIES ********************************/


	protected class YourIdentitiesPanel implements ActionListener {
		private JDialog dialog;

		private JList list;

		private JButton addIdentity;
		private JButton removeIdentity;

		private JButton exportIdentity;
		private JButton importIdentity;

		private JButton closeWindow;


		public YourIdentitiesPanel() {
			dialog = new JDialog(configWindow.getFrame(),
					     I18n.getMessage("thaw.plugin.signature.dialogTitle.yourIdentities"));


			dialog.getContentPane().setLayout(new BorderLayout(5, 5));

			JLabel label = new JLabel(I18n.getMessage("thaw.plugin.signature.dialogTitle.yourIdentities")+" :");
			label.setIcon(IconBox.identity);

			dialog.getContentPane().add(label,
						    BorderLayout.NORTH);

			list = new JList();
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			updateList();

			dialog.getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);

			JPanel southPanel = new JPanel(new BorderLayout());
			southPanel.add(new JLabel(""), BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel(new GridLayout(1, 3));

			addIdentity    = new JButton(IconBox.minAdd);
			removeIdentity = new JButton(IconBox.minRemove);
			importIdentity = new JButton(IconBox.minImportAction);
			exportIdentity = new JButton(IconBox.minExportAction);
			closeWindow    = new JButton(IconBox.minClose);

			addIdentity.setToolTipText(I18n.getMessage("thaw.plugin.signature.addIdentity"));
			removeIdentity.setToolTipText(I18n.getMessage("thaw.plugin.signature.removeIdentity"));
			importIdentity.setToolTipText(I18n.getMessage("thaw.plugin.signature.import"));
			exportIdentity.setToolTipText(I18n.getMessage("thaw.plugin.signature.export"));
			closeWindow.setToolTipText(I18n.getMessage("thaw.common.closeWin"));

			addIdentity.addActionListener(this);
			removeIdentity.addActionListener(this);
			importIdentity.addActionListener(this);
			exportIdentity.addActionListener(this);
			closeWindow.addActionListener(this);

			buttonPanel.add(addIdentity);
			buttonPanel.add(removeIdentity);
			buttonPanel.add(importIdentity);
			buttonPanel.add(exportIdentity);
			buttonPanel.add(closeWindow);

			southPanel.add(buttonPanel, BorderLayout.EAST);

			dialog.getContentPane().add(southPanel, BorderLayout.SOUTH);

			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setSize(500, 300);
			dialog.setVisible(true);
		}


		public void updateList() {
			list.setListData(Identity.getYourIdentities(db));
		}


		private class IdentityAdder implements ThawRunnable {


			public void run() {
				dialog.setEnabled(false);
				String nick = JOptionPane.showInputDialog(dialog,
									  I18n.getMessage("thaw.plugin.signature.enterNick"),
									  I18n.getMessage("thaw.plugin.signature.enterNick"),
									  JOptionPane.QUESTION_MESSAGE);
				dialog.setEnabled(true);

				if (nick != null) {
					Identity id = Identity.generate(db, nick);
					id.insert();
					updateList();
				}
			}

			public void stop() {
				/* \_o< */
			}
		}


		private class IdentityImporter implements ThawRunnable {
			public IdentityImporter() { }

			public void run() {
				FileChooser fc = new FileChooser();
				fc.setTitle(I18n.getMessage("thaw.plugin.signature.import"));
				fc.setDirectoryOnly(false);
				fc.setDialogType(FileChooser.OPEN_DIALOG);

				File file = fc.askOneFile();

				if (file != null) {
					Identity.importIdentity(db, file);
					updateList();
				}
			}

			public void stop() {
				/* \_o< */
			}
		}


		private class IdentityExporter implements ThawRunnable {
			private Identity i;

			public IdentityExporter(Identity i) {
				this.i = i;
			}

			public void run() {
				FileChooser fc = new FileChooser();
				fc.setTitle(I18n.getMessage("thaw.plugin.signature.export"));
				fc.setDirectoryOnly(false);
				fc.setDialogType(FileChooser.SAVE_DIALOG);

				File file = fc.askOneFile();

				if (file != null) {
					i.exportIdentity(file);
					updateList();
				}
			}

			public void stop() {
				/* \_o< */
			}
		}


		private class IdentityDeleter implements ThawRunnable {
			private Identity i;

			public IdentityDeleter(Identity i) {
				this.i = i;
			}

			public void run() {
				int ret = JOptionPane.showConfirmDialog(dialog,
									I18n.getMessage("thaw.plugin.signature.delete.areYouSure"),
									I18n.getMessage("thaw.plugin.signature.delete.areYouSureTitle"),
									JOptionPane.YES_NO_OPTION);
				if (ret != JOptionPane.YES_OPTION) {
					Logger.info(this, "Deletion cancelled");
					return;
				}

				i.delete();
				updateList();
			}

			public void stop() {
				/* \_o< */
			}
		}


		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == addIdentity) {
				Thread th = new ThawThread(new IdentityAdder(), "Identity adder", this);
				th.start();
			}

			if (e.getSource() == removeIdentity) {
				Identity i = (Identity)list.getSelectedValue();
				if (i != null) {
					Thread th = new ThawThread(new IdentityDeleter(i), "Identity deleter", this);
					th.start();
				}
			}

			if (e.getSource() == importIdentity) {
				Thread th = new ThawThread(new IdentityImporter(), "Identity importer", this);
				th.start();
			}

			if (e.getSource() == exportIdentity) {
				Identity i = (Identity)list.getSelectedValue();

				if (i != null) {
					Thread th = new ThawThread(new IdentityExporter(i), "Identity exporter", this);
					th.start();
				}
			}

			if (e.getSource() == closeWindow) {
				dialog.setVisible(false);
				dialog.dispose();
			}
		}


	}
	
	
	/********************* OTHER IDENTITIES **********************************/


	protected class OtherIdentitiesPanel implements ActionListener, TrustListParser.TrustListContainer {
		private JDialog dialog;

		private IdentityTable table;

		private JButton close;

		private JButton setOriginal;
		private JButton exportButton, importButton;


		public  OtherIdentitiesPanel() {
			dialog = new JDialog(configWindow.getFrame(),
					     I18n.getMessage("thaw.plugin.signature.dialogTitle.otherIdentities"));

			dialog.getContentPane().setLayout(new BorderLayout(5, 5));

			JLabel label = new JLabel(I18n.getMessage("thaw.plugin.signature.signatures"));
			label.setIcon(IconBox.peers);

			dialog.getContentPane().add(label, BorderLayout.NORTH);

			table = new IdentityTable(config, "other_identities_table", true);

			dialog.getContentPane().add(new JScrollPane(table.getTable()),
										BorderLayout.CENTER);

			JPanel eastPanel = new JPanel(new BorderLayout());

			JPanel buttonsPanel = new JPanel(new GridLayout(Identity.trustLevelInt.length +4, 1));

			for (int i = 0 ; i < Identity.trustLevelInt.length ; i++) {
				if (Identity.trustLevelInt[i] < 100) {
					JButton button = new JButton(Identity.trustLevelStr[i]);
					buttonsPanel.add(button);
					button.addActionListener(this);
				}
			}

			buttonsPanel.add(new JLabel(""));
			setOriginal = new JButton(I18n.getMessage("thaw.plugin.signature.setOriginal"));
			buttonsPanel.add(setOriginal);
			setOriginal.addActionListener(this);
			
			buttonsPanel.add(new JLabel(""));
			exportButton = new JButton(I18n.getMessage("thaw.plugin.signature.trustList.export.short"),
										IconBox.minExportAction);
			exportButton.setToolTipText(I18n.getMessage("thaw.plugin.signature.trustList.export.long"));
			exportButton.addActionListener(this);
			buttonsPanel.add(exportButton);
			
			importButton = new JButton(I18n.getMessage("thaw.plugin.signature.trustList.import.short"),
										IconBox.minImportAction);
			importButton.setToolTipText(I18n.getMessage("thaw.plugin.signature.trustList.import.long"));
			importButton.addActionListener(this);
			buttonsPanel.add(importButton);


			JPanel eastTopPanel = new JPanel();

			eastTopPanel.add(buttonsPanel);
			eastPanel.add(eastTopPanel, BorderLayout.CENTER);

			JPanel eastBottomPanel = new JPanel();

			close = new JButton(I18n.getMessage("thaw.common.close"),
								IconBox.minClose);
			close.setToolTipText(I18n.getMessage("thaw.common.closeWin"));
			close.addActionListener(this);

			eastBottomPanel.add(close);

			eastPanel.add(eastBottomPanel, BorderLayout.SOUTH);

			dialog.getContentPane().add(eastPanel, BorderLayout.EAST);

			updateList();
			
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setSize(640, 500);
			dialog.setVisible(true);
		}

		public void updateList() {
			table.setIdentities(Identity.getOtherIdentities(db));
		}

		public void actionPerformed(ActionEvent e) {

			if (e.getSource() == close) {

				dialog.setVisible(false);
				dialog.dispose();
				return;

			} else if (e.getSource() == exportButton) {
				
				FileChooser chooser = new FileChooser(I18n.getMessage("thaw.plugin.signature.trustList.export.long"));
				chooser.setDirectoryOnly(false);
				chooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
				
				File file = chooser.askOneFile();

				TrustListParser.exportTrustList(table.getIdentities(), file);

				return;

			} else if (e.getSource() == importButton) {

				FileChooser chooser = new FileChooser(I18n.getMessage("thaw.plugin.signature.trustList.import.long"));
				chooser.setDirectoryOnly(false);
				chooser.setDialogType(javax.swing.JFileChooser.OPEN_DIALOG);
				
				File file = chooser.askOneFile();

				synchronized(db.dbLock) {
					TrustListParser.importTrustList(this, file);
					updateList();
				}

				return;
			}

			int[] rows = table.getTable().getSelectedRows();

			for (int i = 0 ; i < rows.length ; i++) {
				int row = rows[i];

				if (row < 0)
					continue;

				Identity target = table.getIdentity(row);

				if (target == null)
					continue;


				if (e.getSource() == setOriginal) {
					target.setOriginal();

					updateList();

				} else if (e.getSource() instanceof JButton) {
					JButton bt = (JButton)e.getSource();

					target.setTrustLevel(bt.getText());

					updateList();
				}
			}
		}

		/**
		 * called back by the trust list parser when importing
		 */
		public void updateIdentity(Identity importedId) {
			Identity id = Identity.getIdentity(db, importedId.getNick(), importedId.getPublicKey(), true /* create if doesn't exist */);
			id.setTrustLevel(importedId.getTrustLevel());
		}

		public void start() { }

		public void end() { }
	}



	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == yourIdentitiesButton) {
			new YourIdentitiesPanel();
		} else if (e.getSource() == otherIdentitiesButton) {
			new OtherIdentitiesPanel();
		}
	}
}

