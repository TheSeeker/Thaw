package thaw.plugins.queueWatcher;

import java.util.Vector;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import javax.swing.event.TableModelEvent;
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

		this.columnNames.add(I18n.getMessage("thaw.common.file"));
		this.columnNames.add(I18n.getMessage("thaw.common.size"));

		if(!isForInsertions)
			this.columnNames.add(I18n.getMessage("thaw.common.localPath"));

		this.columnNames.add(I18n.getMessage("thaw.common.status"));
		this.columnNames.add(I18n.getMessage("thaw.common.progress"));

		this.resetTable();

		if(queueManager != null) {
			this.reloadQueue();
			queueManager.addObserver(this);
		} else {
			Logger.warning(this, "Unable to connect to QueueManager. Is the connection established ?");
		}
	}




	public int getRowCount() {
		if(this.queries != null)
			return this.queries.size();
		else
			return 0;
	}

	public int getColumnCount() {
		return this.columnNames.size();
	}

	public String getColumnName(int col) {
		String result = (String)this.columnNames.get(col);

		if(col == this.sortedColumn) {
			if(this.isSortedAsc)
				result = result + " >>";
			else
				result = result + " <<";
		}

		return result;
	}


	public static String getPrintableSize(long size) {
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
		if(row >= this.queries.size())
			return null;

		FCPTransferQuery query = (FCPTransferQuery)this.queries.get(row);

		if(column == 0) {
			return query.getFilename();
		}

		if(column == 1) {
			return getPrintableSize(query.getFileSize());
		}

		if(!this.isForInsertions && column == 2) {
			if(query.getPath() != null)
				return query.getPath();
			else
				return I18n.getMessage("thaw.common.unspecified");
		}

		if( (this.isForInsertions && column == 2)
		    || (!this.isForInsertions && column == 3) ) {
			return query.getStatus();
		}

		if( (this.isForInsertions && column == 3
		     || (!this.isForInsertions && column == 4) ) ) {
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

		if(this.queries != null) {
			for(Iterator it = this.queries.iterator();
			    it.hasNext();) {
				Observable query = (Observable)it.next();
				query.deleteObserver(this);
			}
		}

		this.queries = new Vector();

	}

	public synchronized void reloadQueue() {
		try {
			this.resetTable();

			this.addQueries(this.queueManager.getRunningQueue());

			Vector[] pendings = this.queueManager.getPendingQueues();

			for(int i = 0;i < pendings.length ; i++)
				this.addQueries(pendings[i]);
		} catch(java.util.ConcurrentModificationException e) {
			Logger.warning(this, "reloadQueue: Collision !");
			this.reloadQueue();
		}
	}

	public synchronized void addQueries(Vector queries) {
		for(Iterator it = queries.iterator();
		    it.hasNext();) {

			FCPTransferQuery query = (FCPTransferQuery)it.next();

			if(query.getQueryType() == 1 && !this.isForInsertions)
				this.addQuery(query);

			if(query.getQueryType() == 2 && this.isForInsertions)
				this.addQuery(query);

		}
	}

	public synchronized void addQuery(FCPTransferQuery query) {
		if(this.queries.contains(query)) {
			Logger.debug(this, "addQuery() : Already known");
			return;
		}

		((Observable)query).addObserver(this);

		this.queries.add(query);

		this.sortTable();

		int changedRow = this.queries.indexOf(query);

		this.notifyObservers(new TableModelEvent(this, changedRow, changedRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
	}

	public synchronized void removeQuery(FCPTransferQuery query) {
		((Observable)query).deleteObserver(this);

		this.sortTable();

		int changedRow = this.queries.indexOf(query);

		this.queries.remove(query);

		if(changedRow >= 0) {
			this.notifyObservers(new TableModelEvent(this, changedRow, changedRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		}else
			this.notifyObservers();
	}


	public synchronized FCPTransferQuery getQuery(int row) {
		try {
			return (FCPTransferQuery)this.queries.get(row);
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

		for(Iterator queryIt = this.queries.iterator() ;
		    queryIt.hasNext();) {
			newVect.add(queryIt.next());
		}

		return newVect;
	}

	public void notifyObservers() {
		TableModelEvent event = new TableModelEvent(this);

		this.notifyObservers(event);
	}

	public void notifyObservers(int changedRow) {
		TableModelEvent event = new TableModelEvent(this, changedRow);

		this.notifyObservers(event);
	}

	public void notifyObservers(TableModelEvent event) {
		this.fireTableChanged(event);

		/*
		TableModelListener[] listeners = getTableModelListeners();

		for(int i = 0 ; i < listeners.length ; i++) {
			listeners[i].tableChanged(event);
		}
		*/
	}

	public synchronized void update(Observable o, Object arg) {
		int i;

		this.sortTable();

		if( (i = this.queries.indexOf(o)) >= 0) {
			this.notifyObservers(i);
			return;
		}

		if(arg == null) {
			this.reloadQueue();
			return;
		}

		if(o == this.queueManager) {
			FCPTransferQuery query = (FCPTransferQuery)arg;

			if(query.getQueryType() == 1 && this.isForInsertions)
				return;

			if(query.getQueryType() == 2 && !this.isForInsertions)
				return;

			if(this.queueManager.isInTheQueues(query)) { // then it's an adding
				this.addQuery(query);
				return;
			}

			if(this.queries.contains(query)) {
				this.removeQuery(query);
				return;
			}
		}

		Logger.notice(this, "update(): unknow change");
		this.reloadQueue();
	}


	/**
	 * @return false if nothing sorted
	 */
	public boolean sortTable() {
		if(this.sortedColumn < 0 || this.queries.size() <= 0)
			return false;

		Collections.sort(this.queries, new QueryComparator(this.isSortedAsc, this.sortedColumn, this.isForInsertions));

		return true;
	}


	public class ColumnListener extends MouseAdapter {
		private JTable table;

		public ColumnListener(JTable t) {
			this.table = t;
		}

		public void mouseClicked(MouseEvent e) {
			TableColumnModel colModel = this.table.getColumnModel();
			int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
			int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

			int columnsCount = this.table.getColumnCount();

			if (modelIndex < 0)
				return;

			if (QueueTableModel.this.sortedColumn == modelIndex)
				QueueTableModel.this.isSortedAsc = !QueueTableModel.this.isSortedAsc;
			else
				QueueTableModel.this.sortedColumn = modelIndex;


			for (int i = 0; i < columnsCount; i++) {
				TableColumn column = colModel.getColumn(i);
				column.setHeaderValue(QueueTableModel.this.getColumnName(column.getModelIndex()));
			}



			this.table.getTableHeader().repaint();

			QueueTableModel.this.sortTable();
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


			if(this.column == 0) { /* File name */
				if(q1.getFilename() == null)
					return -1;

				if(q2.getFilename() == null)
					return 1;

				result = q1.getFilename().compareTo(q2.getFilename());
			}

			if(this.column == 1) { /* Size */
				result = (new Long(q1.getFileSize())).compareTo(new Long(q2.getFileSize()));
			}

			if( (this.column == 2 && !this.isForInsertionTable) ) { /* localPath */
				if(q1.getPath() == null)
					return -1;

				if(q2.getPath() == null)
					return 1;

				result = q1.getPath().compareTo(q2.getPath());
			}

			if( (this.column == 2 && this.isForInsertionTable)
			    || (this.column == 3 && !this.isForInsertionTable) ) { /* status */

				if(q1.getStatus() == null)
					return -1;

				if(q2.getStatus() == null)
					return 1;

				result = q1.getStatus().compareTo(q2.getStatus());
			}

			if( (this.column == 3 && this.isForInsertionTable)
			    || (this.column == 4 && !this.isForInsertionTable) ) { /* progress */
				if(q1.getProgression() <= 0
				   && q2.getProgression() <= 0) {
					if(q1.isRunning() && !q2.isRunning())
						return 1;

					if(q1.isRunning() && !q2.isRunning())
						return -1;
				}

				result = (new Integer(q1.getProgression())).compareTo(new Integer(q2.getProgression()));
			}


			if (!this.isSortedAsc)
				result = -result;

			return result;
		}

		public boolean isSortedAsc() {
			return this.isSortedAsc;
		}

		public boolean equals(Object obj) {
			if (obj instanceof QueryComparator) {
				QueryComparator compObj = (QueryComparator) obj;
				return compObj.isSortedAsc() == this.isSortedAsc();
			}
			return false;
		}

		public int hashCode(){
			return super.hashCode();
		}
	}

}


