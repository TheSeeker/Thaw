package thaw.plugins.queueWatcher;

import java.util.Vector;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableColumn;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Comparator;
import java.util.Collections;

import thaw.core.*;
import thaw.fcp.*;

public class QueueTableModel extends javax.swing.table.AbstractTableModel implements Observer {
	private static final long serialVersionUID = 20060708;

	private Vector columnNames = new Vector();

        private Vector queries = null;

	private boolean isForInsertions = false;

	
	private boolean isSortedAsc = false;
	private int sortedColumn = -1;
	
	private FCPQueueManager queueManager;


	public QueueTableModel(boolean isForInsertions, FCPQueueManager queueManager) {
		super();

		this.queueManager = queueManager;
		this.isForInsertions = isForInsertions;

		columnNames.add(I18n.getMessage("thaw.common.file"));
		columnNames.add(I18n.getMessage("thaw.common.size"));
		
		if(!isForInsertions)
			columnNames.add(I18n.getMessage("thaw.common.localPath"));

		columnNames.add(I18n.getMessage("thaw.common.status"));
		columnNames.add(I18n.getMessage("thaw.common.progress"));

		resetTable();
		
		if(queueManager != null) {
			reloadQueue();
			queueManager.addObserver(this);
		} else {
			Logger.warning(this, "Unable to connect to QueueManager. Is the connection established ?");
		}
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
		String result = (String)columnNames.get(col);

		if(col == sortedColumn) {
			if(isSortedAsc)
				result = result + " >>";
			else
				result = result + " <<";
		}

		return result;
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

	/**
	 * Don't call notifyObservers !
	 */
	public synchronized void resetTable() {

		if(queries != null) {
			for(Iterator it = queries.iterator();
			    it.hasNext();) {
				Observable query = (Observable)it.next();
				query.deleteObserver(this);
			}
		}

		queries = new Vector();

	}

	public synchronized void reloadQueue() {
		resetTable();
		
		addQueries(queueManager.getRunningQueue());
				
		Vector[] pendings = queueManager.getPendingQueues();
		
		for(int i = 0;i < pendings.length ; i++)
			addQueries(pendings[i]);
		
	}

	public synchronized void addQueries(Vector queries) {
		for(Iterator it = queries.iterator();
		    it.hasNext();) {

			FCPTransferQuery query = (FCPTransferQuery)it.next();

			if(query.getQueryType() == 1 && !isForInsertions)
				addQuery(query);

			if(query.getQueryType() == 2 && isForInsertions)
				addQuery(query);

		}
	}

	public synchronized void addQuery(FCPTransferQuery query) {
		((Observable)query).addObserver(this);

		queries.add(query);

		sortTable();

		int changedRow = queries.indexOf(query);

		notifyObservers(new TableModelEvent(this, changedRow, changedRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
	}

	public synchronized void removeQuery(FCPTransferQuery query) {
		((Observable)query).deleteObserver(this);

		sortTable();

		int changedRow = queries.indexOf(query);
		
		queries.remove(query);

		if(changedRow >= 0) {
			notifyObservers(new TableModelEvent(this, changedRow, changedRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		}else
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
		TableModelEvent event = new TableModelEvent(this);

		notifyObservers(event);
	}

	public void notifyObservers(int changedRow) {
		TableModelEvent event = new TableModelEvent(this, changedRow);

		notifyObservers(event);
	}

	public void notifyObservers(TableModelEvent event) {
		TableModelListener[] listeners = getTableModelListeners();

		for(int i = 0 ; i < listeners.length ; i++) {
			listeners[i].tableChanged(event);
		}
	}

	public synchronized void update(Observable o, Object arg) {
		int i;

		sortTable();

		if( (i = queries.indexOf(o)) >= 0) {
			notifyObservers(i);
			return;
		}

		if(arg == null) {
			reloadQueue();
			return;
		}

		FCPTransferQuery query = (FCPTransferQuery)arg;

		if(query.getQueryType() == 1 && isForInsertions)
			return;

		if(query.getQueryType() == 2 && !isForInsertions)
			return;

		if(queueManager.isInTheQueues(query)) { // then it's an adding
			addQuery(query);
			return;
		}

		if(queries.contains(query)) {
			removeQuery(query);
			return;
		}

		Logger.warning(this, "update(): Unknow change ?!");
		reloadQueue();		
	}


	/**
	 * @return false if nothing sorted
	 */
	public boolean sortTable() {
		if(sortedColumn < 0 || queries.size() <= 0)
			return false;

		Collections.sort(queries, new QueryComparator(isSortedAsc, sortedColumn, isForInsertions));

		return true;
	}

	
	public class ColumnListener extends MouseAdapter {
		private JTable table;
				
		public ColumnListener(JTable t) {
			table = t;
		}
		
		public void mouseClicked(MouseEvent e) {
			TableColumnModel colModel = table.getColumnModel();
			int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
			int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();
			
			int columnsCount = table.getColumnCount();

			if (modelIndex < 0)
				return;

			if (sortedColumn == modelIndex)
				isSortedAsc = !isSortedAsc;
			else
				sortedColumn = modelIndex;

			
			for (int i = 0; i < columnsCount; i++) { 
				TableColumn column = colModel.getColumn(i);
				column.setHeaderValue(getColumnName(column.getModelIndex()));
			}

			

			table.getTableHeader().repaint();

			sortTable();			
		}
	}
	

	public class QueryComparator implements Comparator {
		private boolean isSortedAsc;
		private int column;
		private boolean isForInsertionTable;

		public QueryComparator(boolean sortedAsc, int column,
				       boolean isForInsertionTable) {
			this.isSortedAsc = sortedAsc;
			this.column = column;
			this.isForInsertionTable = isForInsertionTable;
		}
		
		public int compare(Object o1, Object o2) {
			int result = 0;

			if(!(o1 instanceof FCPTransferQuery)
			   || !(o2 instanceof FCPTransferQuery))
				return 0;

			FCPTransferQuery q1 = (FCPTransferQuery)o1;
			FCPTransferQuery q2 = (FCPTransferQuery)o2;

			
			if(column == 0) { /* File name */
				if(q1.getFilename() == null)
					return -1;

				if(q2.getFilename() == null)
					return 1;

				result = q1.getFilename().compareTo(q2.getFilename());
			}

			if(column == 1) { /* Size */
				result = (new Long(q1.getFileSize())).compareTo(new Long(q2.getFileSize()));
			}

			if( (column == 2 && !isForInsertionTable) ) { /* localPath */
				if(q1.getPath() == null)
					return -1;

				if(q2.getPath() == null)
					return 1;

				result = q1.getPath().compareTo(q2.getPath());
			}

			if( (column == 2 && isForInsertionTable)
			    || (column == 3 && !isForInsertionTable) ) { /* status */

				if(q1.getStatus() == null)
					return -1;

				if(q2.getStatus() == null)
					return 1;

				result = q1.getStatus().compareTo(q2.getStatus());
			}

			if( (column == 3 && isForInsertionTable)
			    || (column == 4 && !isForInsertionTable) ) { /* progress */
				if(q1.getProgression() <= 0 
				   && q2.getProgression() <= 0) {
					if(q1.isRunning() && !q2.isRunning())
						return 1;

					if(q1.isRunning() && !q2.isRunning())
						return -1;
				}

				result = (new Integer(q1.getProgression())).compareTo(new Integer(q2.getProgression()));
			}
			

			if (!isSortedAsc)
				result = -result;

			return result;
		}
		
		public boolean isSortedAsc() {
			return isSortedAsc;
		}

		public boolean equals(Object obj) {
			if (obj instanceof QueryComparator) {
				QueryComparator compObj = (QueryComparator) obj;
				return compObj.isSortedAsc() == isSortedAsc();
			}
			return false;
		}

	}

}


