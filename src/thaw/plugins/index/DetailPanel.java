package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;

import java.util.Vector;
import java.util.Iterator;

import java.sql.*;

import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.Config;

import thaw.gui.IconBox;

import thaw.fcp.FCPQueueManager;

import thaw.plugins.Hsqldb;


/**
 * Initially, I wanted to use it to show details about the
 * currently-viewed index, but in the end it will mostly
 * be used for the comments
 */
public class DetailPanel {
	private IndexBrowserPanel indexBrowser;

	private JPanel panel;

	private JButton viewCommentButton;

	private Vector buttonActions;

	private JLabel nmbFilesLabel;
	private JLabel nmbLinksLabel;


	public DetailPanel(FCPQueueManager queueManager, IndexBrowserPanel indexBrowser) {
		this.indexBrowser = indexBrowser;

		nmbFilesLabel = new JLabel(I18n.getMessage("thaw.plugin.index.numberOfFiles").replaceAll("\\?", ""));
		nmbLinksLabel = new JLabel(I18n.getMessage("thaw.plugin.index.numberOfLinks").replaceAll("\\?", ""));

		panel = new JPanel(new BorderLayout());

		JPanel stats = new JPanel(new GridLayout(1, 2));
		stats.add(nmbFilesLabel);
		stats.add(nmbLinksLabel);

		panel.add(stats, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
		buttonActions = new Vector(2);
		JButton button;

		viewCommentButton = new JButton(I18n.getMessage("thaw.plugin.index.comment.comments").replaceAll("\\?", "0"),
				     IconBox.minReadComments);
		buttonActions.add(new IndexManagementHelper.IndexCommentViewer(indexBrowser, viewCommentButton));
		buttonPanel.add(viewCommentButton);

		//button  = new JButton(I18n.getMessage("thaw.plugin.index.comment.add"),
		//		      IconBox.minAddComment);
		//buttonActions.add(new IndexManagementHelper.IndexCommentAdder(queueManager, indexBrowser, button));
		//buttonPanel.add(button);


		panel.add(buttonPanel, BorderLayout.EAST);
	}


	public JPanel getPanel() {
		return panel;
	}



	private void setIndexTarget(Index l) {

		viewCommentButton.setText(I18n.getMessage("thaw.plugin.index.comment.comments").replaceAll("\\?",
													   l == null ? "0" : Integer.toString(l.getNmbComments())));

		for (Iterator it = buttonActions.iterator();
		     it.hasNext();) {
			IndexManagementHelper.IndexAction action = (IndexManagementHelper.IndexAction)it.next();
			action.setTarget((Index)l);
		}
	}


	private void setStats(int nmbFiles, int nmbLinks) {
		nmbFilesLabel.setText(I18n.getMessage("thaw.plugin.index.numberOfFiles").replaceAll("\\?",
												    Integer.toString(nmbFiles)));
		nmbLinksLabel.setText(I18n.getMessage("thaw.plugin.index.numberOfLinks").replaceAll("\\?",
												    Integer.toString(nmbLinks)));
	}


	/* called by IndexBrowserPanel.setList() */
	public void setTarget(FileAndLinkList node) {
		if (node instanceof Index)
			setIndexTarget((Index)node);
		else
			setIndexTarget(null);

		int nmbFilesInt = 0;
		int nmbLinksInt = 0;

		if (node != null) {
			nmbFilesInt = node.getFileList(null, true).size();
			nmbLinksInt = node.getLinkList(null, true).size();
		}

		setStats(nmbFilesInt, nmbLinksInt);
	}


	/* called by IndexTree.valueChanged() */
	public void setTarget(IndexTreeNode node) {
		Hsqldb db = indexBrowser.getDb();
		PreparedStatement st;
		ResultSet rs;

		int nmbFilesInt = 0;
		int nmbLinksInt = 0;

		synchronized(db.dbLock) {
			try {
				if (node instanceof IndexFolder) {
					if (node instanceof IndexRoot) {
						st = db.getConnection().prepareStatement("SELECT count(id) from files");
						rs = st.executeQuery();
						rs.next();
						nmbFilesInt = rs.getInt(1);

						st = db.getConnection().prepareStatement("SELECT count(id) from links");
						rs = st.executeQuery();
						rs.next();
						nmbLinksInt = rs.getInt(1);
					} else {
						st = db.getConnection().prepareStatement("SELECT count(id) "+
											 "FROM files WHERE files.indexParent IN "+
											 "(SELECT indexParents.indexId "+
											 " FROM indexParents "+
											 " WHERE indexParents.folderId = ?)");


						st.setInt(1, node.getId());
						rs = st.executeQuery();
						rs.next();
						nmbFilesInt = rs.getInt(1);


						st = db.getConnection().prepareStatement("SELECT count(id) "+
											 "FROM links WHERE links.indexParent IN "+
											 "(SELECT indexParents.indexId "+
											 " FROM indexParents "+
											 " WHERE indexParents.folderId = ?)");
						st.setInt(1, node.getId());
						rs = st.executeQuery();
						rs.next();
						nmbLinksInt = rs.getInt(1);
					}


				} else if (node instanceof Index) {
					st = db.getConnection().prepareStatement("SELECT count(id) "+
										 "FROM files WHERE files.indexParent = ?");
					st.setInt(1, node.getId());
					rs = st.executeQuery();
					rs.next();
					nmbFilesInt = rs.getInt(1);


					st = db.getConnection().prepareStatement("SELECT count(id) "+
										 "FROM links WHERE links.indexParent = ?");
					st.setInt(1, node.getId());
					rs = st.executeQuery();
					rs.next();
					nmbLinksInt = rs.getInt(1);
				}

			} catch(SQLException e) {
				Logger.error(this, "Exception while counting files/links : "+e.toString());
			}
		}

		setStats(nmbFilesInt, nmbLinksInt);
	}

}

