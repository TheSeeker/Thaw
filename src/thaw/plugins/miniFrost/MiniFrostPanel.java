package thaw.plugins.miniFrost;

import javax.swing.JSplitPane;
import javax.swing.JComponent;

import java.util.Observer;
import java.util.Observable;

import java.util.Vector;
import java.util.Iterator;


import thaw.core.Config;
import thaw.plugins.Hsqldb;
import thaw.plugins.MiniFrost;

import thaw.plugins.miniFrost.interfaces.Board;
import thaw.plugins.miniFrost.interfaces.Draft;


/**
 * Some explanations :<br/>
 * At the beginning, I wanted to the UI like Frost, with a board tree,
 * etc. In the end, a board list is enought <br/>
 * So:
 * <ul>
 * <li>BoardTree => Board list</li>
 * </ul>
 */
public class MiniFrostPanel implements Observer {
	public final static int DEFAULT_VIEW = 0; /* 0 = Gmail ; 1 = Outlook */

	private Config config;
	private Hsqldb db;
	private BoardTree boardTree;

	private MessageTreeTable messageTreeTable;
	private MessagePanel messagePanel;
	private DraftPanel draftPanel;

	private MiniFrost pluginCore;

	private boolean gmailView = true;
	private JSplitPane mainSplit = null;
	private JSplitPane rightSplit = null;


	public MiniFrostPanel(Config config, Hsqldb db, MiniFrost pluginCore) {
		this.config = config;
		this.db = db;
		this.pluginCore = pluginCore;

		gmailView = (DEFAULT_VIEW == 0);

		if (config.getValue("miniFrostView") != null
		    && "1".equals(config.getValue("miniFrostView")))
			gmailView = false;

		/* board tree use some settings provided by the message tree table
		 * to count the unread messages => it must instanciated after */
		messageTreeTable = new MessageTreeTable(this);
		boardTree = new BoardTree(this);
		messagePanel = new MessagePanel(this);
		draftPanel = new DraftPanel(this);

		boardTree.addObserver(messageTreeTable);
		boardTree.addObserver(this);


		JComponent mainComponent;

		if (gmailView) {
			mainComponent = messageTreeTable.getPanel();
			rightSplit = null;
		} else {
			rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
						    messageTreeTable.getPanel(),
						    messagePanel.getPanel());
			mainComponent = rightSplit;
		}

		mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					   boardTree.getPanel(),
					   mainComponent);
	}


	public void displayDraftPanel() {
		saveState();

		messagePanel.hided();
		messageTreeTable.hided();

		if (gmailView)
			mainSplit.setRightComponent(draftPanel.getPanel());
		else
			rightSplit.setRightComponent(draftPanel.getPanel());

		//mainSplit.validate();

		draftPanel.redisplayed();

		loadState();
	}


	public void displayMessageTable() {
		saveState();

		messagePanel.hided();
		draftPanel.hided();

		if (gmailView)
			mainSplit.setRightComponent(messageTreeTable.getPanel());
		else {
			rightSplit.setLeftComponent(messageTreeTable.getPanel());
			rightSplit.setRightComponent(messagePanel.getPanel());
		}

		//mainSplit.validate();

		messageTreeTable.redisplayed();

		loadState();
	}

	public void displayMessage() {
		saveState();

		messageTreeTable.hided();
		draftPanel.hided();

		if (gmailView)
			mainSplit.setRightComponent(messagePanel.getPanel());
		else
			rightSplit.setRightComponent(messagePanel.getPanel());

		messagePanel.redisplayed();
		//mainSplit.validate();

		if (messagePanel.getMessage() != null
		    && !messagePanel.getMessage().isRead()) {
			messagePanel.getMessage().setRead(true);

			int row = messageTreeTable.getRow(messagePanel.getMessage());

			if (row >= 0)
				messageTreeTable.refresh(row);
			else
				messageTreeTable.refresh();

			boardTree.refresh(messagePanel.getMessage().getBoard());

		}

		messagePanel.redisplayed();

		loadState();
	}

	/**
	 * notify major changes : board added / removed
	 */
	public void notifyChange() {
		boardTree.refresh();
	}

	/**
	 * notify a change on this board. usually
	 * that a new non-read message has been added
	 */
	public void notifyChange(Board board) {
		boardTree.refresh(board);

		if (board == messageTreeTable.getBoard()) {
			messageTreeTable.refresh();
		}
	}


	public MiniFrost getPluginCore() {
		return pluginCore;
	}

	public JSplitPane getPanel() {
		return mainSplit;
	}


	public Config getConfig() {
		return config;
	}

	public Hsqldb getDb() {
		return db;
	}

	public BoardTree getBoardTree() {
		return boardTree;
	}

	public MessageTreeTable getMessageTreeTable() {
		return messageTreeTable;
	}

	public MessagePanel getMessagePanel() {
		return messagePanel;
	}

	public DraftPanel getDraftPanel() {
		return draftPanel;
	}

	public void loadState() {
		String val;

		mainSplit.setResizeWeight(0.5);

		if ((val = config.getValue("miniFrostMainSplitPosition")) != null)
			mainSplit.setDividerLocation(Integer.parseInt(val));
		else
			mainSplit.setDividerLocation(150);

		if (rightSplit != null) {
			rightSplit.setResizeWeight(0.5);

			if ((val = config.getValue("miniFrostRightSplitPosition")) != null)
				rightSplit.setDividerLocation(Integer.parseInt(val));
			else
				rightSplit.setDividerLocation(150);
		}

		boardTree.loadState();
	}


	public void saveState() {
		config.setValue("miniFrostMainSplitPosition",
				Integer.toString(mainSplit.getDividerLocation()));
		if (rightSplit != null) {
			config.setValue("miniFrostRightSplitPosition",
					Integer.toString(rightSplit.getDividerLocation()));
		}
		boardTree.saveState();
	}


	public void update(Observable o, Object param) {
		if (o == boardTree) {
			displayMessageTable();
		}
	}


	private Vector drafts = null;

	public void update(Draft draft) {
		if (drafts == null)
			drafts = new Vector();

		if (!draft.isPosting() && !draft.isWaiting())
			drafts.remove(draft);
		else if (drafts.indexOf(draft) < 0)
			drafts.add(draft);

		int waitings = 0;
		int postings = 0;

		for (Iterator it = drafts.iterator();
		     it.hasNext();) {
			Draft draftIt = (Draft)it.next();

			if (draftIt.isPosting()) postings++;
			if (draftIt.isWaiting()) waitings++;
		}

		boardTree.updateDraftValues(waitings, postings);
	}

	public boolean isInGmailView() {
		return gmailView;
	}
}
