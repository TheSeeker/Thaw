/* Yes, I know ... */
package thaw.plugins.miniFrost.frostKSK;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Vector;
import java.util.Iterator;
import java.util.Date;
import java.util.HashMap;

import java.util.Observer;
import java.util.Observable;

import java.sql.*;

import thaw.fcp.FCPGenerateSSK;
import thaw.core.Logger;
import thaw.core.Core;
import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.plugins.MiniFrost;


public class SSKBoardFactory extends KSKBoardFactory {

	public final static String[] DEFAULT_BOARDS_NAME = new String[] {
		"freenet-announce"
	};

	public final static String[] DEFAULT_BOARDS_PUBLICKEY = new String[] {
		"SSK@MGJOpsf32MDiti3I3ipzdLwKAXZFumiih-YE5ABZNQE,gcUpKZ17E18pyAtXzk4FVjLPo8IgpUqiCa~yBB0RSZo,AQACAAE"
	};


	public final static int BOARD_FACTORY_ID = 1;

	private Hsqldb db;
	private Core core;

	private HashMap boards;


	public SSKBoardFactory() {
		super();
	}

	public boolean init(Hsqldb db, Core core, MiniFrost plugin) {
		this.db = db;
		this.core = core;
		this.boards = new HashMap();
		return super.init(db, core, plugin, "frostSSKInitialized");
	}

	protected void addDefaultBoards() {
		for (int i = 0 ; i < DEFAULT_BOARDS_NAME.length ; i++)
			createBoard(DEFAULT_BOARDS_NAME[i],
				    DEFAULT_BOARDS_PUBLICKEY[i],
				    null,
				    false /* warning */);
	}

	public Vector getBoards() {
		Vector v = new Vector();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st
					= db.getConnection().prepareStatement("SELECT frostKSKBoards.id, "+
									      "       frostKSKBoards.name, "+
									      "       frostKSKBoards.lastUpdate, "+
									      "       frostSSKBoards.publicKey, "+
									      "       frostSSKBoards.privateKey "+
									      "FROM frostKSKBoards INNER JOIN frostSSKBoards "+
									      "  ON frostKSKBoards.id = frostSSKBoards.kskBoardId "+
				                                              "ORDER BY LOWER(name)");
				ResultSet set = st.executeQuery();

				while(set.next()) {
					int id = set.getInt("id");
					String name = set.getString("name");
					String publicKey = set.getString("publicKey");
					String privateKey = set.getString("privateKey");
					Date lastUpdate = set.getDate("lastUpdate");

					if (boards.get(name+publicKey) != null)
						v.add(boards.get(name+publicKey));
					else {

						SSKBoard board = new SSKBoard(this,
									      id, name, lastUpdate,
									      publicKey, privateKey);

						v.add(board);
						boards.put(name+publicKey, board);
					}
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't get the board list because : "+e.toString());
		}

		return v;
	}


	protected KSKBoard getBoard(int id) {
		for (Iterator it = boards.values().iterator();
		     it.hasNext();) {
			KSKBoard board = (KSKBoard)it.next();

			if (board.getId() == id)
				return board;
		}

		return null;
	}


	public Vector getAllMessages(String[] keywords, int orderBy,
				     boolean desc, boolean archived,
				     boolean unsigned, int minTrustLevel) {
		/* KSKBoardFactory will do the job for us */
		return new Vector();
	}


	/**
	 * Already answered by KSKBoardFactory
	 */
	public Vector getSentMessages() {
		return new Vector();
	}


	private class NewBoardDialog implements Observer, ActionListener {
		private JDialog dialog;

		private JTextField name;
		private JTextField publicKey;
		private JTextField privateKey;

		private JButton generate;
		private JButton ok;
		private JButton cancel;

		public NewBoardDialog(thaw.core.MainWindow mainWindow) {
			dialog = new JDialog(mainWindow.getMainFrame(),
					     I18n.getMessage("thaw.plugin.miniFrost.FrostSSK"));

			dialog.getContentPane().setLayout(new BorderLayout(5, 5));

			JPanel labelPanel = new JPanel(new GridLayout(4, 1));
			labelPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.board.name")));
			labelPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.board.publicKey")));
			labelPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.board.privateKey")));
			labelPanel.add(generate = new JButton(I18n.getMessage("thaw.plugin.miniFrost.generateKeys")));

			JPanel fieldPanel = new JPanel(new GridLayout(4, 1));
			fieldPanel.add(name = new JTextField(""));
			fieldPanel.add(publicKey = new JTextField(""));
			fieldPanel.add(privateKey = new JTextField(""));

			JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
			buttonPanel.add(cancel = new JButton(I18n.getMessage("thaw.common.cancel")));
			buttonPanel.add(    ok = new JButton(I18n.getMessage("thaw.common.ok")));

			dialog.getContentPane().add(labelPanel, BorderLayout.WEST);
			dialog.getContentPane().add(fieldPanel, BorderLayout.CENTER);
			dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

			generate.addActionListener(this);
			ok.addActionListener(this);
			cancel.addActionListener(this);

			dialog.setSize(700, 150);
			dialog.setVisible(true);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == generate) {
				FCPGenerateSSK generator = new FCPGenerateSSK();

				generator.addObserver(this);
				generator.start(core.getQueueManager());

				return;
			} else if (e.getSource() == ok) {
				createBoard(name.getText(),
					    	publicKey.getText(),
					    	privateKey.getText());
			}

			synchronized(this) {
				this.notifyAll();
			}
			dialog.setVisible(false);
		}

		public void update(Observable o, Object param) {
			if (o instanceof FCPGenerateSSK) {
				FCPGenerateSSK generator = (FCPGenerateSSK)o;
				publicKey.setText(generator.getPublicKey());
				privateKey.setText(generator.getPrivateKey());
				generator.deleteObserver(this);
			}
		}
	}


	public void createBoard(thaw.core.MainWindow mainWindow) {
		NewBoardDialog dialog = new NewBoardDialog(mainWindow);

		synchronized(dialog) {
			try {
				dialog.wait();
			} catch(InterruptedException e) {
				/* \_o< */
			}
		}
	}
	
	public Vector getAllKnownBoards() {
		/* KSKBoardFactory already answered for us */
		return new Vector();
	}

	public String toString() {
		return I18n.getMessage("thaw.plugin.miniFrost.FrostSSK");
	}
}

