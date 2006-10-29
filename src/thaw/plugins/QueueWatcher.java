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

		this.detailPanel = new DetailPanel(core);

		this.queuePanels[0] = new QueuePanel(core, this.detailPanel, false); /* download */
		this.queuePanels[1] = new QueuePanel(core, this.detailPanel, true); /* upload */

		this.panel = new JPanel();

		GridLayout layout = new GridLayout(2, 1, 10, 10);
		this.panel.setLayout(layout);

		if(this.queuePanels[0].getPanel() != null)
			this.panel.add(this.queuePanels[0].getPanel());

		if(this.queuePanels[1].getPanel() != null)
			this.panel.add(this.queuePanels[1].getPanel());

		this.advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

		if(this.advancedMode) {
			this.mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.detailPanel.getPanel(), this.panel);

			if(core.getConfig().getValue("detailPanelFolded") == null
			   || ((new Boolean(core.getConfig().getValue("detailPanelFolded"))).booleanValue()) == true) {
				this.folded = true;
				this.detailPanel.getPanel().setVisible(false);
				this.mainPanel.setDividerLocation(1);
			} else {
				this.folded = false;
				this.detailPanel.getPanel().setVisible(true);
				this.mainPanel.setDividerLocation(DIVIDER_LOCATION);
			}

			this.mainPanel.addPropertyChangeListener(this);
			this.mainPanel.setOneTouchExpandable(true);

			this.panelAdded = this.mainPanel;
		} else {
			this.panelAdded = this.panel;
		}

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.status"),
					    IconBox.minQueue,
					    this.panelAdded);

		//if(core.getConnectionManager() != null && core.getConnectionManager().isConnected()) {
		//	core.getConnectionManager().addObserver(this);
		//}

		//if(core.getQueueManager() != null)
		//    core.getQueueManager().addObserver(this);
		//else {
		//    Logger.warning(this, "Unable to connect to QueueManager. Is the connection established ?");
		//    return false;
		//}

		this.dnd = new DragAndDropManager(core, this.queuePanels);

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"QueueWatcher\" ...");

		this.core.getConfig().setValue("detailPanelFolded", ((new Boolean(this.folded)).toString()));

		this.core.getMainWindow().removeTab(this.panelAdded);

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
				this.queuePanels[0].addToTable(query);

			if(query.getQueryType() == 2)
				this.queuePanels[1].addToTable(query);

		}

	}

	public void propertyChange(PropertyChangeEvent evt) {

		if("dividerLocation".equals( evt.getPropertyName() )) {

			if(System.currentTimeMillis() - this.lastChange < 500) {
				this.lastChange = System.currentTimeMillis();
				return;
			}

			this.lastChange = System.currentTimeMillis();

			this.folded = !this.folded;

			if(this.folded) {
				this.detailPanel.getPanel().setVisible(false);
				this.mainPanel.setDividerLocation(1);
			} else {
				this.detailPanel.getPanel().setVisible(true);
				this.mainPanel.setDividerLocation(DIVIDER_LOCATION);
			}

		}

	}

}
