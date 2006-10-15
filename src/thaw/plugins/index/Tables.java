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

		searchBar = new SearchBar(db, tree, this);

		panel.add(searchBar.getPanel(), BorderLayout.NORTH);
		panel.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					 linkTable.getPanel(),
					 fileTable.getPanel()));
	}


	protected FileTable getFileTable() {
		return fileTable;
	}

	protected LinkTable getLinkTable() {
		return linkTable;
	}

	public void setLinkList(LinkList linkList) {
		getLinkTable().setLinkList(linkList);
	}

	public void setFileList(FileList fileList) {
		getFileTable().setFileList(fileList);
	}

	public void setList(FileAndLinkList l) {
		setFileList(l);
		setLinkList(l);
	}


	public JPanel getPanel() {
		return panel;
	}



}
