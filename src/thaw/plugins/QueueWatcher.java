package thaw.plugins;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.GridLayout;
import java.util.Vector;
import java.util.Iterator;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import thaw.core.*;
import thaw.plugins.queueWatcher.*;

import thaw.fcp.*;

public class QueueWatcher implements thaw.core.Plugin, PropertyChangeListener {
	private Core core;

	//private JPanel mainPanel;
	private JSplitPane mainPanel;

	private QueuePanel[] queuePanels = new QueuePanel[2];
	private DetailPanel detailPanel;
	private DragAndDropManager dnd;

	private JPanel panel;

	private final static int DIVIDER_LOCATION = 310;
	private long lastChange = 0;
	private boolean folded = false;

	private boolean advancedMode = false;

	private java.awt.Container panelAdded;

	public QueueWatcher() {

	}


	public boolean run(Core core) {
		this.core = core;
		
		Logger.info(this, "Starting plugin \"QueueWatcher\" ...");

		detailPanel = new DetailPanel(core);

		queuePanels[0] = new QueuePanel(core, detailPanel, false); /* download */
		queuePanels[1] = new QueuePanel(core, detailPanel, true); /* upload */

		panel = new JPanel();

		GridLayout layout = new GridLayout(2, 1, 10, 10);
		panel.setLayout(layout);

		if(queuePanels[0].getPanel() != null)
			panel.add(queuePanels[0].getPanel());

		if(queuePanels[1].getPanel() != null)
			panel.add(queuePanels[1].getPanel());
		
		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();
		
		if(advancedMode) {
			mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, detailPanel.getPanel(), panel);
			
			if(core.getConfig().getValue("detailPanelFolded") == null
			   || ((new Boolean(core.getConfig().getValue("detailPanelFolded"))).booleanValue()) == true) {
				folded = true;
				detailPanel.getPanel().setVisible(false);
				mainPanel.setDividerLocation(1);
			} else {
				folded = false;
				detailPanel.getPanel().setVisible(true);
				mainPanel.setDividerLocation(DIVIDER_LOCATION);
			}
			
			mainPanel.addPropertyChangeListener(this);
			mainPanel.setOneTouchExpandable(true);

			panelAdded = mainPanel;
		} else {
			panelAdded = panel;
		}

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.status"), 
					    IconBox.minQueue,
					    panelAdded);
			
		//if(core.getConnectionManager() != null && core.getConnectionManager().isConnected()) {
		//	core.getConnectionManager().addObserver(this);
		//}

		//if(core.getQueueManager() != null)
		//    core.getQueueManager().addObserver(this);
		//else {
		//    Logger.warning(this, "Unable to connect to QueueManager. Is the connection established ?");
		//    return false;
		//}
		    
		dnd = new DragAndDropManager(core, queuePanels);

		return true;
	}

	


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"QueueWatcher\" ...");

		core.getConfig().setValue("detailPanelFolded", ((new Boolean(folded)).toString()));

		core.getMainWindow().removeTab(panelAdded);
		
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

	/*
	public void update(Observable o, Object arg) {
		if(o == core.getConnectionManager()) {
			queuePanels[0].resetTable();
			queuePanels[1].resetTable();
		}


		if(o == core.getQueueManager()) {

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
	*/

	public void propertyChange(PropertyChangeEvent evt) {

		if(evt.getPropertyName().equals("dividerLocation")) {

			if(System.currentTimeMillis() - lastChange < 500) {
				lastChange = System.currentTimeMillis();
				return;
			}

			lastChange = System.currentTimeMillis();

			folded = !folded;

			if(folded) {
				detailPanel.getPanel().setVisible(false);
				mainPanel.setDividerLocation(1);
			} else {
				detailPanel.getPanel().setVisible(true);
				mainPanel.setDividerLocation(DIVIDER_LOCATION);
			}

		}

	}

}
