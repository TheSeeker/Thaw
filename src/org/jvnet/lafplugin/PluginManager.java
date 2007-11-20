package org.jvnet.lafplugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Plugin manager for look-and-feels.
 * 
 * @author Kirill Grouchnikov
 * @author Erik Vickroy
 * @author Robert Beeger
 * @author Frederic Lavigne
 * @author Pattrick Gotthardt
 */
public class PluginManager {
	private String mainTag;

	private String pluginTag;

	private String xmlName;

	private Set plugins;

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
	public PluginManager(String xmlName, String mainTag, String pluginTag) {
		this.xmlName = xmlName;
		this.mainTag = mainTag;
		this.pluginTag = pluginTag;
		this.plugins = null;
	}

	// protected String getPluginClass(URL pluginUrl) {
	// InputStream is = null;
	// try {
	// DocumentBuilder builder = DocumentBuilderFactory.newInstance()
	// .newDocumentBuilder();
	// is = pluginUrl.openStream();
	// Document doc = builder.parse(is);
	// Node root = doc.getFirstChild();
	// if (!this.mainTag.equals(root.getNodeName()))
	// return null;
	// NodeList children = root.getChildNodes();
	// for (int i = 0; i < children.getLength(); i++) {
	// Node child = children.item(i);
	// if (!this.pluginTag.equals(child.getNodeName()))
	// continue;
	// if (child.getChildNodes().getLength() != 1)
	// return null;
	// Node text = child.getFirstChild();
	// if (text.getNodeType() != Node.TEXT_NODE)
	// return null;
	// return text.getNodeValue();
	// }
	// return null;
	// } catch (Exception exc) {
	// return null;
	// } finally {
	// if (is != null) {
	// try {
	// is.close();
	// } catch (Exception e) {
	// }
	// }
	// }
	// }
	//
	protected String getPluginClass(URL pluginUrl) {
		InputStream is = null;
		InputStreamReader isr = null;
		try {
			XMLElement xml = new XMLElement();
			is = pluginUrl.openStream();
			isr = new InputStreamReader(is);
			xml.parseFromReader(isr);
			if (!this.mainTag.equals(xml.getName()))
				return null;
			Enumeration children = xml.enumerateChildren();
			while (children.hasMoreElements()) {
				XMLElement child = (XMLElement) children.nextElement();
				if (!this.pluginTag.equals(child.getName()))
					continue;
				if (child.countChildren() != 0)
					return null;
				return child.getContent();
			}
			return null;
		} catch (Exception exc) {
			return null;
		} finally {
			if (isr != null) {
				try {
					isr.close();
				} catch (Exception e) {
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
		}
	}

	protected Object getPlugin(URL pluginUrl) throws Exception {
		String pluginClassName = this.getPluginClass(pluginUrl);
		if (pluginClassName == null)
			return null;
		Class pluginClass = Class.forName(pluginClassName);
		if (pluginClass == null)
			return null;
		Object pluginInstance = pluginClass.newInstance();
		if (pluginInstance == null)
			return null;
		return pluginInstance;
	}

	/**
	 * Returns a collection of all available plugins.
	 * 
	 * @return Collection of all available plugins. The classpath is scanned
	 *         only once.
	 * @see #getAvailablePlugins(boolean)
	 */
	public Set getAvailablePlugins() {
		return this.getAvailablePlugins(false);
	}

	/**
	 * Returns a collection of all available plugins. The parameter specifies
	 * whether the classpath should be rescanned or whether to return the
	 * already found plugins (after first-time scan).
	 * 
	 * @param toReload
	 *            If <code>true</code>, the classpath is scanned for
	 *            available plugins every time <code>this</code> function is
	 *            called. If <code>false</code>, the classpath scan is
	 *            performed only once. The consecutive calls return the cached
	 *            result.
	 * @return Collection of all available plugins.
	 */
	public Set getAvailablePlugins(boolean toReload) {
		if (toReload && (this.plugins != null))
			return this.plugins;

		this.plugins = new HashSet();

		ClassLoader cl = PluginManager.class.getClassLoader();
		try {
			Enumeration urls = cl.getResources(this.xmlName);
			while (urls.hasMoreElements()) {
				URL pluginUrl = (URL) urls.nextElement();
				Object pluginInstance = this.getPlugin(pluginUrl);
				if (pluginInstance != null)
					this.plugins.add(pluginInstance);

			}
		} catch (Exception exc) {
			return null;
		}

		return plugins;
	}
}
