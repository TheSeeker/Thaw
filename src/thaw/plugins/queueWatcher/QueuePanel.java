package thaw.plugins.queueWatcher;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import java.awt.BorderLayout;
import java.util.Vector;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import thaw.core.*;
import thaw.i18n.I18n;

import thaw.fcp.*;

public class QueuePanel implements MouseListener {
	private Core core;

	private JLabel label;

	private JTable table = null;

	private JPanel panel;

	private QueueTableModel tableModel;
	private DetailPanel detailPanel;

	private int lastRowSelected = -1;

	public QueuePanel(Core core, DetailPanel detailPanel, boolean isForInsertionQueue) {
		this.core = core;
		this.detailPanel = detailPanel;
		
		tableModel = new QueueTableModel(isForInsertionQueue);

		table = new JTable(tableModel);

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

		tableModel.addTableModelListener(table);
		table.addMouseListener(this);
	}

	public void resetTable() {
		tableModel.resetTable();
	}

	public void addToTable(FCPQuery query) {
		tableModel.addQuery(query);
	}

	public void refresh() {
		int selected = table.getSelectedRow();

		if(lastRowSelected != selected) {
			lastRowSelected = selected;
			
			if(selected != -1)
				detailPanel.setQuery(tableModel.getQuery(selected));
		}
		
	}

	public JPanel getPanel() {
		return panel;
	}


	public void mouseClicked(MouseEvent e) {
		refresh();
	}

	public void mouseEntered(MouseEvent e) {

	}

	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {

	}

	public void mouseReleased(MouseEvent e) {

	}

}

