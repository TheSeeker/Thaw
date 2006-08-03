package thaw.core;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.Icon;
import java.awt.Font;
import javax.swing.Box;


/**
 * MainWindow. This class create the main window.
 *
 * Main window is divided in three parts:
 *
 * <pre>
 * ------------------------------------
 * | MenuBar                          |
 * ------------------------------------
 * | ToolBar                          |
 * ------------------------------------
 * | Tab 1 | Tab 2 | Tab 3 |          |
 * |----------------------------------|
 * | Tab content                      |
 * |                                  |
 * |                                  |
 * |                                  |
 * |                                  |
 * ------------------------------------
 * | JLabel (status)                  |
 * ------------------------------------
 * </pre>
 *
 * @author <a href="mailto:jflesch@nerim.net">Jerome Flesch</a>
 */
public class MainWindow implements java.awt.event.ActionListener, java.awt.event.WindowListener,
				   java.util.Observer {

	private JFrame mainWindow = null;

	private JMenuBar menuBar = null;
	private JMenu fileMenu = null;

	private JMenuItem reconnectionFileMenuItem = null;
	private JMenuItem optionsFileMenuItem = null;
	private JMenuItem quitFileMenuItem = null;

	private JMenu helpMenu = null;
	private JMenuItem aboutHelpMenuItem = null;

	private JToolBar toolBar = null;
	private JButton connectButton = null;
	private JButton disconnectButton = null;
	private JButton settingsButton = null;
	private JButton quitButton = null;
	

	private JTabbedPane tabbedPane = null;
	private JLabel statusBar = null;

	private Core core = null; /* core is called back when exit() */


	/**
	 * Creates a new <code>MainWindow</code> instance, and so a new Swing window.
	 * @param core a <code>Core</code> value
	 */
	public MainWindow(Core core) {
		this.core = core;

		mainWindow = new JFrame("Thaw");

		mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		mainWindow.setVisible(false);

		try {
			mainWindow.setIconImage(IconBox.blueBunny.getImage());
		} catch(Throwable e) {
			Logger.notice(this, "No icon");
		}
		
		// MENUS
	
		menuBar = new JMenuBar();
		fileMenu = new JMenu(I18n.getMessage("thaw.menu.file"));

		reconnectionFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.reconnect"),
							 IconBox.minReconnectAction);
		optionsFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.options"),
						    IconBox.minSettings);
		quitFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.quit"),
						 IconBox.minQuitAction);
		
		reconnectionFileMenuItem.addActionListener(this);
		optionsFileMenuItem.addActionListener(this);
		quitFileMenuItem.addActionListener(this);

		fileMenu.add(reconnectionFileMenuItem);
		fileMenu.add(optionsFileMenuItem);
		fileMenu.add(quitFileMenuItem);

		menuBar.add(fileMenu);

		helpMenu = new JMenu(I18n.getMessage("thaw.menu.help"));
		
		aboutHelpMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.about"));
		aboutHelpMenuItem.addActionListener(this);

		helpMenu.add(aboutHelpMenuItem);

		menuBar.add(Box.createHorizontalGlue());
		menuBar.add(helpMenu);

		// TOOLBAR
		toolBar = new JToolBar(I18n.getMessage("thaw.toolbar.title"));

		connectButton = new JButton(IconBox.connectAction);
		connectButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.connect"));
		disconnectButton = new JButton(IconBox.disconnectAction);
		disconnectButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.disconnect"));

		settingsButton = new JButton(IconBox.settings);
		settingsButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.settings"));

		quitButton = new JButton(IconBox.quitAction);
		quitButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.quit"));
		

		connectButton.addActionListener(this);
		disconnectButton.addActionListener(this);
		settingsButton.addActionListener(this);
		quitButton.addActionListener(this);

		toolBar.add(connectButton);
		toolBar.add(disconnectButton);
		toolBar.addSeparator();
		toolBar.add(settingsButton);
		toolBar.addSeparator();
		toolBar.add(quitButton);

		updateToolBar();

		// TABBED PANE
		
		tabbedPane = new JTabbedPane();

		// STATUS BAR
		
		statusBar = new JLabel();
		setStatus(null);
		statusBar.setSize(500, 30);


		mainWindow.getContentPane().setLayout(new BorderLayout());

		mainWindow.setJMenuBar(menuBar);
		mainWindow.getContentPane().add(toolBar, BorderLayout.NORTH);
		mainWindow.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		mainWindow.getContentPane().add(statusBar, BorderLayout.SOUTH);

		mainWindow.setSize(790, 550);
		
		mainWindow.addWindowListener(this);

		core.getConnectionManager().addObserver(this);
	}


	/**
	 * Make the window visible or not.
	 */
	public void setVisible(boolean v) {
		mainWindow.setVisible(v);
	}

	
	public JFrame getMainFrame() {
		return mainWindow;
	}


	/** 
	 * Should not be used.
	 * @see #addTab(String, java.awt.Component)
	 * @return In the future, it's possible that it will sometimes return null.
	 */
	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}

	/**
	 * Used to add a tab in the main window.
	 * In the future, even if the interface, this function should remain available.
	 */
	public boolean addTab(String tabName, java.awt.Component panel) {
		tabbedPane.addTab(tabName, panel);

		return true;
	}

	/**
	 * @see #addTab(String, java.awt.Component)
	 */
	public boolean addTab(String tabName, Icon icon, java.awt.Component panel) {
		tabbedPane.addTab(tabName, icon, panel);

		return true;
	}

	
	/**
	 * Used to remove a tab from the main window.
	 */
	public boolean removeTab(java.awt.Component panel) {
		tabbedPane.remove(panel);

		return true;
	}

	
	/**
	 * Used by plugins to add their own menu. Not recommanded for the moment.
	 * Need to find a more elegant way.
	 * @return Check it does not return null.
	 */
	public JMenuBar getMenuBar() {
		return menuBar;
	}


	/**
	 * Called when an element from the menu is called.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == connectButton) {
			core.getPluginManager().stopPlugins();			

			if(!core.initNodeConnection())
				unableToConnect();

			core.getPluginManager().runPlugins();
		}

		if(e.getSource() == disconnectButton) {
			if(!core.canDisconnect()) {
				if(!core.askDeconnectionConfirmation())
					return;
			}

			core.getPluginManager().stopPlugins();

			core.disconnect();

			core.getPluginManager().runPlugins();
		}

		if(e.getSource() == settingsButton) {
			core.getConfigWindow().setVisible(true);
		}

		if(e.getSource() == quitButton) {
			core.exit();
		}

		if(e.getSource() == reconnectionFileMenuItem) {

			if(!core.canDisconnect()) {
				if(!core.askDeconnectionConfirmation())
					return;
			}

			core.getPluginManager().stopPlugins();
			
			if(!core.initNodeConnection())
				unableToConnect();
			
			core.getPluginManager().loadPlugins();
			core.getPluginManager().runPlugins();

		}

		if(e.getSource() == optionsFileMenuItem) {
			core.getConfigWindow().setVisible(true);
		}

		
		if(e.getSource() == quitFileMenuItem) {
			endOfTheWorld();
		}

		if(e.getSource() == aboutHelpMenuItem) {
			showDialogAbout();
		}
	}

	/**
	 * Warns the user by a popup.
	 */
	protected void unableToConnect() {
		new WarningWindow(core, 
				  I18n.getMessage("thaw.warning.unableToConnectTo")+
				  " "+core.getConfig().getValue("nodeAddress")+":"+ core.getConfig().getValue("nodePort"));
	}

	public void update(java.util.Observable o, Object arg) {
		updateToolBar();
	}


	public void updateToolBar() {
		if(core.getConnectionManager().isConnected()) {
			connectButton.setEnabled(false);
			disconnectButton.setEnabled(true);
		} else {
			connectButton.setEnabled(true);
			disconnectButton.setEnabled(false);
		}
	}

	/**
	 * Called when window is closed or 'quit' is choosed is the menu.
	 */
	public void endOfTheWorld() {
		if(core == null) {
			Logger.error(this, "Warning, no ref to core, exiting brutaly");
			System.exit(0);
		} else {
			core.exit();
		}	
	}


	/**
	 * Change text in the status bar.
	 * @param status Null is accepted.
	 */
	public void setStatus(String status) {
		if(status != null)
			statusBar.setText(status);
		else
			statusBar.setText(" ");/* not empty else the status bar disappear */
	}


	public String getStatus() {
		return statusBar.getText();
	}


	public void showDialogAbout() {
		JLabel[] labels = new JLabel[] {
			new JLabel(I18n.getMessage("thaw.about.l1")),
			new JLabel(I18n.getMessage("thaw.about.l2")),
			new JLabel(I18n.getMessage("thaw.about.l3")),
			new JLabel(I18n.getMessage("thaw.about.l4")),
			new JLabel(""),
			new JLabel(I18n.getMessage("thaw.about.l6"))
		};

		labels[0].setFont(new Font("Dialog", Font.BOLD, 30));

		JOptionPane.showMessageDialog(null, labels, I18n.getMessage("thaw.about.title"),
					      JOptionPane.INFORMATION_MESSAGE);
		
	}

	
	public void windowActivated(WindowEvent e) {

	}

	public void windowClosing(WindowEvent e) {
		/* Should be in windowClosed(), but doesn't seem to work */
		if(e.getSource() == mainWindow)
			endOfTheWorld();
	}

	public void windowClosed(WindowEvent e) {
		// add potential warnings here
	}

	public void windowDeactivated(WindowEvent e) {
		// C'est pas comme si on en avait quelque chose à foutre :p
	}

	public void windowDeiconified(WindowEvent e) {
		// idem
	}

	public void windowIconified(WindowEvent e) {
		// idem
	}

	public void windowOpened(WindowEvent e) {
		// idem
	}

}
