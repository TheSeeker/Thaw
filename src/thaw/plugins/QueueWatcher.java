package thaw.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.gui.IconBox;
import thaw.core.Logger;
import thaw.core.MainWindow;
import thaw.fcp.FCPTransferQuery;
import thaw.plugins.queueWatcher.DetailPanel;
import thaw.plugins.queueWatcher.DragAndDropManager;
import thaw.plugins.queueWatcher.QueuePanel;

public class QueueWatcher extends ToolbarModifier implements thaw.core.Plugin, PropertyChangeListener, ChangeListener, ActionListener {
	private Core core;

	//private JPanel mainPanel;
	private JSplitPane mainPanel;

	public final static int DOWNLOAD_PANEL = 0;
	public final static int INSERTION_PANEL = 1;
	private final QueuePanel[] queuePanels = new QueuePanel[2];

	private DetailPanel detailPanel;
	private DragAndDropManager dnd;

	private JSplitPane split;

	private final static int DIVIDER_LOCATION = 310; /* about the details panel */
	private long lastChange = 0;
	private boolean folded = false;

	private boolean advancedMode = false;

	private java.awt.Container panelAdded;

	private JButton removeSelectedButton;


	public boolean run(final Core core) {
		this.core = core;

		Logger.info(this, "Starting plugin \"QueueWatcher\" ...");

		detailPanel = new DetailPanel();

		queuePanels[QueueWatcher.DOWNLOAD_PANEL] = new QueuePanel(core, this, detailPanel, false); /* download */
		queuePanels[QueueWatcher.INSERTION_PANEL] = new QueuePanel(core, this, detailPanel, true); /* upload */

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				       queuePanels[0].getPanel(),
				       queuePanels[1].getPanel());


		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

		if(advancedMode) {
			mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, detailPanel.getPanel(), split);

			if((core.getConfig().getValue("detailPanelFolded") == null)
			   || (((new Boolean(core.getConfig().getValue("detailPanelFolded"))).booleanValue()) == true)) {
				folded = true;
				detailPanel.getPanel().setVisible(false);
				mainPanel.setDividerLocation(1);
			} else {
				folded = false;
				detailPanel.getPanel().setVisible(true);
				mainPanel.setDividerLocation(QueueWatcher.DIVIDER_LOCATION);
			}

			mainPanel.addPropertyChangeListener(this);
			mainPanel.setOneTouchExpandable(true);

			panelAdded = mainPanel;
		} else {
			panelAdded = split;
		}

		split.setSize(MainWindow.DEFAULT_SIZE_X - 150, MainWindow.DEFAULT_SIZE_Y - 150); /* needed to avoid size = 0at the begining */
		split.setResizeWeight(0.5);

		setMainWindow(core.getMainWindow());
		core.getMainWindow().getTabbedPane().addChangeListener(this);
		core.getMainWindow().addTab(I18n.getMessage("thaw.common.status"),
					    IconBox.minQueue,
					    panelAdded);

		split.setResizeWeight(0.5);

		if (core.getConfig().getValue("queuePanelSplitLocation") == null) {
			split.setDividerLocation((0.5));
		} else {
			try {
				split.setDividerLocation(Integer.parseInt(core.getConfig().getValue("queuePanelSplitLocation")));
			} catch(java.lang.IllegalArgumentException e) { /* TODO: Shouldn't happen ! */
				Logger.error(this, "Error while setting split bar position: "+e.toString());
			}
		}

		split.setResizeWeight(0.5);

		dnd = new DragAndDropManager(core, queuePanels);

		stateChanged(null);

		removeSelectedButton = new JButton(IconBox.delete);
		removeSelectedButton.setToolTipText(I18n.getMessage("thaw.common.removeFromTheList"));
		removeSelectedButton.addActionListener(this);
		addButtonToTheToolbar(removeSelectedButton);

		return true;
	}

	/**
	 * See the button 'download' and 'insertion' on each panel
	 * @param panel see DOWNLOAD_PANEL and INSERTION_PANEL
	 */
	public void addButtonListener(final int panel, final ActionListener listener) {
		queuePanels[panel].getButton().addActionListener(listener);
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"QueueWatcher\" ...");

		int splitLocation;

		core.getMainWindow().getTabbedPane().removeChangeListener(this);


		splitLocation = split.getDividerLocation();

		core.getConfig().setValue("queuePanelSplitLocation",
					  Integer.toString(splitLocation));

		core.getConfig().setValue("detailPanelFolded", ((new Boolean(folded)).toString()));
		core.getMainWindow().removeTab(panelAdded);

		purgeButtonList();

		return true;
	}


	public void addMenuItemToTheDownloadTable(final javax.swing.JMenuItem item) {
		queuePanels[0].addMenuItem(item);
	}

	public void addMenuItemToTheInsertionTable(final javax.swing.JMenuItem item) {
		queuePanels[1].addMenuItem(item);
	}

	public void unselectAllExcept(int panel_exception) {
		if (panel_exception == DOWNLOAD_PANEL)
			queuePanels[INSERTION_PANEL].unselectAll();
		else
			queuePanels[DOWNLOAD_PANEL].unselectAll();
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.common.status");
	}


	protected void addToPanels(final Vector queries) {

		for(final Iterator it = queries.iterator();
		    it.hasNext();) {

			final FCPTransferQuery query = (FCPTransferQuery)it.next();

			if(query.getQueryType() == 1)
				queuePanels[0].addToTable(query);

			if(query.getQueryType() == 2)
				queuePanels[1].addToTable(query);

		}

	}

	/**
	 * Called when the split bar position changes.
	 */
	public void propertyChange(final PropertyChangeEvent evt) {

		if("dividerLocation".equals( evt.getPropertyName() )) {

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
				mainPanel.setDividerLocation(QueueWatcher.DIVIDER_LOCATION);
			}

		}

	}

	/**
	 * Called when the JTabbedPane changed (ie change in the selected tab, etc)
	 * @param e can be null.
	 */
	public void stateChanged(final ChangeEvent e) {
		int tabId;

		tabId = core.getMainWindow().getTabbedPane().indexOfTab(I18n.getMessage("thaw.common.status"));

		if (tabId < 0) {
			Logger.warning(this, "Unable to find the tab !");
			return;
		}

		if (core.getMainWindow().getTabbedPane().getSelectedIndex() == tabId) {
			displayButtonsInTheToolbar();
		} else {
			hideButtonsInTheToolbar();
		}
	}

	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == removeSelectedButton) {
			queuePanels[INSERTION_PANEL].removeSelectedTransfers();
			queuePanels[DOWNLOAD_PANEL].removeSelectedTransfers();
		}
	}


	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.queueWatcher;
	}
}
