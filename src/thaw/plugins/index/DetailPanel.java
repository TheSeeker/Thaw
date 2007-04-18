package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.I18n;
import thaw.core.Logger;

import thaw.fcp.FCPQueueManager;


/**
 * Initially, I wanted to use it to show details about the
 * currently-viewed index, but in the end it will mostly
 * be used for the comments
 */
public class DetailPanel {
	private JPanel panel;

	private Vector buttonActions;


	public DetailPanel(FCPQueueManager queueManager, IndexBrowserPanel indexBrowser) {
		panel = new JPanel(new BorderLayout());

		panel.add(new JLabel(""), BorderLayout.CENTER); /* because we need something */

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonActions = new Vector(2);
		JButton button;

		button = new JButton(I18n.getMessage("thaw.plugin.index.comment.comments").replaceAll("\\?", "0"));
		buttonActions.add(new IndexManagementHelper.IndexCommentViewer(button));
		buttonPanel.add(button);

		button  = new JButton(I18n.getMessage("thaw.plugin.index.comment.add"));
		buttonActions.add(new IndexManagementHelper.IndexCommentAdder(queueManager, indexBrowser, button));
		buttonPanel.add(button);


		panel.add(buttonPanel, BorderLayout.EAST);
	}


	public JPanel getPanel() {
		return panel;
	}



	public void setIndexTarget(Index l) {
		for (Iterator it = buttonActions.iterator();
		     it.hasNext();) {
			IndexManagementHelper.IndexAction action = (IndexManagementHelper.IndexAction)it.next();
			action.setTarget((Index)l);
		}
	}

}

