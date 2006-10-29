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

		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout(10, 10));

		this.fileTable = new FileTable(modifiables, queueManager, tree, config, this);
		this.linkTable = new LinkTable(modifiables, db, queueManager, tree);

		this.searchBar = new SearchBar(db, tree, queueManager, this);

		this.split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				       this.linkTable.getPanel(),
				       this.fileTable.getPanel());

		this.panel.add(this.searchBar.getPanel(), BorderLayout.NORTH);
		this.panel.add(this.split, BorderLayout.CENTER);
	}

	public JPanel getPanel() {
		return this.panel;
	}

	public void restoreState() {
		if (this.config.getValue("indexFileLinkSplit") != null)
			this.split.setDividerLocation(Integer.parseInt(this.config.getValue("indexFileLinkSplit")));
	}

	public void saveState() {
		this.config.setValue("indexFileLinkSplit", Integer.toString(this.split.getDividerLocation()));
	}

	protected FileTable getFileTable() {
		return this.fileTable;
	}

	protected LinkTable getLinkTable() {
		return this.linkTable;
	}

	public void setLinkList(LinkList linkList) {
		this.getLinkTable().setLinkList(linkList);
	}

	public void setFileList(FileList fileList) {
		this.getFileTable().setFileList(fileList);
	}

	public void setList(FileAndLinkList l) {
		this.setFileList(l);
		this.setLinkList(l);
	}

}
