package org.jvnet.lafplugin;


/**
 * Basic interface for look-and-feel plugins.
 * 
 * @author Kirill Grouchnikov
 * @author Erik Vickroy
 * @author Robert Beeger
 * @author Frederic Lavigne
 * @author Pattrick Gotthardt
 */
public interface LafPlugin {
	/**
	 * Main XML tag. See
	 * {@link PluginManager#PluginManager(String, String, String)}.
	 */
	public static final String TAG_MAIN = "laf-plugin";
}
