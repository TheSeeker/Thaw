package thaw.plugins.queueWatcher;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Vector;

import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

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

		table.setDefaultRenderer( table.getColumnClass(0), new ProgressRenderer(table, tableModel) );

		tableModel.addTableModelListener(table);
		table.addMouseListener(this);
	}


	private class ProgressRenderer extends DefaultTableCellRenderer {
		private final static long serialVersionUID = 20060709;

		private final Color SUCCESS = Color.GREEN;
		private final Color FAILURE = Color.RED;
		private final Color RUNNING = Color.ORANGE;
		private final Color PENDING = Color.WHITE;

		QueueTableModel model = null;
		JTable tabl = null;

		public ProgressRenderer(JTable table, QueueTableModel model) {
			this.model = model;
			this.tabl = table;
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
							       boolean isSelected, boolean hasFocus,
							       int row, int column) {

			Component cell = super.getTableCellRendererComponent(table, value,
									     isSelected, hasFocus,
									     row, column);

			if(!isSelected) {
	
				FCPQuery query = model.getQuery(row);
				
				if(!query.isRunning() && !query.isFinished())
					cell.setBackground(PENDING);
				if(query.isFinished() && query.isSuccessful())
					cell.setBackground(SUCCESS);
				if(query.isFinished() && !query.isSuccessful())
					cell.setBackground(FAILURE);
				if(query.isRunning() && !query.isFinished())
					cell.setBackground(RUNNING);
			}
			

			return cell;
		}

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

