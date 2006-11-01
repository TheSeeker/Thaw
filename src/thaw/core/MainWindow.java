package thaw.core;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.Icon;
import java.awt.Font;

import java.util.Vector;
import java.util.Iterator;


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
 * |                                  |
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

	private Object lastToolBarModifier = null;

	/**
	 * Creates a new <code>MainWindow</code> instance, and so a new Swing window.
	 * @param core a <code>Core</code> value
	 */
	public MainWindow(Core core) {
		this.core = core;

		this.mainWindow = new JFrame("Thaw");

		this.mainWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		this.mainWindow.setVisible(false);

		try {
			this.mainWindow.setIconImage(IconBox.blueBunny.getImage());
		} catch(Throwable e) {
			Logger.notice(this, "No icon");
		}

		// MENUS

		this.menuBar = new JMenuBar();
		this.fileMenu = new JMenu(I18n.getMessage("thaw.menu.file"));

		this.reconnectionFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.reconnect"),
							 IconBox.minReconnectAction);
		this.optionsFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.options"),
						    IconBox.minSettings);
		this.quitFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.quit"),
						 IconBox.minQuitAction);

		this.reconnectionFileMenuItem.addActionListener(this);
		this.optionsFileMenuItem.addActionListener(this);
		this.quitFileMenuItem.addActionListener(this);

		this.fileMenu.add(this.reconnectionFileMenuItem);
		this.fileMenu.add(this.optionsFileMenuItem);
		this.fileMenu.add(this.quitFileMenuItem);

		this.menuBar.add(this.fileMenu);

		this.helpMenu = new JMenu(I18n.getMessage("thaw.menu.help"));

		this.aboutHelpMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.about"));
		this.aboutHelpMenuItem.addActionListener(this);

		this.helpMenu.add(this.aboutHelpMenuItem);

		//menuBar.add(Box.createHorizontalGlue());
		this.menuBar.add(this.helpMenu);

		// TOOLBAR
		this.connectButton = new JButton(IconBox.connectAction);
		this.connectButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.connect"));
		this.disconnectButton = new JButton(IconBox.disconnectAction);
		this.disconnectButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.disconnect"));

		this.settingsButton = new JButton(IconBox.settings);
		this.settingsButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.settings"));

		this.quitButton = new JButton(IconBox.quitAction);
		this.quitButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.quit"));

		this.connectButton.addActionListener(this);
		this.disconnectButton.addActionListener(this);
		this.settingsButton.addActionListener(this);
		this.quitButton.addActionListener(this);


		// TABBED PANE

		this.tabbedPane = new JTabbedPane();

		// STATUS BAR

		this.statusBar = new JLabel();
		this.setStatus(null);
		this.statusBar.setSize(500, 30);


		this.mainWindow.getContentPane().setLayout(new BorderLayout());

		this.mainWindow.setJMenuBar(this.menuBar);

		/* Toolbar adding: */ changeButtonsInTheToolbar(this, null);
		this.mainWindow.getContentPane().add(this.tabbedPane, BorderLayout.CENTER);
		this.mainWindow.getContentPane().add(this.statusBar, BorderLayout.SOUTH);

		this.mainWindow.setSize(790, 550);

		this.mainWindow.addWindowListener(this);

		core.getConnectionManager().addObserver(this);
	}


	/**
	 * Make the window visible or not.
	 */
	public void setVisible(boolean v) {
		this.mainWindow.setVisible(v);
	}


	public JFrame getMainFrame() {
		return this.mainWindow;
	}


	/**
	 * Should not be used.
	 * @see #addTab(String, java.awt.Component)
	 * @return In the future, it's possible that it will sometimes return null.
	 */
	public JTabbedPane getTabbedPane() {
		return this.tabbedPane;
	}

	/**
	 * @param modifier Correspond to the caller object: it's a security to avoid that a modifier wipe out the buttons from another one
	 */
	public void changeButtonsInTheToolbar(Object modifier, Vector newButtons) {
		JToolBar newToolBar;

		Logger.debug(this, "Called by "+modifier.getClass().getName());

		if (lastToolBarModifier == null || newButtons != null || lastToolBarModifier == modifier) {
			lastToolBarModifier = modifier;
		} else {
			/* Only the modifer who added the buttons can remove them */
			return;
		}

		newToolBar = new JToolBar(I18n.getMessage("thaw.toolbar.title"));

		newToolBar.add(this.connectButton);
		newToolBar.add(this.disconnectButton);
		newToolBar.addSeparator();
		newToolBar.add(this.settingsButton);
		newToolBar.addSeparator();

		if (newButtons != null) {
			for (Iterator it = newButtons.iterator();
			     it.hasNext();) {
				JButton button = (JButton)it.next();
				if (button != null)
					newToolBar.add(button);
				else
					newToolBar.addSeparator();
			}
			newToolBar.addSeparator();
		}

		newToolBar.add(this.quitButton);
		newToolBar.setFloatable(false);

		if (this.toolBar != null)
			this.mainWindow.getContentPane().remove(this.toolBar);
		this.toolBar = newToolBar;
		this.mainWindow.getContentPane().add(this.toolBar, BorderLayout.NORTH);
		this.updateToolBar();
	}


	/**
	 * Used to add a tab in the main window.
	 * In the future, even if the interface, this function should remain available.
	 */
	public boolean addTab(String tabName, java.awt.Component panel) {
		this.tabbedPane.addTab(tabName, panel);

		return true;
	}

	/**
	 * @see #addTab(String, java.awt.Component)
	 */
	public boolean addTab(String tabName, Icon icon, java.awt.Component panel) {
		this.tabbedPane.addTab(tabName, icon, panel);

		return true;
	}

	/**
	 * Used to remove a tab from the main window.
	 */
	public boolean removeTab(java.awt.Component panel) {
		this.tabbedPane.remove(panel);

		return true;
	}

	/**
	 * Used by plugins to add their own menu. Not recommanded for the moment.
	 * Need to find a more elegant way.
	 * @return Check it does not return null.
	 */
	public JMenuBar getMenuBar() {
		return this.menuBar;
	}


	/**
	 * Called when an element from the menu is called.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this.connectButton) {
			this.core.getPluginManager().stopPlugins();

			if(!this.core.initNodeConnection())
				this.unableToConnect();

			this.core.getPluginManager().runPlugins();
		}

		if(e.getSource() == this.disconnectButton) {
			if(!this.core.canDisconnect()) {
				if(!this.core.askDeconnectionConfirmation())
					return;
			}

			this.core.getPluginManager().stopPlugins();

			this.core.disconnect();

			this.core.getPluginManager().runPlugins();
		}

		if(e.getSource() == this.settingsButton) {
			this.core.getConfigWindow().setVisible(true);
		}

		if(e.getSource() == this.quitButton) {
			this.core.exit();
		}

		if(e.getSource() == this.reconnectionFileMenuItem) {

			if(!this.core.canDisconnect()) {
				if(!this.core.askDeconnectionConfirmation())
					return;
			}

			this.core.getPluginManager().stopPlugins();

			if(!this.core.initNodeConnection())
				this.unableToConnect();

			this.core.getPluginManager().loadPlugins();
			this.core.getPluginManager().runPlugins();

		}

		if(e.getSource() == this.optionsFileMenuItem) {
			this.core.getConfigWindow().setVisible(true);
		}

		if(e.getSource() == this.quitFileMenuItem) {
			this.endOfTheWorld();
		}

		if(e.getSource() == this.aboutHelpMenuItem) {
			this.showDialogAbout();
		}
	}

	/**
	 * Warns the user by a popup.
	 */
	protected void unableToConnect() {
		new WarningWindow(this.core,
				  I18n.getMessage("thaw.warning.unableToConnectTo")+
				  " "+this.core.getConfig().getValue("nodeAddress")+":"+ this.core.getConfig().getValue("nodePort"));
	}

	public void update(java.util.Observable o, Object arg) {
		this.updateToolBar();
	}


	public void updateToolBar() {
		if(this.core.getConnectionManager().isConnected()) {
			this.connectButton.setEnabled(false);
			this.disconnectButton.setEnabled(true);
		} else {
			this.connectButton.setEnabled(true);
			this.disconnectButton.setEnabled(false);
		}
	}

	/**
	 * Called when window is closed or 'quit' is choosed is the menu.
	 */
	public void endOfTheWorld() {
		if(this.core == null) {
			Logger.error(this, "Warning, no ref to core, exiting brutaly");
			System.exit(0);
		} else {
			this.core.exit();
		}
	}


	/**
	 * Change text in the status bar.
	 * @param status Null is accepted.
	 */
	public void setStatus(String status) {
		if(status != null)
			this.statusBar.setText(status);
		else
			this.statusBar.setText(" ");/* not empty else the status bar disappear */
	}


	public String getStatus() {
		return this.statusBar.getText();
	}


	public void showDialogAbout() {
		JLabel[] labels = new JLabel[] {
			new JLabel(I18n.getMessage("thaw.about.l1")),
			new JLabel(I18n.getMessage("thaw.about.l2")),
			new JLabel(I18n.getMessage("thaw.about.l3")),
			new JLabel(I18n.getMessage("thaw.about.l4")),
			new JLabel(""),
			new JLabel(I18n.getMessage("thaw.about.l6")),
			new JLabel(""),
			new JLabel(I18n.getMessage("thaw.about.l7")),
			new JLabel(I18n.getMessage("thaw.about.l8"))
		};

		labels[0].setFont(new Font("Dialog", Font.BOLD, 30));

		JOptionPane.showMessageDialog(null, labels, I18n.getMessage("thaw.about.title"),
					      JOptionPane.INFORMATION_MESSAGE);
	}


	public void windowActivated(WindowEvent e) {

	}

	public void windowClosing(WindowEvent e) {
		/* Should be in windowClosed(), but doesn't seem to work */
		if(e.getSource() == this.mainWindow)
			this.endOfTheWorld();
	}

	public void windowClosed(WindowEvent e) {
		// gni
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
