package thaw.plugins.index;

import java.util.Vector;
import java.util.Iterator;

import java.util.Observable;
import java.util.Observer;

import java.sql.*;

import thaw.plugins.Hsqldb;
import thaw.fcp.FCPQueueManager;

import thaw.core.Logger;

public class SearchResult extends Observable implements Observer, FileAndLinkList {

	private Vector fileList = null;
	private Vector linkList = null;

	private String[] search      = null;
	private Vector indexIds = null;

	private Hsqldb db;
	private FCPQueueManager queueManager;

	public SearchResult(Hsqldb hsqldb, String search, IndexTreeNode node, FCPQueueManager queueManager) {
		this.queueManager = queueManager;
		this.search = search.split(" ");
		if (node == null)
			this.indexIds = null;
		else
			this.indexIds = node.getIndexIds();
		this.db = hsqldb;
	}

	protected PreparedStatement makeSearchQuery(String fields, String searchField, String table, Vector indexIds, String[] searchPatterns,
					 String columnToSort, boolean asc) throws SQLException {
		String query = "";
		PreparedStatement st;

		query = "SELECT "+fields+" FROM "+table +" WHERE ";

		if (indexIds != null) {
			query = query +"(FALSE";

			for (Iterator it = indexIds.iterator();
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

		Connection c = this.db.getConnection();
		st = c.prepareStatement(query);

		int i;

		i = 1;

		if (indexIds != null) {
			for (Iterator it = indexIds.iterator();
			     it.hasNext(); i++) {
				st.setInt(i, ((Integer)it.next()).intValue());
			}
		}

		for (int j = 0 ; j < searchPatterns.length; j++) {
			st.setString(i+j, "%"+(searchPatterns[j]).toLowerCase()+"%");
		}

		return st;
	}

	public void loadFiles(String columnToSort, boolean asc) {
		if (this.fileList != null) {
			Logger.notice(this, "Files already loaded, won't reload them");
			return;
		}

		this.fileList = new Vector();

		try {
			PreparedStatement st = this.makeSearchQuery("id, filename, publicKey, localPath, mime, size, category, indexParent", "filename",
							       "files", this.indexIds, this.search, columnToSort, asc);
			if (st.execute()) {
				ResultSet results = st.getResultSet();

				while(results.next()) {
					thaw.plugins.index.File file = new thaw.plugins.index.File(this.db, results, null);
					file.setTransfer(this.queueManager);
					file.addObserver(this);
					this.fileList.add(file);
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Exception while searching: "+e.toString());
		}

		this.setChanged();
		this.notifyObservers();
	}

	public void loadLinks(String columnToSort, boolean asc) {
		if (this.linkList != null) {
			Logger.notice(this, "Links already loaded, won't reload them");
			return;
		}
		this.linkList = new Vector();

		try {
			PreparedStatement st = this.makeSearchQuery("id, publicKey, mark, comment, indexTarget, indexParent", "publicKey",
							       "links", this.indexIds, this.search, columnToSort, asc);
			if (st.execute()) {
				ResultSet results = st.getResultSet();

				while(results.next()) {
					Link link = new Link(this.db, results, null);
					this.linkList.add(link);
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Exception while searching: "+e.toString());
		}

		this.setChanged();
		this.notifyObservers();
	}


	public void update(Observable o, Object param) {
		this.setChanged();
		this.notifyObservers(o);
	}


	public Vector getFileList() {
		return this.fileList;
	}

	public Vector getLinkList() {
		return this.linkList;
	}



	public thaw.plugins.index.File getFile(int index) {
		return (thaw.plugins.index.File)this.fileList.get(index);
	}

	public Link getLink(int index) {
		return (Link)this.linkList.get(index);
	}



	public void unloadFiles() {
		for (Iterator it = this.fileList.iterator();
		     it.hasNext();) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.deleteObserver(this);
		}

		this.fileList = null;
	}

	public void unloadLinks() {
		this.fileList = null;
	}

}
