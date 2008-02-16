package thaw.plugins.indexWebGrapher;

import java.sql.*;

import java.util.Iterator;
import java.util.Vector;


import thaw.plugins.IndexWebGrapher;
import thaw.plugins.Hsqldb;

import thaw.fcp.FreenetURIHelper;
import thaw.core.Logger;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;


public class GraphBuilder implements ThawRunnable {

	private IndexWebGrapher plugin;
	private GraphPanel graphPanel;
	private Hsqldb db;

	private boolean faster;
	private boolean finish;
	private boolean stop;

	public GraphBuilder(IndexWebGrapher plugin,
			    GraphPanel panel,
			    Hsqldb db) {
		this.plugin = plugin;
		this.graphPanel = panel;
		this.db = db;
		this.faster = false;
		this.finish = false;
		this.stop = false;
	}

	private class Refresher implements ThawRunnable {
		public Refresher() {

		}

		public void run() {
			run(true);
		}

		public void run(boolean loop) {
			do {

				graphPanel.recomputeMinMax();
				graphPanel.refresh();

				try {
					Thread.sleep(50);
				} catch(InterruptedException e) {
					/* \_o< */
				}

			} while (loop && !stop && !finish);
		}

		public void stop() {
			/* \_o< */
		}
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
					String key = set.getString("publicKey");

					if (FreenetURIHelper.isObsolete(key))
						continue;

					/* will register itself in the graphPanel */
					new Node(nmb,
						 set.getInt("id") /* index id */,
						 set.getString("displayName"),
						 key,
						 graphPanel);
					nmb++;
				}
				
				st.close();

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

						if (FreenetURIHelper.isObsolete(lnk))
							continue;

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
				
				st.close();
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
				node.setInitialNeightbourPositions();
				x += ((Node.FACTOR_INITIAL_DISTANCE * node.getLinkCount())+1);
			}
		}

		graphPanel.guessZoom();
		graphPanel.recomputeMinMax();
		graphPanel.refresh();

		try {
			Thread.sleep(3000);
		} catch(InterruptedException e) {
			/* \_o< */
		}

		/* === */

		plugin.setProgress(4);
		Logger.info(this, "4) Optimizing placement ...");

		Refresher refresher = new Refresher();
		Thread refresherTh = null;

		int lastStep = 4;
		double totalKinetic = 0.0;
		double sumKinetics = 0.0;
		int nmbKinetics = 0;

		for (int i = 0 ; i < Node.NMB_STEPS && !stop ; i++) {
			int currentStep = (6 * i) / Node.NMB_STEPS;

			if (currentStep != lastStep) {
				plugin.setProgress(currentStep+4);
				lastStep = currentStep;
			}

			if (i == 0)
				graphPanel.guessZoom();

			if (!faster)
				refresher.run(false);
			else {
				if (refresherTh == null) {
					refresherTh = new ThawThread(refresher, "Index web display refresh", this);
					refresherTh.start();
				}
			}

			if (i != 0) {
				sumKinetics += totalKinetic;
				nmbKinetics++;
			}

			if (i != 0 && i%100 == 0) {
				Logger.info(this, "================================");
				Logger.info(this, "- Step "+Integer.toString(i)+"/"+Node.NMB_STEPS);
				Logger.info(this, "- Kinetic : "+Double.toString(totalKinetic));
				Logger.info(this, "- Average kinetic : "+Double.toString(sumKinetics/nmbKinetics));
			}

			totalKinetic = 0.0;

			for (Iterator it = nodes.iterator();
			     it.hasNext();) {
				Node node = (Node)it.next();
				totalKinetic += node.computeVelocity(nodes);
			}

			if (totalKinetic < (Node.MIN_KINETIC)) {
				Logger.info(this, "Wow, seems optimized :)");
				break;
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

		graphPanel.refresh();

		/* === */

		plugin.setProgress(10);
		Logger.info(this, "== Pouf, done ==");

		for (Iterator it = nodes.iterator();
		     it.hasNext();) {
			Node node = (Node)it.next();
			Logger.info(this, node.toString());
		}

		finish = true;

		plugin.endOfProcess();
	} /* /run */


	public void setFasterFlag(boolean faster) {
		this.faster = faster;
	}

	public boolean fasterFlag() {
		return faster;
	}

	public void stop() {
		stop = true;
	}

	public boolean isFinished() {
		return finish;
	}
}
