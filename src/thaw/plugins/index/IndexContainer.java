package thaw.plugins.index;

import java.util.Vector;
import thaw.plugins.index.File;

/**
 * This is what you need to implements to use IndexParser.
 */
public interface IndexContainer extends FileAndLinkList {

	/**
	 * @return a vector of FileContainer
	 */
	public File[] getFileList();

	/**
	 * @return a vector of LinkContainer
	 */
	public Link[] getLinkList();


	/**
	 * @return true if the private key must be published
	 */
	public boolean publishPrivateKey();

	/**
	 * @return the private key of the index (Expected: "SSK@[...]/")
	 */
	public String getPrivateKey();


	/**
	 * Used to update the links. The simplest way to find the correct
	 * is to look at your index list.
	 */
	public String findTheLatestKey(String originalKey);


	/**
	 * @param toGUI if set to false, must *just* return the index name
	 */
	public String toString(boolean toGUI);

	/**
	 * @return for example "Thaw 0.8 r4242"
	 */
	public String getClientVersion();

	public boolean canHaveComments();
	public String getCommentPublicKey();
	public String getCommentPrivateKey();

	/**
	 * @return Vector of Integer
	 */
	public Vector getCommentBlacklistedRev();

	/**
	 * delete all the files / links / comment blacklisted revs / comments keys
	 */
	public void purgeIndex();

	public void setInsertionDate(java.util.Date date);

	/**
	 * return false if the parsing must be interrupted
	 * (because of db errors or anything else)
	 */
	public boolean addFile(String publicKey, long size, String mime);
	public boolean addLink(String publicKey);
	public void setCommentKeys(String publicKey, String privateKey);
	public void addBlackListedRev(int rev);

	public void setPublishPrivateKey(boolean publish);
	public void setPrivateKey(String key);


	public void setCategory(String str);
	public String getCategory();

	public void setClientVersion(String str);
}
