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

	public boolean addAttachment(java.io.File file);
	public boolean addAttachment(Board board);
	public boolean removeAttachment(java.io.File file);
	public boolean removeAttachment(Board board);

	public Vector getAttachments();

	/**
	 * must notify thaw.plugins.MiniFrostPanel at each change
	 */
	public void post(thaw.fcp.FCPQueueManager queueManager);

	public boolean isWaiting();
	public boolean isPosting();

	public Board getBoard();
}
