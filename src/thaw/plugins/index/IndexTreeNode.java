package thaw.plugins.index;

import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import org.w3c.dom.Element;
import org.w3c.dom.Document;


public interface IndexTreeNode {

	public DefaultMutableTreeNode getTreeNode();

	public void setParent(IndexCategory parent);

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
	 * Update from freenet / Update the freenet version, depending of the index kind (recursive)
	 */
	public void update();

	/**
	 * Update from freenet using the given revision
	 * @param rev -1 means the lastest
	 */
	public void updateFromFreenet(int rev);

	public boolean isUpdating();

	/**
	 * Save the state in the database (recursive).
	 */
	public void save();

	/**
	 * Get key(s)
	 */
	public String getPublicKey();
	public String getPrivateKey();

	public Vector getIndexIds();

	/**
	 * All the indexes !
	 */
	public Vector getIndexes();

	public Index getIndex(int id);

	public void addObserver(java.util.Observer o);

	public boolean isModifiable();
	public boolean hasChanged();

	public void register();
	public void unregister();

	/**
	 * Will export private keys too !
	 */
	public Element do_export(Document xmlDoc, boolean withContent);
	public void do_import(Element e);
}
