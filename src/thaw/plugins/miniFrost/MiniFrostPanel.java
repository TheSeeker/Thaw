package thaw.plugins.miniFrost;

import javax.swing.JSplitPane;
import javax.swing.JLabel;

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
 * At the beginning, I wanted to the UI like Frost, with a board tree, a message tree,
 * etc. In the end, I will do like Gmail : Smart. :) <br/>
 * So:
 * <ul>
 * <li>BoardTree => Board list</li>
 * <li>MessageTreeTable => message table</li>
 * </ul>
 */
public class MiniFrostPanel implements Observer {
	private Config config;
	private Hsqldb db;
	private BoardTree boardTree;

	private MessageTreeTable messageTreeTable;
	private MessagePanel messagePanel;
	private DraftPanel draftPanel;

	private MiniFrost pluginCore;

	private JSplitPane mainSplit;


	public MiniFrostPanel(Config config, Hsqldb db, MiniFrost pluginCore) {
		this.config = config;
		this.db = db;
		this.pluginCore = pluginCore;

		boardTree = new BoardTree(this);
		messageTreeTable = new MessageTreeTable(this);
		messagePanel = new MessagePanel(this);
		draftPanel = new DraftPanel(this);

		boardTree.addObserver(this);

		/* so it will be vertical ... don't ask me why ... */
		mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					   boardTree.getPanel(),
					   messageTreeTable.getPanel());
	}


	public void displayDraftPanel() {
		saveState();

		messagePanel.hided();
		messageTreeTable.hided();

		mainSplit.setRightComponent(draftPanel.getPanel());
		mainSplit.validate();

		draftPanel.redisplayed();

		loadState();
	}


	public void displayMessageTable() {
		saveState();

		messagePanel.hided();
		draftPanel.hided();

		mainSplit.setRightComponent(messageTreeTable.getPanel());
		mainSplit.validate();

		messageTreeTable.redisplayed();

		loadState();
	}

	public void displayMessage() {
		saveState();

		messageTreeTable.hided();
		draftPanel.hided();

		mainSplit.setRightComponent(messagePanel.getPanel());
		messagePanel.redisplayed();
		mainSplit.validate();

		if (messagePanel.getMessage() != null
		    && !messagePanel.getMessage().isRead()) {
			messagePanel.getMessage().setRead(true);
			messageTreeTable.refresh(messagePanel.getMessage());
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

		boardTree.loadState();
	}


	public void saveState() {
		config.setValue("miniFrostMainSplitPosition",
				Integer.toString(mainSplit.getDividerLocation()));
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
}
