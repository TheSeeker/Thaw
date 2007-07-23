package thaw.plugins;

import thaw.core.I18n;
import thaw.core.Core;
import thaw.core.Logger;

import thaw.plugins.miniFrost.MiniFrostPanel;
import thaw.plugins.miniFrost.interfaces.BoardFactory;

import thaw.plugins.miniFrost.AutoRefresh;


public class MiniFrost implements thaw.core.Plugin {
	private Core core;
	private Hsqldb hsqldb;

	private MiniFrostPanel miniFrostPanel;
	private AutoRefresh autoRefresh;

	private boolean firstStart;


	public final static BoardFactory[] factories =
		new BoardFactory[] {
			new thaw.plugins.miniFrost.frostKSK.KSKBoardFactory(),
		};


	public boolean run(Core core) {
		this.core = core;

		if (!loadDeps()
		    || !initFactories()
		    || !loadGUI()
		    || !loadAutoRefresh())
			return false;

		return true;
	}


	protected boolean loadDeps() {
		if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
			Logger.info(this, "Loading Hsqldb plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
				Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
				return false;
			}
		}

		hsqldb = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");
		hsqldb.registerChild(this);

		return true;
	}


	protected boolean initFactories() {
		for (int i = 0 ; i < factories.length ; i++) {
			if (!factories[i].init(hsqldb, core, this))
				return false;
		}

		return true;
	}


	public Core getCore() {
		return core;
	}


	protected boolean loadGUI() {
		miniFrostPanel = new MiniFrostPanel(core.getConfig(), hsqldb, this);

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.miniFrost"),
					    thaw.gui.IconBox.readComments,
					    miniFrostPanel.getPanel());

		core.getMainWindow().getMainFrame().validate();

		miniFrostPanel.loadState();

		return true;
	}


	public boolean loadAutoRefresh() {
		autoRefresh = new AutoRefresh(core.getConfig(),
					      miniFrostPanel.getBoardTree());
		return true;
	}

	public boolean stop() {
		if (autoRefresh != null)
			autoRefresh.stop();

		core.getMainWindow().removeTab(miniFrostPanel.getPanel());

		if (hsqldb != null)
			hsqldb.unregisterChild(this);

		return true;
	}

	public BoardFactory[] getFactories() {
		return factories;
	}

	public MiniFrostPanel getPanel() {
		return miniFrostPanel;
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.miniFrost");
	}


	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.readComments;
	}
}
