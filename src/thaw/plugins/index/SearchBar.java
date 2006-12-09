package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import thaw.core.I18n;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;

public class SearchBar implements ActionListener {
	private JPanel panel;

	private JTextField userText;
	private JButton validationButton;

	private IndexBrowserPanel indexBrowser;

	private Tables tables;

	private FCPQueueManager queueManager;

	public SearchBar(final FCPQueueManager queueManager, IndexBrowserPanel indexBrowser) {
		this.indexBrowser = indexBrowser;
		this.queueManager = queueManager;

		panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));

		userText = new JTextField("");
		validationButton = new JButton("  "+I18n.getMessage("thaw.common.search")+"  ");

		final JLabel label = new JLabel(I18n.getMessage("thaw.plugin.index.search.label"));

		panel.add(label, BorderLayout.NORTH);
		panel.add(userText, BorderLayout.CENTER);
		panel.add(validationButton, BorderLayout.EAST);

		userText.addActionListener(this);
		validationButton.addActionListener(this);
	}

	public JPanel getPanel() {
		return panel;
	}

	public void actionPerformed(final ActionEvent e) {
		if (userText.getText() == null)
			return;

		userText.setSelectionStart(0);
		userText.setSelectionEnd(userText.getText().length());

		final SearchResult sr = new SearchResult(indexBrowser.getDb(), userText.getText(), indexBrowser.getIndexTree().getSelectedNode(), queueManager);
		indexBrowser.getTables().setList(sr);
	}

}
