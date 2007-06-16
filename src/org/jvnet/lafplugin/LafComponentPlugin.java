package org.jvnet.lafplugin;

import javax.swing.plaf.metal.MetalTheme;

/**
 * Basic interface for look-and-feel plugins.
 * 
 * @author Kirill Grouchnikov
 * @author Erik Vickroy
 * @author Robert Beeger
 * @author Frederic Lavigne
 * @author Pattrick Gotthardt
 */
public interface LafComponentPlugin extends LafPlugin {
	/**
	 * XML tag for look-and-feel plugins that specify component UI delegates.
	 */
	public static final String COMPONENT_TAG_PLUGIN_CLASS = "component-plugin-class";

	/**
	 * Initializes <code>this</code> plugin.
	 */
	public void initialize();

	/**
	 * Unitializes <code>this</code> plugin.
	 */
	public void uninitialize();

	/**
	 * Retrieves a collection of custom settings based on the specified theme.
	 * The entries in the array should be pairwise, odd being symbolic name of a
	 * setting, and even being the setting value.
	 * 
	 * @param themeInfo
	 *            Theme information object. Can be {@link MetalTheme}, for
	 *            instance or any other LAF-specific object.
	 * @return Collection of custom settings based on the specified theme. The
	 *         entries in the array should be pairwise, odd being symbolic name
	 *         of a setting, and even being the setting value.
	 */
	public Object[] getDefaults(Object themeInfo);
}
