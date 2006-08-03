package thaw.plugins.queueWatcher;

import java.util.Vector;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.JProgressBar;

import thaw.core.*;
import thaw.fcp.*;

public class QueueTableModel extends javax.swing.table.AbstractTableModel implements Observer {
	private static final long serialVersionUID = 20060707;

	private Vector columnNames = new Vector();

        private Vector queries = null;

	private boolean isForInsertions = false;

	public QueueTableModel(boolean isForInsertions) {
		super();

		this.isForInsertions = isForInsertions;

		columnNames.add(I18n.getMessage("thaw.common.file"));
		columnNames.add(I18n.getMessage("thaw.common.size"));
		
		if(!isForInsertions)
			columnNames.add(I18n.getMessage("thaw.common.localPath"));

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
	

	private String getPrintableSize(long size) {
		if(size == 0)
			return I18n.getMessage("thaw.common.unknown");

		if(size < 1024) /* < 1KB */
			return ((new Long(size)).toString() + " B");

		if(size < 1048576) { /* < 1MB */
			long kb = size / 1024;
			return ((new Long(kb)).toString() + " KB");
		}

		if(size < 1073741824) { /* < 1GB */
			long mb = size / 1048576;
			return ((new Long(mb)).toString() + " MB");
		}

		long gb = size / 1073741824;

		return ((new Long(gb)).toString() +" GB");
	}

	public Object getValueAt(int row, int column) {
		if(row >= queries.size())
			return null;

		FCPTransferQuery query = (FCPTransferQuery)queries.get(row);
		
		if(column == 0) {
			return query.getFilename();
		}

		if(column == 1) {
			return getPrintableSize(query.getFileSize());
		}

		if(!isForInsertions && column == 2) {
			if(query.getPath() != null)
				return query.getPath();
			else
				return I18n.getMessage("thaw.common.unspecified");
		}

		if( (isForInsertions && column == 2)
		    || (!isForInsertions && column == 3) ) {
			return query.getStatus();
		}

		if( (isForInsertions && column == 3
		     || (!isForInsertions && column == 4) ) ) {
			if(!query.isFinished() || query.isSuccessful())
				return new Integer(query.getProgression());
			else
				return new Integer(-1);

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

