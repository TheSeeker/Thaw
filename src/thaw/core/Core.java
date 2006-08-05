package thaw.core;

import java.util.Observer;
import java.util.Observable;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.JOptionPane;

import thaw.fcp.*;

/**
 * A "core" contains references to all the main parts of Thaw.
 * The Core has all the functions needed to initialize Thaw / stop Thaw.
 */
public class Core implements Observer {
	private SplashScreen splashScreen = null;

	private MainWindow mainWindow = null;
	private Config config = null;
	private PluginManager pluginManager = null;
	private ConfigWindow configWindow = null;

	private FCPConnection connection = null;
	private FCPQueryManager queryManager = null;
	private FCPQueueManager queueManager = null;

	private FCPClientHello clientHello = null;

	private static String lookAndFeel = null;

	public final static int MAX_CONNECT_TRIES = 3;
	public final static int TIME_BETWEEN_EACH_TRY = 2500;


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
	 * Gives a ref to the object managing the splash screen.
	 */
	public SplashScreen getSplashScreen() {
		return splashScreen;
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
		splashScreen = new SplashScreen();

		splashScreen.display();

		splashScreen.setProgressionAndStatus(0, "Loading configuration ...");
		if(!initConfig())
			return false;

		splashScreen.setProgressionAndStatus(10, "Connecting ...");
		if(!initNodeConnection())
			new WarningWindow(this, I18n.getMessage("thaw.warning.unableToConnectTo")+
					  " "+config.getValue("nodeAddress")+
					  ":"+ config.getValue("nodePort"));


		splashScreen.setProgressionAndStatus(30, "Preparing the main window ...");
		if(!initGraphics())
			return false;

		splashScreen.setProgressionAndStatus(40, "Loading plugins ...");
		if(!initPluginManager())
			return false;

		splashScreen.setProgressionAndStatus(100, "Ready");


		mainWindow.setStatus("Thaw "+Main.VERSION+" : "+I18n.getMessage("thaw.statusBar.ready"));

		splashScreen.hide();

		mainWindow.setVisible(true);

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
	 * If a connection is already established, it will disconnect, so 
	 * if you called canDisconnect() before, then this function can be called safely.
	 * @see #canDisconnect()
	 */
	public boolean initNodeConnection() {
		if(getMainWindow() != null)
			getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.connecting"));

		try {
			if(queueManager != null)
				queueManager.stopScheduler();

			if(connection != null && connection.isConnected()) {
				disconnect();
			}
			
			if(connection == null) {
				connection = new FCPConnection(config.getValue("nodeAddress"),
							       Integer.parseInt(config.getValue("nodePort")),
							       Integer.parseInt(config.getValue("maxUploadSpeed")));
			} else {
				connection.setNodeAddress(config.getValue("nodeAddress"));
				connection.setNodePort(Integer.parseInt(config.getValue("nodePort")));
				connection.setMaxUploadSpeed(Integer.parseInt(config.getValue("maxUploadSpeed")));
			}
			
			if(!connection.connect()) {
				Logger.warning(this, "Unable to connect !");
			}
			
			if(queryManager == null)
				queryManager = new FCPQueryManager(connection);
			
			if(queueManager == null)
				queueManager = new FCPQueueManager(queryManager,
								   config.getValue("thawId"),
								   Integer.parseInt(config.getValue("maxSimultaneousDownloads")),
								   Integer.parseInt(config.getValue("maxSimultaneousInsertions")));
			else {
				queueManager.setThawId(config.getValue("thawId"));
				queueManager.setMaxDownloads(Integer.parseInt(config.getValue("maxSimultaneousDownloads")));
				queueManager.setMaxInsertions(Integer.parseInt(config.getValue("maxSimultaneousInsertions")));
				
			}
				



			if(connection.isConnected()) {
				queryManager.startListening();

				QueueKeeper.loadQueue(queueManager, "thaw.queue.xml");


				clientHello = new FCPClientHello(queryManager, config.getValue("thawId"));
				
				if(!clientHello.start(null)) {
					Logger.warning(this, "Id already used !");
					connection.disconnect();
					new WarningWindow(this, "Unable to connect to "+config.getValue("nodeAddress")+":"+config.getValue("nodePort"));
					return false;
				} else {
					Logger.debug(this, "Hello successful");
					Logger.debug(this, "Node name    : "+clientHello.getNodeName());
					Logger.debug(this, "FCP  version : "+clientHello.getNodeFCPVersion());
					Logger.debug(this, "Node version : "+clientHello.getNodeVersion());
					
					queueManager.startScheduler();

					queueManager.restartNonPersistent();
					
					FCPWatchGlobal watchGlobal = new FCPWatchGlobal(true);
					watchGlobal.start(queueManager);
					
					FCPQueueLoader queueLoader = new FCPQueueLoader(config.getValue("thawId"));
					queueLoader.start(queueManager);
					
				}
								   
			} else {
				return false;
			}

		} catch(Exception e) { /* A little bit not ... "nice" ... */
			Logger.warning(this, "Exception while connecting : "+e.toString()+" ; "+e.getMessage() + " ; "+e.getCause());
			e.printStackTrace();
			return false;
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

		IconBox.loadIcons();

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
		exit(false);
	}


	/**
	 * Makes things nicely ... :)
	 */
	public void disconnect() {
		Logger.info(this, "Disconnecting");
		connection.deleteObserver(this);
		connection.disconnect();

		Logger.info(this, "Saving queue state");
		QueueKeeper.saveQueue(queueManager, "thaw.queue.xml");
	}

	/**
	 * Check if the connection can be interrupted safely.
	 */
	public boolean canDisconnect() {
		return connection == null || !connection.isWriting();
	}

	/**
	 * End of the world.
	 * @param force if true, doesn't check if FCPConnection.isWritting().
	 * @see exit()
	 */
	public void exit(boolean force) {
		if(!force) {
			if(!canDisconnect()) {
				if(!askDeconnectionConfirmation())
					return;
			}
		}

		Logger.info(this, "Stopping scheduler ...");
		if(queueManager != null)
		    queueManager.stopScheduler();

		Logger.info(this, "Hidding main window ...");
		mainWindow.setVisible(false);
		
		Logger.info(this, "Stopping plugins ...");
		pluginManager.stopPlugins();

		disconnect();

		Logger.info(this, "Saving configuration ...");
		if(!config.saveConfig()) {
			Logger.error(this, "Config was not saved correctly !");
		}

		Logger.info(this, "Exiting");
		System.exit(0);
	}


	public boolean askDeconnectionConfirmation() {
		int ret = JOptionPane.showOptionDialog((java.awt.Component)null,
						       I18n.getMessage("thaw.warning.isWriting"),
						       "Thaw - "+I18n.getMessage("thaw.warning.title"),
						       JOptionPane.YES_NO_OPTION, 
						       JOptionPane.WARNING_MESSAGE,
						       (javax.swing.Icon)null,
						       (java.lang.Object[])null,
						       (java.lang.Object)null);
		if(ret == JOptionPane.CLOSED_OPTION
		   || ret == JOptionPane.CANCEL_OPTION
		   || ret == JOptionPane.NO_OPTION)
			return false;

		return true;
	}

	public void update(Observable o, Object target) {
		Logger.debug(this, "Move on the connection (?)");

		if(o == connection && !connection.isConnected()) {
			disconnect();

			int nmbReconnect = 0;

			JDialog warningDialog = new JDialog();
			warningDialog.setTitle("Thaw - reconnection");
			warningDialog.setModal(false);
			warningDialog.setSize(500, 40);

			JPanel warningPanel = new JPanel();

			JLabel warningLabel = new JLabel(I18n.getMessage("thaw.warning.autoreconnecting"),
							 JLabel.CENTER);
			warningPanel.add(warningLabel);
			warningDialog.setContentPane(warningPanel);
			
			warningDialog.setVisible(true);

			for(nmbReconnect = 0;
			    nmbReconnect < MAX_CONNECT_TRIES ;
			    nmbReconnect++) {
				
				try {
					Thread.sleep(TIME_BETWEEN_EACH_TRY);
				} catch(java.lang.InterruptedException e) {
					// brouzouf
				}
				
				Logger.notice(this, "Trying to reconnect ... : "+ Integer.toString(nmbReconnect));

				if(initNodeConnection())
					break;
			}

			warningDialog.setVisible(false);

			
			if(nmbReconnect == MAX_CONNECT_TRIES) {
				while(!initNodeConnection()) {
					int ret = JOptionPane.showOptionDialog((java.awt.Component)null,
									       I18n.getMessage("thaw.warning.disconnected"),
									       "Thaw - "+I18n.getMessage("thaw.warning.title"),
									       JOptionPane.YES_NO_OPTION, 
									       JOptionPane.WARNING_MESSAGE,
									       (javax.swing.Icon)null,
									       (java.lang.Object[])null,
									       (java.lang.Object)null);
				if(ret == JOptionPane.CLOSED_OPTION
				   || ret == JOptionPane.CANCEL_OPTION
				   || ret == JOptionPane.NO_OPTION)
					break;
				}

			}

			getPluginManager().stopPlugins();
			getPluginManager().loadPlugins();
			getPluginManager().runPlugins();
		}
	}

}
