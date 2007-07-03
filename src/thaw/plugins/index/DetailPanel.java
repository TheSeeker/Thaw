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
	private JButton detailsButton;

	private Vector buttonActions;

	private JLabel nmbFilesLabel;
	private JLabel nmbLinksLabel;
	private JLabel insertionDateLabel;

	private DateFormat dateFormat;


	public DetailPanel(FCPQueueManager queueManager, IndexBrowserPanel indexBrowser) {
		this.dateFormat = DateFormat.getDateInstance();
		this.indexBrowser = indexBrowser;

		panel = new JPanel(new BorderLayout());

		panel.add(new JLabel(""), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
		buttonActions = new Vector(2);
		JButton button;

		detailsButton = new JButton(I18n.getMessage("thaw.plugin.index.details"),
					    IconBox.minDetails);
		buttonActions.add(new IndexManagementHelper.IndexDetailsViewer(indexBrowser, detailsButton));
		buttonPanel.add(detailsButton);

		viewCommentButton = new JButton(I18n.getMessage("thaw.plugin.index.comment.comments").replaceAll("\\?", "0"),
						IconBox.minReadComments);
		buttonActions.add(new IndexManagementHelper.IndexCommentViewer(indexBrowser, viewCommentButton));
		buttonPanel.add(viewCommentButton);

		panel.add(buttonPanel, BorderLayout.EAST);
	}


	public JPanel getPanel() {
		return panel;
	}



	public void setTarget(IndexTreeNode l) {

		if (l != null && l instanceof Index)
			viewCommentButton.setText(I18n.getMessage("thaw.plugin.index.comment.comments").replaceAll("\\?",
														   Integer.toString(((Index)l).getNmbComments())));
		else
			viewCommentButton.setText(I18n.getMessage("thaw.plugin.index.comment.comments").replaceAll("\\?",
														   "0"));

		for (Iterator it = buttonActions.iterator();
		     it.hasNext();) {
			IndexManagementHelper.IndexAction action = (IndexManagementHelper.IndexAction)it.next();
			action.setTarget(l);
		}
	}


	/* called by IndexBrowserPanel.setList() */
	public void setTarget(FileAndLinkList node) {
		if (node instanceof Index) {
			setTarget((IndexTreeNode)node);
		} else {
			setTarget((IndexTreeNode)null);
		}
	}

}

