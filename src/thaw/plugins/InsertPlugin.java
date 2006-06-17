package thaw.plugins;

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

import thaw.core.*;
import thaw.i18n.I18n;


public class InsertPlugin implements thaw.core.Plugin {
	private Core core;

	private JPanel globalPanel;

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

	private String[] persistences;
	private JLabel persistenceLabel;
	private JComboBox persistenceSelecter;

	private JButton letsGoButton;

	public InsertPlugin() {

	}


	public boolean run(Core core) {
		this.core = core;
		
		Logger.info(this, "Starting plugin \"InsertPlugin\" ...");

		globalPanel = new JPanel();

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(10, 10));

		subPanel = new JPanel();

		subPanel.setLayout(new GridLayout(3,2, 40, 40));

		
		// FILE SELECTION

		JPanel subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(3, 1));
		browseLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.filesToInsert"));
		subSubPanel.add(browseLabel);
		selectedFiles = new JTextField(20);
		selectedFiles.setEditable(false);
		subSubPanel.add(selectedFiles);
		browseButton = new JButton(I18n.getMessage("thaw.common.selectFiles"));
		subSubPanel.add(browseButton);

		subPanel.add(subSubPanel);


		// KEY TYPE SELECTION

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));
		selectKeyLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.selectKey"));
		subSubPanel.add(selectKeyLabel);
		keyRadioButtons = new JRadioButton[3];
		keyRadioButtons[0] = new JRadioButton(I18n.getMessage("thaw.plugin.insert.KSK"));
		keyRadioButtons[1] = new JRadioButton(I18n.getMessage("thaw.plugin.insert.CHK"));
		keyRadioButtons[1].setSelected(true);
		keyRadioButtons[2] = new JRadioButton(I18n.getMessage("thaw.plugin.insert.SSK"));
		keyRadioGroup = new ButtonGroup();
		for(int i = 0 ; i < keyRadioButtons.length ; i++) {
			keyRadioGroup.add(keyRadioButtons[i]);
			subSubPanel.add(keyRadioButtons[i]);
		}

		subPanel.add(subSubPanel);


		// PERSISTENCE & GLOBAL

		persistences = new String[] {
			I18n.getMessage("thaw.common.persistenceConnection"),
			I18n.getMessage("thaw.common.persistenceReboot"),
			I18n.getMessage("thaw.common.persistenceForever"),
		};

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));
		persistenceLabel = new JLabel(I18n.getMessage("thaw.common.persistence"));
		subSubPanel.add(persistenceLabel);
		persistenceSelecter = new JComboBox(persistences);
		persistenceSelecter.setSelectedItem(I18n.getMessage("thaw.common.persistenceReboot"));
		subSubPanel.add(persistenceSelecter);
		

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
		revField.setEditable(false);
		subSubPanel.add(revField);
		
		selectNameLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.selectName"));
		subSubPanel.add(selectNameLabel);
		nameField = new JTextField(10);
		nameField.setEditable(false);
		subSubPanel.add(nameField);

		subPanel.add(subSubPanel);



		// PRIORITY SELECTION

		priorities = new String[] {
			I18n.getMessage("thaw.plugin.insert.p0"),
			I18n.getMessage("thaw.plugin.insert.p1"),
			I18n.getMessage("thaw.plugin.insert.p2"),
			I18n.getMessage("thaw.plugin.insert.p3"),
			I18n.getMessage("thaw.plugin.insert.p4"),
			I18n.getMessage("thaw.plugin.insert.p5") 
		};

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));
		priorityLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.priority"));
		subSubPanel.add(priorityLabel);
		prioritySelecter = new JComboBox(priorities);
		prioritySelecter.setSelectedItem(I18n.getMessage("thaw.plugin.insert.p3"));
		subSubPanel.add(prioritySelecter);
		
		subPanel.add(subSubPanel);
		


		// PUBLIC / PRIVATE KEY

		subSubPanel = new JPanel();
		subSubPanel.setLayout(new GridLayout(4, 1));

		publicKeyLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.publicKey"));
		subSubPanel.add(publicKeyLabel);
		publicKeyField = new JTextField(20);
		publicKeyField.setEditable(false);
		subSubPanel.add(publicKeyField);

		privateKeyLabel = new JLabel(I18n.getMessage("thaw.plugin.insert.privateKey"));
		subSubPanel.add(privateKeyLabel);
		privateKeyField = new JTextField(20);
		privateKeyField.setEditable(false);
		subSubPanel.add(privateKeyField);

		subPanel.add(subSubPanel);




		mainPanel.add(subPanel, BorderLayout.CENTER);

		letsGoButton = new JButton(I18n.getMessage("thaw.plugin.insert.insertAction"));
		mainPanel.add(letsGoButton, BorderLayout.SOUTH);

		mainPanel.setSize(400, 400);

		globalPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 50));

		globalPanel.add(mainPanel);

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.insertion"), globalPanel);

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"InsertPlugin\" ...");

		core.getMainWindow().removeTab(mainPanel);

		return true;
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.common.insertion");
	}
}
