package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.sql.*; /* I'm lazy */


import java.util.Vector;
import java.util.Iterator;


import thaw.core.Logger;
import thaw.core.Config;
import thaw.fcp.FCPQueueManager;
import thaw.gui.IconBox;
import thaw.core.I18n;


public class CommentTab implements ActionListener {
	private boolean visible;

	private Index index;

	private JPanel tabPanel;
	private JPanel centerPanel;

	private JButton closeTabButton;

	private Vector buttonActions;


	private IndexBrowserPanel indexBrowser;


	public CommentTab(Config config,
			  FCPQueueManager queueManager,
			  IndexBrowserPanel indexBrowser) {
		this.indexBrowser = indexBrowser;

		visible = false;

		tabPanel = new JPanel(new BorderLayout(10, 10));


		JPanel northPanel = new JPanel(new BorderLayout());

		closeTabButton = new JButton(IconBox.minClose);
		closeTabButton.setToolTipText(I18n.getMessage("thaw.common.closeTab"));
		closeTabButton.addActionListener(this);

		northPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.comment.commentList")+" :"));
		northPanel.add(closeTabButton, BorderLayout.EAST);

		JPanel southPanel = new JPanel(new BorderLayout());

		JButton button;

		buttonActions = new Vector();

		button = new JButton(I18n.getMessage("thaw.plugin.index.comment.add"), IconBox.minAddComment);
		buttonActions.add(new IndexManagementHelper.IndexCommentAdder(config, queueManager, indexBrowser, button));

		southPanel.add(new JLabel(""), BorderLayout.CENTER);
		southPanel.add(button, BorderLayout.EAST);


		centerPanel = new JPanel();

		tabPanel.add(northPanel,  BorderLayout.NORTH);
		tabPanel.add(centerPanel, BorderLayout.CENTER);
		tabPanel.add(southPanel,  BorderLayout.SOUTH);
	}



	public void updateCommentList() {
		centerPanel.removeAll();
		centerPanel.setLayout(new BorderLayout());

		JPanel insidePanel = new JPanel();

		Vector comments = null;

		if (index != null)
			comments = index.getComments();

		if (comments != null) {
			insidePanel.setLayout(new GridLayout(comments.size(), 1, 20, 20));

			for (Iterator it = comments.iterator();
			     it.hasNext();) {
				JPanel panel = ((Comment)it.next()).getPanel();

				insidePanel.add(panel);
			}
		}

		centerPanel.add(new JScrollPane(insidePanel), BorderLayout.NORTH);
		centerPanel.add(new JLabel(""), BorderLayout.CENTER);
	}


	/**
	 * will reset the page to 0
	 */
	public void setIndex(Index index) {
		for (Iterator it = buttonActions.iterator();
		     it.hasNext();) {
			IndexManagementHelper.IndexAction action = (IndexManagementHelper.IndexAction)it.next();
			action.setTarget((Index)index);
		}

		this.index = index;

		if (visible)
			updateCommentList();
	}


	public void showTab() {
		if (index == null || visible)
			return;

		Logger.info(this, "Showing comment tab");

		updateCommentList();

		indexBrowser.getMainWindow().addTab(I18n.getMessage("thaw.plugin.index.comment.commentList"),
						    tabPanel);
		indexBrowser.getMainWindow().setSelectedTab(tabPanel);

		visible = true;

	}


	public void hideTab() {
		if (!visible)
			return;

		Logger.info(this, "Hiding comment tab");

		indexBrowser.getMainWindow().removeTab(tabPanel);
		indexBrowser.getMainWindow().setSelectedTab(indexBrowser.getPanel());

		visible = false;
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == closeTabButton)
			hideTab();
	}

}
