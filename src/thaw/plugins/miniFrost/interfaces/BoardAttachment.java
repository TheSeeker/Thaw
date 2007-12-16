package thaw.plugins.miniFrost.interfaces;

import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;

public interface BoardAttachment extends Attachment, java.lang.Comparable {

	/**
	 * Add the specified board to the board list (board list will be refreshed after)
	 */
	public void addBoard(Hsqldb db, FCPQueueManager queueManager);
	
}
