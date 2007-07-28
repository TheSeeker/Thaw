package thaw.plugins.miniFrost.interfaces;

import thaw.plugins.signatures.Identity;


public interface Draft {

	public String getInitialText();

	public boolean allowUnsignedPost();


	public void setText(String txt);

	/**
	 * @param identity if null, unsigned post
	 */
	public void setAuthor(String nick, Identity identity);


	public void post(thaw.fcp.FCPQueueManager queueManager);
}
