package thaw.plugins.index;

import java.util.Vector;

/**
 * List files
 */
public interface FileList {

	public Vector getFileList(String columnToSort, boolean asc);
}
