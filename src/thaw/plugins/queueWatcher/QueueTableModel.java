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
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.gui.IconBox;
import thaw.gui.GUIHelper;
import thaw.core.PluginManager;
import thaw.plugins.TrayIcon;


public class QueueTableModel extends javax.swing.table.AbstractTableModel implements Observer {
	private static final long serialVersionUID = 20060709;
	private final static String totalTimeStr = I18n.getMessage("thaw.plugin.queueWatcher.totalTime");
	private final static String downloadSuccessfulStr = I18n.getMessage("thaw.plugin.queueWatcher.downloadSuccessful");
	private final static String downloadFailedStr = I18n.getMessage("thaw.plugin.queueWatcher.downloadFailed");
	private final static String insertionSuccessfulStr = I18n.getMessage("thaw.plugin.queueWatcher.insertionSuccessful");
	private final static String insertionFailedStr = I18n.getMessage("thaw.plugin.queueWatcher.insertionFailed");
	private final static String unspecifiedStr = I18n.getMessage("thaw.common.unspecified");

	private final Vector columnNames;

    private Vector queries = null;

	private boolean isForInsertions = false;


	private boolean isSortedAsc = false;
	private int sortedColumn = -1;

	private FCPQueueManager queueManager;
	private PluginManager pluginManager;

