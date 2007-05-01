package thaw.plugins.index;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import thaw.core.Config;
import thaw.fcp.FCPQueueManager;

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

	public Tables(final boolean modifiables, FCPQueueManager queueManager, IndexBrowserPanel indexBrowser, final Config config) {
		this.config = config;

		panel = new JPanel();
		panel.setLayout(new BorderLayout(10, 10));

		fileTable = new FileTable(queueManager, indexBrowser, config);
		linkTable = new LinkTable(queueManager, indexBrowser, config);

		searchBar = new SearchBar(indexBrowser);

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
		else
			split.setDividerLocation(100);
	}

	public void saveState() {
		config.setValue("indexFileLinkSplit", Integer.toString(split.getDividerLocation()));
	}

	public void stopRefresh() {
		fileTable.stopRefresher();
	}

	protected FileTable getFileTable() {
		return fileTable;
	}

	protected LinkTable getLinkTable() {
		return linkTable;
	}

	public void setLinkList(final LinkList linkList) {
		getLinkTable().setLinkList(linkList);
	}

	public void setFileList(final FileList fileList) {
		getFileTable().setFileList(fileList);
	}

	public void setList(final FileAndLinkList l) {
		setFileList(l);
		setLinkList(l);
	}

}
