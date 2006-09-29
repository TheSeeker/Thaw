package thaw.plugins.index;

import java.util.Vector;

public interface LinkList {

	public void loadLinks(String columnToSort, boolean asc);

	public Vector getLinkList();

	public Link getLink(int index);

	/**
	 * Can update the database.
	 */
	public void unloadLinks();
}
