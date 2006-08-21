package thaw.plugins.index;

import java.util.Vector;

/**
 * List files, but also links.
 */
public interface FileList {

	public void loadLists(String fileColumnToSort, boolean asc);

	/**
	 * Must returns a copy of the vector it's using !
	 */
	public Vector getFileList();


	public thaw.plugins.index.File getFile(int index);

	/**
	 * Must returns a copy of the vector it's using !
	 */
	public Vector getLinkList();

	/**
	 * Can update the database.
	 */
	public void unloadLists();
}