	public QueueTableModel(boolean isForInsertions,
			       PluginManager pluginManager,
			       final FCPQueueManager queueManager) {
		super();

		this.pluginManager = pluginManager;
		this.queueManager = queueManager;
		this.isForInsertions = isForInsertions;

		columnNames = new Vector();

		columnNames.add(" ");
		columnNames.add(I18n.getMessage("thaw.common.file"));
		columnNames.add(I18n.getMessage("thaw.common.size"));

		if(!isForInsertions)
			columnNames.add(I18n.getMessage("thaw.common.localPath"));

		columnNames.add(I18n.getMessage("thaw.common.status"));
		columnNames.add(I18n.getMessage("thaw.common.progress"));
		columnNames.add(I18n.getMessage("thaw.common.speed"));
		columnNames.add(I18n.getMessage("thaw.common.eta"));

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


	public Object getValueAt(final int row, int column) {
		if(row >= queries.size())
			return null;

		final FCPTransferQuery query = (FCPTransferQuery)queries.get(row);

		if (column == 0) {

			if(query == null)
				return null;

			if(!query.isRunning() && !query.isFinished())
				return " ";

			if(query.isFinished() && query.isSuccessful() &&
			   ( (!(query instanceof FCPClientGet)) || ((FCPClientGet)query).isWritingSuccessful()))
				return IconBox.minGreen;

			if(query.isFinished() && (!query.isSuccessful() ||
						  ((query instanceof FCPClientGet) && !((FCPClientGet)query).isWritingSuccessful()) ) )
				return IconBox.minRed;

			if(query.isRunning() && !query.isFinished())
				return IconBox.minOrange;

			return " ";

		} else if(column == 1) {

			String filename = query.getFilename();

			if (filename == null)
				return "(null)";

			return filename;

		} else if(column == 2) {

			return thaw.gui.GUIHelper.getPrintableSize(query.getFileSize());

		} else if(!isForInsertions && (column == 3)) {
			if(query.getPath() != null)
				return query.getPath();
			else
				return unspecifiedStr;

		} else if( (isForInsertions && (column == 3))
		    || (!isForInsertions && (column == 4)) ) {

			return query.getStatus();
			
		} else if( ((isForInsertions && (column == 4))
				|| (!isForInsertions && (column == 5)) ) ) {

			return query;

		} else if( ((isForInsertions && (column == 5))
			     || (!isForInsertions && (column == 6)) ) ) {

			if (query.isFinished())
				return ""; 
			
			long averageSpeed = query.getAverageSpeed();
		
			if (averageSpeed <= 0)
				return ""; 

			return GUIHelper.getPrintableSize(averageSpeed) + "/s";

		} else if( ((isForInsertions && (column == 6))
			     || (!isForInsertions && (column == 7)) ) ) {

			if (!query.isProgressionReliable())
				return "";

			long remaining = query.getETA();
			
			if (remaining <= 0)
				return "";
			
			if (!query.isFinished())
				return GUIHelper.getPrintableTime(remaining);
			else
				return totalTimeStr + " "+GUIHelper.getPrintableTime(remaining);
		}

		return null;
	}

	public boolean isCellEditable(final int row, final int column) {
		return false;
	}

	/**
	 * Don't call notifyObservers !
	 */
	public void resetTable() {

		if(queries != null) {
			synchronized(queries) {
				for(final Iterator it = queries.iterator();
				    it.hasNext();) {
					final Observable query = (Observable)it.next();
					query.deleteObserver(this);
				}
			}
		}

		queries = new Vector();

	}

	public void reloadQueue() {
		resetTable();

		addQueries(queueManager.getRunningQueue());

		final Vector[] pendings = queueManager.getPendingQueues();

		for(int i = 0;i < pendings.length ; i++)
			addQueries(pendings[i]);
	}

	public void addQueries(final Vector queries) {
		synchronized(queries) {

			for(final Iterator it = queries.iterator();
			    it.hasNext();) {

				final FCPTransferQuery query = (FCPTransferQuery)it.next();

				if((query.getQueryType() == 1) && !isForInsertions)
					addQuery(query);

				if((query.getQueryType() == 2) && isForInsertions)
					addQuery(query);
			}
		}
	}

	public void addQuery(final FCPTransferQuery query) {
		if(queries.contains(query)) {
			Logger.debug(this, "addQuery() : Already known");
			return;
		}

		((Observable)query).addObserver(this);

		synchronized(queries) {
			queries.add(query);
		}

		sortTable();

		final int changedRow = queries.indexOf(query);

		this.notifyObservers(new TableModelEvent(this, changedRow, changedRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
	}

	public void removeQuery(final FCPTransferQuery query) {
		((Observable)query).deleteObserver(this);

		sortTable();

		final int changedRow = queries.indexOf(query);

		synchronized(queries) {
			queries.remove(query);
		}

		if(changedRow >= 0) {
			this.notifyObservers(new TableModelEvent(this, changedRow, changedRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		}else
			this.notifyObservers();
	}


	public FCPTransferQuery getQuery(final int row) {
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
	public Vector getQueries() {
		final Vector newVect = new Vector();

		synchronized(queries) {
			for(final Iterator queryIt = queries.iterator() ;
			    queryIt.hasNext();) {
				newVect.add(queryIt.next());
			}
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

	public void update(final Observable o, final Object arg) {
		if (o == queueManager && arg == null) {
			/* unclear change */
			reloadQueue();
			return;
		}


		if (o == queueManager) {
			final FCPTransferQuery query = (FCPTransferQuery)arg;

			if((query.getQueryType() == 1) && isForInsertions)
				return;

			if((query.getQueryType() == 2) && !isForInsertions)
				return;

			if(queueManager.isInTheQueues(query)) { // then it's an adding
				addQuery(query);
				return;
			}

			if(queries.contains(query)) { // then it's a removing
				removeQuery(query);
				return;
			}
		}


		if (o instanceof FCPTransferQuery
		    && queries.indexOf(o) >= 0
		    && ((FCPTransferQuery)o).isFinished()
		    && !(arg instanceof Long /* update of the total time/ETA */)) {

			String str = null;

			boolean success = ((FCPTransferQuery)o).isSuccessful();

			if (o instanceof FCPClientGet) {
				str = (success ?
				       downloadSuccessfulStr :
				       downloadFailedStr);
			} else if (o instanceof FCPClientPut) {
				str = (success ?
				       insertionSuccessfulStr :
				       insertionFailedStr);
			}

			if (str != null) {
				str = str.replaceAll("X", ((FCPTransferQuery)o).getFilename());
				TrayIcon.popMessage(pluginManager, "Thaw",
						    str, thaw.gui.SysTrayIcon.MSG_INFO);
			}
		}


		int oldPos = -1;
		int i = 0;

		if (queries != null && (i = queries.indexOf(o)) >= 0) {
			oldPos = i;
		}

		sortTable();

		if (queries != null && (i = queries.indexOf(o)) >= 0) {
			if (oldPos != i)
				this.notifyObservers(oldPos);
			this.notifyObservers(i);
			return;
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

		synchronized(queries) {
			Collections.sort(queries, new QueryComparator(isSortedAsc, sortedColumn, isForInsertions));
		}

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

			if (modelIndex < 0 || columnModelIndex < 1)
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
			this.column = column - 1; /* can't sort on the first column */
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
					result = -1;
				else if(q2.getFilename() == null)
					result = 1;
				else 
					result = q1.getFilename().compareTo(q2.getFilename());

			} else if(column == 1) { /* Size */

				result = (new Long(q1.getFileSize())).compareTo(new Long(q2.getFileSize()));

			} else if( ((column == 2) && !isForInsertionTable) ) { /* localPath */

				if(q1.getPath() == null)
					result = -1;
				else if(q2.getPath() == null)
					result = 1;
				else
					result = q1.getPath().compareTo(q2.getPath());

			} else if( ((column == 2) && isForInsertionTable)
						|| ((column == 3) && !isForInsertionTable) ) { /* status */

				if(q1.getStatus() == null)
					result = -1;
				else if(q2.getStatus() == null)
					result = 1;
				else
					result = q1.getStatus().compareTo(q2.getStatus());

			} else if( ((column == 3) && isForInsertionTable)
						|| ((column == 4) && !isForInsertionTable) ) { /* progress */
				boolean b = false;
				
				if((q1.getProgression() <= 0)
				   && (q2.getProgression() <= 0)) {
					
					if(q1.isRunning() && !q2.isRunning()) {
						result = 1;
						b = true;
					} else if(q2.isRunning() && !q1.isRunning()) {
						result = -1;
						b = true;
					}
				}

				if (!b)
					result = (new Integer(q1.getProgression())).compareTo(new Integer(q2.getProgression()));

			} else if( ((column == 4) && isForInsertionTable)
						|| ((column == 5) && !isForInsertionTable) ) { /* progress */

				result = (new Long(q1.getAverageSpeed())).compareTo(new Long(q2.getAverageSpeed()));

			} else if( ((column == 5) && isForInsertionTable)
						|| ((column == 6) && !isForInsertionTable) ) { /* progress */
				
				if (q1.isFinished() && !q2.isFinished())
					result = 1;
				else if (!q1.isFinished() && q2.isFinished())
					result = -1;
				else if (q1.getETA() > 0 && q2.getETA() <= 0)
					result = 1;
				else if (q1.getETA() <= 0 && q2.getETA() > 0)
					result = -1;				
				else {
					result = (new Long(q1.getETA())).compareTo(new Long(q2.getETA()));
					result = -result;
				}

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


