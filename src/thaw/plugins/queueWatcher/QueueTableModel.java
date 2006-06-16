package thaw.plugins.queueWatcher;


import java.util.Vector;

import thaw.core.*;
import thaw.i18n.I18n;


public class QueueTableModel extends javax.swing.table.AbstractTableModel {
	Vector columnNames = new Vector();

	public QueueTableModel(boolean isForInsertions) {
		super();

		columnNames.add(I18n.getMessage("thaw.common.file"));
		columnNames.add(I18n.getMessage("thaw.common.size"));
		columnNames.add(I18n.getMessage("thaw.common.status"));
		columnNames.add(I18n.getMessage("thaw.common.progress"));
	}
	
	
	public int getRowCount() {
		return 0;
	}
	
	public int getColumnCount() {
		return columnNames.size();
	}
	
	public String getColumnName(int col) {
		return (String)columnNames.get(col);
	}
	
	public Object getValueAt(int row, int column) {
		return null;
	}
	
	public boolean isCellEditable(int row, int column) {
			return false;
	}

}

