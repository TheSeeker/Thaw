package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JTextField;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.sql.*; /* I'm lazy */

import java.util.Vector;
import java.util.Iterator;

import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.Core;
import thaw.fcp.FreenetURIHelper;

import thaw.gui.IconBox;

import thaw.plugins.Hsqldb;


/**
 * mix of View (non-static methods) and Modele management (static methods) => berk :p
 */
public class BlackList implements ActionListener {

	private Core core;
	private Hsqldb db;
	private IndexBrowserPanel indexBrowser = null;

	private JPanel panel = null;
	private JButton hideButton = null;
	private JList blackList = null;
	private JButton removeButton = null;
	private JTextField keyField = null;
	private JButton addButton = null;

	private boolean visible;

	private BlackList() {

	}


	public BlackList(Core core, IndexBrowserPanel indexBrowser) {
		this.core = core;
		this.db = indexBrowser.getDb();
		this.indexBrowser = indexBrowser;


		panel = new JPanel(new BorderLayout(5, 5));

		JPanel northPanel = new JPanel(new BorderLayout());

		hideButton = new JButton(IconBox.minClose);
		hideButton.setBorderPainted(false);
		hideButton.setToolTipText(I18n.getMessage("thaw.common.closeTab"));
		hideButton.addActionListener(this);

		northPanel.add(hideButton, BorderLayout.EAST);
		northPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.blackList")), BorderLayout.CENTER);

		JPanel centerPanel = new JPanel(new BorderLayout());
		blackList = new JList();
		blackList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		removeButton = new JButton(I18n.getMessage("thaw.common.remove"), IconBox.delete);
		removeButton.addActionListener(this);

		centerPanel.add(new JScrollPane(blackList), BorderLayout.CENTER);
		centerPanel.add(removeButton, BorderLayout.SOUTH);


		JPanel southPanel = new JPanel(new BorderLayout());

		keyField = new JTextField("");
		addButton = new JButton(I18n.getMessage("thaw.common.add"), IconBox.minAdd);
		addButton.addActionListener(this);

		southPanel.add(new JLabel(I18n.getMessage("thaw.common.key") + " : "), BorderLayout.WEST);
		southPanel.add(keyField, BorderLayout.CENTER);
		southPanel.add(addButton, BorderLayout.EAST);

		panel.add(northPanel, BorderLayout.NORTH);
		panel.add(centerPanel, BorderLayout.CENTER);
		panel.add(southPanel, BorderLayout.SOUTH);

		visible = false;
	}

	public JPanel getPanel() {
		return panel;
	}


	protected class BlackListedLink {
		private int id;
		private String name;
		private String publicKey;

		public BlackListedLink(int id, String name, String key) {
			this.id = id;
			this.name = name;
			this.publicKey = key;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getPublicKey() {
			return publicKey;
		}

		public String toString() {
			return getName();
		}
	}


	public void updateList() {
		if (!visible)
			return;

		Vector list = new Vector();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, name, publicKey FROM indexBlackList ORDER BY name");

				ResultSet res = st.executeQuery();

				while(res.next()) {
					list.add(new BlackListedLink(res.getInt("id"),
								     res.getString("name"),
								     res.getString("publicKey")));
				}

			}
		} catch(SQLException e) {
			Logger.error(this, "SQLException while accessing black list : "+e.toString());
			purgeList();
			return;
		}

		blackList.setListData(list);
	}


	/**
	 * Avoid useless memory use
	 */
	protected void purgeList() {
		blackList.setListData(new Vector());
	}


	public void displayPanel() {
		if (visible) {
			updateList();
			core.getMainWindow().setSelectedTab(panel);
			return;
		}

		visible = true;
		updateList();
		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.index.blackList"),
					    IconBox.minStop, panel);
		core.getMainWindow().setSelectedTab(panel);
	}


	public void hidePanel() {
		if (!visible)
			return;

		visible = false;
		purgeList();
		core.getMainWindow().removeTab(panel);
		core.getMainWindow().setSelectedTab(indexBrowser.getPanel());
	}




	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == hideButton) {
			hidePanel();
			return;
		}

		if (e.getSource() == addButton) {
			if (addToBlackList(db, keyField.getText()))
				keyField.setText("");
			return;
		}


		if (e.getSource() == removeButton) {
			Object[] targets = (Object[])blackList.getSelectedValues();

			for (int i = 0 ; i < targets.length ; i++) {
				BlackListedLink link = (BlackListedLink)targets[i];
				removeFromBlackList(db, link.getPublicKey());
			}

			updateList();

			return;
		}
	}


	public static int isBlackListed(Hsqldb db, String key) {
		key = FreenetURIHelper.cleanURI(key);

		if (key == null) {
			Logger.error(new BlackList(), "isBlackListed() : Invalid key !");
			return -1;
		}

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, publicKey FROM indexBlackList WHERE "+
									 "LOWER(publicKey) LIKE ? LIMIT 1");
				st.setString(1, FreenetURIHelper.getComparablePart(key) +"%");

				ResultSet res = st.executeQuery();

				if (!res.next())
					return -1;

				return res.getInt("id");
			}
		} catch(SQLException e) {
			Logger.error(new BlackList(), "Error while checking if a given key is blacklisted : "+ e.toString());
			return -1;
		}

	}

	/**
	 * @param key must be an USK@ well formed with everything
	 * @return true if success
	 */
	public static boolean addToBlackList(Hsqldb db, String key) {
		if (isBlackListed(db, key) >= 0) {
			Logger.notice(new BlackList(), "Key already blacklisted");
			return false;
		}

		key = FreenetURIHelper.cleanURI(key);

		if (key == null) {
			Logger.error(new BlackList(), "addToBlackList() : Invalid key");
			return false;
		}

		try {
			synchronized(db.dbLock) {
				PreparedStatement st = db.getConnection().prepareStatement("INSERT INTO indexBlackList (publicKey, name) VALUES (?, ?)");

				st.setString(1, key);
				st.setString(2, Index.getNameFromKey(key));
				st.execute();

				st = db.getConnection().prepareStatement("UPDATE links "+
									 "SET blackListed = true "+
									 "WHERE LOWER(publicKey) LIKE ?");
				st.setString(1, FreenetURIHelper.getComparablePart(key) +"%");

				st.execute();

			}
		} catch(SQLException e) {
			Logger.error(new BlackList(), "Error while adding an entry to the blacklist : "+e.toString());
			return false;
		}

		return true;
	}


	/**
	 * @return true if success
	 */
	public static boolean removeFromBlackList(Hsqldb db, String key) {
		int id;

		if ((id = isBlackListed(db, key)) < 0) {
			Logger.notice(new BlackList(), "Key not blacklisted");
			return false;
		}

		try {
			synchronized(db.dbLock) {
				PreparedStatement st = db.getConnection().prepareStatement("SELECT id, publicKey FROM links WHERE "+
											   "LOWER(publicKey) LIKE ?");
				PreparedStatement anotherSt = db.getConnection().prepareStatement("UPDATE links SET blackListed = false WHERE id = ?");

				st.setString(1, FreenetURIHelper.getComparablePart(key) +"%");

				ResultSet res = st.executeQuery();

				while(res.next()) {
					anotherSt.setInt(1, res.getInt("id"));
					anotherSt.execute();
				}


				st = db.getConnection().prepareStatement("DELETE FROM indexBlackList WHERE id = ?");
				st.setInt(1, id);
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(new BlackList(), "Error while removing an entry from the blacklist : "+e.toString());
			return false;
		}

		return true;
	}

}
