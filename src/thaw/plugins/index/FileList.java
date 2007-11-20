package thaw.plugins.index;

/**
 * List files
 */
public interface FileList {

	public File[] getFileList(String columnToSort, boolean asc);
}
