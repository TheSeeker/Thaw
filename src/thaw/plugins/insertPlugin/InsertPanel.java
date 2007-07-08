package thaw.plugins.insertPlugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import thaw.core.Config;
import thaw.gui.FileChooser;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.gui.WarningWindow;
import thaw.fcp.FCPClientPut;
import thaw.plugins.InsertPlugin;
import thaw.core.MainWindow;


public class InsertPanel implements ActionListener, ItemListener, Observer {
	private final static int MIN_PRIORITY = 6;

	private JPanel globalPanel = null;

	private JPanel mainPanel;

	private JPanel subPanel; /* because we won't use the whole space */

	private JLabel browseLabel;
	private JTextField selectedFiles;
	private JButton browseButton;

	private JLabel selectKeyLabel;
	private ButtonGroup keyRadioGroup;
	private JRadioButton[] keyRadioButtons;

	private JLabel selectRevLabel;
	private JTextField revField;

	private JLabel selectNameLabel;
	private JTextField nameField;

	private JLabel publicKeyLabel;
	private JTextField publicKeyField;
	private JLabel privateKeyLabel;
	private JTextField privateKeyField;

	private String[] priorities;
	private JLabel priorityLabel;
	private JComboBox prioritySelecter;

	private String[] globalStr;
	private JLabel globalLabel;
	private JComboBox globalSelecter;

	private JLabel mimeLabel;
	private JComboBox mimeField;

	private JButton letsGoButton;


	private InsertPlugin insertPlugin;
	private int keyType;
	private FCPClientPut lastInsert = null;

	private boolean advancedMode = false;

	private Config config; /* keep a ref to the config for the "lastSourceDirectory" option */
	private MainWindow mainWindow;

	public InsertPanel(final InsertPlugin insertPlugin,
			   final Config config, final MainWindow mainWindow,
			   final boolean advancedMode) {
		this.insertPlugin = insertPlugin;
		this.config = config;
		this.mainWindow = mainWindow;

		this.advancedMode = advancedMode;

		globalPanel = new JPanel();

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(10, 10));

		if(advancedMode) {
			subPanel = new JPanel();
			subPanel.setLayout(new GridLayout(3,2, 20, 20));
		}


		// FILE SELECTION

