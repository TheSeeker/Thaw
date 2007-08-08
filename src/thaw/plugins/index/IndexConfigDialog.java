package thaw.plugins.index;


import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import java.sql.*;

import java.util.Vector;
import java.util.Iterator;


import thaw.fcp.FCPQueueManager;
import thaw.fcp.FreenetURIHelper;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.plugins.Hsqldb;


public class IndexConfigDialog implements ActionListener, MouseListener,
					  ListSelectionListener {

	public final static int SIZE_X = 700;
	public final static int SIZE_Y = 170;
	public final static int CATEGORY_DIALOG_SIZE_X = 400;
	public final static int CATEGORY_DIALOG_SIZE_Y = 400;

	private JDialog frame;

	private IndexBrowserPanel indexBrowser;
	private FCPQueueManager queueManager;
	private Hsqldb db;
	private Index index;


	private JButton okButton;
	private JButton cancelButton;
	private int formState;

	private JButton resetCommentsButton;

	private JTextField publicKeyField       = null;
	private JTextField privateKeyField      = null;
	private JCheckBox  publishPrivateKeyBox = null;
	private JCheckBox  allowCommentsBox     = null;
	private JLabel     categoryLabel        = null;
	private JButton    changeCategory       = null;

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
		this.index        = index;
		this.db           = indexBrowser.getDb();
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

		categoryLabel = new JLabel("");


		resetCommentsButton = new JButton(I18n.getMessage("thaw.plugin.index.comment.reset"));
		resetCommentsButton.setEnabled(index != null && index.getPrivateKey() != null);
		resetCommentsButton.addActionListener(this);

		updateCategoryLabel();

		changeCategory = new JButton(I18n.getMessage("thaw.plugin.index.changeCategory"));
		changeCategory.setEnabled(index != null && index.getPrivateKey() != null);
		changeCategory.addActionListener(this);


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
		indexSettingsPanel.setLayout(new GridLayout(4, 1));

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
		commentPanel.add(resetCommentsButton, BorderLayout.EAST);

		JPanel categoryPanel = new JPanel(new BorderLayout());
		categoryPanel.add(categoryLabel, BorderLayout.CENTER);
		categoryPanel.add(changeCategory, BorderLayout.EAST);

		indexSettingsPanel.add(publishPrivateKeyBox);
		indexSettingsPanel.add(commentPanel);
		indexSettingsPanel.add(categoryPanel);

		indexSettingsPanel.add(mainButtonPanel);

		frame.getContentPane().add(indexSettingsPanel, BorderLayout.SOUTH);
	}


	private boolean showDialog() {

		/* let's rock'n'rool :p */

		frame.setSize(SIZE_X, SIZE_Y);
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



	private JDialog categoryDialog;
	private JList categoryList;
	private JTextField categoryField;
	private JButton categoryOkButton;
	private JButton categoryCancelButton;


	public void updateCategoryLabel() {
		String cat = (index == null ? null : index.getCategory());

		if (cat == null)
			cat = I18n.getMessage("thaw.plugin.index.categoryUnspecified");

		categoryLabel.setText(I18n.getMessage("thaw.plugin.index.category")
				      +": "+cat);
	}

	public Vector getCategories() {
		Vector v = new Vector();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, name "+
									 "FROM categories "+
									 "ORDER BY name");

				ResultSet set = st.executeQuery();

				while(set.next()) {
					v.add(set.getString("name"));
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't get index categories list because : "+e.toString());
		}

		return v;
	}


	public void showCategoryDialog() {
		Vector cats = getCategories();

		categoryDialog = new JDialog(frame,
					     I18n.getMessage("thaw.plugin.index.category"));

		categoryList = new JList(cats);
		categoryOkButton = new JButton(I18n.getMessage("thaw.common.ok"));
		categoryCancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
		categoryField = new JTextField("");

		categoryList.addListSelectionListener(this);
		categoryOkButton.addActionListener(this);
		categoryCancelButton.addActionListener(this);

		String cat = index.getCategory();

		if (cat != null) {
			categoryList.setSelectedIndex(cats.indexOf(cat));
			categoryField.setText(cat);
		}


		categoryDialog.getContentPane().setLayout(new BorderLayout(5, 5));


		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(categoryOkButton);
		buttonPanel.add(categoryCancelButton);

		JPanel southPanel = new JPanel(new GridLayout(2, 1));
		southPanel.add(categoryField);
		southPanel.add(buttonPanel);


		categoryDialog.getContentPane().add(new JLabel(I18n.getMessage("thaw.plugin.index.categories")),
						    BorderLayout.NORTH);
		categoryDialog.getContentPane().add(new JScrollPane(categoryList),
						    BorderLayout.CENTER);
		categoryDialog.getContentPane().add(southPanel,
						    BorderLayout.SOUTH);

		categoryDialog.setSize(CATEGORY_DIALOG_SIZE_X, CATEGORY_DIALOG_SIZE_Y);
		categoryDialog.setVisible(true);
	}


	public void valueChanged(ListSelectionEvent e) {
		categoryField.setText((String)categoryList.getSelectedValue());
	}


	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() == okButton) {
			formState = 1;
			synchronized(this) {
				notifyAll();
			}
			return;

		} else if (e.getSource() == cancelButton) {

			formState = 2;
			synchronized(this) {
				notifyAll();
			}
			return;

		} else if (e.getSource() == resetCommentsButton) {

			index.regenerateCommentKeys(queueManager);

		} else if (e.getSource() == changeCategory) {

			showCategoryDialog();

		} else if (e.getSource() == categoryOkButton) {

			index.setCategory(categoryField.getText());
			categoryDialog.setVisible(false);
			updateCategoryLabel();

		} else if (e.getSource() == categoryCancelButton) {

			categoryDialog.setVisible(false);

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
