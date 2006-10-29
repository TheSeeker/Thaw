package thaw.core;

import java.util.Observer;
import java.util.Observable;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

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
	public final static int TIME_BETWEEN_EACH_TRY = 3000;


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
		return this.config;
	}

	/**
	 * Gives a ref to the object managing the splash screen.
	 */
	public SplashScreen getSplashScreen() {
		return this.splashScreen;
	}

	/**
	 * Gives a ref to the object managing the main window.
	 */
	public MainWindow getMainWindow() {
		return this.mainWindow;
	}

	/**
	 * Gives a ref to the object managing the config window.
	 */
	public ConfigWindow getConfigWindow() {
		return this.configWindow;
	}

	/**
	 * Gives a ref to the plugin manager.
	 */
	public PluginManager getPluginManager() {
		return this.pluginManager;
	}


	/**
	 * Here really start the program.
	 * @return true is success, false if not
	 */
	public boolean initAll() {
		this.splashScreen = new SplashScreen();

		this.splashScreen.display();

		this.splashScreen.setProgressionAndStatus(0, "Loading configuration ...");
		if(!this.initConfig())
			return false;

		this.splashScreen.setProgressionAndStatus(10, "Connecting ...");
		if(!this.initNodeConnection())
			new WarningWindow(this, I18n.getMessage("thaw.warning.unableToConnectTo")+
					  " "+this.config.getValue("nodeAddress")+
					  ":"+ this.config.getValue("nodePort"));


		this.splashScreen.setProgressionAndStatus(30, "Preparing the main window ...");
		if(!this.initGraphics())
			return false;

		this.splashScreen.setProgressionAndStatus(40, "Loading plugins ...");
		if(!this.initPluginManager())
			return false;

		this.splashScreen.setProgressionAndStatus(100, "Ready");


		this.mainWindow.setStatus("Thaw "+Main.VERSION+" : "+I18n.getMessage("thaw.statusBar.ready"));

		this.splashScreen.hide();

		this.mainWindow.setVisible(true);

		return true;
	}



	/**
	 * Init configuration. May re-set I18n.
	 */
	public boolean initConfig() {
		this.config = new Config();
		this.config.loadConfig();

		this.config.setDefaultValues();

		return true;
	}


	/**
	 * Init the connection to the node.
	 * If a connection is already established, it will disconnect, so
	 * if you called canDisconnect() before, then this function can be called safely.
	 * @see #canDisconnect()
	 */
	public boolean initNodeConnection() {
		if(this.getMainWindow() != null)
			this.getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.connecting"));

		try {
			if(this.queueManager != null)
				this.queueManager.stopScheduler();

			if(this.connection != null && this.connection.isConnected()) {
				this.disconnect();
			}

			if(this.connection == null) {
				this.connection = new FCPConnection(this.config.getValue("nodeAddress"),
							       Integer.parseInt(this.config.getValue("nodePort")),
							       Integer.parseInt(this.config.getValue("maxUploadSpeed")),
							       Boolean.valueOf(this.config.getValue("multipleSockets")).booleanValue());
			} else {
				this.connection.setNodeAddress(this.config.getValue("nodeAddress"));
				this.connection.setNodePort(Integer.parseInt(this.config.getValue("nodePort")));
				this.connection.setMaxUploadSpeed(Integer.parseInt(this.config.getValue("maxUploadSpeed")));
			}

			if(!this.connection.connect()) {
				Logger.warning(this, "Unable to connect !");
			}

			if(this.queryManager == null)
				this.queryManager = new FCPQueryManager(this.connection);

			if(this.queueManager == null)
				this.queueManager = new FCPQueueManager(this.queryManager,
								   this.config.getValue("thawId"),
								   Integer.parseInt(this.config.getValue("maxSimultaneousDownloads")),
								   Integer.parseInt(this.config.getValue("maxSimultaneousInsertions")));
			else {
				this.queueManager.setThawId(this.config.getValue("thawId"));
				this.queueManager.setMaxDownloads(Integer.parseInt(this.config.getValue("maxSimultaneousDownloads")));
				this.queueManager.setMaxInsertions(Integer.parseInt(this.config.getValue("maxSimultaneousInsertions")));

			}




			if(this.connection.isConnected()) {
				this.queryManager.startListening();

				QueueKeeper.loadQueue(this.queueManager, "thaw.queue.xml");


				this.clientHello = new FCPClientHello(this.queryManager, this.config.getValue("thawId"));

				if(!this.clientHello.start(null)) {
					Logger.warning(this, "Id already used !");
					this.connection.disconnect();
					new WarningWindow(this, "Unable to connect to "+this.config.getValue("nodeAddress")+":"+this.config.getValue("nodePort"));
					return false;
				} else {
					Logger.debug(this, "Hello successful");
					Logger.debug(this, "Node name    : "+this.clientHello.getNodeName());
					Logger.debug(this, "FCP  version : "+this.clientHello.getNodeFCPVersion());
					Logger.debug(this, "Node version : "+this.clientHello.getNodeVersion());

					this.queueManager.startScheduler();

					this.queueManager.restartNonPersistent();

					FCPWatchGlobal watchGlobal = new FCPWatchGlobal(true);
					watchGlobal.start(this.queueManager);

					FCPQueueLoader queueLoader = new FCPQueueLoader(this.config.getValue("thawId"));
					queueLoader.start(this.queueManager);

				}

			} else {
				return false;
			}

		} catch(Exception e) { /* A little bit not ... "nice" ... */
			Logger.warning(this, "Exception while connecting : "+e.toString()+" ; "+e.getMessage() + " ; "+e.getCause());
			e.printStackTrace();
			return false;
		}

		if(this.connection.isConnected())
			this.connection.addObserver(this);

		if(this.getMainWindow() != null)
			this.getMainWindow().setStatus(I18n.getMessage("thaw.statusBar.ready"));

		return true;
	}


	public FCPConnection getConnectionManager() {
		return this.connection;
	}

	public FCPQueueManager getQueueManager() {
		return this.queueManager;
	}

	/**
	 * FCPClientHello object contains all the information given by the node when the connection
	 * was initiated.
	 */
	public FCPClientHello getClientHello() {
		return this.clientHello;
	}


	/**
	 * To call before initGraphics() !
	 * @param lAndF LookAndFeel name
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
		this.initializeLookAndFeel();

		IconBox.loadIcons();

		this.mainWindow = new MainWindow(this);

		this.configWindow = new ConfigWindow(this);
		this.configWindow.setVisible(false);

		return true;
	}

	/**
	 * Init plugin manager.
	 */
	public boolean initPluginManager() {
		this.pluginManager = new PluginManager(this);

		if(!this.pluginManager.loadPlugins())
			return false;

		if(!this.pluginManager.runPlugins())
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
		Logger.info(this, "Disconnecting");
		this.connection.deleteObserver(this);
		this.connection.disconnect();

		Logger.info(this, "Saving queue state");
		QueueKeeper.saveQueue(this.queueManager, "thaw.queue.xml");
	}

	/**
	 * Check if the connection can be interrupted safely.
	 */
	public boolean canDisconnect() {
		return this.connection == null || !this.connection.isWriting();
	}

	/**
	 * End of the world.
	 * @param force if true, doesn't check if FCPConnection.isWritting().
	 * @see #exit()
	 */
	public void exit(boolean force) {
		if(!force) {
			if(!this.canDisconnect()) {
				if(!this.askDeconnectionConfirmation())
					return;
			}
		}

		Logger.info(this, "Stopping scheduler ...");
		if(this.queueManager != null)
		    this.queueManager.stopScheduler();

		Logger.info(this, "Hidding main window ...");
		this.mainWindow.setVisible(false);

		Logger.info(this, "Stopping plugins ...");
		this.pluginManager.stopPlugins();

		this.disconnect();

		Logger.info(this, "Saving configuration ...");
		if(!this.config.saveConfig()) {
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

		if(o == this.connection && !this.connection.isConnected()) {
			this.disconnect();

			int nmbReconnect = 0;

			JDialog warningDialog = new JDialog();
			warningDialog.setTitle("Thaw - reconnection");
			warningDialog.setModal(false);
			warningDialog.setSize(500, 40);

			warningDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

			JPanel warningPanel = new JPanel();

			JLabel warningLabel = new JLabel(I18n.getMessage("thaw.warning.autoreconnecting"),
							 SwingConstants.CENTER);
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

				if(this.initNodeConnection())
					break;
			}

			warningDialog.setVisible(false);


			if(nmbReconnect == MAX_CONNECT_TRIES) {
				while(!this.initNodeConnection()) {
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

			this.getPluginManager().stopPlugins();
			this.getPluginManager().loadPlugins();
			this.getPluginManager().runPlugins();
		}
	}

}
