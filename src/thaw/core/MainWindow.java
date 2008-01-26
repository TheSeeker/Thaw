package thaw.core;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.event.WindowListener;

import thaw.gui.TabbedPane;
import thaw.gui.IconBox;


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
public class MainWindow implements java.awt.event.ActionListener,
				   WindowListener,
				   java.util.Observer {

	public final static int DEFAULT_SIZE_X = 790;
	public final static int DEFAULT_SIZE_Y = 550;

	private JFrame mainWindow = null;

	private JMenuBar menuBar = null;
	private JMenu fileMenu = null;

	private Vector fileMenuList = null;

	private JMenuItem reconnectionFileMenuItem = null;
	private JMenuItem optionsFileMenuItem = null;
	private JMenuItem quitFileMenuItem = null;

	private JMenu helpMenu = null;
	private Vector menuList = null;
	private JMenuItem aboutHelpMenuItem = null;

	private JToolBar toolBar = null;
	private JButton connectButton = null;
	private JButton disconnectButton = null;
	private JButton settingsButton = null;
	private JButton quitButton = null;

	private TabbedPane tabbedPane = null;
	private JLabel statusBar = null;

	private Core core = null; /* core is called back when exit() */

	private Object lastToolBarModifier = null;

	/**
	 * Creates a new <code>MainWindow</code> instance, and so a new Swing window.
	 * @param core a <code>Core</code> value
	 */
	public MainWindow(final Core core) {
		this.core = core;

		mainWindow = new JFrame("Thaw");

		mainWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		mainWindow.setVisible(false);

		try {
			mainWindow.setIconImage(IconBox.blueBunny.getImage());
		} catch(final Throwable e) {
			Logger.notice(this, "No icon");
		}

		// MENUS

		menuBar = new JMenuBar();
		menuList = new Vector();


		fileMenu = new JMenu(I18n.getMessage("thaw.menu.file"));
		fileMenuList = new Vector();

		reconnectionFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.reconnect"),
							 IconBox.minReconnectAction);
		optionsFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.options"),
						    IconBox.minSettings);
		quitFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.quit"),
						 IconBox.minQuitAction);

		fileMenuList.add(reconnectionFileMenuItem);
		fileMenuList.add(optionsFileMenuItem);
		fileMenuList.add(quitFileMenuItem);

		reconnectionFileMenuItem.addActionListener(this);
		optionsFileMenuItem.addActionListener(this);
		quitFileMenuItem.addActionListener(this);

		fileMenu.add(reconnectionFileMenuItem);
		fileMenu.add(optionsFileMenuItem);
		fileMenu.add(quitFileMenuItem);

		menuBar.add(fileMenu);
		menuList.add(fileMenu);

		helpMenu = new JMenu(I18n.getMessage("thaw.menu.help"));

		aboutHelpMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.about"),
						  IconBox.minHelp);
		aboutHelpMenuItem.addActionListener(this);

		helpMenu.add(aboutHelpMenuItem);

		//menuBar.add(Box.createHorizontalGlue());
		menuBar.add(helpMenu);
		menuList.add(helpMenu);

		// TOOLBAR
		connectButton = new JButton(IconBox.connectAction);
		connectButton.setBorderPainted(false);
		connectButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.connect"));

		disconnectButton = new JButton(IconBox.disconnectAction);
		disconnectButton.setBorderPainted(false);
		disconnectButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.disconnect"));

		settingsButton = new JButton(IconBox.settings);
		settingsButton.setBorderPainted(false);
		settingsButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.settings"));

		quitButton = new JButton(IconBox.quitAction);
		quitButton.setBorderPainted(false);
		quitButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.quit"));

		connectButton.addActionListener(this);
		disconnectButton.addActionListener(this);
		settingsButton.addActionListener(this);
		quitButton.addActionListener(this);


		// TABBED PANE

		tabbedPane = new TabbedPane();

		// STATUS BAR

		statusBar = new JLabel();
		setStatus(null, null);
		statusBar.setSize(500, 30);


		mainWindow.getContentPane().setLayout(new BorderLayout(5,5));

		mainWindow.setJMenuBar(menuBar);

		/* Toolbar adding: */ changeButtonsInTheToolbar(this, null);
		mainWindow.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		mainWindow.getContentPane().add(statusBar, BorderLayout.SOUTH);

		mainWindow.setSize(MainWindow.DEFAULT_SIZE_X, MainWindow.DEFAULT_SIZE_Y);

		mainWindow.addWindowListener(this);

		core.getConnectionManager().addObserver(this);

		if (core.getConfig().getValue("mainWindowSizeX") != null
		    && core.getConfig().getValue("mainWindowSizeY") != null) {
			try {
				mainWindow.setSize(Integer.parseInt(core.getConfig().getValue("mainWindowSizeX")),
						   Integer.parseInt(core.getConfig().getValue("mainWindowSizeY")));
			} catch(NumberFormatException e) {
				Logger.warning(this, "Exception while setting the main window size");
			}
		}

	}


	public void addWindowListener(WindowListener wl) {
		mainWindow.addWindowListener(wl);
	}

	public void removeWindowListener(WindowListener wl) {
		mainWindow.removeWindowListener(wl);
	}


	public void connectionHasChanged() {
		core.getConnectionManager().addObserver(this);
	}



	/**
	 * Make the window visible or not.
	 */
	public void setVisible(final boolean v) {
		if (!v || !core.isStopping())
			mainWindow.setVisible(v);
	}


	public boolean isVisible() {
		return mainWindow.isVisible();
	}

	public void setIconified() {
		int state = mainWindow.getExtendedState();

		state |= JFrame.ICONIFIED;

		mainWindow.setExtendedState(state);
	}

	public void setNonIconified() {
		int state = mainWindow.getExtendedState();

		state &= ~JFrame.ICONIFIED;

		mainWindow.setExtendedState(state);
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

	public Object getLastToolbarModifier() {
		return lastToolBarModifier;
	}

	/**
	 * @param modifier Correspond to the caller object: it's a security to avoid that a modifier wipe out the buttons from another one
	 * @param newButtons JButton vector : if null, then it means to remove the buttons from the toolbar. Only the object having currently its buttons displayed will be able to remove them, other will simply be ignored.
	 */
	public void changeButtonsInTheToolbar(final Object modifier, final Vector newButtons) {
		JToolBar newToolBar;

		Logger.debug(this, "changeButtonsInTheToolbar() : Called by "+modifier.getClass().getName());
		Logger.debug(this, newButtons == null ? "-> no button" : Integer.toString(newButtons.size()) + " buttons");

		if ((lastToolBarModifier == null) || (newButtons != null) || (lastToolBarModifier == modifier)) {
			lastToolBarModifier = modifier;
		} else
			/* Only the modifier who added the buttons can remove them */
			return;

		if (newButtons == null)
			lastToolBarModifier = null;

		newToolBar = new JToolBar(I18n.getMessage("thaw.toolbar.title"));
		newToolBar.setBorderPainted(false);
		newToolBar.add(connectButton);
		newToolBar.add(disconnectButton);
		newToolBar.addSeparator();
		newToolBar.add(settingsButton);
		newToolBar.addSeparator();

		if (newButtons != null) {
			for (final Iterator it = newButtons.iterator();
			     it.hasNext();) {
				final JButton button = (JButton)it.next();
				if (button != null) {
					button.setBorderPainted(false);
					newToolBar.add(button);
				} else
					newToolBar.addSeparator();
			}
			newToolBar.addSeparator();
		}

		newToolBar.add(quitButton);
		newToolBar.setFloatable(false);

		if (toolBar != null) {
			mainWindow.getContentPane().remove(toolBar);
		}

		toolBar = newToolBar;

		mainWindow.getContentPane().add(toolBar, BorderLayout.NORTH);
		updateToolBar();
		mainWindow.getContentPane().validate();
	}

	public void resetLastKnowToolBarModifier() {
		lastToolBarModifier = null;
	}


	/**
	 * Used to add a tab in the main window.
	 * In the future, even if the interface change,
	 * this function should remain available.
	 */
	public boolean addTab(final String tabName, final java.awt.Component panel) {
		return addTab(tabName, IconBox.add, panel);
	}

	/**
	 * Used to add a tab in the main window.
	 * In the future, even if the interface change,
	 * this function should remain available
	 * @see #addTab(String, java.awt.Component)
	 */
	public boolean addTab(final String tabName, final Icon icon,
			      final java.awt.Component panel) {
		tabbedPane.addTab(tabName, icon, panel);

		return true;
	}

	public boolean setSelectedTab(java.awt.Component c) {
		tabbedPane.setSelectedComponent(c);
		return true;
	}


	/**
	 * Used to remove a tab from the main window.
	 */
	public boolean removeTab(final java.awt.Component panel) {
		tabbedPane.remove(panel);

		return true;
	}

	/**
	 * Used by plugins to add their own menu.
	 */
	public void insertMenuAt(JMenu menu, int position) {
		menuList.add(position, menu);
		refreshMenuBar();
	}

	public void removeMenu(JMenu menu) {
		menuList.remove(menu);
		refreshMenuBar();
	}

	protected void refreshMenuBar() {
		Logger.info(this, "Display "+
			    Integer.toString(menuList.size())+
			    " menus in the main window");

		/* rebuilding menubar */
		JMenuBar bar = new JMenuBar();

		for (Iterator it = menuList.iterator();
		     it.hasNext();) {
			JMenu m = (JMenu)it.next();
			bar.add(m);
		}

		mainWindow.setJMenuBar(bar);
		menuBar = bar;
		mainWindow.validate(); /* no getContentPane() ! else it won't work ! */
	}


	/**
	 * Used by plugins to add their own menu / menuItem to the menu 'file'.
	 */
	public void insertInFileMenuAt(Object newItem, int position) {
		fileMenuList.add(position, newItem);
		refreshFileMenu();
	}

	public void removeFromFileMenu(Object item) {
		fileMenuList.remove(item);
		refreshFileMenu();
	}

	protected void refreshFileMenu() {
		/* rebuilding menubar */
		JMenu m = new JMenu(I18n.getMessage("thaw.menu.file"));

		for (Iterator it = fileMenuList.iterator();
		     it.hasNext();) {
			Object e = it.next();
			if (e instanceof JMenuItem)
				m.add((JMenuItem)e);
			else
				m.add((JMenu)e);
		}

		menuList.remove(fileMenu);
		fileMenu = m;
		menuList.add(0, fileMenu);

		refreshMenuBar();
	}


	/**
	 * Called when an element from the menu is called.
	 */
	public void actionPerformed(final ActionEvent e) {
		if(e.getSource() == connectButton) {
			core.reconnect(false);
		}

		if(e.getSource() == disconnectButton) {
			if(!core.canDisconnect()) {
				if(!core.askDeconnectionConfirmation())
					return;
			}

			core.getPluginManager().stopPlugins();
			core.disconnect();
			core.getPluginManager().loadAndRunPlugins();
		}

		if(e.getSource() == settingsButton) {
			setEnabled(false);
			core.getConfigWindow().setVisible(true);
		}

		if(e.getSource() == quitButton) {
			endOfTheWorld();
		}

		if(e.getSource() == reconnectionFileMenuItem) {

			if(!core.canDisconnect()) {
				if(!core.askDeconnectionConfirmation())
					return;
			}

			core.reconnect(false);
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
		new thaw.gui.WarningWindow(core,
					   I18n.getMessage("thaw.warning.unableToConnectTo")+
					   " "+core.getConfig().getValue("nodeAddress")+":"+ core.getConfig().getValue("nodePort"));
	}

	public void update(final java.util.Observable o, final Object arg) {
		updateToolBar();
	}


	public void updateToolBar() {
		if( core.getConnectionManager() != null &&
		   (core.getConnectionManager().isConnected() || core.isReconnecting())) {
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
		if (mainWindow != null) {
			java.awt.Dimension size = mainWindow.getSize();

			core.getConfig().setValue("mainWindowSizeX",
						  Integer.toString((new Double(size.getWidth())).intValue()));
			core.getConfig().setValue("mainWindowSizeY",
						  Integer.toString((new Double(size.getHeight())).intValue()));
		}

		if(core == null) {
			Logger.error(this, "Warning, no ref to core, exiting brutaly");
			System.exit(0);
		} else {
			core.exit();
		}
	}


	public void setStatus(final javax.swing.Icon icon, final String status) {
		setStatus(icon, status, java.awt.Color.BLACK);
	}


	/**
	 * Change text in the status bar.
	 * @param status Null is accepted.
	 */
	public void setStatus(final javax.swing.Icon icon, final String status, java.awt.Color color) {
		if(status != null) {
			statusBar.setText(status);
		} else {
			statusBar.setText(" ");/* not empty else the status bar disappear */
		}

		if (icon != null)
			statusBar.setIcon(icon);

		if (color != null)
			statusBar.setForeground(color);
	}


	public String getStatus() {
		return statusBar.getText();
	}

	/**
	 * @param pos can be BorderLayout.EAST or BorderLayout.WEST
	 */
	public void addComponent(java.awt.Component c, Object pos) {
		mainWindow.getContentPane().add(c, pos);
	}

	protected void setEnabled(boolean value) {
		mainWindow.setEnabled(value);
	}

	/**
	 * @see #addComponent(java.awt.Component, Object)
	 */
	public void removeComponent(java.awt.Component c) {
		mainWindow.getContentPane().remove(c);
	}


	public void showDialogAbout() {
		final JComponent[] labels = new JComponent[] {
			new JTextField("Thaw "+Main.VERSION),
			new JLabel(I18n.getMessage("thaw.about.l02")),
			new JLabel(I18n.getMessage("thaw.about.l03")),
			new JLabel(I18n.getMessage("thaw.about.l04")),
			new JLabel(""),
			new JLabel(I18n.getMessage("thaw.about.l06")),
			new JLabel(""),
			new JLabel(I18n.getMessage("thaw.about.l07")),
			new JLabel(I18n.getMessage("thaw.about.l08")),
			new JLabel(I18n.getMessage("thaw.about.l09")),
			new JLabel(I18n.getMessage("thaw.about.l10")),
			new JLabel(I18n.getMessage("thaw.about.l11")),
			new JLabel(I18n.getMessage("thaw.about.l12")),
			new JLabel(I18n.getMessage("thaw.about.l13")),
			new JLabel(I18n.getMessage("thaw.about.l14")),
			new JLabel(I18n.getMessage("thaw.about.l15"))
		};


		for (int i = 0 ; i < labels.length ; i++) {
			if (labels[i] instanceof JTextField) {
				((JTextField)labels[i]).setEditable(false);
			}
		}


		((JTextField)labels[0]).setFont(new Font("Dialog", Font.BOLD, 30));

		JOptionPane.showMessageDialog(null, labels, I18n.getMessage("thaw.about.title"),
					      JOptionPane.INFORMATION_MESSAGE);
	}


	public void windowActivated(final WindowEvent e) {

	}

	public void windowClosing(final WindowEvent e) {
		/* Should be in windowClosed(), but doesn't seem to work */
		if(e.getSource() == mainWindow)
			endOfTheWorld();
	}

	public void windowClosed(final WindowEvent e) {
		// gni
	}

	public void windowDeactivated(final WindowEvent e) {
		// C'est pas comme si on en avait quelque chose a foutre :p
	}

	public void windowDeiconified(final WindowEvent e) {
		// idem
	}

	public void windowIconified(final WindowEvent e) {
		// idem
	}

	public void windowOpened(final WindowEvent e) {
		// idem
	}

}
