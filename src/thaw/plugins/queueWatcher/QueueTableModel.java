package thaw.plugins.queueWatcher;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import thaw.core.I18n;
import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;

public class QueueTableModel extends javax.swing.table.AbstractTableModel implements Observer {
	private static final long serialVersionUID = 20060708;

	private final Vector columnNames = new Vector();

        private Vector queries = null;

	private boolean isForInsertions = false;


	private boolean isSortedAsc = false;
	private int sortedColumn = -1;

	private FCPQueueManager queueManager;


	public QueueTableModel(boolean isForInsertions, final FCPQueueManager queueManager) {
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

	public String getColumnName(final int col) {
		String result = (String)columnNames.get(col);

		if(col == sortedColumn) {
			if(isSortedAsc)
				result = result + " >>";
			else
				result = result + " <<";
		}

		return result;
	}


	public static String getPrintableSize(final long size) {
		if(size == 0)
			return I18n.getMessage("thaw.common.unknown");

		if(size < 1024) /* < 1KB */
			return ((new Long(size)).toString() + " B");

		if(size < 1048576) { /* < 1MB */
			final long kb = size / 1024;
			return ((new Long(kb)).toString() + " KB");
		}

		if(size < 1073741824) { /* < 1GB */
			final long mb = size / 1048576;
			return ((new Long(mb)).toString() + " MB");
		}

		final long gb = size / 1073741824;

		return ((new Long(gb)).toString() +" GB");
	}

	public Object getValueAt(final int row, final int column) {
		if(row >= queries.size())
			return null;

		final FCPTransferQuery query = (FCPTransferQuery)queries.get(row);

		if(column == 0)
			return query.getFilename();

		if(column == 1)
			return QueueTableModel.getPrintableSize(query.getFileSize());

		if(!isForInsertions && (column == 2)) {
			if(query.getPath() != null)
				return query.getPath();
			else
				return I18n.getMessage("thaw.common.unspecified");
		}

		if( (isForInsertions && (column == 2))
		    || (!isForInsertions && (column == 3)) )
			return query.getStatus();

		if( ((isForInsertions && (column == 3))
		     || (!isForInsertions && (column == 4)) ) ) {
			if(!query.isFinished() || query.isSuccessful())
				return new Integer(query.getProgression());
			else
				return new Integer(-1);

		}

		return null;
	}

	public boolean isCellEditable(final int row, final int column) {
		return false;
	}

	/**
	 * Don't call notifyObservers !
	 */
	public synchronized void resetTable() {

		if(queries != null) {
			for(final Iterator it = queries.iterator();
			    it.hasNext();) {
				final Observable query = (Observable)it.next();
				query.deleteObserver(this);
			}
		}

		queries = new Vector();

	}

	public synchronized void reloadQueue() {
		try {
			resetTable();

			addQueries(queueManager.getRunningQueue());

			final Vector[] pendings = queueManager.getPendingQueues();

			for(int i = 0;i < pendings.length ; i++)
				addQueries(pendings[i]);
		} catch(final java.util.ConcurrentModificationException e) {
			Logger.warning(this, "reloadQueue: Collision !");
			reloadQueue();
		}
	}

	public synchronized void addQueries(final Vector queries) {
		for(final Iterator it = queries.iterator();
		    it.hasNext();) {

			final FCPTransferQuery query = (FCPTransferQuery)it.next();

			if((query.getQueryType() == 1) && !isForInsertions)
				addQuery(query);

			if((query.getQueryType() == 2) && isForInsertions)
				addQuery(query);

		}
	}

	public synchronized void addQuery(final FCPTransferQuery query) {
		if(queries.contains(query)) {
			Logger.debug(this, "addQuery() : Already known");
			return;
		}

		((Observable)query).addObserver(this);

		queries.add(query);

		sortTable();

		final int changedRow = queries.indexOf(query);

		this.notifyObservers(new TableModelEvent(this, changedRow, changedRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
	}

	public synchronized void removeQuery(final FCPTransferQuery query) {
		((Observable)query).deleteObserver(this);

		sortTable();

		final int changedRow = queries.indexOf(query);

		queries.remove(query);

		if(changedRow >= 0) {
			this.notifyObservers(new TableModelEvent(this, changedRow, changedRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		}else
			this.notifyObservers();
	}


	public synchronized FCPTransferQuery getQuery(final int row) {
		try {
			return (FCPTransferQuery)queries.get(row);
		} catch(final java.lang.ArrayIndexOutOfBoundsException e) {
			Logger.notice(this, "Query not found, row: "+row);
			return null;
		}
	}

	/**
	 * returns a *copy*
	 */
	public synchronized Vector getQueries() {
		final Vector newVect = new Vector();

		for(final Iterator queryIt = queries.iterator() ;
		    queryIt.hasNext();) {
			newVect.add(queryIt.next());
		}

		return newVect;
	}

	public void notifyObservers() {
		final TableModelEvent event = new TableModelEvent(this);

		this.notifyObservers(event);
	}

	public void notifyObservers(final int changedRow) {
		final TableModelEvent event = new TableModelEvent(this, changedRow);

		this.notifyObservers(event);
	}

	public void notifyObservers(final TableModelEvent event) {
		fireTableChanged(event);

		/*
		TableModelListener[] listeners = getTableModelListeners();

		for(int i = 0 ; i < listeners.length ; i++) {
			listeners[i].tableChanged(event);
		}
		*/
	}

	public synchronized void update(final Observable o, final Object arg) {
		int i;

		sortTable();

		if( (i = queries.indexOf(o)) >= 0) {
			this.notifyObservers(i);
			return;
		}

		if(arg == null) {
			reloadQueue();
			return;
		}

		if(o == queueManager) {
			final FCPTransferQuery query = (FCPTransferQuery)arg;

			if((query.getQueryType() == 1) && isForInsertions)
				return;

			if((query.getQueryType() == 2) && !isForInsertions)
				return;

			if(queueManager.isInTheQueues(query)) { // then it's an adding
				addQuery(query);
				return;
			}

			if(queries.contains(query)) {
				removeQuery(query);
				return;
			}
		}

		Logger.debug(this, "update(): unknow change");
		reloadQueue();
	}


	/**
	 * @return false if nothing sorted
	 */
	public boolean sortTable() {
		if((sortedColumn < 0) || (queries.size() <= 0))
			return false;

		Collections.sort(queries, new QueryComparator(isSortedAsc, sortedColumn, isForInsertions));

		return true;
	}


	public class ColumnListener extends MouseAdapter {
		private JTable table;

		public ColumnListener(final JTable t) {
			table = t;
		}

		public void mouseClicked(final MouseEvent e) {
			final TableColumnModel colModel = table.getColumnModel();
			final int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
			final int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

			final int columnsCount = table.getColumnCount();

			if (modelIndex < 0)
				return;

			if (sortedColumn == modelIndex)
				isSortedAsc = !isSortedAsc;
			else
				sortedColumn = modelIndex;


			for (int i = 0; i < columnsCount; i++) {
				final TableColumn column = colModel.getColumn(i);
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

		public QueryComparator(final boolean sortedAsc, final int column,
				       final boolean isForInsertionTable) {
			isSortedAsc = sortedAsc;
			this.column = column;
			this.isForInsertionTable = isForInsertionTable;
		}

		public int compare(final Object o1, final Object o2) {
			int result = 0;

			if(!(o1 instanceof FCPTransferQuery)
			   || !(o2 instanceof FCPTransferQuery))
				return 0;

			final FCPTransferQuery q1 = (FCPTransferQuery)o1;
			final FCPTransferQuery q2 = (FCPTransferQuery)o2;


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

			if( ((column == 2) && !isForInsertionTable) ) { /* localPath */
				if(q1.getPath() == null)
					return -1;

				if(q2.getPath() == null)
					return 1;

				result = q1.getPath().compareTo(q2.getPath());
			}

			if( ((column == 2) && isForInsertionTable)
			    || ((column == 3) && !isForInsertionTable) ) { /* status */

				if(q1.getStatus() == null)
					return -1;

				if(q2.getStatus() == null)
					return 1;

				result = q1.getStatus().compareTo(q2.getStatus());
			}

			if( ((column == 3) && isForInsertionTable)
			    || ((column == 4) && !isForInsertionTable) ) { /* progress */
				if((q1.getProgression() <= 0)
				   && (q2.getProgression() <= 0)) {
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

		public boolean equals(final Object obj) {
			if (obj instanceof QueryComparator) {
				final QueryComparator compObj = (QueryComparator) obj;
				return compObj.isSortedAsc() == isSortedAsc();
			}
			return false;
		}

		public int hashCode(){
			return super.hashCode();
		}
	}

}


