package thaw.plugins.miniFrost.interfaces;

import javax.swing.tree.TreeNode;
import java.util.Vector;

import thaw.plugins.signatures.Identity;


public interface Message extends Comparable {

	public String getMsgId();
	public String getInReplyToId();

	//public int getParentId();

	public String getSubject();
	public Author getSender();
	public java.util.Date getDate();

	public int getRev();

	public boolean isArchived();
	public boolean isRead();

	public Identity encryptedFor();

	public void setRead(boolean read);
	public void setArchived(boolean archived);

	public Board getBoard();

	/**
	 * SubMessage vector. (Don't store / cache !)
	 */
	public Vector getSubMessages();

	/**
	 * @return null if none
	 */
	public Vector getAttachments();

	public boolean equals(Object o);
}
