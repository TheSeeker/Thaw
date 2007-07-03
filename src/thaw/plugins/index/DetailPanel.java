package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;

import java.util.Vector;
import java.util.Iterator;

import java.sql.*;

import java.text.DateFormat;


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
	private JLabel insertionDateLabel;

	private DateFormat dateFormat;


	public DetailPanel(FCPQueueManager queueManager, IndexBrowserPanel indexBrowser) {
		this.dateFormat = DateFormat.getDateInstance();
		this.indexBrowser = indexBrowser;

		nmbFilesLabel = new JLabel(I18n.getMessage("thaw.plugin.index.numberOfFiles").replaceAll("\\?", ""));
		nmbLinksLabel = new JLabel(I18n.getMessage("thaw.plugin.index.numberOfLinks").replaceAll("\\?", ""));
		insertionDateLabel = new JLabel(I18n.getMessage("thaw.plugin.index.insertionDate").replaceAll("\\?", ""));

		panel = new JPanel(new BorderLayout());

		JPanel stats = new JPanel(new GridLayout(1, 3));
		stats.add(nmbFilesLabel);
		stats.add(nmbLinksLabel);
		stats.add(insertionDateLabel);

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


	private void setInsertionDate(Index index) {
		if (index == null) {
			insertionDateLabel.setText(I18n.getMessage("thaw.plugin.index.insertionDate").replaceAll("\\?", ""));
			return;
		}

		String dateStr = null;

		java.sql.Date dateSql = index.getDate();

		if (dateSql != null)
			dateStr = dateFormat.format(dateSql);

		if (dateStr != null)
			insertionDateLabel.setText(I18n.getMessage("thaw.plugin.index.insertionDate").replaceAll("\\?", dateStr));
		else if (dateSql != null) {
			Logger.warning(this, "There is a date in the db, but I'm unable to print it");
		}
	}


	/* called by IndexBrowserPanel.setList() */
	public void setTarget(FileAndLinkList node) {
		if (node instanceof Index) {
			setIndexTarget((Index)node);
			setInsertionDate((Index)node);
		} else {
			setIndexTarget(null);
			setInsertionDate(null);
		}

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

					setInsertionDate(null);


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

					setInsertionDate((Index)node);

				}

			} catch(SQLException e) {
				Logger.error(this, "Exception while counting files/links : "+e.toString());
			}
		}

		setStats(nmbFilesInt, nmbLinksInt);
	}

}

