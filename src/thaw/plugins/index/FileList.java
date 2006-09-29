package thaw.plugins.index;

import java.util.Vector;

/**
 * List files
 */
public interface FileList {

	public void loadFiles(String fileColumnToSort, boolean asc);

	/**
	 * Must returns a copy of the vector it's using !
	 */
	public Vector getFileList();


	public thaw.plugins.index.File getFile(int index);

	/**
	 * Can update the database.
	 */
	public void unloadFiles();
}
