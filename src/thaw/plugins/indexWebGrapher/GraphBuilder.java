package thaw.plugins.indexWebGrapher;

import java.sql.*;

import java.util.Iterator;
import java.util.Vector;


import thaw.plugins.IndexWebGrapher;
import thaw.plugins.Hsqldb;

import thaw.fcp.FreenetURIHelper;
import thaw.core.Logger;


public class GraphBuilder implements Runnable {

	private IndexWebGrapher plugin;
	private GraphPanel graphPanel;
	private Hsqldb db;

	public GraphBuilder(IndexWebGrapher plugin,
			    GraphPanel panel,
			    Hsqldb db) {
		this.plugin = plugin;
		this.graphPanel = panel;
		this.db = db;
	}

	public void run() {
		Logger.info(this, "=== Starting ===");

		/* === */

		plugin.setProgress(0);
		Logger.info(this, "0) Loading all the nodes ...");

		graphPanel.reinit();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, displayName, publicKey "+
									 "FROM indexes");

				ResultSet set = st.executeQuery();

				int nmb = 0;

				while(set.next()) {
					/* will register itself in the graphPanel */
					new Node(nmb,
						 set.getInt("id") /* index id */,
						 set.getString("displayName"),
						 set.getString("publicKey"),
						 graphPanel);
					nmb++;
				}

				Logger.info(this, Integer.toString(nmb)+" nodes loaded");
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't load the nodes because : "+e.toString());
			return;
		}

		/* === */

		plugin.setProgress(1);
		Logger.info(this, "1) Loading links ...");

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT publicKey FROM links WHERE indexParent = ?");

				for (Iterator it = (new Vector(graphPanel.getNodeList())).iterator();
				     it.hasNext(); ) {
					Node node = (Node)it.next();

					st.setInt(1, node.getIndexId());

					ResultSet set = st.executeQuery();

					while(set.next()) {
						String lnk = set.getString("publicKey");

						Node target = graphPanel.getNode(lnk);

						if (target == null) {
							target = new Node(graphPanel.getLastUsedId()+1,
									  -1 /* indexId */,
									  FreenetURIHelper.getFilenameFromKey(lnk).replaceAll(".frdx", ""),
									  lnk,
									  graphPanel);
						}

						node.setLinkTo(target);
					}
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't load the links because : "+e.toString());
			return;
		}


		/* === */

		plugin.setProgress(2);
		Logger.info(this, "2) Sorting the nodes according to their number of links ...");

		Vector nodes = new Vector(graphPanel.getNodeList());
		java.util.Collections.sort(nodes);


		/* === */

		plugin.setProgress(3);
		Logger.info(this, "3) Initial placing of the nodes ...");

		double x = 0.0;

		for (Iterator it = nodes.iterator();
		     it.hasNext();) {
			Node node = (Node)it.next();
			if (!node.isPositionSet()) {
				node.setPosition(x, 0.0);
				node.setInitialNeightboorPositions();
				x += (node.getLinkCount()+1);
			}
		}

		/* === */

		plugin.setProgress(4);
		Logger.info(this, "4) Optimizing placement ...");

		for (int i = 0 ; i < 100 ; i++) {
			for (Iterator it = nodes.iterator();
			     it.hasNext();) {
				Node node = (Node)it.next();
				node.computeVelocity(nodes);
			}

			boolean move = false;

			for (Iterator it = nodes.iterator();
			     it.hasNext();) {
				Node node = (Node)it.next();
				move |= node.applyVelocity();
			}

			if (!move) {
				Logger.info(this, "Wow, fully optimized ?!");
				break;
			}
		}

		/* === */

		plugin.setProgress(9);
		Logger.info(this, "Drawing ...");

		graphPanel.guessZoom();
		graphPanel.refresh();

		/* === */

		plugin.setProgress(10);
		Logger.info(this, "== Pouf, done ==");
	} /* /run */

}