		JPanel subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(3, 1));
		browseLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.filesToInsert"));
		subSubPanel.add(browseLabel);
		selectedFiles = new JTextField(20);
		selectedFiles.setEditable(true);
		subSubPanel.add(selectedFiles);
		browseButton = new JButton(I18n.getMessage("thaw.common.selectFiles"));
		browseButton.addActionListener(this);
		subSubPanel.add(browseButton);

		if(advancedMode)
			subPanel.add(subSubPanel);
		else
			mainPanel.add(subSubPanel, BorderLayout.CENTER);


		// KEY TYPE SELECTION

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));
		selectKeyLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.selectKey"));
		subSubPanel.add(selectKeyLabel);
		keyRadioButtons = new JRadioButton[3];
		keyRadioButtons[0] = new JRadioButton(I18n.getMessage("thaw.plugin.insert.CHK"));
		keyRadioButtons[0].setSelected(true);
		keyType = 0;
		keyRadioButtons[1] = new JRadioButton(I18n.getMessage("thaw.plugin.insert.KSK"));
		keyRadioButtons[2] = new JRadioButton(I18n.getMessage("thaw.plugin.insert.SSK"));
		keyRadioGroup = new ButtonGroup();
		for(int i = 0 ; i < keyRadioButtons.length ; i++) {
			keyRadioButtons[i].addItemListener(this);
			keyRadioGroup.add(keyRadioButtons[i]);
			subSubPanel.add(keyRadioButtons[i]);
		}

		if(advancedMode)
			subPanel.add(subSubPanel);


		/* GLOBAL */

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));

		globalStr = new String[] {
			I18n.getMessage("thaw.common.true"),
			I18n.getMessage("thaw.common.false"),
		};

		globalLabel = new JLabel(I18n.getMessage("thaw.common.globalQueue"));
		subSubPanel.add(globalLabel);
		globalSelecter = new JComboBox(globalStr);
		globalSelecter.setSelectedItem(I18n.getMessage("thaw.common.true"));
		subSubPanel.add(globalSelecter);

		if(advancedMode)
			subPanel.add(subSubPanel);


		// PRIORITY SELECTION

		priorities = new String[] {
			I18n.getMessage("thaw.plugin.priority.p0"),
			I18n.getMessage("thaw.plugin.priority.p1"),
			I18n.getMessage("thaw.plugin.priority.p2"),
			I18n.getMessage("thaw.plugin.priority.p3"),
			I18n.getMessage("thaw.plugin.priority.p4"),
			I18n.getMessage("thaw.plugin.priority.p5"),
			I18n.getMessage("thaw.plugin.priority.p6")
		};

		subSubPanel.setLayout(new GridLayout(4, 1));
		priorityLabel = new JLabel(I18n.getMessage("thaw.common.priority"));
		subSubPanel.add(priorityLabel);
		prioritySelecter = new JComboBox(priorities);
		prioritySelecter.setSelectedItem(I18n.getMessage("thaw.plugin.priority.p4"));
		subSubPanel.add(prioritySelecter);

		if(advancedMode) {
			subPanel.add(subSubPanel);
			subPanel.add(subSubPanel);
		}

		// REVISION SELECTION

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));

		selectRevLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.selectRev"));
		subSubPanel.add(selectRevLabel);
		revField = new JTextField(4);
		revField.setEditable(true);
		subSubPanel.add(revField);

		// NAME
		selectNameLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.selectName"));
		subSubPanel.add(selectNameLabel);
		nameField = new JTextField(10);
		nameField.setEditable(true);
		subSubPanel.add(nameField);

		if(advancedMode)
			subPanel.add(subSubPanel);


		setRevAndNameVisible(false);


		// MIME TYPE

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));

		mimeLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.mime"));

		final Vector mimes = (Vector)DefaultMIMETypes.getAllMIMETypes().clone();
		mimes.add(0, "");

		mimeField = new JComboBox(mimes);
		mimeField.setEditable(true);
		mimeField.setPreferredSize(new Dimension(75, 20));

		subSubPanel.add(mimeLabel);
		subSubPanel.add(mimeField);

		if(advancedMode)
			subPanel.add(subSubPanel);


		// PUBLIC / PRIVATE KEY

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));

		publicKeyLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.publicKey"));
		subSubPanel.add(publicKeyLabel);
		publicKeyField = new JTextField(20);
		publicKeyField.setEditable(true);
		subSubPanel.add(publicKeyField);

		privateKeyLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.privateKey"));
		subSubPanel.add(privateKeyLabel);
		privateKeyField = new JTextField(20);
		privateKeyField.setEditable(true);
		subSubPanel.add(privateKeyField);

		if(advancedMode)
			subPanel.add(subSubPanel);

		setKeysVisible(false);

		if(advancedMode)
			mainPanel.add(subPanel, BorderLayout.CENTER);

		letsGoButton = new JButton(I18n.getMessage("thaw.plugin.insert.insertAction"));
		letsGoButton.setPreferredSize(new Dimension(200, 40));

		letsGoButton.addActionListener(this);

		mainPanel.add(letsGoButton, BorderLayout.SOUTH);

		if(advancedMode)
			mainPanel.setSize(400, 400);
		else
			mainPanel.setSize(150, 250);

		globalPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

		globalPanel.add(mainPanel);
	}


	public JPanel getPanel() {
		return globalPanel;
	}


	public void actionPerformed(final ActionEvent e) {
		if(e.getSource() == letsGoButton) {
			int rev = -1;
			String name = null;
			String privateKey = null;
			int priority = 6;
			boolean global = true;
			//int persistence = 0;

			if((selectedFiles.getText() == null)
			   || "".equals( selectedFiles.getText() )) {
				new WarningWindow(mainWindow,
						  I18n.getMessage("thaw.plugin.insert.specifyFile"));
				return;
			}

			if((keyType == FCPClientPut.KEY_TYPE_KSK) || (keyType == FCPClientPut.KEY_TYPE_SSK)) {
				if((nameField.getText() == null)
				   || "".equals( nameField.getText() )
				   || (revField.getText() == null)
				   || revField.getText().equals("")) {
					new WarningWindow(mainWindow,
							  I18n.getMessage("thaw.plugin.insert.specifyNameAndRev"));
					return;
				}

				rev = Integer.parseInt(revField.getText());
				name = nameField.getText();
			}

			if(keyType == FCPClientPut.KEY_TYPE_SSK) {
				if((privateKeyField.getText() != null)
				   && !"".equals( privateKeyField.getText() )) {
					privateKey = privateKeyField.getText();

					if((privateKey != null)
					   && !"".equals( privateKey )) {
						privateKey = privateKey.replaceFirst("SSK@", "");
						privateKey = privateKey.replaceFirst("USK@", "");
						final String[] split = privateKey.split("/");
						privateKey = split[0];
					} else {
						privateKey = null;
					}
				}
			}

			for(int i = 0 ; i <= InsertPanel.MIN_PRIORITY ; i++) {
				if(I18n.getMessage("thaw.plugin.priority.p"+ Integer.toString(i)).equals(prioritySelecter.getSelectedItem())) {
					priority = i;
				}
			}

			if(((String)globalSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.true")))
				global = true;
			if(((String)globalSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.false")))
				global = false;

			String mimeType = null;

			if((mimeField.getSelectedItem() != null) && !((String)mimeField.getSelectedItem()).equals(""))
				mimeType = (String)mimeField.getSelectedItem();

			insertPlugin.insertFile(selectedFiles.getText(),
						keyType, rev, name, privateKey, priority,
						global, FCPClientPut.PERSISTENCE_FOREVER, mimeType);

			selectedFiles.setText("");
		}

		if(e.getSource() == browseButton) {
			final FileChooser fileChooser;
			Vector files;

			String lastDir = null;

			if (config.getValue("lastSourceDirectory") != null)
				lastDir = config.getValue("lastSourceDirectory");

			if (lastDir == null)
				fileChooser = new FileChooser();
			else
				fileChooser = new FileChooser(lastDir);

			fileChooser.setTitle(I18n.getMessage("thaw.common.selectFile"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			if( (files = fileChooser.askManyFiles()) == null) {
				Logger.info(this, "Nothing selected");
				return;
			}

			if (files.size() > 0) {
				config.setValue("lastSourceDirectory", fileChooser.getFinalDirectory());
			}

			String fileList = "";

			int i;

			i = 0;

			for(final Iterator it = files.iterator();
			    it.hasNext(); ) {
				final File file = (File)it.next();

				if(i >= 1)
					fileList = fileList + ";";
				fileList = fileList + file.getPath();

				i++;
			}

			selectedFiles.setText(fileList);

			if(keyType != FCPClientPut.KEY_TYPE_CHK)
				nameField.setText(getFileNameFromPath());
		}
	}


	public String getFileNameFromPath() {
		if((selectedFiles.getText() == null) || "".equals( selectedFiles.getText() ))
			return "";

		final String[] cutcut = selectedFiles.getText().split(File.separator.replaceAll("\\\\", "\\\\\\\\"));

		return cutcut[cutcut.length - 1];
	}

	public void itemStateChanged(final ItemEvent e) {
		if((e.getItem() == keyRadioButtons[0])
		   && (e.getStateChange() == ItemEvent.SELECTED)) { /* CHK */
			setKeysVisible(false);
			setRevAndNameVisible(false);

			resetOptionalFields();

			keyType = FCPClientPut.KEY_TYPE_CHK;

			return;
		}

		if((e.getItem() == keyRadioButtons[1])
		   && (e.getStateChange() == ItemEvent.SELECTED)) { /* KSK */
			setKeysVisible(false);
			setRevAndNameVisible(true);

			resetOptionalFields();

			keyType = FCPClientPut.KEY_TYPE_KSK;
			return;
		}

		if((e.getItem() == keyRadioButtons[2])
		   && (e.getStateChange() == ItemEvent.SELECTED)) { /* SSK */
			setRevAndNameVisible(true);
			setKeysVisible(true);

			resetOptionalFields();

			keyType = FCPClientPut.KEY_TYPE_SSK;
			return;
		}
	}


	public void setLastInserted(final FCPClientPut lastInserted) {
		lastInsert = lastInserted;
	}

	private void setRevAndNameVisible(final boolean v) {
		selectRevLabel.setVisible(v);
		revField.setVisible(v);
		selectNameLabel.setVisible(v);
		nameField.setVisible(v);
	}

	private void setKeysVisible(final boolean v) {
		publicKeyLabel.setVisible(v);
		publicKeyField.setVisible(v);
		privateKeyLabel.setVisible(v);
		privateKeyField.setVisible(v);
	}

	private void resetOptionalFields() {
		revField.setText("0");
		nameField.setText(getFileNameFromPath());
		privateKeyField.setText("");
		publicKeyField.setText("");
	}

	public void update(final Observable o, final Object param) {
		if(o == lastInsert) {
			final FCPClientPut clientPut = (FCPClientPut)o;

			if(clientPut.getKeyType() == 2) {
				Logger.info(this, "Updating display");

				if(clientPut.getPublicKey() != null) {
					String publicKey = clientPut.getPublicKey();
					publicKey = publicKey.replaceFirst("SSK@", "");
					publicKey = publicKey.replaceFirst("USK@", "");
					final String[] split = publicKey.split("/");
					publicKeyField.setText(split[0]);
				} else {
					publicKeyField.setText("");
				}

				if(clientPut.getPrivateKey() != null) {
					String privateKey = clientPut.getPrivateKey();
					privateKey = privateKey.replaceFirst("SSK@", "");
					privateKey = privateKey.replaceFirst("USK@", "");
					final String[] split = privateKey.split("/");
					privateKeyField.setText(split[0]);

				} else {
					privateKeyField.setText("");
				}

			} else {
				publicKeyField.setText("");
				privateKeyField.setText("");
			}

		} else {
			o.deleteObserver(this);
		}
	}
}

