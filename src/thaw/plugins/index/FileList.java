package thaw.plugins.index;

import java.util.Vector;

/**
 * List files
 */
public interface FileList {

	public File[] getFileList(String columnToSort, boolean asc);
}
