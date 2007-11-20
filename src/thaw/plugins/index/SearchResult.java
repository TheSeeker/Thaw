package thaw.plugins.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import thaw.core.Logger;
import thaw.plugins.Hsqldb;

public class SearchResult implements FileAndLinkList {

	private String[] search = null;

	private Hsqldb db;
	private IndexTreeNode node;

	public SearchResult(final Hsqldb hsqldb, final String search, final IndexTreeNode node) {
		this.search = search.split(" ");
		this.node = node;
		db = hsqldb;
	}


	public String getWhereClause(boolean hasFilename) {
		String where = null;
		String column = hasFilename ? "filename" : "publicKey";

		if (node instanceof IndexFolder) {
			if (node.getId() >= 0) {
				where = "indexParent IN "+
					"(SELECT indexParents.indexId FROM indexParents WHERE indexParents.folderId = ?)";
			}
		}

		if (node instanceof Index) {
			where = "indexParent = ? ";
		}

		for (int i = 0 ; i < search.length ; i++) {
			if (where == null) {
				where = " LOWER("+column+") LIKE ?";
			} else {
				where+=" AND LOWER("+column+") LIKE ?";
			}
		}

		return where;
	}

	public void fillInStatement(PreparedStatement st) throws SQLException {
		int i, j;

		i = 1;

		if ( (node instanceof Index)
		     || node.getId() >= 0 ) {
			st.setInt(i, node.getId());
			i++;
		}

		for (j = 0 ; j < search.length ; j++) {
			st.setString(i, "%" + search[j].toLowerCase() + "%");
			i++;
		}
	}

	public File[] getFileList(String col, boolean asc) {

		if (col == null)
			col = "filename";

		java.util.LinkedList v = new java.util.LinkedList();

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, filename, publicKey, localPath, mime, size, indexParent "+
									 "FROM files "+
									 "WHERE "+getWhereClause(true)+" ORDER by "+col+ (asc ? "" : " DESC"));

				fillInStatement(st);

				ResultSet set = st.executeQuery();

				while(set.next()) {
					v.add(new File(db,
						       set.getInt("id"),
						       set.getString("filename"),
						       set.getString("publicKey"),
						       (set.getString("localPath") != null ? new java.io.File(set.getString("localPath")) : null),
						       set.getString("mime"),
						       set.getLong("size"),
						       set.getInt("indexParent")));
				}

			} catch(SQLException e) {
				Logger.error(this, "Error while searching: "+e.toString());
			}
		}

		return (File[])v.toArray(new File[0]);
	}

	public Link[] getLinkList(String col, boolean asc) {
		java.util.LinkedList v = new java.util.LinkedList();

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, publicKey, indexParent, blackListed "+
									 "FROM links "+
									 "WHERE "+getWhereClause(false));
				fillInStatement(st);
				ResultSet set = st.executeQuery();

				while(set.next()) {
					if (!set.getBoolean("blackListed")) {
						v.add(new Link(db,
							       set.getInt("id"),
							       set.getString("publicKey"),
							       false,
							       set.getInt("indexParent") ));
					}
				}

			} catch(SQLException e) {
				Logger.error(this, "Error while searching: "+e.toString());
			}
		}

		return (Link[])v.toArray(new Link[0]);
	}

}
