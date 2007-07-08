package thaw.plugins.fetchPlugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import thaw.core.Core;
import thaw.gui.FileChooser;
import thaw.gui.GUIHelper;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.gui.WarningWindow;
import thaw.plugins.FetchPlugin;

public class FetchPanel implements java.awt.event.ActionListener, MouseListener {

	private JPanel mainPanel;
	private JPanel centeredPart; /* (below is the validation button) */
	private JButton validationButton;

	private JPanel filePanel;
	private JLabel fileLabel;
	private JTextArea fileList;
	private JButton pasteButton;
	private JButton loadListButton;

	private JPanel belowPanel; /* 1 x 2 */

	private JPanel priorityPanel; /* 2 x 1 */
	private JLabel priorityLabel;
	private String[] priorities;
	private JComboBox prioritySelecter;

	private JLabel destinationLabel;
	private JPanel dstChoosePanel; /* 3 x 1 */
	private JTextField destinationField;
	private JButton destinationButton;

	private JPanel queuePanel;
	private JLabel queueLabel;
	private String[] queues;
	private JComboBox queueSelecter;

	private JPopupMenu rightClickMenu;

	private final  Core core;
	private final FetchPlugin fetchPlugin;

	private boolean advancedMode;

	public FetchPanel(final Core core, final FetchPlugin fetchPlugin) {
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

		new GUIHelper.PasteHelper(pasteButton, fileList);
		fileList.addMouseListener(this);

		final JPanel buttonPanel = new JPanel();
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

		rightClickMenu = new JPopupMenu();
		final JMenuItem item = new JMenuItem(I18n.getMessage("thaw.common.paste"));
		new GUIHelper.PasteHelper(item, fileList);
		rightClickMenu.add(item);

		/*** Putting things together ***/
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


	public void actionPerformed(final java.awt.event.ActionEvent e) {
		if(e.getSource() == validationButton) {
			int priority = 6;
			boolean globalQueue = true;


			if(((String)queueSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.false")))
				globalQueue = false;

			for(int i = 0; i < priorities.length ; i++) {
				if(((String)prioritySelecter.getSelectedItem()).equals(I18n.getMessage("thaw.plugin.priority.p"+i)))
					priority = i;
			}

			if((destinationField.getText() == null) || "".equals( destinationField.getText() )) {
				new WarningWindow(core, I18n.getMessage("thaw.plugin.fetch.chooseADestination"));
				return;
			}


			fetchPlugin.fetchFiles(fileList.getText().split("\n"),
					       priority, thaw.fcp.FCPClientGet.PERSISTENCE_FOREVER,
					       globalQueue, destinationField.getText());

			fileList.setText("");
		}


		if(e.getSource() == destinationButton) {
			FileChooser fileChooser;

			if((destinationField.getText() != null) && !"".equals( destinationField.getText() )) {
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

		if(e.getSource() == loadListButton) {
			final FileChooser fileChooser = new FileChooser();
			File toParse = null;

			fileChooser.setTitle(I18n.getMessage("thaw.plugin.fetch.loadKeyListFromFile"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

			toParse = fileChooser.askOneFile();

			if(toParse == null) {
				Logger.info(this, "Nothing to parse");
				return;
			}

			final Vector keys = KeyFileFilter.extractKeys(toParse);

			if((keys == null) || (keys.size() <= 0)) {
				new WarningWindow(core, "No key found !");
				return;
			}


			String result = fileList.getText();

			for(final Iterator i = keys.iterator(); i.hasNext() ;) {
				final String key = (String)i.next();

				result = result + key + "\n";
			}

			fileList.setText(result);
		}
	}



	public void mouseClicked(final MouseEvent e) { }
	public void mouseEntered(final MouseEvent e) { }
	public void mouseExited(final MouseEvent e) { }
	public void mousePressed(final MouseEvent e) {
		showPopupMenu(e);
	}

	public void mouseReleased(final MouseEvent e) {
		showPopupMenu(e);
	}

	protected void showPopupMenu(final MouseEvent e) {
		if(e.isPopupTrigger()) {
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

}

