package thaw.plugins.index;

import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import thaw.core.I18n;

import thaw.fcp.FCPQueueManager;

import thaw.plugins.Hsqldb;

public class SearchBar implements ActionListener {
	private JPanel panel;

	private JTextField userText;
	private JButton validationButton;

	private Hsqldb db;
	private IndexTree tree;

	private Tables tables;

	private FCPQueueManager queueManager;

	public SearchBar(Hsqldb db, IndexTree indexTree, FCPQueueManager queueManager, Tables tables) {
		this.db = db;
		this.tree = indexTree;
		this.tables = tables;
		this.queueManager = queueManager;

		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout(10, 10));

		this.userText = new JTextField("");
		this.validationButton = new JButton("  "+I18n.getMessage("thaw.common.search")+"  ");

		this.panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.search.label")), BorderLayout.WEST);
		this.panel.add(this.userText, BorderLayout.CENTER);
		this.panel.add(this.validationButton, BorderLayout.EAST);

		this.userText.addActionListener(this);
		this.validationButton.addActionListener(this);
	}

	public JPanel getPanel() {
		return this.panel;
	}

	public void actionPerformed(ActionEvent e) {
		if (this.userText.getText() == null)
			return;

		this.userText.setSelectionStart(0);
		this.userText.setSelectionEnd(this.userText.getText().length());

		SearchResult sr = new SearchResult(this.db, this.userText.getText(), this.tree.getSelectedNode(), this.queueManager);
		this.tables.setList(sr);
	}

}
