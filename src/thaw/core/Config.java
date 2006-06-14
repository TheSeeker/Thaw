package thaw.core;

import java.util.HashMap;
import java.util.Vector;
import java.io.File;


/**
 * This class the thaw config.
 *
 * @author <a href="mailto:jflesch@nerim.net">Jerome Flesch</a>
 */
public class Config {

	private File configFile = new File("thaw.conf.xml"); /* Default name */

	private HashMap parameters = null; /* String (param) -> String (value) */
	private Vector pluginNames = null; /* String (plugin names) */


	public Config() {
		this(null);
	}

	public Config(String filename) {
		if(filename != null)
			configFile = new File(filename);

		parameters = new HashMap();
		pluginNames = new Vector();
	}

	/**
	 * Returns the corresponding value
	 * @return null if the value doesn't exit in the config.
	 */
	public String getValue(String key) {
		try {
			return ((String)parameters.get(key));
		} catch(Exception e) { /* I should see for the correct exception */
			Logger.notice(this, "Unknow key in configuration: '"+key+"'");
			return null;
		}
	}

	/**
	 * Set the value in the config.
	 */
	public void setValue(String key, String value) {
		if(value != null)
			Logger.info(this, "Setting value '"+key+"' to '"+value+"'");
		else
			Logger.info(this, "Setting value '"+key+"' to null");
		parameters.put(key, value);
	}

	/**
	 * Add the plugin at the end of the plugin list.
	 */
	public void addPlugin(String name) {
		pluginNames.add(name);
	}

	/**
	 * Add the plugin at the end of the given position (shifting already existing).
	 */
	public void addPlugin(String name, int position) {
		pluginNames.add(position, name);
	}

	/**
	 * Give a vector containing the whole list of plugins.
	 */
	public Vector getPluginNames() {
		return pluginNames;
	}

	/**
	 * Remove the given plugin.
	 */
	public void removePlugin(String name) {
		for(int i = 0; i < pluginNames.size() ; i++) {
			String currentPlugin = (String)pluginNames.get(i);

			if(currentPlugin.equals(name))
				pluginNames.remove(i);
		}
	}


	/**
	 * Load the configuration.
	 * @return true if success, else false.
	 */
	public boolean loadConfig() {
		if(configFile == null) {
			Logger.error(this, "loadConfig(): No file specified !");
			return false;
		}

		if(!configFile.exists() || !configFile.canRead()) {
			Logger.notice(this, "Unable to read config file '"+configFile.getPath()+"'");
			return false;
		}

		
		/* TODO */

		return true;
	}


	/**
	 * Save the configuration.
	 * @return true if success, else false.
	 */
	public boolean saveConfig() {
		if(configFile == null) {
			Logger.error(this, "saveConfig(): No file specified !");
			return false;
		}

		if(!configFile.exists() || !configFile.canWrite()) {
			Logger.warning(this, "Unable to write config file '"+configFile.getPath()+"'");
			return false;
		}
		
		
		/* TODO */

		return true;	
	}

}
