package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JButton;


/**
 * Initially, I wanted to use it to show details about the
 * currently-viewed index, but in the end it will mostly
 * be used for the comments
 */
public class DetailPanel {
	private JPanel panel;

	private JButton seeComment;
	private JButton addComment;

	public DetailPanel() {
		panel = new JPanel();
	}


	public JPanel getPanel() {
		return panel;
	}


	public void setList(final FileAndLinkList l) {

	}

}

