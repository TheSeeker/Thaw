package thaw.plugins.fetchPlugin;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import javax.swing.JFileChooser;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.io.File;
import java.util.Vector;
import java.util.Iterator;

import thaw.core.*;
import thaw.plugins.FetchPlugin;

public class FetchPanel implements java.awt.event.ActionListener {

	private JPanel mainPanel = null;
	private JPanel centeredPart = null; /* (below is the validation button) */
	private JButton validationButton = null;

	private JPanel filePanel = null;
	private JLabel fileLabel = null;
	private JTextArea fileList = null;
	private JButton pasteButton = null;
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

	private JPanel queuePanel = null;
	private JLabel queueLabel = null;
	private String[] queues = null;
	private JComboBox queueSelecter = null;

	private Core core;
	private FetchPlugin fetchPlugin;

	private boolean advancedMode;

	public FetchPanel(Core core, FetchPlugin fetchPlugin) {
		this.core = core;
		this.fetchPlugin = fetchPlugin;

		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(20, 20));

		centeredPart = new JPanel();
		centeredPart.setLayout(new BorderLayout(10, 10));

		validationButton = new JButton(I18n.getMessage("thaw.common.fetch"));
		validationButton.setPreferredSize(new Dimension(300, 40));
		
		validationButton.addActionListener(this);

		filePanel = new JPanel();
		filePanel.setLayout(new BorderLayout());


		/* FILE LIST */

		fileList = new JTextArea();
		fileLabel = new JLabel(I18n.getMessage("thaw.plugin.fetch.keyList"));

		loadListButton = new JButton(I18n.getMessage("thaw.plugin.fetch.loadKeyListFromFile"));
		loadListButton.addActionListener(this);

		pasteButton = new JButton(I18n.getMessage("thaw.plugin.fetch.pasteFromClipboard"));
		pasteButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1,2));
		buttonPanel.add(pasteButton);
		buttonPanel.add(loadListButton);

		filePanel.add(fileLabel, BorderLayout.NORTH);
		filePanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
		filePanel.add(buttonPanel, BorderLayout.SOUTH);

		
		/* below panel */

		
		belowPanel = new JPanel();
		if(advancedMode)
			belowPanel.setLayout(new GridLayout(2, 2, 10, 10));
		else
			belowPanel.setLayout(new GridLayout(1, 2, 10, 10));



		/* PRIORITY */
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
		prioritySelecter.setSelectedItem(I18n.getMessage("thaw.plugin.priority.p4"));

		priorityPanel.add(priorityLabel);
		priorityPanel.add(prioritySelecter);
		

		/* QUEUE */
		queuePanel = new JPanel();
	        queuePanel.setLayout(new GridLayout(2, 1, 5, 5));
		
		queueLabel = new JLabel(I18n.getMessage("thaw.common.globalQueue"));
		queues = new String [] {
			I18n.getMessage("thaw.common.true"),
			I18n.getMessage("thaw.common.false"),
		};
		queueSelecter = new JComboBox(queues);

		queuePanel.add(queueLabel);
		queuePanel.add(queueSelecter);

		/* DESTINATION */
		destinationLabel = new JLabel(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));

		dstChoosePanel = new JPanel();
		dstChoosePanel.setLayout(new GridLayout(3,1, 5, 5));
		
		destinationField = new JTextField("");
		if(core.getConfig().getValue("lastDestinationDirectory") != null)
			destinationField.setText(core.getConfig().getValue("lastDestinationDirectory"));
		destinationField.setEditable(true);
		
		destinationButton = new JButton(I18n.getMessage("thaw.plugin.fetch.chooseDestination"));
		destinationButton.addActionListener(this);
		
		dstChoosePanel.add(destinationLabel);
		dstChoosePanel.add(destinationField);
		dstChoosePanel.add(destinationButton);

		if(advancedMode) {
			belowPanel.add(priorityPanel);
			//belowPanel.add(persistencePanel);
			belowPanel.add(queuePanel);
		}

		belowPanel.add(dstChoosePanel);
		
		if(!advancedMode) {
			belowPanel.add(new JPanel());
		}

		centeredPart.add(filePanel, BorderLayout.CENTER);
		centeredPart.add(belowPanel, BorderLayout.SOUTH);
		
		mainPanel.add(centeredPart, BorderLayout.CENTER);
		mainPanel.add(validationButton, BorderLayout.SOUTH);
	}


	public JPanel getPanel() {
		return mainPanel;
	}

	
	public void actionPerformed(java.awt.event.ActionEvent e) {
		if(e.getSource() == validationButton) {
			int priority = 6;
			boolean globalQueue = true;


			if(((String)queueSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.false")))
				globalQueue = false;

			
			for(int i = 0; i < priorities.length ; i++) {
				if(((String)prioritySelecter.getSelectedItem()).equals(I18n.getMessage("thaw.plugin.priority.p"+i)))
					priority = i;
			}

			if(destinationField.getText() == null || destinationField.getText().equals("")) {
				new thaw.core.WarningWindow(core, I18n.getMessage("thaw.plugin.fetch.chooseADestination"));
				return;
			}
			


			fetchPlugin.fetchFiles(fileList.getText().split("\n"),
					       priority, 0, globalQueue,
					       destinationField.getText());

			fileList.setText("");
		}


		if(e.getSource() == destinationButton) {
			FileChooser fileChooser;

			if(destinationField.getText() != null && !destinationField.getText().equals("")) {
				fileChooser = new FileChooser(destinationField.getText());
			} else {
				fileChooser = new FileChooser();
			}

			File dir = null;
			
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));
			fileChooser.setDirectoryOnly(true);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

			dir = fileChooser.askOneFile();

			if(dir == null) {
				Logger.info(this, "Selection canceled");
				return;
			}

			destinationField.setText(dir.getPath());
			core.getConfig().setValue("lastDestinationDirectory", destinationField.getText());

		}

		if(e.getSource() == pasteButton) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			Clipboard cp = tk.getSystemClipboard();

			try {
				String result;
				Transferable contents = cp.getContents(null);

				boolean hasTransferableText = ((contents != null) &&
							       contents.isDataFlavorSupported(DataFlavor.stringFlavor));

				if ( hasTransferableText ) {
					result = (String)contents.getTransferData(DataFlavor.stringFlavor);
					fileList.setText(fileList.getText() + "\n" + result);
				} else {
					Logger.info(this, "Nothing to get from clipboard");
				}
			} catch(Exception exception) {
				Logger.notice(this, "Exception while pasting: "+exception.toString());
			}
		}

		if(e.getSource() == loadListButton) {
			FileChooser fileChooser = new FileChooser();
			File toParse = null;

			fileChooser.setTitle(I18n.getMessage("thaw.plugin.fetch.loadKeyListFromFile"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			
			toParse = fileChooser.askOneFile();

			if(toParse == null) {
				Logger.info(this, "Nothing to parse");
				return;
			}
			
			Vector keys = KeyFileFilter.extractKeys(toParse);

			if(keys == null || keys.size() <= 0) {
				new WarningWindow(core, "No key found !");
				return;
			}


			String result = fileList.getText();

			for(Iterator i = keys.iterator(); i.hasNext() ;) {
				String key = (String)i.next();

				result = result + key + "\n";
			}
			
			fileList.setText(result);

		}
	}
}

