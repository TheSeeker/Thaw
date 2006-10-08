package thaw.plugins.index;

import java.sql.*;

import javax.swing.JTree;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;

import java.sql.*;

import thaw.fcp.*;
import thaw.plugins.Hsqldb;

import thaw.core.*;

public class IndexCategory extends DefaultMutableTreeNode implements IndexTreeNode {

	private int id;
	private IndexCategory parent;
	private String name;
	private boolean modifiables;

	private Hsqldb db;
	private FCPQueueManager queueManager;
	
	public IndexCategory(Hsqldb db, FCPQueueManager queueManager,
			     int id, IndexCategory parent,
			     String name, 
			     boolean modifiables) {
		super(name, true);

		this.id = id;
		this.name = name;
		this.parent = parent;
		this.modifiables = modifiables;

		this.db = db;
		this.queueManager = queueManager;

		setUserObject(this);

	}

	public void setParent(IndexCategory parent) {
		this.parent = parent;
	}

	public DefaultMutableTreeNode getTreeNode() {
		return this;
	}
	
	/**
	 * Insert the category into the database.
	 */
	public boolean create() {
		try {
			/* Rahh ! Hsqldb doesn't support getGeneratedKeys() ! 8/ */

			Connection c = db.getConnection();
			PreparedStatement st;

			st = c.prepareStatement("SELECT id FROM indexCategories ORDER BY id DESC LIMIT 1");
			st.execute();
			
			try {
				ResultSet key = st.getResultSet();
				key.next();
				id = key.getInt(1) + 1;
			} catch(SQLException e) {
				id = 0;
			}

			st = c.prepareStatement("INSERT INTO indexCategories (id, name, positionInTree, modifiableIndexes, parent) "+
								  "VALUES (?, ?,?,?,?)");

			st.setInt(1, id);
			st.setString(2, name);
			st.setInt(3, 0);

			st.setBoolean(4, modifiables);

			if(parent.getId() >= 0)
				st.setInt(5, parent.getId());
			else
				st.setNull(5, Types.INTEGER);
			
			st.execute();

			return true;
		} catch(SQLException e) {
			Logger.error(this, "Unable to insert the new index category in the db, because: "+e.toString());

			return false;
		}
	}

