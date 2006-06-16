
package thaw.core;


import thaw.i18n.I18n;

/**
 * A "core" contains references to all the main parts of Thaw.
 *
 */
public class Core {
	private MainWindow mainWindow = null;
	private Config config = null;
	private PluginManager pluginManager = null;
	private ConfigWindow configWindow = null;


	/**
	 * Creates a core, but do nothing else (no initialization).
	 */
	public Core() {
		Logger.info(this, "Thaw, version "+Main.VERSION, true);
		Logger.info(this, "2006(c) Freenet project", true);
		Logger.info(this, "under GPL license (see gpl.txt joined)", true);
	}

	/**
	 * Gives a ref to the object containing the config.
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * Gives a ref to the object managing the main window.
	 */
	public MainWindow getMainWindow() {
		return mainWindow;
	}

	/**
	 * Gives a ref to the object managing the config window.
	 */
	public ConfigWindow getConfigWindow() {
		return configWindow;
	}

	/**
	 * Gives a ref to the plugin manager.
	 */
	public PluginManager getPluginManager() {
		return pluginManager;
	}


	/**
	 * Here really start the program.
	 * @return true is success, false if not
	 */
	public boolean initAll() {
		if(!initI18n())
			return false;

		if(!initConfig())
			return false;

		if(!initGraphics())
			return false;

		mainWindow.setStatus(I18n.getMessage("thaw.statusBar.initPlugins"));

		if(!initPluginManager())
			return false;


		mainWindow.setStatus(I18n.getMessage("thaw.statusBar.ready"));

		return true;
	}


	/** 
	 * Init I18n with default values.
	 */
	public boolean initI18n() {
		// Hum, nothing to do ?

		return true;
	}


	/**
	 * Init configuration. May re-set I18n.
	 */
	public boolean initConfig() {
		config = new Config();
		config.loadConfig();

		if(config.isEmpty())
			config.setDefaultValues();
		
		return true;
	}


	/**
	 * Init graphics.
	 */
	public boolean initGraphics() {
		mainWindow = new MainWindow(this);
		mainWindow.setVisible(true);

		configWindow = new ConfigWindow(this);
		configWindow.setVisible(false);

		return true;
	}

	/**
	 * Init plugin manager.
	 */
	public boolean initPluginManager() {
		pluginManager = new PluginManager(this);

		if(!pluginManager.loadPlugins())
			return false;
		
		if(!pluginManager.runPlugins())
			return false;

		return true;
	}


	/**
	 * End of the world.
	 */
	public void exit() {
		Logger.info(this, "Stopping plugins ...");
		pluginManager.stopPlugins();

		Logger.info(this, "Saving configuration ...");
		if(!config.saveConfig()) {
			Logger.error(this, "Config was not saved correctly !");
		}

		Logger.info(this, "Exiting");
		System.exit(0);
	}



}
