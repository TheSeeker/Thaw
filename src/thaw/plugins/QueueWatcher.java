package thaw.plugins;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JButton;

import thaw.core.*;
import thaw.i18n.I18n;
import thaw.plugins.queueWatcher.*;


public class QueueWatcher implements thaw.core.Plugin {
	private Core core;

	private JPanel mainPanel;

	private QueuePanel[] queuePanels = new QueuePanel[2];
	private DetailPanel detailPanel;
	private JPanel panel;


	private JPanel buttonPanel;
	private JButton buttonCopyInsertedKeys;
	private JButton buttonCopyDownloadedKeys;

	public QueueWatcher() {

	}


	public boolean run(Core core) {
		this.core = core;
		
		Logger.info(this, "Starting plugin \"QueueWatcher\" ...");

		mainPanel = new JPanel();

		mainPanel.setLayout(new BorderLayout());

		queuePanels[0] = new QueuePanel(core, false); /* download */
		queuePanels[1] = new QueuePanel(core, true); /* upload */
		detailPanel = new DetailPanel(core);

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

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));

		buttonCopyInsertedKeys = new JButton(I18n.getMessage("thaw.plugin.copySelectedAndInsertedKeys"));
		buttonCopyDownloadedKeys = new JButton(I18n.getMessage("thaw.plugin.copySelectedAndDownloadedKeys"));

		buttonPanel.add(buttonCopyInsertedKeys);
		buttonPanel.add(buttonCopyDownloadedKeys);

		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.status"), mainPanel);

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"QueueWatcher\" ...");

		core.getMainWindow().removeTab(panel);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.common.status");
	}

}
