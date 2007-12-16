package thaw.plugins.miniFrost.interfaces;

import java.util.Vector;

import thaw.plugins.signatures.Identity;


public interface Draft {

	public String getSubject();

	/**
	 * Returned result may contains $sender$, $dateAndTime$.
	 * They will be replaced.
	 * @return a default value if setText() was never called
	 */
	public String getText();

	/**
	 * @return can return null if unknown
	 */
	public String getAuthorNick();

	/**
	 * @return can return null if unknown
	 */
	public Identity getAuthorIdentity();
	
	/**
	 * @param id null for anybody
	 */
	public void setRecipient(Identity id);
	public Identity getRecipient();

	public boolean allowUnsignedPost();

	public void setSubject(String txt);
	public void setText(String txt);

	/**
	 * @param identity if null, unsigned post
	 */
	public void setAuthor(String nick, Identity identity);

	/**
	 * @param date the date provided is already GMT-ized
	 */
	public void setDate(java.util.Date date);
	public java.util.Date getDate();

	/**
	 * @param i specify the position of the id line in the text
	 */
	public void setIdLinePos(int i);


	/**
	 * @param i specify the length of the id line in the text
	 */
	public void setIdLineLen(int i);


	public boolean addAttachment(java.io.File file);
	public boolean addAttachment(Board board);
	public boolean addAttachment(thaw.plugins.index.Index index);

	public boolean removeAttachment(Attachment attachment);


	/**
	 * @return can return null if none
	 */
	public Vector getAttachments();

	/**
	 * must notify thaw.plugins.MiniFrostPanel at each change
	 */
	public void post(thaw.fcp.FCPQueueManager queueManager);

	public boolean isWaiting();
	public boolean isPosting();

	public Board getBoard();
}
