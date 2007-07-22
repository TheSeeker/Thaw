package thaw.plugins.miniFrost.interfaces;

import javax.swing.tree.TreeNode;
import java.util.Vector;

import thaw.plugins.signatures.Identity;


public interface Message /* extends TreeNode */ {

	//public int getParentId();

	public String getSubject();
	public Author getSender();
	public java.util.Date getDate();

	public boolean isArchived();
	public boolean isRead();

	public void setRead(boolean read);
	public void setArchived(boolean archived);

	public Board getBoard();

	/**
	 * SubMessage vector. (Don't store / cache !)
	 */
	public Vector getSubMessages();
}
