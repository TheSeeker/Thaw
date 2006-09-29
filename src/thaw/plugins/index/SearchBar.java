package thaw.plugins.index;

import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.BorderLayout;

import thaw.core.I18n;

public class SearchBar {
	private JPanel panel;

	private JTextField userText;
	private JButton validationButton;

	public SearchBar(FileTable fileTable, LinkTable linkTable) {
		panel = new JPanel();
		panel.setLayout(new BorderLayout(10, 10));

		userText = new JTextField("");
		validationButton = new JButton("  "+I18n.getMessage("thaw.common.search")+"  ");

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.search.label")), BorderLayout.WEST);
		panel.add(userText, BorderLayout.CENTER);
		panel.add(validationButton, BorderLayout.EAST);
	}

	public JPanel getPanel() {
		return panel;
	}

}
