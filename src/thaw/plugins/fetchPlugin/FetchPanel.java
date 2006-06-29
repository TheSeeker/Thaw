package thaw.plugins.fetchPlugin;

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
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Dimension;

import thaw.core.*;
import thaw.i18n.I18n;

public class FetchPanel {

	private JPanel mainPanel = null;
	private JPanel leftPart = null; /* (right part = validation button) */
	private JButton validationButton = null;

	private JPanel filePanel = null;
	private JLabel fileLabel = null;
	private JTextArea fileList = null;
	private JButton loadListButton = null;
	

	private JPanel belowPanel = null; /* 1 x 2 */

	private JPanel priorityPanel = null; /* 2 x 1 */
	private JLabel priorityLabel = null;
	private String[] priorities = null;
	private JComboBox prioritySelecter = null;

	private JLabel destinationLabel = null;
	private JPanel dstChoosePanel = null; /* 3 x 1 */
	private JTextField destinationField = null;
	private JButton destinationButton = null;


	public FetchPanel() {
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(20, 20));

		leftPart = new JPanel();
		leftPart.setLayout(new BorderLayout(10, 10));

		validationButton = new JButton(I18n.getMessage("thaw.common.fetch"));
		validationButton.setPreferredSize(new Dimension(300, 40));
		
		filePanel = new JPanel();
		filePanel.setLayout(new BorderLayout());

		fileList = new JTextArea();
		fileLabel = new JLabel(I18n.getMessage("thaw.plugin.fetch.keyList"));
		loadListButton = new JButton(I18n.getMessage("thaw.plugin.fetch.loadKeyListFromFile"));
		
		filePanel.add(fileLabel, BorderLayout.NORTH);
		filePanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
		filePanel.add(loadListButton, BorderLayout.SOUTH);

		
		belowPanel = new JPanel();
		belowPanel.setLayout(new GridLayout(1, 2, 10, 10));

		priorityPanel = new JPanel();
		priorityPanel.setLayout(new GridLayout(2, 1, 5, 5));

		priorityLabel = new JLabel(I18n.getMessage("thaw.common.priority"));
		priorities = new String[] {
			I18n.getMessage("thaw.plugin.priority.p0"),
			I18n.getMessage("thaw.plugin.priority.p1"),
			I18n.getMessage("thaw.plugin.priority.p2"),
			I18n.getMessage("thaw.plugin.priority.p3"),
			I18n.getMessage("thaw.plugin.priority.p4"),
			I18n.getMessage("thaw.plugin.priority.p5"),
			I18n.getMessage("thaw.plugin.priority.p6") 
			
		};
		prioritySelecter = new JComboBox(priorities);
		prioritySelecter.setSelectedItem(I18n.getMessage("thaw.plugin.priority.p3"));

		priorityPanel.add(priorityLabel);
		priorityPanel.add(prioritySelecter);
		
		destinationLabel = new JLabel(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));

		dstChoosePanel = new JPanel();
		dstChoosePanel.setLayout(new GridLayout(3,1, 5, 5));
		
		destinationField = new JTextField("");
		destinationField.setEditable(false);
		
		destinationButton = new JButton(I18n.getMessage("thaw.plugin.fetch.chooseDestination"));
		
		dstChoosePanel.add(destinationLabel);
		dstChoosePanel.add(destinationField);
		dstChoosePanel.add(destinationButton);

		belowPanel.add(priorityPanel);
		belowPanel.add(dstChoosePanel);
		

		leftPart.add(filePanel, BorderLayout.CENTER);
		leftPart.add(belowPanel, BorderLayout.SOUTH);
		
		mainPanel.add(leftPart, BorderLayout.CENTER);
		mainPanel.add(validationButton, BorderLayout.SOUTH);
	}


	public JPanel getPanel() {
		return mainPanel;
	}

}

