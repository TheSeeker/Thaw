package thaw.plugins;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.util.Observer;
import java.util.Observable;
import java.util.Vector;
import java.util.Iterator;

import thaw.core.*;
import thaw.i18n.I18n;
import thaw.plugins.queueWatcher.*;

import thaw.fcp.*;

public class QueueWatcher implements thaw.core.Plugin, Observer {
	private Core core;

	private JPanel mainPanel;

	private QueuePanel[] queuePanels = new QueuePanel[2];
	private DetailPanel detailPanel;
	private JPanel panel;

	public QueueWatcher() {

	}


	public boolean run(Core core) {
		this.core = core;
		
		Logger.info(this, "Starting plugin \"QueueWatcher\" ...");

		mainPanel = new JPanel();

		mainPanel.setLayout(new BorderLayout());

		detailPanel = new DetailPanel(core);

		queuePanels[0] = new QueuePanel(core, detailPanel, false); /* download */
		queuePanels[1] = new QueuePanel(core, detailPanel, true); /* upload */

		panel = new JPanel();

		GridLayout layout = new GridLayout(2, 1);
		layout.setVgap(10);
		panel.setLayout(layout);

		mainPanel.add(panel, BorderLayout.CENTER);

		if(queuePanels[0].getPanel() != null)
			panel.add(queuePanels[0].getPanel());

		if(queuePanels[1].getPanel() != null)
			panel.add(queuePanels[1].getPanel());
		

		if(detailPanel.getPanel() != null) {
			mainPanel.add(detailPanel.getPanel(), BorderLayout.EAST);
		}

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.status"), mainPanel);

		if(core.getQueueManager() != null)
		    core.getQueueManager().addObserver(this);
		else {
		    Logger.warning(this, "Unable to connect to QueueManager. Is the connection established ?");
		    return false;
		}
		    

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"QueueWatcher\" ...");

		core.getMainWindow().removeTab(mainPanel);
		
		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.common.status");
	}

	protected void addToPanels(Vector queries) {

		for(Iterator it = queries.iterator();
		    it.hasNext();) {

			FCPTransferQuery query = (FCPTransferQuery)it.next();

			if(query.getQueryType() == 1)
				queuePanels[0].addToTable(query);

			if(query.getQueryType() == 2)
				queuePanels[1].addToTable(query);

		}

	}

	public void update(Observable o, Object arg) {

		FCPQueueManager manager = (FCPQueueManager)o;

		try {
			queuePanels[0].resetTable();
			queuePanels[1].resetTable();
			
			addToPanels(manager.getRunningQueue());
			
			Vector[] pendings = manager.getPendingQueues();
			
			for(int i = 0;i < pendings.length ; i++)
				addToPanels(pendings[i]);

		} catch(java.util.ConcurrentModificationException e) {
			Logger.notice(this, "Collision while updating queue panels");
		}
		
	}

}