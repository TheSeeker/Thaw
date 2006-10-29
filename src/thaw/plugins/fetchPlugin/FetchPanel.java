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

		this.advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

		this.mainPanel = new JPanel();
		this.mainPanel.setLayout(new BorderLayout(20, 20));

		this.centeredPart = new JPanel();
		this.centeredPart.setLayout(new BorderLayout(10, 10));

		this.validationButton = new JButton(I18n.getMessage("thaw.common.fetch"));
		this.validationButton.setPreferredSize(new Dimension(300, 40));

		this.validationButton.addActionListener(this);

		this.filePanel = new JPanel();
		this.filePanel.setLayout(new BorderLayout());


		/* FILE LIST */

		this.fileList = new JTextArea();
		this.fileLabel = new JLabel(I18n.getMessage("thaw.plugin.fetch.keyList"));

		this.loadListButton = new JButton(I18n.getMessage("thaw.plugin.fetch.loadKeyListFromFile"));
		this.loadListButton.addActionListener(this);

		this.pasteButton = new JButton(I18n.getMessage("thaw.plugin.fetch.pasteFromClipboard"));
		this.pasteButton.addActionListener(this);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1,2));
		buttonPanel.add(this.pasteButton);
		buttonPanel.add(this.loadListButton);

		this.filePanel.add(this.fileLabel, BorderLayout.NORTH);
		this.filePanel.add(new JScrollPane(this.fileList), BorderLayout.CENTER);
		this.filePanel.add(buttonPanel, BorderLayout.SOUTH);


		/* below panel */

		this.belowPanel = new JPanel();
		if(this.advancedMode)
			this.belowPanel.setLayout(new GridLayout(2, 2, 10, 10));
		else
			this.belowPanel.setLayout(new GridLayout(1, 2, 10, 10));



		/* PRIORITY */
		this.priorityPanel = new JPanel();
		this.priorityPanel.setLayout(new GridLayout(2, 1, 5, 5));

		this.priorityLabel = new JLabel(I18n.getMessage("thaw.common.priority"));
		this.priorities = new String[] {
			I18n.getMessage("thaw.plugin.priority.p0"),
			I18n.getMessage("thaw.plugin.priority.p1"),
			I18n.getMessage("thaw.plugin.priority.p2"),
			I18n.getMessage("thaw.plugin.priority.p3"),
			I18n.getMessage("thaw.plugin.priority.p4"),
			I18n.getMessage("thaw.plugin.priority.p5"),
			I18n.getMessage("thaw.plugin.priority.p6")
		};
		this.prioritySelecter = new JComboBox(this.priorities);
		this.prioritySelecter.setSelectedItem(I18n.getMessage("thaw.plugin.priority.p4"));

		this.priorityPanel.add(this.priorityLabel);
		this.priorityPanel.add(this.prioritySelecter);


		/* QUEUE */
		this.queuePanel = new JPanel();
	        this.queuePanel.setLayout(new GridLayout(2, 1, 5, 5));

		this.queueLabel = new JLabel(I18n.getMessage("thaw.common.globalQueue"));
		this.queues = new String [] {
			I18n.getMessage("thaw.common.true"),
			I18n.getMessage("thaw.common.false"),
		};
		this.queueSelecter = new JComboBox(this.queues);

		this.queuePanel.add(this.queueLabel);
		this.queuePanel.add(this.queueSelecter);

		/* DESTINATION */
		this.destinationLabel = new JLabel(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));

		this.dstChoosePanel = new JPanel();
		this.dstChoosePanel.setLayout(new GridLayout(3,1, 5, 5));

		this.destinationField = new JTextField("");
		if(core.getConfig().getValue("lastDestinationDirectory") != null)
			this.destinationField.setText(core.getConfig().getValue("lastDestinationDirectory"));
		this.destinationField.setEditable(true);

		this.destinationButton = new JButton(I18n.getMessage("thaw.plugin.fetch.chooseDestination"));
		this.destinationButton.addActionListener(this);

		this.dstChoosePanel.add(this.destinationLabel);
		this.dstChoosePanel.add(this.destinationField);
		this.dstChoosePanel.add(this.destinationButton);

		if(this.advancedMode) {
			this.belowPanel.add(this.priorityPanel);
			//belowPanel.add(persistencePanel);
			this.belowPanel.add(this.queuePanel);
		}

		this.belowPanel.add(this.dstChoosePanel);

		if(!this.advancedMode) {
			this.belowPanel.add(new JPanel());
		}

		this.centeredPart.add(this.filePanel, BorderLayout.CENTER);
		this.centeredPart.add(this.belowPanel, BorderLayout.SOUTH);

		this.mainPanel.add(this.centeredPart, BorderLayout.CENTER);
		this.mainPanel.add(this.validationButton, BorderLayout.SOUTH);
	}


	public JPanel getPanel() {
		return this.mainPanel;
	}


	public void actionPerformed(java.awt.event.ActionEvent e) {
		if(e.getSource() == this.validationButton) {
			int priority = 6;
			boolean globalQueue = true;


			if(((String)this.queueSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.false")))
				globalQueue = false;

			for(int i = 0; i < this.priorities.length ; i++) {
				if(((String)this.prioritySelecter.getSelectedItem()).equals(I18n.getMessage("thaw.plugin.priority.p"+i)))
					priority = i;
			}

			if(this.destinationField.getText() == null || "".equals( this.destinationField.getText() )) {
				new thaw.core.WarningWindow(this.core, I18n.getMessage("thaw.plugin.fetch.chooseADestination"));
				return;
			}


			this.fetchPlugin.fetchFiles(this.fileList.getText().split("\n"),
					       priority, 0, globalQueue,
					       this.destinationField.getText());

			this.fileList.setText("");
		}


		if(e.getSource() == this.destinationButton) {
			FileChooser fileChooser;

			if(this.destinationField.getText() != null && !"".equals( this.destinationField.getText() )) {
				fileChooser = new FileChooser(this.destinationField.getText());
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

			this.destinationField.setText(dir.getPath());
			this.core.getConfig().setValue("lastDestinationDirectory", this.destinationField.getText());

		}

		if(e.getSource() == this.pasteButton) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			Clipboard cp = tk.getSystemClipboard();

			try {
				String result;
				Transferable contents = cp.getContents(null);

				boolean hasTransferableText = ((contents != null) &&
							       contents.isDataFlavorSupported(DataFlavor.stringFlavor));

				if ( hasTransferableText ) {
					result = (String)contents.getTransferData(DataFlavor.stringFlavor);
					this.fileList.setText(this.fileList.getText() + "\n" + result);
				} else {
					Logger.info(this, "Nothing to get from clipboard");
				}
			} catch(Exception exception) {
				Logger.notice(this, "Exception while pasting: "+exception.toString());
			}
		}

		if(e.getSource() == this.loadListButton) {
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
				new WarningWindow(this.core, "No key found !");
				return;
			}


			String result = this.fileList.getText();

			for(Iterator i = keys.iterator(); i.hasNext() ;) {
				String key = (String)i.next();

				result = result + key + "\n";
			}

			this.fileList.setText(result);
		}
	}
}

