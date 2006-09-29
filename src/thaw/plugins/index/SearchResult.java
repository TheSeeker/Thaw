package thaw.plugins.index;

import java.util.Vector;


public class SearchResult implements FileList {

	private Vector fileList = null;

	public SearchResult() {

	}

	public void loadFiles(String columnToSort, boolean asc) {
		fileList = new Vector();
	}

	public Vector getFileList() {
		return fileList;
	}

	public thaw.plugins.index.File getFile(int index) {
		return null;
	}

	public void unloadFiles() {
		fileList = null;
	}	

}
