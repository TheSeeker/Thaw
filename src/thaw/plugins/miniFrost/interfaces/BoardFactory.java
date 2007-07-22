package thaw.plugins.miniFrost.interfaces;

import thaw.plugins.miniFrost.BoardFolder;
import thaw.plugins.MiniFrost;

import java.util.Vector;

import thaw.plugins.Hsqldb;


public interface BoardFactory {

	/**
	 * Init
	 */
	public boolean init(Hsqldb db, thaw.core.Core core,
			    MiniFrost miniFrost);

	/**
	 * @return all the boards managed by this factory
	 */
	public Vector getBoards();


	/**
	 * display the dialog asking for a name, etc.
	 * the tree will be reloaded after that
	 */
	public void createBoard(thaw.core.MainWindow mainWindow /*BoardFolder parent*/);


	/**
	 * For example 'frost boards' ; Use I18n ...
	 */
	public String toString();
}
