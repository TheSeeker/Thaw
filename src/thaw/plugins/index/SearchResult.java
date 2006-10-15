package thaw.plugins.index;

import java.util.Vector;
import java.util.Iterator;

import java.util.Observable;
import java.util.Observer;

import java.sql.*;

import thaw.plugins.Hsqldb;

import thaw.core.Logger;

public class SearchResult extends Observable implements FileAndLinkList {

	private Vector fileList = null;
	private Vector linkList = null;

	private String[] search      = null;
	private Vector indexIds = null;

	private Hsqldb db;

	public SearchResult(Hsqldb hsqldb, String search, IndexTreeNode node) {
		this.search = search.split(" ");
		this.indexIds = node.getIndexIds();
		this.db = hsqldb;
	}

	protected PreparedStatement makeSearchQuery(String fields, String table, Vector indexIds, String[] searchPatterns,
					 String columnToSort, boolean asc) throws SQLException {
		String query = "";
		PreparedStatement st;

		query = "SELECT "+fields+" FROM "+table+" WHERE (FALSE";

		for (Iterator it = indexIds.iterator();
		     it.hasNext();) {
			it.next();
			query = query + " OR indexParent = ?";
		}

		query = query + ") AND (TRUE";

		for (int i = 0 ; i < searchPatterns.length; i++) {
			query = query + " AND LOWER(publicKey) LIKE ?";
		}

		query = query +")";

		if(columnToSort != null) {
			query = query + "ORDER BY " + columnToSort;
			
			if(!asc)
				query = query + " DESC";
		}

		Connection c = db.getConnection();
		st = c.prepareStatement(query);

		int i;

		i = 1;

		for (Iterator it = indexIds.iterator();
		     it.hasNext(); i++) {
			st.setInt(i, (Integer)it.next());
		}

		for (int j = 0 ; j < searchPatterns.length; j++) {
			st.setString(i+j, "%"+(searchPatterns[j]).toLowerCase()+"%");
		}

		return st;
	}

	public void loadFiles(String columnToSort, boolean asc) {
		if (fileList != null) {
			Logger.notice(this, "Files already loaded, won't reload them");
			return;
		}

		fileList = new Vector();

		try {
			PreparedStatement st = makeSearchQuery("id, publicKey, localPath, mime, size, category, indexParent",
							       "files", indexIds, search, columnToSort, asc);
			if (st.execute()) {
				ResultSet results = st.getResultSet();

				while(results.next()) {
					thaw.plugins.index.File file = new thaw.plugins.index.File(db, results, null);
					fileList.add(file);
				}
			}
		} catch(SQLException e) {
			Logger.warning(this, "Exception while searching: "+e.toString());
		}

		setChanged();
		notifyObservers();
	}

	public void loadLinks(String columnToSort, boolean asc) {
		if (linkList != null) {
			Logger.notice(this, "Links already loaded, won't reload them");
			return;
		}
		linkList = new Vector();

		try {
			PreparedStatement st = makeSearchQuery("id, publicKey, mark, comment, indexTarget, indexParent",
							       "links", indexIds, search, columnToSort, asc);
			if (st.execute()) {
				ResultSet results = st.getResultSet();

				while(results.next()) {
					Link link = new Link(db, results, null);
					linkList.add(link);
				}
			}
		} catch(SQLException e) {
			Logger.warning(this, "Exception while searching: "+e.toString());
		}

		setChanged();
		notifyObservers();
	}


	public Vector getFileList() {
		return fileList;
	}

	public Vector getLinkList() {
		return linkList;
	}



	public thaw.plugins.index.File getFile(int index) {
		return null;
	}

	public Link getLink(int index) {
		return null;
	}



	public void unloadFiles() {
		fileList = null;
	}

	public void unloadLinks() {
		fileList = null;
	}

}
