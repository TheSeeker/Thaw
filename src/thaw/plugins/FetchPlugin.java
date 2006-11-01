package thaw.plugins;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.WindowConstants;

import thaw.core.*;
import thaw.plugins.fetchPlugin.*;

import thaw.fcp.*;

public class FetchPlugin implements thaw.core.Plugin, ActionListener {
	private Core core;

	private FetchPanel fetchPanel = null;

	private JFrame fetchFrame = null;
	private JButton buttonInToolBar = null;

	private QueueWatcher queueWatcher;

	public FetchPlugin() {

	}


	public boolean run(Core core) {
		this.core = core;

		Logger.info(this, "Starting plugin \"FetchPlugin\" ...");

		this.fetchPanel = new FetchPanel(core, this);

		//core.getMainWindow().addTab(I18n.getMessage("thaw.common.download"),
		//			    IconBox.minDownloads,
		//			    this.fetchPanel.getPanel());


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


		if(core.getPluginManager().getPlugin("thaw.plugins.QueueWatcher") == null) {
			Logger.info(this, "Loading QueueWatcher plugin");

			if(!core.getPluginManager().loadPlugin("thaw.plugins.QueueWatcher")
			   || !core.getPluginManager().runPlugin("thaw.plugins.QueueWatcher")) {
				Logger.error(this, "Unable to load thaw.plugins.QueueWatcher !");
				return false;
			}
		}

		queueWatcher = (QueueWatcher)core.getPluginManager().getPlugin("thaw.plugins.QueueWatcher");

		queueWatcher.addButtonToTheToolbar(buttonInToolBar);

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"FetchPlugin\" ...");

		//this.core.getMainWindow().removeTab(this.fetchPanel.getPanel());

		if (queueWatcher != null)
			queueWatcher.removeButtonFromTheToolbar(buttonInToolBar);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.common.download");
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonInToolBar)
			fetchFrame.setVisible(true);
	}

	public void fetchFiles(String[] keys, int priority,
			       int persistence, boolean globalQueue,
			       String destination) {

		for(int i = 0 ; i < keys.length ; i++) {
			if(keys[i].length() < 10)
				continue;

			String[] subKey = keys[i].split("\\?"); /* Because of VolodyA :p */

			String key = FreenetURIHelper.cleanURI(subKey[0]);

			this.core.getQueueManager().addQueryToThePendingQueue(new FCPClientGet(key,
											  priority,
											  persistence,
											  globalQueue, -1,
											  destination));
		}

		fetchFrame.setVisible(false);
	}

}
