package thaw.core;

import java.util.HashMap;
import java.util.Vector;
import java.io.File;
import java.util.Iterator;
import java.util.Random;

/* XML */
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.DOMImplementation;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**
 * This class the thaw config.
 *
 * @author <a href="mailto:jflesch@gmail.com">Jerome Flesch</a>
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
			this.configFile = new File(filename);

		this.parameters = new HashMap();
		this.pluginNames = new Vector();
	}

	/**
	 * Returns the corresponding value
	 * @return null if the value doesn't exit in the config.
	 */
	public String getValue(String key) {
		try {
			return ((String)this.parameters.get(key));
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
		this.parameters.put(key, value);
	}

	/**
	 * Add the plugin at the end of the plugin list.
	 */
	public void addPlugin(String name) {
		this.pluginNames.add(name);
	}

	/**
	 * Add the plugin at the end of the given position (shifting already existing).
	 */
	public void addPlugin(String name, int position) {
		this.pluginNames.add(position, name);
	}

	/**
	 * Give a vector containing the whole list of plugins.
	 */
	public Vector getPluginNames() {
		return this.pluginNames;
	}

	/**
	 * Remove the given plugin.
	 */
	public void removePlugin(String name) {
		for(int i = 0; i < this.pluginNames.size() ; i++) {
			String currentPlugin = (String)this.pluginNames.get(i);

			if(currentPlugin.equals(name))
				this.pluginNames.remove(i);
		}
	}


	/**
	 * Load the configuration.
	 * @return true if success, else false.
	 */
	public boolean loadConfig() {
		if(this.configFile == null) {
			Logger.error(this, "loadConfig(): No file specified !");
			return false;
		}

		if(!this.configFile.exists() || !this.configFile.canRead()) {
			Logger.notice(this, "Unable to read config file '"+this.configFile.getPath()+"'");
			return false;
		}


		Document xmlDoc = null;
		DocumentBuilderFactory xmlFactory = null;
		DocumentBuilder xmlBuilder = null;

		Element rootEl = null;

		xmlFactory = DocumentBuilderFactory.newInstance();

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(javax.xml.parsers.ParserConfigurationException e) {
			Logger.warning(this, "Unable to load config because: "+e);
			return false;
		}

		try {
			xmlDoc = xmlBuilder.parse(this.configFile);
		} catch(org.xml.sax.SAXException e) {
			Logger.warning(this, "Unable to load config because: "+e);
			return false;
		} catch(java.io.IOException e) {
			Logger.warning(this, "Unable to load config because: "+e);
			return false;
		}

		rootEl = xmlDoc.getDocumentElement();


		NodeList params = rootEl.getElementsByTagName("param");

		for(int i = 0;i < params.getLength(); i++) {
			Element paramEl;
			Node paramNode = params.item(i);

			if(paramNode != null && paramNode.getNodeType() == Node.ELEMENT_NODE) {
				paramEl = (Element)paramNode;
				this.parameters.put(paramEl.getAttribute("name"), paramEl.getAttribute("value"));
			}
		}

		NodeList plugins = rootEl.getElementsByTagName("plugin");

		for(int i = 0;i < plugins.getLength(); i++) {

			Element pluginEl;
			Node pluginNode = plugins.item(i);

			if(pluginNode != null && pluginNode.getNodeType() == Node.ELEMENT_NODE) {
				pluginEl = (Element)pluginNode;
				this.pluginNames.add(pluginEl.getAttribute("name"));
			}
		}


		return true;
	}


	/**
	 * Save the configuration.
	 * @return true if success, else false.
	 */
	public boolean saveConfig() {
		StreamResult configOut;

		if(this.configFile == null) {
			Logger.error(this, "saveConfig(): No file specified !");
			return false;
		}

		try {
			if( (!this.configFile.exists() && !this.configFile.createNewFile())
			    || !this.configFile.canWrite()) {
				Logger.warning(this, "Unable to write config file '"+this.configFile.getPath()+"' (can't write)");
				return false;
			}
		} catch(java.io.IOException e) {
			Logger.warning(this, "Error while checking perms to save config: "+e);
		}



		configOut = new StreamResult(this.configFile);

		Document xmlDoc = null;
		DocumentBuilderFactory xmlFactory = null;
		DocumentBuilder xmlBuilder = null;
		DOMImplementation impl = null;

		Element rootEl = null;

		xmlFactory = DocumentBuilderFactory.newInstance();

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(this, "Unable to save configuration because: "+e.toString());
			return false;
		}


		impl = xmlBuilder.getDOMImplementation();

		xmlDoc = impl.createDocument(null, "config", null);

		rootEl = xmlDoc.getDocumentElement();


		Iterator entries = this.parameters.keySet().iterator();

		while(entries.hasNext()) {
			String entry = (String)entries.next();
			String value = (String)this.parameters.get(entry);

			Element paramEl = xmlDoc.createElement("param");
			paramEl.setAttribute("name", entry);
			paramEl.setAttribute("value", value);

			rootEl.appendChild(paramEl);
		}

		Iterator plugins = this.pluginNames.iterator();

		while(plugins.hasNext()) {
			String pluginName = (String)plugins.next();
			Element pluginEl = xmlDoc.createElement("plugin");

			pluginEl.setAttribute("name", pluginName);

			rootEl.appendChild(pluginEl);
		}


		/* Serialization */
		DOMSource domSource = new DOMSource(xmlDoc);
		TransformerFactory transformFactory = TransformerFactory.newInstance();

		Transformer serializer;

		try {
			serializer = transformFactory.newTransformer();
		} catch(javax.xml.transform.TransformerConfigurationException e) {
			Logger.error(this, "Unable to save configuration because: "+e.toString());
			return false;
		}

		serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");

		/* final step */
		try {
			serializer.transform(domSource, configOut);
		} catch(javax.xml.transform.TransformerException e) {
			Logger.error(this, "Unable to save configuration because: "+e.toString());
			return false;
		}


		return true;
	}


	public boolean isEmpty() {
		if(this.parameters.keySet().size() == 0)
			return true;
		return false;
	}

	/**
	 * Set the value only if it doesn't exits.
	 */
	public void setDefaultValue(String name, String val) {
		if (this.getValue(name) == null)
			this.setValue(name, val);
	}

	public void setDefaultValues() {
		this.setDefaultValue("nodeAddress", "127.0.0.1");
		this.setDefaultValue("nodePort", "9481");
		this.setDefaultValue("maxSimultaneousDownloads", "-1");
		this.setDefaultValue("maxSimultaneousInsertions", "-1");
		this.setDefaultValue("maxUploadSpeed", "-1");
		this.setDefaultValue("thawId", "thaw_"+Integer.toString((new Random()).nextInt(1000)));
		this.setDefaultValue("advancedMode", "false");
		this.setDefaultValue("userNickname", "Another anonymous");
		this.setDefaultValue("multipleSockets", "true");
	}

}
