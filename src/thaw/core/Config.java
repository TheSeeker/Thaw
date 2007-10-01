package thaw.core;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**
 * This class manages the thaw config.
 *
 * @author <a href="mailto:jflesch@gmail.com">Jerome Flesch</a>
 */
public class Config {

	public static String CONFIG_FILE_NAME = "thaw.conf.xml";
	private final File configFile;

	private final HashMap parameters; /* String (param) -> String (value) */
	private final HashMap listeners;  /* String (param) -> Vector -> Plugin */
	private final Vector pluginNames; /* String (plugin names) */

	private final Core core;


	public Config(Core core, final String filename) {
		this.core = core;

		configFile = new File(filename);

		parameters = new HashMap();
		pluginNames = new Vector();
		listeners = new HashMap();
	}

	/**
	 * @return null if the value doesn't exit in the config.
	 */
	public String getValue(final String key) {
		return ((String)parameters.get(key));
	}


	private boolean listenChanges = false;
	private Vector pluginsToReload = null;

	/**
	 * called when majors changed will be done to the config
	 * and will imply some plugin reloading
	 */
	public void startChanges() {
		listenChanges = true;
		pluginsToReload = new Vector();
	}

	/**
	 * Set the value in the config.
	 */
	public void setValue(final String key, final String value) {
		Logger.debug(this, "Setting value '"+key+"' to '"+value+"'");

		String currentValue = getValue(key);

		if ( (currentValue != null && !currentValue.equals(value))
		     || (currentValue == null && value != null ) ) {

			/* we get the plugin list to reload */
			Vector pluginList = (Vector)listeners.get(key);

			if (listenChanges && pluginList != null) {
				for (Iterator it = pluginList.iterator();
				     it.hasNext();) {
					Plugin plugin = (Plugin)it.next();

					/* if the plugin is not already in the plugin list to
					 * reload, we add it */
					if (pluginsToReload.indexOf(plugin) < 0) {
						Logger.notice(this, "Will have to reload '"+plugin.getClass().getName()+"' "+
							      "because '"+key+"' was changed from '"+currentValue+"' to '"+value+"'");
						pluginsToReload.add(plugin);
					}

				}
			}

			/* and to finish, we set the value */
			parameters.put(key, value);
		}
	}

	/**
	 * called after startChanges. Will reload the plugin listening for the changed
	 * values
	 */
	public void applyChanges() {
		for (Iterator it = pluginsToReload.iterator();
		     it.hasNext();) {
			Plugin plugin = (Plugin)it.next();
			core.getPluginManager().stopPlugin(plugin.getClass().getName());
			core.getPluginManager().runPlugin(plugin.getClass().getName());
		}

		cancelChanges();
	}

	/**
	 * Will not undo the changes do to the values, but reset to 0 the plugin list to reload
	 * Use it only if you know what you're doing !
	 */
	public void cancelChanges() {
		listenChanges = false;
		pluginsToReload = null;
	}

	/**
	 * Add the plugin at the end of the plugin list.
	 */
	public void addPlugin(final String name) {
		pluginNames.add(name);
	}

