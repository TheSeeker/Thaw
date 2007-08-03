package thaw.plugins.index;


import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import thaw.fcp.FCPQueueManager;
import thaw.fcp.FreenetURIHelper;
import thaw.core.I18n;
import thaw.core.Logger;


public class IndexConfigDialog implements ActionListener, MouseListener {
	private JDialog frame;

	private IndexBrowserPanel indexBrowser;
	private FCPQueueManager queueManager;
	private Index index;


	private JButton okButton;
	private JButton cancelButton;
	private int formState;

	private JButton resetCommentsButton;

	private JTextField publicKeyField       = null;
	private JTextField privateKeyField      = null;
	private JCheckBox  publishPrivateKeyBox = null;
	private JCheckBox  allowCommentsBox     = null;

	private JPopupMenu popupMenuA;
	private JPopupMenu popupMenuB;


	/**
	 * Use it to add an already existing index (won't add it ; just get the basic values)
	 * Various fields will be disabled (publish private key, etc).
	 */
	public IndexConfigDialog(IndexBrowserPanel indexBrowser,
				 FCPQueueManager queueManager) {
		this(indexBrowser, queueManager, null);
	}

	/**
	 * Use it to modify an existing index (will modify automagically)
	 * @param index if index is provided, changes will be automagically set
	 */
	public IndexConfigDialog(IndexBrowserPanel indexBrowser,
				 FCPQueueManager queueManager,
				 Index index) {
		this.indexBrowser = indexBrowser;
		this.queueManager = queueManager;
		this.index = index;
	}


	public String getPublicKey() {
		String key = publicKeyField.getText();

		if (!FreenetURIHelper.isAKey(key))
			return null;

		return key;
	}

	public String getPrivateKey() {
		String key = privateKeyField.getText();

		if (!FreenetURIHelper.isAKey(key))
			return null;

		return key;
	}

	public boolean getPublishPrivateKey() {
		return publishPrivateKeyBox.isSelected();
	}

	public boolean getAllowComments() {
		return allowCommentsBox.isSelected();
	}



	/**
	 * If this function return true, you can use the get[...]() function
	 * to get the user input.
	 * (Note: What a mess !)
	 * @return false if the user cancelled
	 */
	public boolean promptUser() {
		prepareDialog();
		return (showDialog());
	}



	private void prepareDialog() {
		frame = new JDialog(indexBrowser.getMainWindow().getMainFrame(),
				    I18n.getMessage("thaw.plugin.index.indexSettings"));

		frame.getContentPane().setLayout(new BorderLayout());


		/* main elements (fields, checkboxes, etc) */

		publicKeyField       = new JTextField((index == null) ?
						      "USK@" :
						      index.getPublicKey());

		privateKeyField      = new JTextField((index == null) ?
						      "SSK@" :
						      index.getPrivateKey());

		publishPrivateKeyBox = new JCheckBox(I18n.getMessage("thaw.plugin.index.publishPrivateKey"),
						     ((index == null) ?
						      false :
						      index.publishPrivateKey()));

		publishPrivateKeyBox.setEnabled(index != null && index.getPrivateKey() != null);

		allowCommentsBox     = new JCheckBox(I18n.getMessage("thaw.plugin.index.allowComments"),
						     (index == null) ?
						     false :
						     (index.getCommentPublicKey() != null));

		allowCommentsBox.setEnabled((index == null) ?
					    false :
					    (index.getPrivateKey() != null));


		resetCommentsButton = new JButton(I18n.getMessage("thaw.plugin.index.comment.reset"));
		resetCommentsButton.addActionListener(this);


		/* public & private keys */

		final JPanel labelPanel = new JPanel();
		final JPanel textFieldPanel = new JPanel();

		labelPanel.setLayout(new GridLayout(2, 1));
		textFieldPanel.setLayout(new GridLayout(2, 1));

		labelPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.indexKey")+ " "), BorderLayout.WEST);
		textFieldPanel.add(publicKeyField, BorderLayout.CENTER);

		popupMenuA = new JPopupMenu();
		JMenuItem item = new JMenuItem(I18n.getMessage("thaw.common.paste"));
		popupMenuA.add(item);
		new thaw.gui.GUIHelper.PasteHelper(item, publicKeyField);
		publicKeyField.addMouseListener(this);


		labelPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.indexPrivateKey")+" "), BorderLayout.WEST);
		textFieldPanel.add(privateKeyField, BorderLayout.CENTER);

		popupMenuB = new JPopupMenu();
		item = new JMenuItem(I18n.getMessage("thaw.common.paste"));
		popupMenuB.add(item);
		new thaw.gui.GUIHelper.PasteHelper(item, privateKeyField);
		privateKeyField.addMouseListener(this);


		frame.getContentPane().add(labelPanel, BorderLayout.WEST);
		frame.getContentPane().add(textFieldPanel, BorderLayout.CENTER);


		/** various other settings **/


		final JPanel indexSettingsPanel = new JPanel();
		indexSettingsPanel.setLayout(new GridLayout(3, 1));

		final JPanel mainButtonPanel = new JPanel();
		mainButtonPanel.setLayout(new GridLayout(1, 2));

		cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
		okButton = new JButton(I18n.getMessage("thaw.common.ok"));

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);

