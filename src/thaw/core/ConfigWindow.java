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

	private boolean needConnectionReset = false;

	public ConfigWindow(Core core) {
		this.core = core;
		this.needConnectionReset = false;

		this.advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

		this.configWin = new JFrame(I18n.getMessage("thaw.config.windowName"));

		this.tabs = new JTabbedPane();

		this.buttons = new JPanel();
		this.buttons.setLayout(new GridLayout(1, 2));

		this.okButton = new JButton(I18n.getMessage("thaw.config.okButton"));
		this.cancelButton = new JButton(I18n.getMessage("thaw.config.cancelButton"));

		this.buttons.add(this.okButton);
		this.buttons.add(this.cancelButton);

		this.nodeConfigPanel = new NodeConfigPanel(this, core);
		this.pluginConfigPanel = new PluginConfigPanel(this, core);
		this.thawConfigPanel = new ThawConfigPanel(this, core);

		this.addTabs();

		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setVgap(20);
		borderLayout.setVgap(20);

		this.configWin.getContentPane().setLayout(borderLayout);

		this.configWin.getContentPane().add(this.tabs, BorderLayout.CENTER);
		this.configWin.getContentPane().add(this.buttons, BorderLayout.SOUTH);

		this.tabs.setSize(600, 350);
		this.okButton.setSize(100, 50);

		this.configWin.setSize(600, 400);
		this.configWin.setResizable(false);

		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);

		this.configWin.addWindowListener(this);
	}


	/**
	 * Remove them and re-add them.
	 */
	private void addTabs() {
		this.tabs.remove( this.thawConfigPanel.getPanel() );
		this.tabs.remove( this.nodeConfigPanel.getPanel() );
		this.tabs.remove( this.pluginConfigPanel.getPanel() );

		this.tabs.add("Thaw", this.thawConfigPanel.getPanel());
		this.tabs.add(I18n.getMessage("thaw.common.node"), this.nodeConfigPanel.getPanel());
		if(this.advancedMode)
			this.tabs.add(I18n.getMessage("thaw.common.plugins"), this.pluginConfigPanel.getPanel());
	}


	/**
	 * Make [dis]appear the config window.
	 */
	public void setVisible(boolean v) {
		if(v == true) {
			this.setChanged();
			this.notifyObservers(null);
		}

		this.configWin.setVisible(v);
	}

	public boolean addTab(String name, java.awt.Component panel) {
		this.tabs.add(name, panel);
		return true;
	}

	public boolean removeTab(java.awt.Component panel) {
		this.tabs.remove(panel);
		return true;
	}

	/**
	 * Get a ref to the JFrame.
	 */
	public JFrame getFrame() {
		return this.configWin;
	}

	/**
	 * Get a ref to validation button.
	 */
	public JButton getOkButton() {
		return this.okButton;
	}


	/**
	 * Get a ref to cancel button.
	 */
	public JButton getCancelButton() {
		return this.cancelButton;
	}

	public void willNeedConnectionReset() {
		this.needConnectionReset = true;
	}

	/**
	 * Called when apply button is pressed.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this.okButton && !this.core.canDisconnect()) {
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

		if(e.getSource() == this.okButton
		   || e.getSource() == this.cancelButton) {

			this.setChanged();
			this.notifyObservers(e.getSource());

			this.setVisible(false);
		}

		if(e.getSource() == this.okButton) {
			this.advancedMode = Boolean.valueOf(this.core.getConfig().getValue("advancedMode")).booleanValue();

			/* should reinit the whole connection correctly */
			this.core.getPluginManager().stopPlugins();

			if(this.needConnectionReset && !this.core.initNodeConnection()) {
				new WarningWindow(this.core, I18n.getMessage("thaw.warning.unableToConnectTo")+ " "+this.core.getConfig().getValue("nodeAddress")+":"+ this.core.getConfig().getValue("nodePort"));
			}

			this.needConnectionReset = false;

			this.core.getPluginManager().loadPlugins();
			this.core.getPluginManager().runPlugins();

			/* reinit config win */
			this.addTabs();
		}
	}



	public void windowActivated(WindowEvent e) {

	}

	public void windowClosing(WindowEvent e) {
		this.setChanged();
		this.notifyObservers(this.cancelButton); /* Equivalent to a click on the cancel button */
	}

	public void windowClosed(WindowEvent e) {
		// add potential warnings here
	}

	public void windowDeactivated(WindowEvent e) {
		// C'est pas comme si on en avait quelque chose a foutre :p
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
