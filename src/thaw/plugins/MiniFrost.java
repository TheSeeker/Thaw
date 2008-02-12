package thaw.plugins;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import thaw.core.I18n;
import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.Config;

import thaw.plugins.miniFrost.MiniFrostPanel;
import thaw.plugins.miniFrost.interfaces.BoardFactory;

import thaw.plugins.miniFrost.AutoRefresh;
import thaw.plugins.miniFrost.MiniFrostConfigTab;
import thaw.plugins.miniFrost.RegexpBlacklist;


public class MiniFrost implements thaw.core.Plugin, ChangeListener {
	public final static int DEFAULT_ARCHIVE_AFTER = 7; /* days */
	public final static int DEFAULT_DELETE_AFTER  = 60; /* days */
	public final static boolean DISPLAY_AS_TREE = true;

	private Core core;
	private Hsqldb hsqldb;
	private WebOfTrust wot;

	private MiniFrostPanel miniFrostPanel;
	private MiniFrostConfigTab configTab;
	private AutoRefresh autoRefresh;

	private RegexpBlacklist regexpBlacklist;

	public final static BoardFactory[] factories =
		new BoardFactory[] {
			new thaw.plugins.miniFrost.KnownBoardListProvider(),
			new thaw.plugins.miniFrost.frostKSK.KSKBoardFactory(),
			new thaw.plugins.miniFrost.frostKSK.SSKBoardFactory(),
			new thaw.plugins.miniFrost.SpecialBoardFactory()
		};


	public boolean run(Core core) {
		this.core = core;

		core.getConfig().addListener("advancedMode",                  this);
		core.getConfig().addListener("miniFrostAutoRefreshMaxBoards", this);
		core.getConfig().addListener("miniFrostArchiveAfter",         this);
		core.getConfig().addListener("miniFrostDeleteAfter",          this);
		core.getConfig().addListener("miniFrostView",                 this);
		core.getConfig().addListener("checkbox_miniFrost_seeTree",    this);

		if (!loadDeps()
		    || !initFactories()
		    || !cleanUp(core.getConfig())
		    || !loadGUI()
		    || !loadAutoRefresh())
			return false;

		regexpBlacklist = new RegexpBlacklist(hsqldb);

		configTab = new MiniFrostConfigTab(core.getConfig(),
						   core.getConfigWindow(),
						   regexpBlacklist);
		configTab.display();
		
		miniFrostPanel.setVisible(true);
		
		core.getMainWindow().getTabbedPane().addChangeListener(this);

		return true;
	}


	public RegexpBlacklist getRegexpBlacklist() {
		return regexpBlacklist;
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
		
		if(core.getPluginManager().getPlugin("thaw.plugins.WebOfTrust") == null) {
			Logger.info(this, "Loading WoT plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.WebOfTrust") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.WebOfTrust")) {
				Logger.error(this, "Unable to load thaw.plugins.WebOfTrust !");
				return false;
			}
		}

		wot = (WebOfTrust)core.getPluginManager().getPlugin("thaw.plugins.WebOfTrust");
		wot.registerChild(this);

		return true;
	}


	protected boolean initFactories() {
		for (int i = 0 ; i < factories.length ; i++) {
			if (!factories[i].init(hsqldb, core, wot, this))
				return false;
		}

		return true;
	}


	public Core getCore() {
		return core;
	}


	protected boolean cleanUp(Config config) {
		int archiveAfter = DEFAULT_ARCHIVE_AFTER;
		int deleteAfter = DEFAULT_DELETE_AFTER;

		if (config.getValue("miniFrostArchiveAfter") != null)
			archiveAfter = Integer.parseInt(config.getValue("miniFrostArchiveAfter"));

		if (config.getValue("miniFrostDeleteAfter") != null)
			deleteAfter = Integer.parseInt(config.getValue("miniFrostDeleteAfter"));

		boolean b = true;

		for (int i = 0 ; i < factories.length ; i++) {
			if (!factories[i].cleanUp(archiveAfter, deleteAfter))
				b = false;
		}

		return b;
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

	public void stop() {
		core.getMainWindow().getTabbedPane().removeChangeListener(this);
		
		if (autoRefresh != null)
			autoRefresh.stop();

		if (miniFrostPanel != null)
			core.getMainWindow().removeTab(miniFrostPanel.getPanel());
		else
			Logger.warning(this, "MiniFrost not started, can't stop.");

		if (configTab != null)
			configTab.hide();

		if (hsqldb != null)
			hsqldb.unregisterChild(this);
		
		if (wot != null)
			wot.unregisterChild(this);
		
		miniFrostPanel.setVisible(false);
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


	public void stateChanged(ChangeEvent arg0) {
		int tabId;

		tabId = core.getMainWindow().getTabbedPane().indexOfTab(I18n.getMessage("thaw.plugin.miniFrost"));

		if (tabId < 0) {
			Logger.warning(this, "Unable to find the tab !");
			return;
		}

		miniFrostPanel.setVisible(core.getMainWindow().getTabbedPane().getSelectedIndex() == tabId);		
	}
}
