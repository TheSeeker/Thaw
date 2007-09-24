package thaw.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.Plugin;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;
import thaw.core.I18n;
import thaw.gui.IconBox;

import thaw.plugins.Hsqldb;

import java.sql.*;


public class IndexTreeRebuilder implements Plugin, ActionListener {
	private Core core;
	private Hsqldb db;

	public IndexTreeRebuilder() {
	}

	public boolean run(Core core) {
		this.core = core;

		if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
			Logger.info(this, "Loading Hsqldb plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
				Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
				return false;
			}
		}

		db = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");
		db.registerChild(this);

		return true;
	}

	public void stop() {

		if (db != null)
			db.unregisterChild(this);
	}

	public void actionPerformed(ActionEvent e) {

	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.index.treeRebuilder");
	}

	public javax.swing.ImageIcon getIcon() {
		return null;
	}
}

