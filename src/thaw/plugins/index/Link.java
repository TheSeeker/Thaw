package thaw.plugins.index;

import java.sql.*;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import thaw.core.Logger;
import thaw.plugins.Hsqldb;

public class Link extends java.util.Observable {
	private int id;

	private String indexName = null;
	private String key = null;

	private Index parent = null;

	private Hsqldb db;


	public Link(Hsqldb hsqldb, String indexName, String key, Index parent) {
		this.indexName = indexName;
		if (key != null)
			this.key = key.trim();
		else
			this.key = null;
		this.parent = parent;
		this.db = hsqldb;
	}

	public Link(Hsqldb hsqldb, String key, Index parent) {
		this(hsqldb, Index.getNameFromKey(key), key, parent);
	}

	public Link(Hsqldb hsqldb, ResultSet resultSet, Index parent) throws SQLException {
		this.db = hsqldb;
		this.id = resultSet.getInt("id");
		this.key = resultSet.getString("publicKey").trim();

		this.indexName = Index.getNameFromKey(this.key);

		this.parent = parent;
	}

	public Link(Hsqldb hsqldb, Element linkElement, Index parent) {
		this.db = hsqldb;
		this.key = linkElement.getAttribute("key");

		if (this.key != null)
			this.key = this.key.trim();

		this.indexName = Index.getNameFromKey(this.key);

		this.parent = parent;
	}

	public String getKey() {
		return this.key;
	}

	public void setParent(Index index) {
		this.parent = index;
	}

	public Index getParent() {
		return this.parent;
	}

	public String getIndexName() {
		return this.indexName;
	}

	public void setIndexKey(String key) {
		this.key = key;

		if (this.key != null)
			this.key = this.key.trim();

		this.setChanged();
		this.notifyObservers();
	}

	public String getIndexKey() {
		return this.key;
	}

	public void insert() {
		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("SELECT id FROM links ORDER BY id DESC LIMIT 1");

			try {
				if(st.execute()) {
					ResultSet result = st.getResultSet();
					result.next();
					this.id = result.getInt("id")+1;
				} else
					this.id = 1;
			} catch(SQLException e) {
				this.id = 1;
			}

			st = this.db.getConnection().prepareStatement("INSERT INTO links (id, publicKey, "+
								 "mark, comment, indexParent, indexTarget) "+
								 "VALUES (?, ?, ?, ?, ?, ?)");
			st.setInt(1, this.id);

			if(this.key != null)
				st.setString(2, this.key);
			else
				st.setString(2, this.indexName);

			st.setInt(3, 0);
			st.setString(4, "No comment");
			st.setInt(5, this.parent.getId());
			st.setNull(6, Types.INTEGER);

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to insert link to '"+this.indexName+"' because: "+e.toString());
		}

	}


	public boolean isInTheDatabase() {
		if (this.parent == null) {
			Logger.notice(this, "isInTheDatabase(): No parent !");
			return false;
		}

		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("SELECT publicKey from links WHERE publicKey = ? AND indexParent = ?");

			st.setString(1, this.key);

			st.setInt(2, this.getParent().getId());

			if(st.execute()) {
				ResultSet result = st.getResultSet();
				if (result != null && result.next()) {
					return true;
				}
			}

		} catch(SQLException e) {
			Logger.error(this, "Unable to check if link '"+this.key+"' exists because: "+e.toString());
		}

		return false;
	}


	public void delete() {
		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("DELETE FROM links WHERE id = ?");
			st.setInt(1, this.id);

			st.execute();

		} catch(SQLException e) {
			Logger.error(this, "Unable to remove link to '"+this.indexName+"' because: "+e.toString());
		}
	}

	public void update() {
		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("UPDATE links SET publicKey = ?, indexParent = ? WHERE id = ?");

			if(this.key != null)
				st.setString(1, this.key);
			else
				st.setString(1, this.indexName);

			st.setInt(2, this.getParent().getId());

			st.setInt(3, this.id);

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to update link to '"+this.indexName+"' because: "+e.toString());
		}
	}


	public Element getXML(Document xmlDoc) {
		Element link = xmlDoc.createElement("index");

		link.setAttribute("key", this.key);

		return link;
	}
}
