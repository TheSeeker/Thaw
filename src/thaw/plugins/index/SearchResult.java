package thaw.plugins.index;

import java.util.Vector;


public class SearchResult implements FileList {
	private Vector fileList = null;

	public SearchResult() {

	}

	public void loadLists(String fileColumnToSort, boolean asc) {
		fileList = new Vector();
	}

	public Vector getFileList() {
		return fileList;
	}

	public thaw.plugins.index.File getFile(int index) {
		return null;
	}

	public Vector getLinkList() {
		return null;
	}

	public void unloadLists() {
		fileList = null;
	}	

}
