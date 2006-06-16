package thaw.plugins.queueWatcher;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.Vector;

import thaw.core.*;
import thaw.i18n.I18n;


public class QueuePanel {
	private Core core;

	private JLabel label;
	private JTable table = null;
	private JPanel panel;


	public QueuePanel(Core core, boolean isForInsertionQueue) {
		this.core = core;
		
		table = new JTable(new QueueTableModel(isForInsertionQueue));

		table.setShowGrid(true);
		
		if(isForInsertionQueue) {
			label = new JLabel(I18n.getMessage("thaw.common.insertions"));
		} else {
			label = new JLabel(I18n.getMessage("thaw.common.downloads"));
		}

		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		panel.add(label, BorderLayout.NORTH);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
	}

	public JPanel getPanel() {
		return panel;
	}
}