	public void delete() {
		if(children == null)
			children = loadChildren();

		for(Iterator it = children.iterator();
		    it.hasNext();) {
			IndexTreeNode child = (IndexTreeNode)it.next();
			child.delete();
		}

		children = null;

		try {
			if(id < 0)
				return;
			
			Connection c = db.getConnection();
			PreparedStatement st = c.prepareStatement("DELETE FROM indexCategories WHERE id = ?");
			st.setInt(1, id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to delete the index category '"+name+"', because: "+e.toString());
		}
	}

	public void rename(String name) {

		try {
			Connection c = db.getConnection();
			PreparedStatement st = c.prepareStatement("UPDATE indexCategories SET name = ? WHERE id = ?");
			
			st.setString(1, name);
			st.setInt(2, id);

			st.execute();

			this.name = name;
		} catch(SQLException e) {
			Logger.error(this, "Unable to rename the index category '"+this.name+"' in '"+name+"', because: "+e.toString());
		}
	}

	public String getKey() {
		String result = "";
		
		for(Iterator it = children.iterator();
		    it.hasNext();) {
			IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			result = result + node.getKey() + "\n";
		}
		
		return result;
	}

	public void addObserver(java.util.Observer o) {
		if(children == null)
			children = loadChildren();

		for(Iterator it = children.iterator();
		    it.hasNext();) {
			IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			node.addObserver(o);
		}
	}

	public void update() {
		for (Enumeration e = children() ; e.hasMoreElements() ;) {
			((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement()).getUserObject()).update();
		}
	}

	public boolean isUpdating() {
		for (Enumeration e = children() ; e.hasMoreElements() ;) {
			if(((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement())).isUpdating())
				return true;
		}

		return false;
	}

	public void save() {
		saveThis();

		for (Enumeration e = children() ; e.hasMoreElements() ;) {
			((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement()).getUserObject()).save();
		}
	}


	public void saveThis() {
		if(getId() < 0)
			return;

		try {
			Connection c = db.getConnection();
			PreparedStatement st = c.prepareStatement("UPDATE indexCategories SET name = ?, positionInTree = ?, modifiableIndexes = ?, parent = ? WHERE id = ?");

			st.setString(1, name);
			st.setInt(2, getParent().getIndex(this));
			
			st.setBoolean(3, modifiables);

			if( ((IndexTreeNode)getParent()).getId() < 0)
				st.setNull(4, Types.INTEGER);
			else
				st.setInt(4, ((IndexTreeNode)getParent()).getId());
			
			st.setInt(5, getId());

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to save index category state '"+toString()+"' because : "+e.toString());
		}
	}

	public int getId() {
		return id;
	}

	public Vector loadChildren() {
		Vector children = new Vector();

		Logger.debug(this, "loadChildren()");

		loadChildCategories(children);
		loadChildIndexes(children);

		cleanChildList(children);
		
		Logger.debug(this, "Children: "+children.size());

		for(Iterator it = children.iterator();
		    it.hasNext(); ) {
			add((MutableTreeNode)it.next());
		}

		return children;
	}


	public void loadChildIndexes(Vector children) {

		ResultSet result;

		String query;

		query = "SELECT id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision FROM indexes";

		if(id < 0)
			query = query + " WHERE parent IS NULL";
		else
			query = query + " WHERE parent = "+Integer.toString(id);

		if(modifiables) {
			query = query + " AND privateKey IS NOT NULL";
		} else {
			query = query + " AND privateKey IS NULL";
		}

		query = query + " ORDER BY positionInTree DESC";

		try {
			result = db.executeQuery(query);

			if(result == null) {
				Logger.warning(this, "Unable to get child indexes for '"+toString()+"'");
				return;
			}
			
			while(result.next()) {
				int id = result.getInt("id");
				int position = result.getInt("positionInTree");

				String realName = result.getString("originalName");
				String displayName = result.getString("displayName");

				String publicKey = result.getString("publicKey");
				String privateKey = result.getString("privateKey");
				
				int revision = result.getInt("revision");

				String author = result.getString("author");

				set(children, position, (new Index(db, queueManager, id, this,
								   realName, displayName,
								   publicKey, privateKey, revision,
								   author,
								   modifiables)).getTreeNode());
			}
		} catch (java.sql.SQLException e) {
			Logger.warning(this, "SQLException while getting child of index category '"+name+"': "+e.toString());
		}

	}

	public void loadChildCategories(Vector children) {
		
		ResultSet result;
		
		String query;

		query = "SELECT id, name, positionInTree, modifiableIndexes FROM indexCategories";

		if(id < 0)
			query = query + " WHERE parent IS NULL";
		else
			query = query + " WHERE parent = "+Integer.toString(id);

		if(modifiables) {
			query = query + " AND modifiableIndexes = TRUE";
		} else {
			query = query + " AND modifiableIndexes = FALSE";
		}

		query = query + " ORDER BY positionInTree DESC";

		try {
			result = db.executeQuery(query);
			
			if(result == null) {
				Logger.error(this, "Unable to get child categories for '"+toString()+"'");
				return;
			}

			while(result.next()) {
				int id = result.getInt("id");
				int position = result.getInt("positionInTree");
				String name = result.getString("name");
				
				IndexCategory cat = new IndexCategory(db, queueManager, id, this, name, modifiables);
				cat.loadChildren();
				set(children, position, cat);
			}
		} catch (java.sql.SQLException e) {
			Logger.error(this, "SQLException while getting child of index category '"+name+"': "+e.toString());
		}

	}

	protected void cleanChildList(Vector children) {
		while(children.remove(null)) {
			Logger.warning(this, "cleanChildList() : null removed !");
		}
	}

	public void set(Vector children, int position, TreeNode node) {
		if(node == this || node == null)
			return;

		if(children.size() <= position) {
			children.setSize(position+1);
		} else {

			if(children.get(position) != null) {
				Logger.warning(this,
					       "Collision in tree position: Moving "+Integer.toString(position)+
					       " to "+Integer.toString(children.size()));
				children.add(node);
				return;
			}
		}

		children.set(position, node);
	}

	public String toString() {
		return name;
	}

	public boolean isLeaf() {
		return false;
	}

}
