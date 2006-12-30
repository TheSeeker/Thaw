package thaw.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import thaw.core.Core;
import thaw.core.FreenetURIHelper;
import thaw.core.I18n;
import thaw.core.IconBox;
import thaw.core.Logger;
import thaw.fcp.FCPClientGet;
import thaw.plugins.fetchPlugin.FetchPanel;

public class FetchPlugin implements thaw.core.Plugin, ActionListener {
	private Core core;

	private FetchPanel fetchPanel = null;

	private JFrame fetchFrame = null;
	private JButton buttonInToolBar = null;
	private JMenuItem menuItem = null;

	private QueueWatcher queueWatcher;

	public FetchPlugin() {

	}


	public boolean run(final Core core) {
		this.core = core;

		Logger.info(this, "Starting plugin \"FetchPlugin\" ...");

		fetchPanel = new FetchPanel(core, this);

		// Prepare the frame

		fetchFrame = new JFrame(I18n.getMessage("thaw.common.download"));
		fetchFrame.setVisible(false);
		fetchFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		fetchFrame.setContentPane(fetchPanel.getPanel());
		fetchFrame.setSize(650, 500);

		// Add the button to the toolbar when the QueueWatch tab is selected
		buttonInToolBar = new JButton(IconBox.downloads);
		buttonInToolBar.setToolTipText(I18n.getMessage("thaw.common.download"));
		buttonInToolBar.addActionListener(this);

		menuItem = new JMenuItem(I18n.getMessage("thaw.common.addDownloads"));
		menuItem.addActionListener(this);

		if(core.getPluginManager().getPlugin("thaw.plugins.QueueWatcher") == null) {
			Logger.notice(this, "Loading QueueWatcher plugin");

			if(!core.getPluginManager().loadPlugin("thaw.plugins.QueueWatcher")
			   || !core.getPluginManager().runPlugin("thaw.plugins.QueueWatcher")) {
				Logger.error(this, "Unable to load thaw.plugins.QueueWatcher !");
				return false;
			}
		}

		queueWatcher = (QueueWatcher)core.getPluginManager().getPlugin("thaw.plugins.QueueWatcher");

		queueWatcher.addButtonToTheToolbar(buttonInToolBar, 0);
		queueWatcher.addMenuItemToTheDownloadTable(menuItem);
		queueWatcher.addButtonListener(QueueWatcher.DOWNLOAD_PANEL, this);

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"FetchPlugin\" ...");

		if (queueWatcher != null)
			queueWatcher.removeButtonFromTheToolbar(buttonInToolBar);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.common.download");
	}

	public void actionPerformed(final ActionEvent e) {
		fetchFrame.setVisible(true);
	}

	public void fetchFiles(final String[] keys, final int priority,
			       final int persistence, final boolean globalQueue,
			       final String destination) {

		for(int i = 0 ; i < keys.length ; i++) {
			if(keys[i].length() < 10)
				continue;

			final String[] subKey = keys[i].split("\\?"); /* Because of VolodyA :p */

			final String key = FreenetURIHelper.cleanURI(subKey[0]);

			if (key != null)
				core.getQueueManager().addQueryToThePendingQueue(new FCPClientGet(key,
												  priority,
												  persistence,
												  globalQueue, -1,
												  destination));
		}

		fetchFrame.setVisible(false);
	}

}