		mainButtonPanel.add(okButton);
		mainButtonPanel.add(cancelButton);

		JPanel commentPanel = new JPanel(new BorderLayout());
		commentPanel.add(allowCommentsBox, BorderLayout.CENTER);

		if (index != null && index.getPrivateKey() != null)
			commentPanel.add(resetCommentsButton, BorderLayout.EAST);


		indexSettingsPanel.add(publishPrivateKeyBox);
		indexSettingsPanel.add(commentPanel);
		indexSettingsPanel.add(mainButtonPanel);

		frame.getContentPane().add(indexSettingsPanel, BorderLayout.SOUTH);
	}


	private boolean showDialog() {

		/* let's rock'n'rool :p */

		frame.setSize(700, 140);
		frame.setVisible(true);

		try {
			synchronized(this) {
				wait();
			}
		} catch(final InterruptedException e) {
			/* \_o< */
		}

		frame.setVisible(false);
		frame.dispose();

		if (formState == 2) /* cancelled */
			return false;

		if (index != null) {
			updateValues(index);
		}

		return true;
	}


	public void updateValues(Index index) {
		index.setPublicKey(getPublicKey());
		index.setPrivateKey(getPrivateKey());

		if (getPrivateKey() != null)
			index.setPublishPrivateKey(getPublishPrivateKey());

		if (index.canHaveComments() && !getAllowComments()) {
			Logger.notice(this, "Purging comments ...");
			index.purgeCommentKeys();
		} else if (!index.canHaveComments() && getAllowComments()) {
			Logger.notice(this, "Purging comments & regenerating keys ...");
			index.regenerateCommentKeys(queueManager);
		}
	}


	public synchronized void actionPerformed(final ActionEvent e) {
		if (e.getSource() == okButton) {
			formState = 1;
			synchronized(this) {
				notifyAll();
			}
			return;
		}

		if (e.getSource() == cancelButton) {
			formState = 2;
			synchronized(this) {
				notifyAll();
			}
			return;
		}

		if (e.getSource() == resetCommentsButton) {
			index.regenerateCommentKeys(queueManager);
		}
	}

	public void mouseClicked(final MouseEvent e) { }
	public void mouseEntered(final MouseEvent e) { }
	public void mouseExited(final MouseEvent e) { }

	public void mousePressed(final MouseEvent e) {
		showPopupMenu(e);
	}

	public void mouseReleased(final MouseEvent e) {
		showPopupMenu(e);
	}

	protected void showPopupMenu(final MouseEvent e) {
		if(e.isPopupTrigger()) {
			if (e.getComponent() == publicKeyField)
				popupMenuA.show(e.getComponent(), e.getX(), e.getY());
			if (e.getComponent() == privateKeyField)
				popupMenuB.show(e.getComponent(), e.getX(), e.getY());
		}
	}
}
