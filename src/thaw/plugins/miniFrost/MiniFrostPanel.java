package thaw.plugins.miniFrost;

import javax.swing.JSplitPane;
import javax.swing.JLabel;

import java.util.Observer;
import java.util.Observable;

import thaw.core.Config;
import thaw.plugins.Hsqldb;
import thaw.plugins.MiniFrost;

import thaw.plugins.miniFrost.interfaces.Board;


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
	private MiniFrost pluginCore;

	private JSplitPane mainSplit;


	public MiniFrostPanel(Config config, Hsqldb db, MiniFrost pluginCore) {
		this.config = config;
		this.db = db;
		this.pluginCore = pluginCore;

		boardTree = new BoardTree(this);
		messageTreeTable = new MessageTreeTable(this);
		messagePanel = new MessagePanel(this);

		boardTree.addObserver(this);

		mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, /* so it will be vertical ... don't ask me why ... */
					   boardTree.getPanel(),
					   messageTreeTable.getPanel());
	}

	public void displayMessageTable() {
		saveState();
		mainSplit.setRightComponent(messageTreeTable.getPanel());
		mainSplit.validate();
		loadState();
	}

	public void displayMessage() {
		saveState();
		mainSplit.setRightComponent(messagePanel.getPanel());
		mainSplit.validate();

		if (messagePanel.getMessage() != null) {
			messagePanel.getMessage().setRead(true);
			messageTreeTable.refresh(messagePanel.getMessage());
			boardTree.refresh(messagePanel.getMessage().getBoard());
		}

		loadState();
	}


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

}
