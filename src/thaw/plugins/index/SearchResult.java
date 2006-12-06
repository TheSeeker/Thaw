package thaw.plugins.index;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;

public class SearchResult extends Observable implements Observer, FileAndLinkList {

	private Vector fileList = null;
	private Vector linkList = null;

	private String[] search      = null;
	private Vector indexIds = null;

	private Hsqldb db;
	private FCPQueueManager queueManager;

	public SearchResult(final Hsqldb hsqldb, final String search, final IndexTreeNode node, final FCPQueueManager queueManager) {
		this.queueManager = queueManager;
		this.search = search.split(" ");
		if (node == null)
			indexIds = null;
		else
			indexIds = node.getIndexIds();
		db = hsqldb;
	}

	protected PreparedStatement makeSearchQuery(final String fields, final String searchField, final String table, final Vector indexIds, final String[] searchPatterns,
					 final String columnToSort, boolean asc) throws SQLException {
		String query = "";
		PreparedStatement st;

		query = "SELECT "+fields+" FROM "+table +" WHERE ";

		if (indexIds != null) {
			query = query +"(FALSE";

			for (final Iterator it = indexIds.iterator();
			     it.hasNext();) {
				it.next();
				query = query + " OR indexParent = ?";
			}

			query = query + ") AND ";
		}

		query = query + "(TRUE";

		for (int i = 0 ; i < searchPatterns.length; i++) {
			query = query + " AND LOWER("+searchField+") LIKE ?";
		}

		query = query +")";

		if(columnToSort != null) {
			query = query + "ORDER BY " + columnToSort;

			if(!asc)
				query = query + " DESC";
		}

		final Connection c = db.getConnection();
		st = c.prepareStatement(query);

		int i;

		i = 1;

		if (indexIds != null) {
			for (final Iterator it = indexIds.iterator();
			     it.hasNext(); i++) {
				st.setInt(i, ((Integer)it.next()).intValue());
			}
		}

		for (int j = 0 ; j < searchPatterns.length; j++) {
			st.setString(i+j, "%"+(searchPatterns[j]).toLowerCase()+"%");
		}

		return st;
	}

	public void loadFiles(final String columnToSort, final boolean asc) {
		if (fileList != null) {
			Logger.notice(this, "Files already loaded, won't reload them");
			return;
		}

		fileList = new Vector();

		try {
			final PreparedStatement st = makeSearchQuery("id, filename, publicKey, localPath, mime, size, category, indexParent", "filename",
							       "files", indexIds, search, columnToSort, asc);
			if (st.execute()) {
				final ResultSet results = st.getResultSet();

				while(results.next()) {
					final thaw.plugins.index.File file = new thaw.plugins.index.File(db, results, null);
					file.setTransfer(queueManager);
					file.addObserver(this);
					fileList.add(file);
				}
			}
		} catch(final SQLException e) {
			Logger.error(this, "Exception while searching: "+e.toString());
		}

		setChanged();
		this.notifyObservers();
	}

	public void loadLinks(final String columnToSort, final boolean asc) {
		if (linkList != null) {
			Logger.notice(this, "Links already loaded, won't reload them");
			return;
		}
		linkList = new Vector();

		try {
			final PreparedStatement st = makeSearchQuery("id, publicKey, mark, comment, indexTarget, indexParent", "publicKey",
							       "links", indexIds, search, columnToSort, asc);
			if (st.execute()) {
				final ResultSet results = st.getResultSet();

				while(results.next()) {
					final Link link = new Link(db, results, null);
					linkList.add(link);
				}
			}
		} catch(final SQLException e) {
			Logger.error(this, "Exception while searching: "+e.toString());
		}

		setChanged();
		this.notifyObservers();
	}


	public void update(final Observable o, final Object param) {
		setChanged();
		this.notifyObservers(o);
	}


	public Vector getFileList() {
		return fileList;
	}

	public Vector getLinkList() {
		return linkList;
	}



	public thaw.plugins.index.File getFile(final int index) {
		return (thaw.plugins.index.File)fileList.get(index);
	}

	public Link getLink(final int index) {
		return (Link)linkList.get(index);
	}



	public void unloadFiles() {
		for (final Iterator it = fileList.iterator();
		     it.hasNext();) {
			final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.deleteObserver(this);
		}

		fileList = null;
	}

	public void unloadLinks() {
		fileList = null;
	}

}
