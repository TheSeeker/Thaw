package thaw.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;


import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.Plugin;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;
import thaw.core.I18n;
import thaw.gui.IconBox;
import thaw.gui.WarningWindow;

import thaw.plugins.Hsqldb;

import java.sql.*;


public class IndexTreeRebuilder implements Plugin {
	private Core core;
	private Hsqldb db;

	public IndexTreeRebuilder() {
	}


	private class Rebuilder implements ThawRunnable {
		private boolean running;
		private Plugin parent;

		public Rebuilder(Plugin parent) {
			running = true;
			this.parent = parent;
		}

		public void rebuild() throws SQLException {

			/* TODO */

		}

		public void run() {

			if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
				Logger.info(this, "Loading Hsqldb plugin");

				if(core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb") == null
				   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
					Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
					return;
				}
			}

			db = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");

			if (db == null) {
				Logger.error(this, "Can't access the db !");
			} else {

				db.registerChild(parent);

				if (running)
					core.getPluginManager().stopPlugin("thaw.plugins.IndexBrowser");

				if (running) {
					try {
						rebuild();
					} catch(SQLException e) {
						/* wow, getting creepy */
						Logger.error(this, "Index tree rebuild failed : "+e.toString());
						new WarningWindow(core,
								  I18n.getMessage("thaw.plugin.index.treeRebuilder.failed"));
					}
				}

				if (running)
					core.getPluginManager().runPlugin("thaw.plugins.IndexBrowser");

				db.unregisterChild(parent);
			}

			if (running)
				new WarningWindow(core,
						  I18n.getMessage("thaw.plugin.index.treeRebuilder.finished"));

			core.getPluginManager().stopPlugin("thaw.plugins.IndexTreeRebuilder");
			core.getPluginManager().unloadPlugin("thaw.plugins.IndexTreeRebuilder");

			core.getConfigWindow().getPluginConfigPanel().refreshList();
		}

		public void stop() {
			running = false;
		}
	}


	public boolean run(Core core) {
		this.core = core;

		ThawThread th = new ThawThread(new Rebuilder(this),
					       "Index tree rebuilder",
					       this);
		th.start();

		return true;
	}

	public void stop() {

	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.index.treeRebuilder");
	}

	public javax.swing.ImageIcon getIcon() {
		return null;
	}
}

