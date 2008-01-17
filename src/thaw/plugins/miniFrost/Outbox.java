package thaw.plugins.miniFrost;

import java.util.Vector;
import java.util.Iterator;

import thaw.plugins.MiniFrost;
import thaw.plugins.miniFrost.interfaces.Author;
import thaw.plugins.miniFrost.interfaces.Board;
import thaw.plugins.miniFrost.interfaces.Draft;
import thaw.plugins.miniFrost.interfaces.Message;
import thaw.plugins.signatures.Identity;
import thaw.core.I18n;


public class Outbox implements Board {
	
	private MiniFrost miniFrost;

	public Outbox(MiniFrost miniFrost) {
		this.miniFrost = miniFrost;
	}
	
	public boolean destroy() {
		/* just can't */
		return false;
	}

	public Draft getDraft(Message inReplyTo) {
		/* just can't */
		return null;
	}
	
	private static class DraftAuthorShell implements thaw.plugins.miniFrost.interfaces.Author {
		private Draft draft;

		public DraftAuthorShell(Draft draft) {
			this.draft = draft;
		}
		
		public Identity getIdentity() {
			return draft.getAuthorIdentity();
		}
		
		public String toString() {
			return draft.getAuthorNick();
		}
		
		public String toString(boolean noDup) {
			return toString();
		}
	}
	
	private static class DraftSubMessageShell implements thaw.plugins.miniFrost.interfaces.SubMessage {
		private Draft draft;
		
		public DraftSubMessageShell(Draft draft) {
			this.draft = draft;
		}
		
		public Author getAuthor() {
			return new DraftAuthorShell(draft);
		}

		public java.util.Date getDate() {
			return draft.getDate();
		}

		public String getMessage() {
			return draft.getText();
		}
	}
	
	private static class DraftShell implements thaw.plugins.miniFrost.interfaces.Message {
		private Draft draft;

		public DraftShell(Draft draft) {
			this.draft = draft;
		}

		public String getMsgId() {
			return "kwain.net";
		}

		public String getInReplyToId() {
			return null;
		}

		public String getSubject() {
			return draft.getSubject();
		}

		public Author getSender() {
			return new DraftAuthorShell(draft);
		}
		
		public java.util.Date getDate() {
			return draft.getDate();
		}

		/**
		 * @return < 0 if must not be displayed
		 */
		public int getRev() {
			return -1;
		}

		public boolean isArchived() {
			return false;
		}
		
		public boolean isRead() {
			return true;
		}

		public Identity encryptedFor() {
			return null;
		}

		public void setRead(boolean read) {
			/* ni ! */
		}
	
		public void setArchived(boolean archived) {
			/* ni ! */
		}

		public Board getBoard() {
			return draft.getBoard();
		}

		/**
		 * SubMessage vector. (Don't store / cache !)
		 */
		public Vector getSubMessages() {
			Vector v = new Vector();
			v.add(new DraftSubMessageShell(draft));
			return v;
		}

		/**
		 * @return null if none
		 */
		public Vector getAttachments() {
			return draft.getAttachments();
		}

		public boolean equals(Object o) {
			return (o == this);
		}
		
		public int compareTo(Object o) { 
			if (getDate() == null && ((Message)o).getDate() != null)
				return -1;

			if (getDate() != null && ((Message)o).getDate() == null)
				return 1;

			if (getDate() == null && ((Message)o).getDate() == null)
				return 0;

			int c = getDate().compareTo( ((Message)o).getDate());

			return -1 * c;
		}
	}

	public Vector getMessages(String[] keywords, int orderBy, boolean desc,
			boolean archived, boolean read, boolean unsigned, int minTrustLevel) {
		
		Vector drafts = miniFrost.getPanel().getDrafts();
		Vector msgs = new Vector();
		
		if (drafts == null)
			return msgs;
		
		for (Iterator it = drafts.iterator();
			it.hasNext();) {
			Draft draft = (Draft)it.next();
			msgs.add(new DraftShell(draft));
		}
		
		return msgs;
	}
	
	private final static String outboxStr = I18n.getMessage("thaw.plugin.miniFrost.outbox");

	public String getName() {
		return outboxStr;
	}

	public int getNewMessageNumber(boolean unsigned, boolean archived, int minTrustLevel) {
		Vector drafts = miniFrost.getPanel().getDrafts();
	
		return ((drafts == null) ? 0 : drafts.size());
	}

	public Message getNextUnreadMessage(boolean unsigned, boolean archived,
			int minTrustLevel) {
		/* always */
		return null;
	}

	public boolean isRefreshing() {
		/* never refreshing */
		return false;
	}

	public void refresh() {
		/* can't */
	}

	public int compareTo(Object arg0) {
		return -1;
	}

	public boolean equals(Object o) {
		return false;
	}
	
	public String toString() {
		return getName();
	}
}
