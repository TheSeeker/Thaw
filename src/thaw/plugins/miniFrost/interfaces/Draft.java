package thaw.plugins.miniFrost.interfaces;

import thaw.plugins.signatures.Identity;


public interface Draft {

	public String getSubject();

	/**
	 * Returned result may contains $sender$, $dateAndTime$.
	 * They will be replaced.
	 * @return a default value if setText() was never called
	 */
	public String getText();

	public boolean allowUnsignedPost();

	public void setSubject(String txt);
	public void setText(String txt);

	/**
	 * @param identity if null, unsigned post
	 */
	public void setAuthor(String nick, Identity identity);

	/**
	 * @param date don't forget to GMT-ize it when formatting it
	 */
	public void setDate(java.util.Date date);

	public boolean addAttachment(java.io.File file);
	public boolean addAttachment(Board board);
	public boolean removeAttachment(java.io.File file);
	public boolean removeAttachment(Board board);

	/**
	 * must notify thaw.plugins.MiniFrostPanel at each change
	 */
	public void post(thaw.fcp.FCPQueueManager queueManager);

	public boolean isWaiting();
	public boolean isPosting();
}
