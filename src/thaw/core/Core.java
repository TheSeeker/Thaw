package thaw.core;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import thaw.fcp.FCPClientHello;
import thaw.fcp.FCPConnection;
import thaw.fcp.FCPQueryManager;
import thaw.fcp.FCPQueueLoader;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPWatchGlobal;

import thaw.gui.IconBox;

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
	public final static int TIME_BETWEEN_EACH_TRY = 5000;

	private ReconnectionManager reconnectionManager = null;


	/**
	 * Creates a core, but do nothing else (no initialization).
	 */
	public Core() {
		Logger.info(this, "Thaw, version "+Main.VERSION, true);
		Logger.info(this, "2006(c) Freenet project", true);
		Logger.info(this, "Released under GPL license version 2 or later (see http://www.fsf.org/licensing/licenses/gpl.html)", true);
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
		IconBox.loadIcons();

		splashScreen = new SplashScreen();

		splashScreen.display();

		splashScreen.setProgressionAndStatus(0, "Loading configuration ...");
		splashScreen.addIcon(IconBox.settings);
		if(!initConfig())
			return false;

		splashScreen.setProgressionAndStatus(10, "Connecting ...");
		splashScreen.addIcon(IconBox.connectAction);
		if(!initConnection())
			new WarningWindow(this, I18n.getMessage("thaw.warning.unableToConnectTo")+
					  " "+ config.getValue("nodeAddress")+
					  ":"+ config.getValue("nodePort"));


		splashScreen.setProgressionAndStatus(30, "Preparing the main window ...");
		splashScreen.addIcon(IconBox.mainWindow);
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
		config = new Config(Config.CONFIG_FILE_NAME);

		if(!config.loadConfig()){
			config.setDefaultValues();
		}

		return true;
	}


	/**
	 * Init the connection to the node.
	 * If a connection is already established, it will disconnect, so
	 * if you called canDisconnect() before, then this function can be called safely.
	 * @see #canDisconnect()
	 */
	public boolean initConnection() {
		boolean ret = true;

		if(getMainWindow() != null) {
			getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.connecting"), java.awt.Color.RED);

		}

		try {
			if(queueManager != null)
				queueManager.stopScheduler();

			if((connection != null) && connection.isConnected()) {
				subDisconnect();
			}

			if(connection == null) {
				connection = new FCPConnection(config.getValue("nodeAddress"),
							       Integer.parseInt(config.getValue("nodePort")),
							       Integer.parseInt(config.getValue("maxUploadSpeed")),
							       Boolean.valueOf(config.getValue("multipleSockets")).booleanValue(),
							       Boolean.valueOf(config.getValue("sameComputer")).booleanValue());
			} else { /* connection is not recreate to avoid troubles with possible observers etc */
				connection.deleteObserver(this);
				connection.setNodeAddress(config.getValue("nodeAddress"));
				connection.setNodePort(Integer.parseInt(config.getValue("nodePort")));
				connection.setMaxUploadSpeed(Integer.parseInt(config.getValue("maxUploadSpeed")));
				connection.setDuplicationAllowed(Boolean.valueOf(config.getValue("multipleSockets")).booleanValue());
				connection.setLocalSocket(Boolean.valueOf(config.getValue("sameComputer")).booleanValue());
			}

			if(!connection.connect()) {
				Logger.warning(this, "Unable to connect !");
				ret = false;
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




			if(ret && connection.isConnected()) {
				queryManager.startListening();

				QueueKeeper.loadQueue(queueManager, "thaw.queue.xml");


				clientHello = new FCPClientHello(queryManager, config.getValue("thawId"));

				if(!clientHello.start(null)) {
					Logger.warning(this, "Id already used !");
					subDisconnect();
					ret = false;
				} else {
					Logger.debug(this, "Hello successful");
					Logger.debug(this, "Node name    : "+clientHello.getNodeName());
					Logger.debug(this, "FCP  version : "+clientHello.getNodeFCPVersion());
					Logger.debug(this, "Node version : "+clientHello.getNodeVersion());

					queueManager.startScheduler();

					queueManager.restartNonPersistent();

					final FCPWatchGlobal watchGlobal = new FCPWatchGlobal(true);
					watchGlobal.start(queueManager);

					final FCPQueueLoader queueLoader = new FCPQueueLoader(config.getValue("thawId"));
					queueLoader.start(queueManager);

				}
			}

		} catch(final Exception e) { /* A little bit not ... "nice" ... */
			Logger.warning(this, "Exception while connecting : "+e.toString()+" ; "+e.getMessage() + " ; "+e.getCause());
			e.printStackTrace();
			return false;
		}

		if(ret && connection.isConnected())
			connection.addObserver(this);

		if(getMainWindow() != null) {
			if (ret)
				getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.ready"));
			else
				getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.disconnected"), java.awt.Color.RED);
		}

		return ret;
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
	 * @param lAndF LookAndFeel name
	 */
	public static void setLookAndFeel(final String lAndF) {
		Core.lookAndFeel = lAndF;
	}


	/**
	 * This method sets the look and feel specified with setLookAndFeel().
	 * If none was specified, the System Look and Feel is set.
	 */
	private void initializeLookAndFeel() { /* non static, else I can't call correctly Logger functions */

		JFrame.setDefaultLookAndFeelDecorated(false); /* Don't touch my window decorations ! */
		JDialog.setDefaultLookAndFeelDecorated(false);

		try {
			if (Core.lookAndFeel == null) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} else {
				UIManager.setLookAndFeel(Core.lookAndFeel);
			}
		} catch (final Exception e) {
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
		this.exit(false);
	}


	/**
	 * Makes things nicely ... :)
	 */
	public void disconnect() {
		if (reconnectionManager != null) {
			reconnectionManager.stop();
			reconnectionManager = null;
		}

		subDisconnect();
	}


	public void subDisconnect() {
		Logger.info(this, "Disconnecting");

		if (mainWindow != null) {
			mainWindow.setStatus(I18n.getMessage("thaw.statusBar.disconnected"), java.awt.Color.RED);

			/* not null because we want to force the cleaning */
			mainWindow.changeButtonsInTheToolbar(this, new java.util.Vector());
		}

		if (connection != null) {
			connection.deleteObserver(this);
			connection.disconnect();
			Logger.info(this, "Saving queue state");
			QueueKeeper.saveQueue(queueManager, "thaw.queue.xml");
		} else {
			Logger.warning(this, "No connection ?!");
		}
	}

	/**
	 * Check if the connection can be interrupted safely.
	 */
	public boolean canDisconnect() {
		return (connection == null) || !connection.isWriting();
	}

	/**
	 * End of the world.
	 * @param force if true, doesn't check if FCPConnection.isWriting().
	 * @see #exit()
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
		final int ret = JOptionPane.showOptionDialog((java.awt.Component)null,
						       I18n.getMessage("thaw.warning.isWriting"),
						       "Thaw - "+I18n.getMessage("thaw.warning.title"),
						       JOptionPane.YES_NO_OPTION,
						       JOptionPane.WARNING_MESSAGE,
						       (javax.swing.Icon)null,
						       (java.lang.Object[])null,
						       (java.lang.Object)null);
		if((ret == JOptionPane.CLOSED_OPTION)
		   || (ret == JOptionPane.CANCEL_OPTION)
		   || (ret == JOptionPane.NO_OPTION))
			return false;

		return true;
	}


	protected class ReconnectionManager implements Runnable {
		private boolean running = true;

		public ReconnectionManager() {
			running = true;
		}

		public void run() {
			Logger.notice(this, "Starting reconnection process !");

			getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.connecting"), java.awt.Color.RED);
			getPluginManager().stopPlugins(); /* don't forget there is the status bar plugin */
			getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.connecting"), java.awt.Color.RED);

			subDisconnect();

			while(running) {
				try {
					Thread.sleep(Core.TIME_BETWEEN_EACH_TRY);
				} catch(final java.lang.InterruptedException e) {
					// brouzouf
				}

				Logger.notice(this, "Trying to reconnect ...");
				if(initConnection())
					break;
			}

			if (running) {
				getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.ready"));
			} else {
				getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.disconnected"), java.awt.Color.RED);
			}

			if (running) {
				getPluginManager().loadPlugins();
				getPluginManager().runPlugins();
			}

			reconnectionManager = null;

			getMainWindow().connectionHasChanged();
		}

		public void stop() {
			Logger.warning(this, "Canceling reconnection ...");
			running = false;
		}
	}

	/**
	 * use Thread => will also do all the work related to the plugins
	 */
	public void reconnect() {
		if (reconnectionManager == null) {
			reconnectionManager = new ReconnectionManager();
			final Thread th = new Thread(reconnectionManager);
			th.start();
		} else {
			Logger.warning(this, "Already trying to reconnect !");
		}
	}


	public boolean isReconnecting() {
		return (reconnectionManager != null);
	}


	public void update(final Observable o, final Object target) {
		Logger.debug(this, "Move on the connection (?)");

		if((o == connection) && !connection.isConnected()) {
			reconnect();
		}
	}

}
