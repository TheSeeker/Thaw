package thaw.plugins.miniFrost;

import java.util.Vector;
import java.util.Iterator;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;

import java.util.Observer;
import java.util.Observable;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.sql.*;

import thaw.plugins.Hsqldb;
import thaw.core.Logger;
import thaw.core.I18n;
import thaw.core.ConfigWindow;


/**
 * Simply store a list of regexp in the db.
 * Board.refresh() can next use to know if they must ignore or not a message.
 */
public class RegexpBlacklist implements Observer, ActionListener {

	public final static String REGEXP_MANUAL_URL = "http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html#sum";

	private Hsqldb db;

	private Vector blacklist;


	private JPanel    panel;
	private JTextArea textArea;

	private JButton applyButton;


	public RegexpBlacklist(Hsqldb db) {
		this.db = db;
		blacklist = new Vector();

		createTable();
		loadBlackList();


		panel = new JPanel(new BorderLayout(5, 5));


		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		topPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.regexpBlacklistLongDesc")));
		topPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.seeSunManual").replaceAll("X", REGEXP_MANUAL_URL)));

		textArea = new JTextArea("");

		panel.add(topPanel, BorderLayout.NORTH);
		panel.add(textArea, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(new JLabel(""), BorderLayout.CENTER);
		applyButton = new JButton(I18n.getMessage("thaw.common.apply"));
		applyButton.addActionListener(this);
		bottomPanel.add(applyButton, BorderLayout.EAST);

		panel.add(bottomPanel, BorderLayout.SOUTH);
	}

	private boolean sendQuery(final String query) {
		try {
			db.executeQuery(query);
			return true;
		} catch(final SQLException e) {
			Logger.notice(e, "While (re)creating sql tables: "+e.toString());
			return false;
		}
	}

	private void createTable() {
		sendQuery("CREATE CACHED TABLE regexpBlacklist ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "regexp VARCHAR(512) NOT NULL)");
	}


	public void loadBlackList() {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT regexp FROM regexpBlacklist");
				ResultSet set = st.executeQuery();

				while(set.next()) {
					synchronized(blacklist) {
						blacklist.add(set.getString("regexp"));
					}				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while loading the regexp blacklist: "+e.toString());
			return;
		}
	}


	public void saveBlacklist() {
		try {
			synchronized(db.dbLock) {

				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM regexpBlacklist");
				st.execute();

				st = db.getConnection().prepareStatement("INSERT INTO regexpBlackList (regexp) "+
									 "VALUES (?)");

				synchronized(blacklist) {
					for (Iterator it = blacklist.iterator();
					     it.hasNext();) {
						st.setString(1, (String)it.next());
						st.execute();
					}
				}

			}
		} catch(SQLException e) {
			Logger.error(this, "Error while saving the regexp blacklist: "+e.toString());
			return;
		}
	}


	public boolean isBlacklisted(String str) {
		if (str == null)
			return true;

		synchronized(blacklist) {
			for (Iterator it = blacklist.iterator();
			     it.hasNext();) {
				String regexp = ((String)it.next());

				try {
					if (str.matches(".*"+regexp+".*"))
						return true;
				} catch(java.util.regex.PatternSyntaxException e) {
					Logger.error(e, "Invalid regexp in the blacklist : "+regexp);
				}
			}
		}

		return false;
	}

	/**
	 * @return null if all the regexp are valids, else return the first invalid one
	 */
	public static String validateBlacklist(Vector blacklist) {
		synchronized(blacklist) {
			for (Iterator it = blacklist.iterator();
			     it.hasNext();) {
				String regexp = (String)it.next();

				try {
					"".matches(".*"+regexp+".*");
				} catch(java.util.regex.PatternSyntaxException e) {
					Logger.error(e, "Invalid regexp in the blacklist : "+regexp);
					return regexp;
				}
			}
		}

		return null;
	}


	public void refresh() {
		StringBuffer buf = new StringBuffer("");

		synchronized(blacklist) {
			for (Iterator it = blacklist.iterator();
			     it.hasNext();) {
				buf.append((String)it.next());
				buf.append("\n");
			}
		}

		textArea.setText(buf.toString());
	}

	private ConfigWindow window;


	public void displayTab(ConfigWindow window) {
		this.window = window;

		refresh();

		window.addTab(I18n.getMessage("thaw.plugin.miniFrost.regexpBlacklist"),
			      thaw.gui.IconBox.minStop,
			      panel);
		window.setSelectedTab(panel);
		window.addObserver(this);
	}


	public void hideTab(ConfigWindow window) {
		window.deleteObserver(this);
		window.removeTab(panel);
		window = null;
	}

	public void actionPerformed(ActionEvent e) {
		update(null, e.getSource());
	}

	public void update(Observable o, Object param) {

		if (param == applyButton || param == window.getOkButton()) {

			String[] split = textArea.getText().split("\n");

			Vector newRegexps = new Vector();

			for (int i = 0 ; i < split.length ; i++) {
				if (split[i] != null && !"".equals(split[i]))
					newRegexps.add(split[i]);
			}

			String r;

			if ((r = validateBlacklist(newRegexps)) == null) {
				blacklist = newRegexps;
				saveBlacklist();
			} else {
				String txt = I18n.getMessage("thaw.plugin.miniFrost.invalidRegexp").replaceAll("X", r);

				JOptionPane.showMessageDialog(window.getFrame(),
							      txt, txt,
							      JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		if (param != applyButton)
			hideTab(window);
	}
}
