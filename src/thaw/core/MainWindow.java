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

import thaw.i18n.I18n;


/**
 * MainWindow. This class create the main window.
 *
 * Main window is divided in three parts:
 *
 * <pre>
 * ------------------------------------
 * | MenuBar                          |
 * ------------------------------------
 * | Tabbed Pane                      |
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
public class MainWindow implements java.awt.event.ActionListener, java.awt.event.WindowListener {
	private JFrame mainWindow = null;

	private JMenuBar menuBar = null;
	private JMenu fileMenu = null;
	
	private JMenuItem optionsFileMenuItem = null;
	private JMenuItem quitFileMenuItem = null;

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

		mainWindow.setVisible(false);
		
		menuBar = new JMenuBar();
		fileMenu = new JMenu(I18n.getMessage("thaw.menu.file"));
		optionsFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.options"));
		quitFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.quit"));
		
		optionsFileMenuItem.addActionListener(this);
		quitFileMenuItem.addActionListener(this);

		fileMenu.add(optionsFileMenuItem);
		fileMenu.add(quitFileMenuItem);
		menuBar.add(fileMenu);

		tabbedPane = new JTabbedPane();

		statusBar = new JLabel();
		setStatus(null);
		statusBar.setSize(500, 30);

		mainWindow.getContentPane().setLayout(new BorderLayout());

		mainWindow.setJMenuBar(menuBar);
		mainWindow.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		mainWindow.getContentPane().add(statusBar, BorderLayout.SOUTH);

		mainWindow.setSize(790, 550);
		
		mainWindow.addWindowListener(this);
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
		if(e.getSource() == optionsFileMenuItem) {
			core.getConfigWindow().setVisible(true);
		}

		
		if(e.getSource() == quitFileMenuItem) {
			endOfTheWorld();
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
