package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;

import thaw.fcp.FCPQueueManager;

import thaw.plugins.Hsqldb;

/**
 * Contains a FileTable, a LinkTable, and a SearchBar
 */
public class Tables {
	private JPanel panel;
	
	private SearchBar searchBar;
	private FileTable fileTable;
	private LinkTable linkTable;

	public Tables(boolean modifiables, Hsqldb db, FCPQueueManager queueManager, IndexTree tree) {
		panel = new JPanel();
		panel.setLayout(new BorderLayout(10, 10));

		fileTable = new FileTable(modifiables, queueManager);
		linkTable = new LinkTable(modifiables, db, queueManager, tree);

		searchBar = new SearchBar(fileTable, linkTable);

		panel.add(searchBar.getPanel(), BorderLayout.NORTH);
		panel.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					 linkTable.getPanel(),
					 fileTable.getPanel()));
	}


	public FileTable getFileTable() {
		return fileTable;
	}

	public LinkTable getLinkTable() {
		return linkTable;
	}

	public JPanel getPanel() {
		return panel;
	}



}
