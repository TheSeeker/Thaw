package thaw.plugins.miniFrost.interfaces;

import thaw.plugins.miniFrost.BoardFolder;

import javax.swing.tree.MutableTreeNode;
import java.util.Vector;

public interface Board {

	/**
	 * @return folder db id
	 */
	//public int getParentId();

	/**
	 * Called when the tree is being built.
	 * This function must NOT modify/update the db !
	 * It's just for the tree.
	 * <br/>
	 * setParent() must update the bdd
	 */
	//public void setParentFolder(BoardFolder b);


	/**
	 * just return what was given to setParentFolder().
	 * getParent() must return the same thing !
	 */
	//public BoardFolder getParentFolder();


	public final static int ORDER_SUBJECT = 0;
	public final static int ORDER_SENDER  = 1;
	public final static int ORDER_DATE    = 2;

	/**
	 * don't store/cache the messages,
	 * just give them.
	 * @param keywords can be null
	 * @param orderBy specify an order
	 * @param desc
	 * @param archived If true, archived messages will also be returned
	 */
	public Vector getMessages(String[] keywords,
				  int orderBy,
				  boolean desc,
				  boolean archived);


	/**
	 * @return null if none
	 */
	public Message getNextUnreadMessage();


	/**
	 * must refresh() the board list each time a new message is found
	 * and when the refresh is finished.
	 * MUST NOT BE BLOCKING.
	 */
	public void refresh();

	public boolean isRefreshing();

	public int getNewMessageNumber();

	public void destroy();

	/**
	 * Always return the board name,
	 * without anything more
	 */
	public String toString();
}
