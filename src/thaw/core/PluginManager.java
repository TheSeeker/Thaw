package thaw.core;

import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.Iterator;

/**
 * Manages plugins :)
 */
public class PluginManager {
	private final static String[] defaultPlugins = {"thaw.plugins.QueueWatcher",
							"thaw.plugins.InsertPlugin",
							"thaw.plugins.FetchPlugin",
							"thaw.plugins.StatusBar",
							"thaw.plugins.IndexBrowser",
							"thaw.plugins.IndexEditor"};

	private Core core = null;

	// LinkedHashMap because I want to keep a predictible plugin order.
	private LinkedHashMap plugins = null; // String (pluginName) -> Plugin


	/**
	 * Need a ref to the core to pass it to the plugins (and to access config)
	 */
	public PluginManager(Core core) {
		this.core = core;
		this.plugins = new LinkedHashMap();
	}


	/**
	 * Returns the whole plugin list.
	 */
	public LinkedHashMap getPlugins() {
		return this.plugins;
	}


	/**
	 * Load plugin from config or from default list.
	 * Reload if already loaded.
	 */
	public boolean loadPlugins() {
		this.plugins = new LinkedHashMap();

		Vector pluginNames;

		if(this.core.getConfig().getPluginNames().size() == 0) {
			/* Then we load the config with the default plugins */
			for(int i = 0 ; i < defaultPlugins.length ; i++) {
				this.core.getConfig().addPlugin(defaultPlugins[i]);
			}
		}

		pluginNames = this.core.getConfig().getPluginNames();

		Iterator pluginIt = pluginNames.iterator();

		int progressJump = 10 / pluginNames.size();
		this.core.getSplashScreen().setProgression(40);

		while(pluginIt.hasNext()) {
			String pluginName = (String)pluginIt.next();

			this.core.getSplashScreen().setProgressionAndStatus(this.core.getSplashScreen().getProgression()+progressJump,
								       "Loading plugin '"+pluginName+"' ...");

			this.loadPlugin(pluginName);
		}

		return true;
	}

	/**
	 * Start plugins.
	 */
	public boolean runPlugins() {
		Iterator pluginIt;

		if (this.plugins == null) {
			Logger.error(this, "No plugin to run ?!");
			return false;
		}

		pluginIt = (new Vector(this.plugins.values())).iterator();

		int progressJump = 50 / this.plugins.size();

		this.core.getSplashScreen().setProgression(50);

		while(pluginIt.hasNext()) {
			Plugin plugin = (Plugin)pluginIt.next();

			try {
				if (plugin != null) {
					Logger.info(this, "Running plugin '"+plugin.getClass().getName()+"'");

					this.core.getSplashScreen().setProgressionAndStatus(this.core.getSplashScreen().getProgression()+progressJump,
											    "Starting plugin '"+plugin.getClass().getName()+"' ...");
					plugin.run(this.core);

				}
				else
					Logger.notice(this, "Plugin == null ?");

			} catch(Exception e) {
				Logger.error(this, "Unable to run the plugin '"+plugin.getClass().getName()+"' because: "+e+":");
				e.printStackTrace();
			}
		}

		return true;
	}


	/**
	 * Stop all plugins.
	 */
	public boolean stopPlugins() {
		Iterator pluginIt;

		if (this.plugins == null) {
			Logger.error(this, "No plugin to stop ?!");
			return false;
		}


		pluginIt = this.plugins.values().iterator();

		while(pluginIt.hasNext()) {
			Plugin plugin = (Plugin)pluginIt.next();

			try {
				if (plugin != null)
					plugin.stop();
				else
					Logger.notice(this, "Plugin == null ?!!");
			} catch(Exception e) {
				Logger.error(this, "Unable to stop the plugin "+
					     "'"+plugin.getClass().getName()+"'"+
					     ", because: "+e.toString());
				e.printStackTrace();
			}
		}

		return true;
	}


	/**
	 * Load a given plugin (without adding it to the config or running it).
	 */
	public boolean loadPlugin(String className) {
		Logger.info(this, "Loading plugin: '"+className+"'");

		try {
			if(this.plugins.get(className) != null) {
				Logger.warning(this, "loadPlugin(): Plugin '"+className+"' already loaded");
				return false;
			}

			this.plugins.put(className, Class.forName(className).newInstance());

		} catch(Exception e) {
			Logger.warning(this, "loadPlugin('"+className+"'): Exception: "+e);
			return false;
		}

		return true;
	}


	/**
	 * Run a given plugin.
	 */
	public boolean runPlugin(String className) {
		Logger.info(this, "Starting plugin: '"+className+"'");

		try {
			((Plugin)this.plugins.get(className)).run(this.core);

		} catch(Exception e) {
			Logger.warning(this, "runPlugin('"+className+"'): Exception: "+e);
			return false;
		}

		return true;
	}


	/**
	 * Stop a given plugin.
	 */
	public boolean stopPlugin(String className) {
		Logger.info(this, "Stopping plugin: '"+className+"'");

		try {
			((Plugin)this.plugins.get(className)).stop();

		} catch(Exception e) {
			Logger.warning(this, "runPlugin('"+className+"'): Exception: "+e);
			return false;
		}

		return true;
	}


	/**
	 * Unload a given plugin (without adding it to the config or running it).
	 */
	public boolean unloadPlugin(String className) {
		try {
			if(this.plugins.get(className) == null) {
				Logger.warning(this, "unloadPlugin(): Plugin '"+className+"' already unloaded");
				return false;
			}

			this.plugins.remove(className);

		} catch(Exception e) {
			Logger.warning(this, "unloadPlugin('"+className+"'): Exception: "+e);
			return false;
		}

		return true;
	}

	public Plugin getPlugin(String className) {
		return (Plugin)this.plugins.get(className);
	}
}
