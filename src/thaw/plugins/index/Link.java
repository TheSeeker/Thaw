package thaw.plugins.index;

import java.sql.*;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
		this.key = key;
		this.parent = parent;
		this.db = hsqldb;
	}

	public Link(Hsqldb hsqldb, String key, Index parent) {
		this(hsqldb, Index.getNameFromKey(key), key, parent);
	}

	public Link(Hsqldb hsqldb, ResultSet resultSet, Index parent) throws SQLException {
		this.db = hsqldb;
		this.id = resultSet.getInt("id");
		this.key = resultSet.getString("publicKey");

		this.indexName = Index.getNameFromKey(this.key);

		this.parent = parent;
	}

	public Link(Hsqldb hsqldb, Element linkElement, Index parent) {
		this.db = hsqldb;
		this.key = linkElement.getAttribute("key");

		this.indexName = Index.getNameFromKey(this.key);

		this.parent = parent;
	}

	public String getKey() {
		return key;
	}

	public void setParent(Index index) {
		this.parent = index;
	}

	public Index getParent() {
		return parent;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexKey(String key) {
		this.key = key;

		setChanged();
		notifyObservers();
	}

	public String getIndexKey() {
		return key;
	}

	public void insert() {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT id FROM links ORDER BY id DESC LIMIT 1");

			try {
				if(st.execute()) {
					ResultSet result = st.getResultSet();
					result.next();
					id = result.getInt("id")+1;
				} else
					id = 1;
			} catch(SQLException e) {
				id = 1;
			}
			

			st = db.getConnection().prepareStatement("INSERT INTO links (id, publicKey, "+
								 "mark, comment, indexParent, indexTarget) "+
								 "VALUES (?, ?, ?, ?, ?, ?)");
			st.setInt(1, id);

			if(key != null)
				st.setString(2, key);
			else
				st.setString(2, indexName);

			st.setInt(3, 0);
			st.setString(4, "No comment");
			st.setInt(5, parent.getId());
			st.setNull(6, Types.INTEGER);

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to insert link to '"+indexName+"' because: "+e.toString());
		}
		
	
	}

	public void delete() {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("DELETE FROM links WHERE id = ?");
			st.setInt(1, id);

			st.execute();

		} catch(SQLException e) {
			Logger.error(this, "Unable to remove link to '"+indexName+"' because: "+e.toString());
		}
	}

	public void update() {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("UPDATE links SET publicKey = ?, indexParent = ? WHERE id = ?");

			if(key != null)
				st.setString(1, key);
			else
				st.setString(1, indexName);

			st.setInt(2, getParent().getId());

			st.setInt(3, id);

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to update link to '"+indexName+"' because: "+e.toString());
		}
	}


	public Element getXML(Document xmlDoc) {
		Element link = xmlDoc.createElement("index");

		link.setAttribute("key", key);

		return link;
	}
}
