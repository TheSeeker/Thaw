
package thaw.core;

import java.util.Observer;
import java.util.Observable;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import thaw.i18n.I18n;
import thaw.fcp.*;

/**
 * A "core" contains references to all the main parts of Thaw.
 *
 */
public class Core implements Observer {
	private MainWindow mainWindow = null;
	private Config config = null;
	private PluginManager pluginManager = null;
	private ConfigWindow configWindow = null;

	private FCPConnection connection = null;
	private FCPQueryManager queryManager = null;
	private FCPQueueManager queueManager = null;

	private FCPClientHello clientHello = null;

	private static String lookAndFeel = null;


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

		if(!initNodeConnection())
			return false;

		if(!initGraphics())
			return false;

		if(!initPluginManager())
			return false;

		mainWindow.setStatus("Thaw "+Main.VERSION+" : "+I18n.getMessage("thaw.statusBar.ready"));

		mainWindow.setVisible(true);

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
	 * Init the connection to the node.
	 * If a connection is already established, it will disconnect, so this function could be call safely later.
	 */
	public boolean initNodeConnection() {
		if(getMainWindow() != null)
			getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.connecting"));

		try {

			if(connection != null && connection.isConnected()) {
				connection.deleteObserver(this);
				connection.disconnect();
			}
			
			connection = new FCPConnection(config.getValue("nodeAddress"),
						       (new Integer(config.getValue("nodePort"))).intValue(),
						       (new Integer(config.getValue("maxUploadSpeed"))).intValue());
			
			if(!connection.connect()) {
				new WarningWindow(this, "Unable to connect to "+config.getValue("nodeAddress")+":"+
						  config.getValue("nodePort"));
				
				/* Not returning false,
				   else it will break the loading */
			}
			
			queryManager = new FCPQueryManager(connection);
			queueManager = new FCPQueueManager(queryManager,
							   config.getValue("thawId"),
							   (new Integer(config.getValue("maxSimultaneousDownloads"))).intValue(),
							   (new Integer(config.getValue("maxSimultaneousInsertions"))).intValue());

			if(connection.isConnected()) {
				queryManager.startListening();

				QueueKeeper.loadQueue(queueManager, "thaw.queue.xml");


				clientHello = new FCPClientHello(queryManager, config.getValue("thawId"));
				
				if(!clientHello.start(null)) {
					new WarningWindow(this, I18n.getMessage("thaw.error.idAlreadyUsed"));
					connection.disconnect();
				} else {
					Logger.debug(this, "Hello successful");
					Logger.debug(this, "Node name    : "+clientHello.getNodeName());
					Logger.debug(this, "FCP  version : "+clientHello.getNodeFCPVersion());
					Logger.debug(this, "Node version : "+clientHello.getNodeVersion());
					
					queueManager.startScheduler();

					queueManager.restartNonPersistent();
					
					FCPWatchGlobal watchGlobal = new FCPWatchGlobal(true);
					watchGlobal.start(queueManager);
					
					FCPQueueLoader queueLoader = new FCPQueueLoader();
					queueLoader.start(queueManager, config.getValue("thawId"));
					
				}
								   
			}

		} catch(Exception e) { /* A little bit not ... "nice" ... */
			Logger.warning(this, "Exception while connecting : "+e.toString()+" ; "+e.getMessage() + " ; "+e.getCause());
			e.printStackTrace();
			new WarningWindow(this, "Unable to connect to the node. Please check your configuration.");
		}

		if(connection.isConnected())
			connection.addObserver(this);

		if(getMainWindow() != null)
			getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.ready"));

		return true;
	}


	public FCPConnection getConnectionManager() {
		return connection;
	}

	public FCPQueueManager getQueueManager() {
		return queueManager;
	}

	/**
	 * FCPClientHello object contains all the information given by the node when the connection
	 * was initiated.
	 */
	public FCPClientHello getClientHello() {
		return clientHello;
	}

	
	/**
	 * To call before initGraphics() !
	 * @arg lAndF LookAndFeel name
	 */
	public static void setLookAndFeel(String lAndF) {
		lookAndFeel = lAndF;
	}


	/**
	 * This method sets the look and feel specified with setLookAndFeel().
	 * If none was specified, the System Look and Feel is set.
	 */
	private void initializeLookAndFeel() { /* non static, else I can't call correctly Logger functions */

		JFrame.setDefaultLookAndFeelDecorated(false); /* Don't touch my window decorations ! */
		JDialog.setDefaultLookAndFeelDecorated(false);

		try {
			if (lookAndFeel == null) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} else {
				UIManager.setLookAndFeel(lookAndFeel);
			}
		} catch (Exception e) {
			Logger.warning(this, "Exception while setting the L&F : " + e.getMessage());
			Logger.warning(this, "Using the default lookAndFeel");
		}

	}


	/**
	 * Init graphics.
	 */
	public boolean initGraphics() {
		initializeLookAndFeel();

		mainWindow = new MainWindow(this);

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
		Logger.info(this, "Stopping scheduler ...");
		if(queueManager != null)
		    queueManager.stopScheduler();
		
		Logger.info(this, "Stopping plugins ...");
		pluginManager.stopPlugins();

		Logger.info(this, "Disconnecting ...");
		connection.deleteObserver(this);
		connection.disconnect();

		Logger.info(this, "Saving queue state ...");
		QueueKeeper.saveQueue(queueManager, "thaw.queue.xml");

		Logger.info(this, "Saving configuration ...");
		if(!config.saveConfig()) {
			Logger.error(this, "Config was not saved correctly !");
		}

		Logger.info(this, "Exiting");
		System.exit(0);
	}



	public void update(Observable o, Object target) {
		Logger.debug(this, "Move on the connection (?)");

		if(o == connection && !connection.isConnected()) {
			new WarningWindow(this, "We have been disconnected");
			connection.deleteObserver(this);
		}
	}

}