	/**
	 * Add the plugin at the end of the given position (shifting already existing).
	 */
	public void addPlugin(final String name, final int position) {
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
	public void removePlugin(final String name) {
		for(int i = 0; i < pluginNames.size() ; i++) {
			final String currentPlugin = (String)pluginNames.get(i);

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


		Document xmlDoc = null;
		DocumentBuilderFactory xmlFactory = null;
		DocumentBuilder xmlBuilder = null;

		Element rootEl = null;

		xmlFactory = DocumentBuilderFactory.newInstance();

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(final javax.xml.parsers.ParserConfigurationException e) {
			Logger.warning(this, "Unable to load config because: "+e);
			return false;
		}

		try {
			xmlDoc = xmlBuilder.parse(configFile);
		} catch(final org.xml.sax.SAXException e) {
			Logger.warning(this, "Unable to load config because: "+e);
			return false;
		} catch(final java.io.IOException e) {
			Logger.warning(this, "Unable to load config because: "+e);
			return false;
		}

		rootEl = xmlDoc.getDocumentElement();


		final NodeList params = rootEl.getElementsByTagName("param");

		for(int i = 0;i < params.getLength(); i++) {
			Element paramEl;
			final Node paramNode = params.item(i);

			if((paramNode != null) && (paramNode.getNodeType() == Node.ELEMENT_NODE)) {
				paramEl = (Element)paramNode;
				parameters.put(paramEl.getAttribute("name"), paramEl.getAttribute("value"));
			}
		}

		final NodeList plugins = rootEl.getElementsByTagName("plugin");

		for(int i = 0;i < plugins.getLength(); i++) {

			Element pluginEl;
			final Node pluginNode = plugins.item(i);

			if((pluginNode != null) && (pluginNode.getNodeType() == Node.ELEMENT_NODE)) {
				pluginEl = (Element)pluginNode;
				pluginNames.add(pluginEl.getAttribute("name"));
			}
		}


		return true;
	}


	/**
	 * Save the configuration.
	 *
	 * @return true if success, else false.
	 */
	public boolean saveConfig() {
		StreamResult configOut;

		if(configFile == null) {
			Logger.error(this, "saveConfig(): No file specified !");
			return false;
		}

		try {
			if( (!configFile.exists() && !configFile.createNewFile())
			    || !configFile.canWrite()) {
				Logger.warning(this, "Unable to write config file '"+configFile.getPath()+"' (can't write)");
				return false;
			}
		} catch(final java.io.IOException e) {
			Logger.warning(this, "Error while checking perms to save config: "+e);
		}

		configOut = new StreamResult(configFile);

		Document xmlDoc = null;
		DocumentBuilderFactory xmlFactory = null;
		DocumentBuilder xmlBuilder = null;
		DOMImplementation impl = null;

		Element rootEl = null;

		xmlFactory = DocumentBuilderFactory.newInstance();

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(final javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(this, "Unable to save configuration because: "+e.toString());
			return false;
		}


		impl = xmlBuilder.getDOMImplementation();

		xmlDoc = impl.createDocument(null, "config", null);

		rootEl = xmlDoc.getDocumentElement();


		final Iterator entries = parameters.keySet().iterator();

		while(entries.hasNext()) {
			final String entry = (String)entries.next();
			final String value = (String)parameters.get(entry);

			final Element paramEl = xmlDoc.createElement("param");
			paramEl.setAttribute("name", entry);
			paramEl.setAttribute("value", value);

			rootEl.appendChild(paramEl);
		}

		final Iterator plugins = pluginNames.iterator();

		while(plugins.hasNext()) {
			final String pluginName = (String)plugins.next();
			final Element pluginEl = xmlDoc.createElement("plugin");

			pluginEl.setAttribute("name", pluginName);

			rootEl.appendChild(pluginEl);
		}


		/* Serialization */
		final DOMSource domSource = new DOMSource(xmlDoc);
		final TransformerFactory transformFactory = TransformerFactory.newInstance();

		Transformer serializer;

		try {
			serializer = transformFactory.newTransformer();
		} catch(final javax.xml.transform.TransformerConfigurationException e) {
			Logger.error(this, "Unable to save configuration because: "+e.toString());
			return false;
		}

		serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");

		/* final step */
		try {
			serializer.transform(domSource, configOut);
		} catch(final javax.xml.transform.TransformerException e) {
			Logger.error(this, "Unable to save configuration because: "+e.toString());
			return false;
		}


		return true;
	}


	public boolean isEmpty() {
		if(parameters.keySet().size() == 0)
			return true;
		return false;
	}

	/**
	 * Set the value only if it doesn't exits.
	 */
	public void setDefaultValue(final String name, final String val) {
		if (getValue(name) == null)
			setValue(name, val);
	}

	/**
	 * don't override the values if already existing
	 */
	public void setDefaultValues() {
		setDefaultValue("nodeAddress", "127.0.0.1");
		setDefaultValue("nodePort", "9481");
		setDefaultValue("maxSimultaneousDownloads", "-1");
		setDefaultValue("maxSimultaneousInsertions", "-1");
		setDefaultValue("maxUploadSpeed", "-1");
		setDefaultValue("thawId", "thaw_"+Integer.toString((new Random()).nextInt(1000)));
		setDefaultValue("advancedMode", "false");
		setDefaultValue("userNickname", "Another anonymous");
		setDefaultValue("multipleSockets", "true");
		setDefaultValue("downloadLocally", "true");
		setDefaultValue("sameComputer", "true");
	}


	public void addListener(String name, Plugin plugin) {

		Vector pluginList = (Vector)listeners.get(name);

		if (pluginList == null) {
			pluginList = new Vector();
			listeners.put(name, pluginList);
		}

		if (pluginList.indexOf(plugin) < 0)
			pluginList.add(plugin);
	}
}
