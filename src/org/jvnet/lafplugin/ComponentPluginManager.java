package org.jvnet.lafplugin;

import java.util.Iterator;
import java.util.Set;

import javax.swing.UIDefaults;

/**
 * Plugin manager for look-and-feels.
 * 
 * @author Kirill Grouchnikov
 * @author Erik Vickroy
 * @author Robert Beeger
 * @author Frederic Lavigne
 * @author Pattrick Gotthardt
 */
public class ComponentPluginManager extends PluginManager {
	/**
	 * Simple constructor.
	 * 
	 * @param xmlName
	 *            The name of XML file that contains plugin configuration.
	 * @param mainTag
	 *            The main tag in the XML configuration file.
	 * @param pluginTag
	 *            The tag that corresponds to a single plugin kind. Specifies
	 *            the plugin kind that will be located in
	 *            {@link #getAvailablePlugins(boolean)}.
	 */
	public ComponentPluginManager(String xmlName) {
		super(xmlName, LafComponentPlugin.TAG_MAIN,
				LafComponentPlugin.COMPONENT_TAG_PLUGIN_CLASS);
	}

	/**
	 * Helper function to initialize all available component plugins of
	 * <code>this</code> plugin manager. Calls the
	 * {@link LafComponentPlugin#initialize()} of all available component
	 * plugins.
	 */
	public void initializeAll() {
		Set availablePlugins = this.getAvailablePlugins();
		for (Iterator iterator = availablePlugins.iterator(); iterator
				.hasNext();) {
			Object pluginObject = iterator.next();
			if (pluginObject instanceof LafComponentPlugin)
				((LafComponentPlugin) pluginObject).initialize();
		}
	}

	/**
	 * Helper function to uninitialize all available component plugins of
	 * <code>this</code> plugin manager. Calls the
	 * {@link LafComponentPlugin#uninitialize()} of all available component
	 * plugins.
	 */
	public void uninitializeAll() {
		Set availablePlugins = this.getAvailablePlugins();
		for (Iterator iterator = availablePlugins.iterator(); iterator
				.hasNext();) {
			Object pluginObject = iterator.next();
			if (pluginObject instanceof LafComponentPlugin)
				((LafComponentPlugin) pluginObject).uninitialize();
		}
	}

	/**
	 * Helper function to process the (possibly) theme-dependent default
	 * settings of all available component plugins of <code>this</code> plugin
	 * manager. Calls the {@link LafComponentPlugin#getDefaults(Object)} of all
	 * available plugins and puts the respective results in the specified table.
	 * 
	 * @param table
	 *            The table that will be updated with the (possibly)
	 *            theme-dependent default settings of all available component
	 *            plugins.
	 * @param themeInfo
	 *            LAF-specific information on the current theme.
	 */
	public void processAllDefaultsEntries(UIDefaults table, Object themeInfo) {
		Set availablePlugins = this.getAvailablePlugins();
		for (Iterator iterator = availablePlugins.iterator(); iterator
				.hasNext();) {
			Object pluginObject = iterator.next();
			if (pluginObject instanceof LafComponentPlugin) {
				Object[] defaults = ((LafComponentPlugin) pluginObject)
						.getDefaults(themeInfo);
				if (defaults != null) {
					table.putDefaults(defaults);
				}
			}
		}
	}
}
