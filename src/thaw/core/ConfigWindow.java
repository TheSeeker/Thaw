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
import thaw.gui.TabbedPane;
import thaw.gui.IconBox;


/**
 * ConfigWindow. Create the window used by user to config everything.
 * Composed by a tabbed pane containing a NodeConfigPanel and a PluginConfigPanel, and below the tabbed pane,
 *   a JButton to validate.
 * Notify observer when a button (Ok / Cancel) is clicked (gives the button in arg), or when
 *  window is set visible (arg == null).
 */
public class ConfigWindow extends Observable implements ActionListener, java.awt.event.WindowListener {
	private JDialog configWin;
	private TabbedPane tabs;

	private JPanel buttons;
	private JButton okButton;
	private JButton cancelButton;

	private ThawConfigPanel thawConfigPanel;
	private NodeConfigPanel nodeConfigPanel;
	private PluginConfigPanel pluginConfigPanel;

	private Core core;

	private boolean needConnectionReset = false;


	public ConfigWindow(final Core core) {
		this.core = core;
		needConnectionReset = false;

		configWin = new JDialog(core.getMainWindow().getMainFrame(), I18n.getMessage("thaw.config.windowName"));

		tabs = new TabbedPane();

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

		//tabs.setSize(600, 360);
		//okButton.setSize(100, 40);

		configWin.setSize(700, 470);
		//configWin.setResizable(false);

		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		configWin.addWindowListener(this);
		setVisible(false);
	}


	/**
	 * Remove them and re-add them.
	 */
	private void addTabs() {
		removeTab( thawConfigPanel.getPanel() );
		removeTab( nodeConfigPanel.getPanel() );
		removeTab( pluginConfigPanel.getPanel() );

		addTab("Thaw", IconBox.blueBunny, thawConfigPanel.getPanel());
		addTab(I18n.getMessage("thaw.config.nodeConnection"), IconBox.minConnectAction, nodeConfigPanel.getPanel());
		addTab(I18n.getMessage("thaw.common.plugins"), IconBox.minPlugins, pluginConfigPanel.getPanel());
	}


	/**
	 * Make [dis]appear the config window.
	 */
	public void setVisible(final boolean v) {
		if (v) {
			setChanged();
			this.notifyObservers(null);
		}

		configWin.setVisible(v);

		if (v)
			configWin.repaint();
		else if (core.isStopping())
			configWin.dispose();
	}

	public boolean addTab(final String name, final java.awt.Component panel) {
		tabs.addTab(name, panel);
		return true;
	}

	public boolean addTab(final String name, javax.swing.ImageIcon icon, final java.awt.Component panel) {
		tabs.addTab(name, icon, panel);
		return true;
	}

	public boolean setSelectedTab(java.awt.Component c) {
		tabs.setSelectedComponent(c);
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

	public PluginConfigPanel getPluginConfigPanel() {
		return pluginConfigPanel;
	}

	/**
	 * Used to update the MDNSPanel
	 */
	public NodeConfigPanel getNodeConfigPanel() {
		return nodeConfigPanel;
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
		if((e.getSource() == okButton) && !core.canDisconnect() && needConnectionReset) {
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


		if(e.getSource() == okButton) {
			close(false, true);

			/** the reloader will apply the changes */
			core.getConfig().startChanges();

			setChanged();
			notifyObservers(okButton);

			synchronized(PluginManager.pluginLock) {
				Reloader reloader = new Reloader(needConnectionReset);
				Thread reload = new ThawThread(reloader, "Config reloader", this);
				reload.start();
			}

			needConnectionReset = false;
		}


		if(e.getSource() == cancelButton) {
			close();
		}
	}


	/**
	 * We reload the change in another thread to avoid UI freeze
	 */
	public class Reloader implements ThawRunnable {
		private boolean resetConnection;
		private boolean running;

		public Reloader(boolean resetConnection) {
			this.resetConnection = resetConnection;
			this.running = true;
		}

		public void apply() {
			PleaseWaitDialog dialog = null;

			if (resetConnection) {
				dialog = new PleaseWaitDialog(core.getMainWindow());
			}

			/* Imply a whole reset => all the plugins will be reloaded
			 */
			if (resetConnection) {
				core.getConfig().cancelChanges();

				if (running)
					core.getPluginManager().stopPlugins();

				/* should reinit the whole connection correctly */
				if (running && resetConnection && !core.initConnection()) {
					if (dialog == null)
						new thaw.gui.WarningWindow(core.getMainWindow().getMainFrame(),
									   I18n.getMessage("thaw.warning.unableToConnectTo")+
									   " "+core.getConfig().getValue("nodeAddress")+
									   ":"+ core.getConfig().getValue("nodePort"));
					else
						new thaw.gui.WarningWindow(dialog.getDialog(),
									   I18n.getMessage("thaw.warning.unableToConnectTo")+
									   " "+core.getConfig().getValue("nodeAddress")+
									   ":"+ core.getConfig().getValue("nodePort"));
				}

				needConnectionReset = false;

				/* put back the config tab */
				if (running)
					addTabs();

				if (running)
					core.getPluginManager().loadAndRunPlugins();

			} else { /* !resetConnection */
				if (running)
					core.getConfig().applyChanges();
			}

			if (resetConnection) {
				dialog.dispose();
			}
		}
		
		public void run() {
			synchronized(PluginManager.pluginLock) {
				apply();
			}
		}

		public void stop() {
			running = false;
		}
	}

	/* not for later : the cancelbutton just call this */
	public void close() {
		close(true, true);
	}


	public void close(boolean notifyCancel, boolean hideWin) {
		if (notifyCancel) {
			setChanged();
			notifyObservers(cancelButton); /* Equivalent to a click on the cancel button */
		}

		core.getMainWindow().setEnabled(true);

		if (hideWin)
			setVisible(false);
	}


	public void setEnabled(boolean value) {
		configWin.setEnabled(value);
	}


	public void windowActivated(final WindowEvent e) {

	}

	public void windowClosing(final WindowEvent e) {
		close(true, false);
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
