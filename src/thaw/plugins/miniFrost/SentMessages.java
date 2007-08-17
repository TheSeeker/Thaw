package thaw.plugins.miniFrost;

import java.util.Vector;
import java.util.Collections;

import thaw.core.I18n;
import thaw.plugins.MiniFrost;
import thaw.core.Logger;

import thaw.plugins.miniFrost.interfaces.BoardFactory;
import thaw.plugins.miniFrost.interfaces.Draft;
import thaw.plugins.miniFrost.interfaces.Message;


public class SentMessages
	implements thaw.plugins.miniFrost.interfaces.Board {

	private MiniFrost miniFrost;

	public SentMessages(MiniFrost miniFrost) {
		this.miniFrost = miniFrost;
	}

	public String getName() {
		return I18n.getMessage("thaw.plugin.miniFrost.sentBox");
	}

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
				  int minTrustLevel) {
		BoardFactory[] factories = miniFrost.getFactories();

		Vector v = new Vector();

		for (int i = 0 ; i < factories.length ; i++) {
			v.addAll(factories[i].getSentMessages());
		}

		Collections.sort(v);

		return v;

	}

	public Draft getDraft(Message inReplyTo) {
		return null;
	}

	/**
	 * @return null if none
	 */
	public Message getNextUnreadMessage(boolean unsigned, boolean archived, int minTrustLevel) {
		return null;
	}


	/**
	 * must refresh() the board list each time a new message is found
	 * and when the refresh is finished.
	 * MUST NOT BE BLOCKING.
	 */
	public void refresh() {
		return;
	}

	public boolean isRefreshing() {
		return false;
	}

	public int getNewMessageNumber(boolean unsigned, boolean archived, int minTrustLevel) {
		return 0;
	}

	public boolean destroy() {
		Logger.warning(this, "Can't destroy this 'board'");
		return false;
	}

	/**
	 * Always return the board name,
	 * without anything more
	 */
	public String toString() {
		return getName();
	}


	public int compareTo(Object o) {
		//return toString().compareToIgnoreCase(o.toString());
		return -1; /* always */
	}

	public boolean equals(Object o) {
		return false;
	}
}
