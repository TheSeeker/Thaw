package thaw.plugins.miniFrost.interfaces;

import thaw.plugins.MiniFrost;

import java.util.Vector;

import thaw.plugins.Hsqldb;


public interface BoardFactory {

	/**
	 * Init
	 */
	public boolean init(Hsqldb db, thaw.core.Core core,
			    MiniFrost miniFrost);


	public boolean cleanUp(int archiveAfter, int deleteAfter);

	/**
	 * @return all the boards managed by this factory
	 */
	public Vector getBoards();


	/**
	 * similar to Board.getMessages()
	 * @param orderBy see Board
	 */
	public Vector getAllMessages(String[] keywords, int orderBy,
				     boolean desc, boolean archived,
				     boolean read,
				     boolean unsigned, int minTrustLevel);

	public Vector getSentMessages();

	/**
	 * @return a list of BoardAttachment
	 */
	public Vector getAllKnownBoards();

	/**
	 * display the dialog asking for a name, etc.
	 * the tree will be reloaded after that
	 */
	public void createBoard(thaw.core.MainWindow mainWindow /*BoardFolder parent*/);


	/**
	 * For example 'frost boards' ; Use I18n ...
	 * @return null if the user can't create any board with this factory
	 */
	public String toString();
}
