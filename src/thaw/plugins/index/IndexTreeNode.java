package thaw.plugins.index;

import java.util.Observer;

import javax.swing.tree.MutableTreeNode;
import org.w3c.dom.Element;
import org.w3c.dom.Document;


import thaw.fcp.FCPQueueManager;

public interface IndexTreeNode {

	public MutableTreeNode getTreeNode();

	public void setParent(int id);

	/**
	 * Allow to know if it's a dumb node or a real one in the tree
	 */
	public boolean isInTree();

	/**
	 * get Id of this node in the database.
	 */
	public int getId();


	/**
	 * Change the name of the node.
	 */
	public void rename(String name);

	/**
	 * Remove the node from the database. (recursive)
	 */
	public void delete();

	/**
	 * Get key(s)
	 */
	public String getPublicKey();
	public String getPrivateKey();

	public boolean isModifiable();
	public boolean hasChanged();
	public boolean setHasChangedFlag(boolean flag);

	/**
	 * for internal use only !
	 */
	public boolean setHasChangedFlagInMem(boolean flag);

	/**
	 * Will export private keys too !
	 */
	public Element do_export(Document xmlDoc, boolean withContent);
	public void do_import(IndexBrowserPanel indexBrowser, Element e);


	/**
	 * @return the number of transfer started
	 */
	public int insertOnFreenet(Observer o, IndexBrowserPanel indexBrowser, FCPQueueManager queueManager);
	public int downloadFromFreenet(Observer o, IndexTree indexTree, FCPQueueManager queueManager);
	public int downloadFromFreenet(Observer o, IndexTree indexTree, FCPQueueManager queueManager, int rev);


	public void forceHasChangedReload();
}
