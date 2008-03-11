package thaw.core;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * Manages plugins :)
 */
public class PluginManager {
	private final static String[] defaultPlugins = {
		"thaw.plugins.QueueWatcher",
		"thaw.plugins.FetchPlugin",
		"thaw.plugins.InsertPlugin",
		"thaw.plugins.StatusBar",
		"thaw.plugins.ThemeSelector",
		"thaw.plugins.Hsqldb",
		"thaw.plugins.IndexBrowser",
		"thaw.plugins.IndexExporter",
		"thaw.plugins.TransferLogs"
	};

	private final static String[] knownPlugins = {
		"thaw.plugins.PeerMonitor",
		"thaw.plugins.QueueWatcher",
		"thaw.plugins.FetchPlugin",
		"thaw.plugins.InsertPlugin",
		"thaw.plugins.StatusBar",
		"thaw.plugins.TrayIcon",
		"thaw.plugins.ThemeSelector",
		"thaw.plugins.Hsqldb",
		"thaw.plugins.Signatures",
		"thaw.plugins.WebOfTrust",
		"thaw.plugins.WebOfTrustViewer",
		"thaw.plugins.IndexBrowser",
		"thaw.plugins.IndexExporter",
		//"thaw.plugins.IndexTreeRebuilder",
		"thaw.plugins.MiniFrost",
		//"thaw.plugins.Restarter",
		"thaw.plugins.TransferLogs",
		"thaw.plugins.NodeConfigurator",
		"thaw.plugins.MDns",
		//"thaw.plugins.IndexWebGrapher",
		"thaw.plugins.SqlConsole",
		"thaw.plugins.LogConsole"
	};

	private Core core = null;

	// LinkedHashMap because I want to keep a predictible plugin order.
	private LinkedHashMap plugins = null; // String (pluginName) -> Plugin

	public final static Object pluginLock = new Object();

	/**
	 * Need a ref to the core to pass it to the plugins (and to access config)
	 */
	public PluginManager(final Core core) {
		this.core = core;
		plugins = new LinkedHashMap();
	}


	/**
	 * Returns the whole loaded plugin list.
	 */
	public LinkedHashMap getPlugins() {
		return plugins;
	}


	public static String[] getKnownPlugins() {
		return knownPlugins;
	}


	/**
	 * Load plugin from config or from default list.
	 * Reload if already loaded.
	 */
	public boolean loadAndRunPlugins() {
		synchronized(pluginLock) {
			plugins = new LinkedHashMap();

			Vector pluginNames;

			if(core.getConfig().getPluginNames().size() == 0) {
				Logger.notice(this, "Loading default plugin list");
				/* Then we load the config with the default plugins */
				for(int i = 0 ; i < PluginManager.defaultPlugins.length ; i++) {
					core.getConfig().addPlugin(PluginManager.defaultPlugins[i]);
				}
			}

			/* we duplicate the vector to avoid collisions */
			/* (remember : plugins can load other plugins */
			pluginNames = new Vector(core.getConfig().getPluginNames());

			final Iterator pluginIt = pluginNames.iterator();

			final int progressJump = (100-40) / pluginNames.size();
			
			if (core.getSplashScreen() != null)
				core.getSplashScreen().setProgression(40);

			while(pluginIt.hasNext()) {
				final String pluginName = (String)pluginIt.next();

				if (core.getSplashScreen() != null)
					core.getSplashScreen().setProgressionAndStatus(core.getSplashScreen().getProgression()+progressJump,
																	"Loading plugin '"+pluginName.replaceFirst("thaw.plugins.", "")+"' ...");

				if (loadPlugin(pluginName) == null) {
					Logger.notice(this, "Plugin alread loaded");
				} else {
					runPlugin(pluginName);
				}
			}
		}

		return true;
	}


	/**
	 * Stop all plugins.
	 */
	public boolean stopPlugins() {
		synchronized(pluginLock) {
			Iterator pluginIt;

			if (plugins == null) {
				Logger.error(this, "No plugin to stop ?!");
				return false;
			}


			pluginIt = plugins.values().iterator();

			while(pluginIt.hasNext()) {
				final Plugin plugin = (Plugin)pluginIt.next();

				try {
					Logger.info(this, "Stopping plugin '"+plugin.getClass().getName()+"'");

					if (plugin != null)
						plugin.stop();
					else
						Logger.error(this, "Plugin == null !?");
				} catch(final Exception e) {
					Logger.error(this, "Unable to stop the plugin "+
							"'"+plugin.getClass().getName()+"'"+
							", because: "+e.toString());
					e.printStackTrace();
				}
			}

			return true;
		}
	}

	/**
	 * Load a given plugin (without adding it to the config or running it).
	 */
	public Plugin loadPlugin(final String className) {
		synchronized(pluginLock) {
			Plugin plugin = null;
	
			Logger.info(this, "Loading plugin: '"+className+"'");
	
			try {
				if ( plugins.get(className) != null) {
					Logger.debug(this, "loadPlugin(): Plugin '"+className+"' already loaded");
					return null;
				}
	
				//Logger.info(this, "Loading plugin '"+className+"'");
	
				plugin = (Plugin)Class.forName(className).newInstance();
	
				plugins.put(className, plugin);
	
			} catch(final Exception e) {
				Logger.error(this, "loadPlugin('"+className+"'): Exception: "+e);
				e.printStackTrace();
				return null;
			}
	
			return plugin;
		}
	}


	/**
	 * Run a given plugin.
	 */
	public boolean runPlugin(final String className) {
		synchronized(pluginLock) {
			Logger.info(this, "Starting plugin: '"+className+"'");
	
			try {
				Plugin plugin = (Plugin)plugins.get(className);
	
				javax.swing.ImageIcon icon;
	
				if (core.getSplashScreen() != null) {
					if ((icon = plugin.getIcon()) != null)
						core.getSplashScreen().addIcon(icon);
					else
						core.getSplashScreen().addIcon(thaw.gui.IconBox.add);
				}
	
				plugin.run(core);
	
			} catch(final Exception e) {
				Logger.error(this, "runPlugin('"+className+"'): Exception: "+e);
				e.printStackTrace();
				return false;
			}
	
			return true;
		}
	}


	/**
	 * Stop a given plugin.
	 */
	public boolean stopPlugin(final String className) {
		synchronized(pluginLock) {
			Logger.info(this, "Stopping plugin: '"+className+"'");
	
			try {
				((Plugin)plugins.get(className)).stop();
	
			} catch(final Exception e) {
				Logger.error(this, "stopPlugin('"+className+"'): Exception: "+e);
				e.printStackTrace();
				return false;
			}
	
			return true;
		}
	}


	/**
	 * Unload a given plugin (without adding it to the config or running it).
	 */
	public boolean unloadPlugin(final String className) {
		synchronized(pluginLock) {
			try {
				if(plugins.get(className) == null) {
					Logger.notice(this, "unloadPlugin(): Plugin '"+className+"' already unloaded");
					return false;
				}

				plugins.remove(className);
				core.getConfig().removePlugin(className);

			} catch(final Exception e) {
				Logger.error(this, "unloadPlugin('"+className+"'): Exception: "+e);
				e.printStackTrace();
				return false;
			}

			return true;
		}
	}

	public Plugin getPlugin(final String className) {
		return (Plugin)plugins.get(className);
	}
}
