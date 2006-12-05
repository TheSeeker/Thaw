package thaw.plugins.insertPlugin;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JFileChooser;
import java.io.File;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.Observer;
import java.util.Observable;
import java.util.Vector;

import java.util.Iterator;

import thaw.core.*;
import thaw.plugins.InsertPlugin;
import thaw.fcp.*;



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

	public InsertPanel(InsertPlugin insertPlugin, boolean advancedMode) {
		this.insertPlugin = insertPlugin;

		this.advancedMode = advancedMode;

		this.globalPanel = new JPanel();

		this.mainPanel = new JPanel();
		this.mainPanel.setLayout(new BorderLayout(10, 10));

		if(advancedMode) {
			this.subPanel = new JPanel();
			this.subPanel.setLayout(new GridLayout(3,2, 20, 20));
		}


		// FILE SELECTION

		JPanel subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(3, 1));
		this.browseLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.filesToInsert"));
		subSubPanel.add(this.browseLabel);
		this.selectedFiles = new JTextField(20);
		this.selectedFiles.setEditable(true);
		subSubPanel.add(this.selectedFiles);
		this.browseButton = new JButton(I18n.getMessage("thaw.common.selectFiles"));
		this.browseButton.addActionListener(this);
		subSubPanel.add(this.browseButton);

		if(advancedMode)
			this.subPanel.add(subSubPanel);
		else
			this.mainPanel.add(subSubPanel, BorderLayout.CENTER);


		// KEY TYPE SELECTION

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));
		this.selectKeyLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.selectKey"));
		subSubPanel.add(this.selectKeyLabel);
		this.keyRadioButtons = new JRadioButton[3];
		this.keyRadioButtons[0] = new JRadioButton(I18n.getMessage("thaw.plugin.insert.CHK"));
		this.keyRadioButtons[0].setSelected(true);
		this.keyType = 0;
		this.keyRadioButtons[1] = new JRadioButton(I18n.getMessage("thaw.plugin.insert.KSK"));
		this.keyRadioButtons[2] = new JRadioButton(I18n.getMessage("thaw.plugin.insert.SSK"));
		this.keyRadioGroup = new ButtonGroup();
		for(int i = 0 ; i < this.keyRadioButtons.length ; i++) {
			this.keyRadioButtons[i].addItemListener(this);
			this.keyRadioGroup.add(this.keyRadioButtons[i]);
			subSubPanel.add(this.keyRadioButtons[i]);
		}

		if(advancedMode)
			this.subPanel.add(subSubPanel);


		/* GLOBAL */

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));

		this.globalStr = new String[] {
			I18n.getMessage("thaw.common.true"),
			I18n.getMessage("thaw.common.false"),
		};

		this.globalLabel = new JLabel(I18n.getMessage("thaw.common.globalQueue"));
		subSubPanel.add(this.globalLabel);
		this.globalSelecter = new JComboBox(this.globalStr);
		this.globalSelecter.setSelectedItem(I18n.getMessage("thaw.common.true"));
		subSubPanel.add(this.globalSelecter);

		if(advancedMode)
			this.subPanel.add(subSubPanel);


		// PRIORITY SELECTION

		this.priorities = new String[] {
			I18n.getMessage("thaw.plugin.priority.p0"),
			I18n.getMessage("thaw.plugin.priority.p1"),
			I18n.getMessage("thaw.plugin.priority.p2"),
			I18n.getMessage("thaw.plugin.priority.p3"),
			I18n.getMessage("thaw.plugin.priority.p4"),
			I18n.getMessage("thaw.plugin.priority.p5"),
			I18n.getMessage("thaw.plugin.priority.p6")
		};

		subSubPanel.setLayout(new GridLayout(4, 1));
		this.priorityLabel = new JLabel(I18n.getMessage("thaw.common.priority"));
		subSubPanel.add(this.priorityLabel);
		this.prioritySelecter = new JComboBox(this.priorities);
		this.prioritySelecter.setSelectedItem(I18n.getMessage("thaw.plugin.priority.p4"));
		subSubPanel.add(this.prioritySelecter);

		if(advancedMode) {
			this.subPanel.add(subSubPanel);
			this.subPanel.add(subSubPanel);
		}

		// REVISION SELECTION

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));

		this.selectRevLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.selectRev"));
		subSubPanel.add(this.selectRevLabel);
		this.revField = new JTextField(4);
		this.revField.setEditable(true);
		subSubPanel.add(this.revField);

		// NAME
		this.selectNameLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.selectName"));
		subSubPanel.add(this.selectNameLabel);
		this.nameField = new JTextField(10);
		this.nameField.setEditable(true);
		subSubPanel.add(this.nameField);

		if(advancedMode)
			this.subPanel.add(subSubPanel);


		this.setRevAndNameVisible(false);


		// MIME TYPE

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));

		this.mimeLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.mime"));

		Vector mimes = (Vector)DefaultMIMETypes.getAllMIMETypes().clone();
		mimes.add(0, "");

		this.mimeField = new JComboBox(mimes);
		this.mimeField.setEditable(true);
		this.mimeField.setPreferredSize(new Dimension(75, 20));

		subSubPanel.add(this.mimeLabel);
		subSubPanel.add(this.mimeField);

		if(advancedMode)
			this.subPanel.add(subSubPanel);


		// PUBLIC / PRIVATE KEY

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));

		this.publicKeyLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.publicKey"));
		subSubPanel.add(this.publicKeyLabel);
		this.publicKeyField = new JTextField(20);
		this.publicKeyField.setEditable(true);
		subSubPanel.add(this.publicKeyField);

		this.privateKeyLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.privateKey"));
		subSubPanel.add(this.privateKeyLabel);
		this.privateKeyField = new JTextField(20);
		this.privateKeyField.setEditable(true);
		subSubPanel.add(this.privateKeyField);

		if(advancedMode)
			this.subPanel.add(subSubPanel);

		this.setKeysVisible(false);

		if(advancedMode)
			this.mainPanel.add(this.subPanel, BorderLayout.CENTER);

		this.letsGoButton = new JButton(I18n.getMessage("thaw.plugin.insert.insertAction"));
		this.letsGoButton.setPreferredSize(new Dimension(200, 40));

		this.letsGoButton.addActionListener(this);

		this.mainPanel.add(this.letsGoButton, BorderLayout.SOUTH);

		if(advancedMode)
			this.mainPanel.setSize(400, 400);
		else
			this.mainPanel.setSize(150, 250);

		this.globalPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

		this.globalPanel.add(this.mainPanel);
	}


	public JPanel getPanel() {
		return this.globalPanel;
	}


	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this.letsGoButton) {
			int rev = -1;
			String name = null;
			String privateKey = null;
			int priority = 6;
			boolean global = true;
			//int persistence = 0;

			if(this.selectedFiles.getText() == null
			   || "".equals( this.selectedFiles.getText() )) {
				new WarningWindow(null, I18n.getMessage("thaw.plugin.insert.specifyFile"));
				return;
			}

			if(this.keyType == 1 || this.keyType == 2) {
				if(this.nameField.getText() == null
				   || "".equals( this.nameField.getText() )
				   || this.revField.getText() == null
				   || this.revField.getText().equals("")) {
					new WarningWindow(null, I18n.getMessage("thaw.plugin.insert.specifyNameAndRev"));
					return;
				}

				rev = Integer.parseInt(this.revField.getText());
				name = this.nameField.getText();
			}

			if(this.keyType == 2) {
				if(this.privateKeyField.getText() != null
				   && !"".equals( this.privateKeyField.getText() )) {
					privateKey = this.privateKeyField.getText();

					if(privateKey != null
					   && !"".equals( privateKey )) {
						privateKey = privateKey.replaceFirst("SSK@", "");
						privateKey = privateKey.replaceFirst("USK@", "");
						String[] split = privateKey.split("/");
						privateKey = split[0];
					} else {
						privateKey = null;
					}
				}
			}

			for(int i = 0 ; i <= MIN_PRIORITY ; i++) {
				if(I18n.getMessage("thaw.plugin.priority.p"+ Integer.toString(i)).equals(this.prioritySelecter.getSelectedItem())) {
					priority = i;
				}
			}

			if(((String)this.globalSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.true")))
				global = true;
			if(((String)this.globalSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.false")))
				global = false;

			String mimeType = null;

			if(this.mimeField.getSelectedItem() != null && !((String)this.mimeField.getSelectedItem()).equals(""))
				mimeType = (String)this.mimeField.getSelectedItem();

			insertPlugin.insertFile(selectedFiles.getText(),
						keyType, rev, name, privateKey, priority,
						global, 0, mimeType);

			selectedFiles.setText("");
		}

		if(e.getSource() == this.browseButton) {
			FileChooser fileChooser = new FileChooser();
			Vector files;

			fileChooser.setTitle(I18n.getMessage("thaw.common.selectFile"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			if( (files = fileChooser.askManyFiles()) == null) {
				Logger.info(this, "Nothing selected");
				return;
			}

			String fileList = "";

			int i;

			i = 0;

			for(Iterator it = files.iterator();
			    it.hasNext(); ) {
				File file = (File)it.next();

				if(i >= 1)
					fileList = fileList + ";";
				fileList = fileList + file.getPath();

				i++;
			}

			this.selectedFiles.setText(fileList);

			if(this.keyType != 0)
				this.nameField.setText(this.getFileNameFromPath());
		}
	}


	public String getFileNameFromPath() {
		if(this.selectedFiles.getText() == null || "".equals( this.selectedFiles.getText() ))
			return "";

		String[] cutcut = this.selectedFiles.getText().split(File.separator.replaceAll("\\\\", "\\\\\\\\"));

		return cutcut[cutcut.length - 1];
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getItem() == this.keyRadioButtons[0]
		   && e.getStateChange() == ItemEvent.SELECTED) { /* CHK */
			this.setKeysVisible(false);
			this.setRevAndNameVisible(false);

			this.resetOptionalFields();

			this.keyType = 0;

			return;
		}

		if(e.getItem() == this.keyRadioButtons[1]
		   && e.getStateChange() == ItemEvent.SELECTED) { /* KSK */
			this.setKeysVisible(false);
			this.setRevAndNameVisible(true);

			this.resetOptionalFields();

			this.keyType = 1;
			return;
		}

		if(e.getItem() == this.keyRadioButtons[2]
		   && e.getStateChange() == ItemEvent.SELECTED) { /* SSK */
			this.setRevAndNameVisible(true);
			this.setKeysVisible(true);

			this.resetOptionalFields();

			this.keyType = 2;
			return;
		}
	}


	public void setLastInserted(FCPClientPut lastInserted) {
		this.lastInsert = lastInserted;
	}

	private void setRevAndNameVisible(boolean v) {
		this.selectRevLabel.setVisible(v);
		this.revField.setVisible(v);
		this.selectNameLabel.setVisible(v);
		this.nameField.setVisible(v);
	}

	private void setKeysVisible(boolean v) {
		this.publicKeyLabel.setVisible(v);
		this.publicKeyField.setVisible(v);
		this.privateKeyLabel.setVisible(v);
		this.privateKeyField.setVisible(v);
	}

	private void resetOptionalFields() {
		this.revField.setText("0");
		this.nameField.setText(this.getFileNameFromPath());
		this.privateKeyField.setText("");
		this.publicKeyField.setText("");
	}

	public void update(Observable o, Object param) {
		if(o == this.lastInsert) {
			FCPClientPut clientPut = (FCPClientPut)o;

			if(clientPut.getKeyType() == 2) {
				Logger.info(this, "Updating display");

				if(clientPut.getPublicKey() != null) {
					String publicKey = clientPut.getPublicKey();
					publicKey = publicKey.replaceFirst("SSK@", "");
					publicKey = publicKey.replaceFirst("USK@", "");
					String[] split = publicKey.split("/");
					this.publicKeyField.setText(split[0]);
				} else {
					this.publicKeyField.setText("");
				}

				if(clientPut.getPrivateKey() != null) {
					String privateKey = clientPut.getPrivateKey();
					privateKey = privateKey.replaceFirst("SSK@", "");
					privateKey = privateKey.replaceFirst("USK@", "");
					String[] split = privateKey.split("/");
					this.privateKeyField.setText(split[0]);

				} else {
					this.privateKeyField.setText("");
				}

			} else {
				this.publicKeyField.setText("");
				this.privateKeyField.setText("");
			}

		} else {
			o.deleteObserver(this);
		}
	}
}

