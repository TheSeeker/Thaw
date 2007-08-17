package thaw.plugins.miniFrost.interfaces;

import thaw.plugins.miniFrost.BoardFolder;

import javax.swing.tree.MutableTreeNode;
import java.util.Vector;

public interface Board extends Comparable {

	public final static int ORDER_SUBJECT = 0;
	public final static int ORDER_SENDER  = 1;
	public final static int ORDER_DATE    = 2;

	public String getName();

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
				  boolean archived,
				  boolean read,
				  boolean unsigned,
				  int minTrustLevel);

	public Draft getDraft(Message inReplyTo);

	/**
	 * @return null if none
	 */
	public Message getNextUnreadMessage(boolean unsigned, int minTrustLevel);


	/**
	 * must refresh() the board list each time a new message is found
	 * and when the refresh is finished.
	 * MUST NOT BE BLOCKING.
	 */
	public void refresh();

	public boolean isRefreshing();

	public int getNewMessageNumber(boolean unsigned,
				       boolean archived,
				       int minTrustLevel);

	public boolean destroy();

	/**
	 * Always return the board name,
	 * without anything more
	 */
	public String toString();

	public boolean equals(Object o);
}
