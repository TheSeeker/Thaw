package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import thaw.core.I18n;

public class SearchBar implements ActionListener {
	private JPanel panel;

	private JTextField userText;
	private JButton validationButton;

	private IndexBrowserPanel indexBrowser;

	public SearchBar(IndexBrowserPanel indexBrowser) {
		this.indexBrowser = indexBrowser;

		panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));

		userText = new JTextField("");
		validationButton = new JButton(I18n.getMessage("thaw.common.search"),
					       thaw.gui.IconBox.minSearch);

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

	public void clear() {
		userText.setText("");
	}

	public void actionPerformed(final ActionEvent e) {
		if (userText.getText() == null)
			return;

		userText.setSelectionStart(0);
		userText.setSelectionEnd(userText.getText().length());

		IndexTreeNode node = indexBrowser.getIndexTree().getSelectedNode();

		if (node == null)
			node = indexBrowser.getIndexTree().getRoot();

		final SearchResult sr = new SearchResult(indexBrowser.getDb(),
							 userText.getText().trim(),
							 node);
		indexBrowser.getTables().setList(sr);
	}

}
