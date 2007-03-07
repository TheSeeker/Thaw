package thaw.core;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Observable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;



/**
 * ConfigWindow. Create the window used by user to config everything.
 * Composed by a tabbed pane containing a NodeConfigPanel and a PluginConfigPanel, and below the tabbed pane,
 *   a JButton to validate.
 * Notify observer when a button (Ok / Cancel) is clicked (gives the button in arg), or when
 *  window is set visible (arg == null).
 */
public class ConfigWindow extends Observable implements ActionListener, java.awt.event.WindowListener {
	private JDialog configWin;
	private JTabbedPane tabs;

	private JPanel buttons;
	private JButton okButton;
	private JButton cancelButton;

	private ThawConfigPanel thawConfigPanel;
	private NodeConfigPanel nodeConfigPanel;
	private PluginConfigPanel pluginConfigPanel;

	private Core core;

	private boolean advancedMode = false;

	private boolean needConnectionReset = false;

	public ConfigWindow(final Core core) {
		this.core = core;
		needConnectionReset = false;

		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

		configWin = new JDialog(core.getMainWindow().getMainFrame(), I18n.getMessage("thaw.config.windowName"));

		tabs = new JTabbedPane();

		buttons = new JPanel();
		buttons.setLayout(new GridLayout(1, 2));

		okButton = new JButton(I18n.getMessage("thaw.config.okButton"));
		cancelButton = new JButton(I18n.getMessage("thaw.config.cancelButton"));

		buttons.add(okButton);
		buttons.add(cancelButton);

		nodeConfigPanel = new NodeConfigPanel(this, core);
		pluginConfigPanel = new PluginConfigPanel(this, core);
		thawConfigPanel = new ThawConfigPanel(this, core);

		addTabs();

		final BorderLayout borderLayout = new BorderLayout();
		borderLayout.setVgap(20);
		borderLayout.setVgap(20);

		configWin.getContentPane().setLayout(borderLayout);

		configWin.getContentPane().add(tabs, BorderLayout.CENTER);
		configWin.getContentPane().add(buttons, BorderLayout.SOUTH);

		tabs.setSize(600, 360);
		okButton.setSize(100, 40);

		configWin.setSize(600, 400);
		configWin.setResizable(false);

		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		configWin.addWindowListener(this);
	}


	/**
	 * Remove them and re-add them.
	 */
	private void addTabs() {
		removeTab( thawConfigPanel.getPanel() );
		removeTab( nodeConfigPanel.getPanel() );
		removeTab( pluginConfigPanel.getPanel() );

		addTab("Thaw", thawConfigPanel.getPanel());
		addTab(I18n.getMessage("thaw.common.node"), IconBox.minConnectAction, nodeConfigPanel.getPanel());
		if(advancedMode)
			addTab(I18n.getMessage("thaw.common.plugins"), IconBox.minSettings, pluginConfigPanel.getPanel());
	}


	/**
	 * Make [dis]appear the config window.
	 */
	public void setVisible(final boolean v) {
		if(v == true) {
			setChanged();
			this.notifyObservers(null);
		}

		configWin.setVisible(v);
	}

	public boolean addTab(final String name, final java.awt.Component panel) {
		tabs.addTab(name, panel);
		return true;
	}

	public boolean addTab(final String name, javax.swing.ImageIcon icon, final java.awt.Component panel) {
		tabs.addTab(name, icon, panel);
		return true;
	}

	public boolean removeTab(final java.awt.Component panel) {
		tabs.remove(panel);
		return true;
	}

	/**
	 * Get a ref to the JFrame.
	 */
	public JDialog getFrame() {
		return configWin;
	}

	/**
	 * Get a ref to validation button.
	 */
	public JButton getOkButton() {
		return okButton;
	}


	/**
	 * Get a ref to cancel button.
	 */
	public JButton getCancelButton() {
		return cancelButton;
	}

	/**
	 * Call this function if a change made by the user
	 * need a connection reset with the plugins reset
	 */
	public void willNeedConnectionReset() {
		needConnectionReset = true;
	}

	/**
	 * Called when apply button is pressed.
	 */
	public void actionPerformed(final ActionEvent e) {
		if((e.getSource() == okButton) && !core.canDisconnect()) {
			final int ret = JOptionPane.showOptionDialog((java.awt.Component)null,
								       I18n.getMessage("thaw.warning.isWritingSoApplyLater"),
								       I18n.getMessage("thaw.warning.title"),
								       JOptionPane.YES_NO_OPTION,
								       JOptionPane.WARNING_MESSAGE,
								       (javax.swing.Icon)null,
								       (java.lang.Object[])null,
								       (java.lang.Object)null);
			if((ret == JOptionPane.CLOSED_OPTION) || (ret > 0))
				return;
		}

		if((e.getSource() == okButton)
		   || (e.getSource() == cancelButton)) {

			setChanged();
			this.notifyObservers(e.getSource());

			setVisible(false);
		}

		if(e.getSource() == okButton) {
			advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();


			Reloader reloader = new Reloader(needConnectionReset);
			Thread reload = new Thread(reloader);
			reload.start();

			needConnectionReset = false;
		}
	}


	/**
	 * We reload the change in another thread to avoid UI freeze
	 */
	public class Reloader implements Runnable {
		private boolean resetConnection;

		public Reloader(boolean resetConnection) {
			this.resetConnection = resetConnection;
		}

		public void run() {

			/* should reinit the whole connection correctly */
			core.getPluginManager().stopPlugins();

			if (resetConnection && !core.initConnection()) {
				new WarningWindow(core, I18n.getMessage("thaw.warning.unableToConnectTo")+ " "+core.getConfig().getValue("nodeAddress")+":"+ core.getConfig().getValue("nodePort"));
			}

			needConnectionReset = false;

			/* put back the config tab */
			addTabs();

			core.getPluginManager().loadPlugins();
			core.getPluginManager().runPlugins();
		}
	}


	public void windowActivated(final WindowEvent e) {

	}

	public void windowClosing(final WindowEvent e) {
		setChanged();
		this.notifyObservers(cancelButton); /* Equivalent to a click on the cancel button */
	}

	public void windowClosed(final WindowEvent e) {
		// add potential warnings here
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
