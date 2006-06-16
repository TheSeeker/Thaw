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

	private NodeConfigPanel nodeConfigPanel;
	private PluginConfigPanel pluginConfigPanel;

	private Core core;


	public ConfigWindow(Core core) {
		this.core = core;

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

		tabs.add(I18n.getMessage("thaw.common.node"), nodeConfigPanel.getPanel());
		tabs.add(I18n.getMessage("thaw.common.plugins"), pluginConfigPanel.getPanel());


		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setVgap(20);
		borderLayout.setVgap(20);
		configWin.setLayout(borderLayout);
		configWin.add(tabs, BorderLayout.CENTER);
		configWin.add(buttons, BorderLayout.SOUTH);

		tabs.setSize(600, 350);
		okButton.setSize(100, 50);

		configWin.setSize(600, 400);
		configWin.setResizable(false);

		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		configWin.addWindowListener(this);
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
		if(e.getSource() == okButton
		   || e.getSource() == cancelButton) {

			setChanged();
			notifyObservers(e.getSource());

			setVisible(false);
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
