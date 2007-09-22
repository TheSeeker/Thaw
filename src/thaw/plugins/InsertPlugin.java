package thaw.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.gui.IconBox;
import thaw.core.Logger;
import thaw.gui.WarningWindow;
import thaw.fcp.FCPClientPut;
import thaw.plugins.insertPlugin.InsertPanel;


public class InsertPlugin implements thaw.core.Plugin, ActionListener {
	private Core core;

	private InsertPanel insertPanel;
	private JScrollPane scrollPane;

	private JFrame insertionFrame;
	private JButton buttonInToolBar;

	private JMenuItem menuItem;

	private QueueWatcher queueWatcher;


	public InsertPlugin() {

	}


	public boolean run(final Core core) {
		this.core = core;

		core.getConfig().addListener("advancedMode", this);

		Logger.info(this, "Starting plugin \"InsertPlugin\" ...");

		insertPanel = new InsertPanel(this,
					      core.getConfig(), core.getMainWindow(),
					      Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue());

		scrollPane = new JScrollPane(insertPanel.getPanel());

		insertionFrame = new JFrame(I18n.getMessage("thaw.common.insertion"));
		insertionFrame.setVisible(false);
		insertionFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		insertionFrame.setContentPane(scrollPane);

		if ((core.getConfig().getValue("advancedMode") == null)
		    || Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue()) {
			insertionFrame.setSize(750, 450);
		} else {
			insertionFrame.setSize(350, 200);
		}

		buttonInToolBar = new JButton(IconBox.insertions);
		buttonInToolBar.setToolTipText(I18n.getMessage("thaw.common.insertion"));
		buttonInToolBar.addActionListener(this);

		menuItem = new JMenuItem(I18n.getMessage("thaw.common.addInsertions"), IconBox.minInsertions);
		menuItem.addActionListener(this);

		if(core.getPluginManager().getPlugin("thaw.plugins.QueueWatcher") == null) {
			Logger.notice(this, "Loading QueueWatcher plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.QueueWatcher") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.QueueWatcher")) {
				Logger.error(this, "Unable to load thaw.plugins.QueueWatcher !");
				return false;
			}
		}

		queueWatcher = (QueueWatcher)core.getPluginManager().getPlugin("thaw.plugins.QueueWatcher");

		queueWatcher.addButtonToTheToolbar(buttonInToolBar, 1);
		queueWatcher.addMenuItemToTheInsertionTable(menuItem);
		queueWatcher.addButtonListener(QueueWatcher.INSERTION_PANEL, this);

		return true;
	}


	public void stop() {
		Logger.info(this, "Stopping plugin \"InsertPlugin\" ...");

		if (queueWatcher != null)
			queueWatcher.removeButtonFromTheToolbar(buttonInToolBar);
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.common.insertion");
	}


	public void actionPerformed(final ActionEvent e) {
		insertionFrame.setVisible(true);
	}


	/**
	 * Note: public key is found from private one.
	 * @param fileList File list, separated by ';'
	 * @param keyType 0 = CHK ; 1 = KSK ; 2 = SSK
	 * @param rev  ignored if key == CHK
	 * @param name ignored if key == CHK
	 * @param privateKey ignored if key == CHK/KSK ; can be null if it has to be generated
	 * @param persistence 0 = Forever ; 1 = Until node reboot ; 2 = Until the app disconnect
	 * @param mimeType null = autodetect
	 */
	public boolean insertFile(final String fileList, final int keyType,
				  final int rev, final String name,
				  final String privateKey,
				  final int priority, final boolean global,
				  final int persistence, final String mimeType) {

		FCPClientPut clientPut = null;
		final String[] files = fileList.split(";");

		if((keyType > 0) && (files.length > 1)) {
			new WarningWindow(core, "Can't insert multiple SSH@ / KSK@ files at the same time. Use jSite.");
			return false;
		}

		for(int i = 0 ; i < files.length ; i++) {

			if((privateKey != null) && !"".equals( privateKey )) {
				clientPut = new FCPClientPut(new File(files[i]), keyType, rev, name,
							     "USK@"+privateKey+"/", priority,
							     global, persistence);
			} else {
				clientPut = new FCPClientPut(new File(files[i]), keyType, rev, name,
							     null, priority,
							     global, persistence);
			}

			if(mimeType != null) {
				Logger.notice(this, "Mime type forced to "+mimeType);
				clientPut.setMetadata("ContentType", mimeType);
			}

			insertPanel.setLastInserted(clientPut);
			clientPut.addObserver(insertPanel);

			core.getQueueManager().addQueryToThePendingQueue(clientPut);

		}

		insertionFrame.setVisible(false);

		return true;
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.insertions;
	}
}
