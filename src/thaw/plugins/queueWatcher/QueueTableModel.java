package thaw.plugins.queueWatcher;

import java.util.Vector;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.JProgressBar;

import thaw.core.*;
import thaw.i18n.I18n;
import thaw.fcp.*;

public class QueueTableModel extends javax.swing.table.AbstractTableModel implements Observer {
	private static final long serialVersionUID = 20060707;

	private Vector columnNames = new Vector();

        private Vector queries = null;

	public QueueTableModel(boolean isForInsertions) {
		super();

		columnNames.add(I18n.getMessage("thaw.common.file"));
		columnNames.add(I18n.getMessage("thaw.common.size"));
		columnNames.add(I18n.getMessage("thaw.common.status"));
		columnNames.add(I18n.getMessage("thaw.common.progress"));

		resetTable();
	}
	


	
	public int getRowCount() {
		if(queries != null)
			return queries.size();
		else
			return 0;
	}
	
	public int getColumnCount() {
		return columnNames.size();
	}
	
	public String getColumnName(int col) {
		return (String)columnNames.get(col);
	}
	
	public Object getValueAt(int row, int column) {
		if(row >= queries.size())
			return null;

		FCPQuery query = (FCPQuery)queries.get(row);
		
		if(column == 0) {
			String[] plop = query.getFileKey().split("/");
			return plop[plop.length-1];
		}

		if(column == 1) {
			return ((new Long(query.getFileSize())).toString() + " B"); /* TODO : Convert to KB / MB / GB */
		}

		if(column == 2) {
			return query.getStatus();
		}

		if(column == 3) {
			return ((new Integer(query.getProgression())).toString() + " %");
		}

		return null;
	}
	
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public void resetTable() {
		if(queries != null) {
			for(Iterator it = queries.iterator();
			    it.hasNext();) {
				Observable query = (Observable)it.next();
				query.deleteObserver(this);
			}
		}

		queries = new Vector();

		notifyObservers();
	}

	public void addQuery(FCPQuery query) {
		JProgressBar bar;

		((Observable)query).addObserver(this);
		
		bar = new JProgressBar(0, 100);
		bar.setStringPainted(true);
		bar.setString((new Integer(query.getProgression())).toString()+"%");
		bar.setValue(query.getProgression());

		queries.add(query);

		notifyObservers();
	}

	public FCPQuery getQuery(int row) {
		return (FCPQuery)queries.get(row);
	}

	public void notifyObservers() {
		TableModelListener[] listeners = getTableModelListeners();

		/* TODO : Sort queries by progression */

		for(int i = 0 ; i < listeners.length ; i++) {
			listeners[i].tableChanged(new TableModelEvent(this));
		}
	}

	public void update(Observable o, Object arg) {
		notifyObservers();
	}

}

