package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;

import thaw.fcp.FCPQueueManager;

import thaw.core.Config;
import thaw.plugins.Hsqldb;

/**
 * Contains a FileTable, a LinkTable, and a SearchBar
 */
public class Tables {
	private JPanel panel;
	
	private SearchBar searchBar;
	private FileTable fileTable;
	private LinkTable linkTable;

	private JSplitPane split;
	private Config config;

	public Tables(boolean modifiables, Hsqldb db, FCPQueueManager queueManager, IndexTree tree, Config config) {
		this.config = config;

		panel = new JPanel();
		panel.setLayout(new BorderLayout(10, 10));

		fileTable = new FileTable(modifiables, queueManager);
		linkTable = new LinkTable(modifiables, db, queueManager, tree);

		searchBar = new SearchBar(db, tree, this);

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					 linkTable.getPanel(),
				       fileTable.getPanel());

		panel.add(searchBar.getPanel(), BorderLayout.NORTH);
		panel.add(split, BorderLayout.CENTER);
	}

	public JPanel getPanel() {
		return panel;
	}

	public void restoreState() {
		if (config.getValue("indexFileLinkSplit") != null)
			split.setDividerLocation(Integer.parseInt(config.getValue("indexFileLinkSplit")));
	}

	public void saveState() {
		config.setValue("indexFileLinkSplit", Integer.toString(split.getDividerLocation()));
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

}
