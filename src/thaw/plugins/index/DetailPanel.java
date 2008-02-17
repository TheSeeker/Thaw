package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.I18n;
import thaw.gui.IconBox;

import thaw.fcp.FCPQueueManager;


/**
 * Initially, I wanted to use it to show details about the
 * currently-viewed index, but in the end it will mostly
 * be used for the comments
 */
public class DetailPanel {
	private JPanel panel;

	private JButton viewCommentButton;
	private JButton detailsButton;

	private Vector buttonActions;

	public DetailPanel(FCPQueueManager queueManager, IndexBrowserPanel indexBrowser) {

		panel = new JPanel(new BorderLayout());

		panel.add(new JLabel(""), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
		buttonActions = new Vector(2);

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



	public void setTargets(Vector targets) {
		
		IndexTreeNode l;
		
		if (targets.size() != 1)
			l = null;
		else
			l = (IndexTreeNode)targets.get(0);
		
		setTarget(l);
	}

	public void setTarget(IndexTreeNode l) {
		
		if (l != null && l instanceof Index)
			viewCommentButton.setText(I18n.getMessage("thaw.plugin.index.comment.comments").replaceAll("\\?",
														   Integer.toString(((Index)l).getNmbComments())));
		else
			viewCommentButton.setText(I18n.getMessage("thaw.plugin.index.comment.comments").replaceAll("\\?",
														   "0"));
		
		Vector v = new Vector();
		if (l != null)
			v.add(l);

		for (Iterator it = buttonActions.iterator();
		     it.hasNext();) {
			IndexManagementHelper.IndexAction action = (IndexManagementHelper.IndexAction)it.next();
			action.setTargets(v);
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

