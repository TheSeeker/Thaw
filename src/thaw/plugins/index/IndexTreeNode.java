package thaw.plugins.index;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;


public interface IndexTreeNode {
	
	public DefaultMutableTreeNode getTreeNode();

	/**
	 * get Id of this node in the database.
	 */
	public int getId();

	/**
	 * Insert the node in the database.
	 */
	public boolean create();

	/**
	 * Change the name of the node.
	 */
	public void rename(String name);
	
	/**
	 * Remove the node from the database. (recursive)
	 */
	public void delete();

	/**
	 * Update from freenet / Update the freenet version. (recursive)
	 */
	public void update();

	public boolean isUpdating();

	/**
	 * Save the state in the database (recursive).
	 */
	public void save();

	/**
	 * Get (public) key(s)
	 */
	public String getKey();

	public void addObserver(java.util.Observer o);
}
