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

import thaw.core.*;
import thaw.i18n.I18n;
import thaw.plugins.InsertPlugin;
import thaw.fcp.*;

public class InsertPanel implements ActionListener, ItemListener, Observer {
	private final static int MIN_PRIORITY = 6;

	private JPanel globalPanel = null;

	private JPanel mainPanel;

	private JPanel subPanel; /* because we won't use the whole space */


	private JLabel browseLabel;
	private JTextField selectedFiles; /* TODO: it was planned to support directory insertion */
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

	private JButton letsGoButton;


	private InsertPlugin insertPlugin;
	private int keyType;
	private FCPClientPut lastInsert = null;

	public InsertPanel(InsertPlugin insertPlugin) {
		this.insertPlugin = insertPlugin;

		globalPanel = new JPanel();

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(10, 10));

		subPanel = new JPanel();

		subPanel.setLayout(new GridLayout(3,2, 20, 20));

		
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

		subPanel.add(subSubPanel);


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

		subPanel.add(subSubPanel);



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

		subPanel.add(subSubPanel);


		setRevAndNameVisible(false);


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

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));
		priorityLabel = new JLabel(I18n.getMessage("thaw.common.priority"));
		subSubPanel.add(priorityLabel);
		prioritySelecter = new JComboBox(priorities);
		prioritySelecter.setSelectedItem(I18n.getMessage("thaw.plugin.priority.p3"));
		subSubPanel.add(prioritySelecter);
		
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

		subPanel.add(subSubPanel);

		setKeysVisible(false);


		mainPanel.add(subPanel, BorderLayout.CENTER);

		letsGoButton = new JButton(I18n.getMessage("thaw.plugin.insert.insertAction"));
		letsGoButton.setPreferredSize(new Dimension(200, 40));

		letsGoButton.addActionListener(this);

		mainPanel.add(letsGoButton, BorderLayout.SOUTH);

		mainPanel.setSize(400, 400);

		globalPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

		globalPanel.add(mainPanel);
	}

	
	public JPanel getPanel() {
		return globalPanel;
	}


	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == letsGoButton) {
			int rev = -1;
			String name = null;
			String privateKey = null;
			int priority = 6;
			boolean global = true;
			//int persistence = 0;

			if(selectedFiles.getText() == null
			   || selectedFiles.getText().equals("")) {
				new WarningWindow(null, I18n.getMessage("thaw.plugin.insert.specifyFile"));
				return;
			}

			if(keyType == 1 || keyType == 2) {
				if(nameField.getText() == null
				   || nameField.getText().equals("")
				   || revField.getText() == null
				   || revField.getText().equals("")) {
					new WarningWindow(null, I18n.getMessage("thaw.plugin.insert.specifyNameAndRev"));
					return;
				}

				rev = Integer.parseInt(revField.getText());
				name = nameField.getText();
			}

			if(keyType == 2) {
				if(privateKeyField.getText() != null
				   && !privateKeyField.getText().equals("")) {
					privateKey = privateKeyField.getText();

					if(privateKey != null
					   && !privateKey.equals("")) {
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
				if(I18n.getMessage("thaw.plugin.priority.p"+ Integer.toString(i)).equals((String)prioritySelecter.getSelectedItem())) {
					priority = i;
				}
			}

			if(((String)globalSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.true")))
				global = true;
			if(((String)globalSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.false")))
				global = false;
			
			

			insertPlugin.insertFile(selectedFiles.getText(),
						keyType, rev, name, privateKey, priority,
						global, 0);
		}

		if(e.getSource() == browseButton) {
			FileChooser fileChooser = new FileChooser();
			File[] files;

			fileChooser.setTitle(I18n.getMessage("thaw.common.selectFile"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			if( (files = fileChooser.askManyFiles()) == null) {
				Logger.info(this, "Nothing selected");
				return;
			}
			
			String fileList = "";

			for(int i = 0 ; i < files.length ; i++) {
				if(i >= 1)
					fileList = fileList + ";";
				fileList = fileList + files[i].getPath();
			}

			selectedFiles.setText(fileList);
			
			if(keyType != 0)
				nameField.setText(getFileNameFromPath());
		}
	}


	public String getFileNameFromPath() {
		if(selectedFiles.getText() == null || selectedFiles.getText().equals(""))
			return "";
		
		String[] cutcut = selectedFiles.getText().split(File.separator.replaceAll("\\\\", "\\\\\\\\"));

		return cutcut[cutcut.length - 1];
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getItem() == keyRadioButtons[0]
		   && e.getStateChange() == ItemEvent.SELECTED) { /* CHK */
			setKeysVisible(false);
			setRevAndNameVisible(false);
			
			resetOptionalFields();

			keyType = 0;

			return;
		}

		if(e.getItem() == keyRadioButtons[1]
		   && e.getStateChange() == ItemEvent.SELECTED) { /* KSK */
			setKeysVisible(false);
			setRevAndNameVisible(true);

			resetOptionalFields();

			keyType = 1;
			return;
		}

		if(e.getItem() == keyRadioButtons[2]
		   && e.getStateChange() == ItemEvent.SELECTED) { /* SSK */
			setRevAndNameVisible(true);
			setKeysVisible(true);

			resetOptionalFields();

			keyType = 2;
			return;
		}
	}


	public void setLastInserted(FCPClientPut lastInserted) {
		this.lastInsert = lastInserted;
	}

	private void setRevAndNameVisible(boolean v) {
		selectRevLabel.setVisible(v);
		revField.setVisible(v);
		
		selectNameLabel.setVisible(v);
		nameField.setVisible(v);
	}

	private void setKeysVisible(boolean v) {
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

	public void update(Observable o, Object param) {
		if(o == lastInsert) {
			FCPClientPut clientPut = (FCPClientPut)o;
			
			if(clientPut.getKeyType() == 2) {
				Logger.info(this, "Updating display");
				
				if(clientPut.getPublicKey() != null) {
					String publicKey = clientPut.getPublicKey();
					publicKey = publicKey.replaceFirst("SSK@", "");
					publicKey = publicKey.replaceFirst("USK@", "");
					String[] split = publicKey.split("/");
					publicKeyField.setText(split[0]);
				} else {
					publicKeyField.setText("");
				}

				if(clientPut.getPrivateKey() != null) {
					String privateKey = clientPut.getPrivateKey();
					privateKey = privateKey.replaceFirst("SSK@", "");
					privateKey = privateKey.replaceFirst("USK@", "");
					String[] split = privateKey.split("/");
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

