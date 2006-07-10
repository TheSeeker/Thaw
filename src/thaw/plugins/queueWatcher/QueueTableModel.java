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

		FCPTransferQuery query = (FCPTransferQuery)queries.get(row);
		
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

	public synchronized void resetTable() {

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

	public synchronized void addQuery(FCPTransferQuery query) {
		((Observable)query).addObserver(this);
		
		queries.add(query);

		notifyObservers();
	}

	public synchronized void removeQuery(FCPTransferQuery query) {
		((Observable)query).deleteObserver(this);

		queries.remove(query);

		notifyObservers();
	}


	public synchronized FCPTransferQuery getQuery(int row) {
		try {
			return (FCPTransferQuery)queries.get(row);
		} catch(java.lang.ArrayIndexOutOfBoundsException e) {
			Logger.notice(this, "Query not found, row: "+row);
			return null;
		}
	}

	/**
	 * returns a *copy*
	 */
	public synchronized Vector getQueries() {
		Vector newVect = new Vector();

		for(Iterator queryIt = queries.iterator() ;
		    queryIt.hasNext();) {
			newVect.add(queryIt.next());
		}

		return newVect;
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

