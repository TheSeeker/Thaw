package thaw.core;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import java.util.Observable;
import java.awt.GridLayout;
import javax.swing.JOptionPane;

import thaw.i18n.I18n;


/**
 * ConfigWindow. Create the window used by user to config everything.
 * Composed by a tabbed pane containing a NodeConfigPanel and a PluginConfigPanel, and below the tabbed pane,
 *   a JButton to validate.
 * Notify observer when a button (Ok / Cancel) is clicked (gives the button in arg), or when
 *  window is set visible (arg == null).
 */
public class ConfigWindow extends Observable implements ActionListener, java.awt.event.WindowListener {
	private JFrame configWin;
	private JTabbedPane tabs;

	private JPanel buttons;
	private JButton okButton;
	private JButton cancelButton;

	private ThawConfigPanel thawConfigPanel;
	private NodeConfigPanel nodeConfigPanel;
	private PluginConfigPanel pluginConfigPanel;

	private Core core;

	private boolean advancedMode = false;

	public ConfigWindow(Core core) {
		this.core = core;

		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

		configWin = new JFrame(I18n.getMessage("thaw.config.windowName"));

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

		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setVgap(20);
		borderLayout.setVgap(20);

		configWin.getContentPane().setLayout(borderLayout);

		configWin.getContentPane().add(tabs, BorderLayout.CENTER);
		configWin.getContentPane().add(buttons, BorderLayout.SOUTH);

		tabs.setSize(600, 350);
		okButton.setSize(100, 50);

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
		tabs.remove( thawConfigPanel.getPanel() );
		tabs.remove( nodeConfigPanel.getPanel() );
		tabs.remove( pluginConfigPanel.getPanel() );

		tabs.add("Thaw", thawConfigPanel.getPanel());
		tabs.add(I18n.getMessage("thaw.common.node"), nodeConfigPanel.getPanel());
		if(advancedMode)
			tabs.add(I18n.getMessage("thaw.common.plugins"), pluginConfigPanel.getPanel());
	}


	/**
	 * Make [dis]appear the config window.
	 */
	public void setVisible(boolean v) {
		if(v == true) {
			setChanged();
			notifyObservers(null);
		}

		configWin.setVisible(v);
	}

	public boolean addTab(String name, java.awt.Component panel) {
		tabs.add(name, panel);
		return true;
	}

	public boolean removeTab(java.awt.Component panel) {
		tabs.remove(panel);
		return true;
	}

	/**
	 * Get a ref to the JFrame.
	 */
	public JFrame getFrame() {
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
	 * Called when apply button is pressed.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == okButton && !core.canDisconnect()) {
			int ret = JOptionPane.showOptionDialog((java.awt.Component)null,
								       I18n.getMessage("thaw.warning.isWritingSoApplyLater"),
								       I18n.getMessage("thaw.warning.title"),
								       JOptionPane.YES_NO_OPTION, 
								       JOptionPane.WARNING_MESSAGE,
								       (javax.swing.Icon)null,
								       (java.lang.Object[])null,
								       (java.lang.Object)null);
			if(ret == JOptionPane.CLOSED_OPTION || ret > 0)
				return;
		}

		if(e.getSource() == okButton
		   || e.getSource() == cancelButton) {

			setChanged();
			notifyObservers(e.getSource());

			setVisible(false);
		}

		if(e.getSource() == okButton) {
			advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

			/* should reinit the whole connection correctly */
			core.getPluginManager().stopPlugins();
			core.initNodeConnection();
			core.getPluginManager().loadPlugins();
			core.getPluginManager().runPlugins();

			/* reinit config win */
			addTabs();
		}
	}



	public void windowActivated(WindowEvent e) {

	}

	public void windowClosing(WindowEvent e) {
		setChanged();
		notifyObservers(cancelButton); /* Equivalent to a click on the cancel button */
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
