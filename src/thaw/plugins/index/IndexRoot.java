package thaw.plugins.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import thaw.core.I18n;
import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;

public class IndexRoot extends IndexFolder implements IndexTreeNode {

	private FCPQueueManager queueManager;
	private IndexBrowserPanel indexBrowser;

	public IndexRoot(final FCPQueueManager queueManager,
			 final IndexBrowserPanel indexBrowser,
			 final String name,
			 final boolean loadOnTheFly) {
		super(indexBrowser.getDb(), -1, loadOnTheFly);

		this.queueManager = queueManager;
		this.indexBrowser = indexBrowser;

		if (!loadOnTheFly) /* anyway loading for this folder will be done immediatly ... :p */
			loadChildren();
	}


	public IndexFolder getNewImportFolder(Hsqldb db) {
		String fname = null;

		/* TODO : Don't do like this
		 *        Use the database :
		 *            ask '[importedFolderName]%' ORDER BY name DESC LIMIT 1
		 *        and pray there is no importedFolder number #10 or more,
		 *        or that the database will return the good one :p
		 */

		synchronized(db.dbLock) {
			try {
				int i;
				PreparedStatement select;

				select = db.getConnection().prepareStatement("SELECT id from indexfolders where parent is null and name = ? LIMIT 1");

				for (i = 0 ; i < 100 ; i++) {
					fname = I18n.getMessage("thaw.plugin.index.importedFolderName")+" - "+Integer.toString(i);
					select.setString(1, fname);

					ResultSet set = select.executeQuery();

					if (!set.next())
						break;
				}
			} catch(SQLException e) {
				Logger.error(this, "Unable to find a name for the import folder !");
				return null;
			}
		}

		if (fname == null)
			return null;

		return IndexManagementHelper.addIndexFolder(indexBrowser, this, fname);
	}

}

