package thaw.plugins.miniFrost;

import java.util.Collections;
import java.util.Vector;

import javax.swing.JOptionPane;

import thaw.core.Core;
import thaw.core.MainWindow;
import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.plugins.MiniFrost;
import thaw.plugins.WebOfTrust;
import thaw.plugins.miniFrost.interfaces.BoardAttachment;
import thaw.plugins.miniFrost.interfaces.BoardFactory;

public class KnownBoardListProvider implements BoardFactory {
	private Hsqldb db;
	private Core core;
	private MiniFrost miniFrost;
	
	public KnownBoardListProvider() {
		
	}

	public boolean init(Hsqldb db, Core core, WebOfTrust wot, MiniFrost miniFrost) {
		this.db = db;
		this.core = core;
		this.miniFrost = miniFrost;

		return true;
	}
	
	public boolean cleanUp(int archiveAfter, int deleteAfter) {

		return true;
	}

	public void createBoard(MainWindow mainWindow) {
		Vector boardList = new Vector();
		BoardFactory[] factories = miniFrost.getFactories();
		
		for (int i = 0; i < factories.length ; i++) {
			boardList.addAll(factories[i].getAllKnownBoards());
		}
		
		Collections.sort(boardList);
		
		Object[] boardListAr = boardList.toArray();
		
		if (boardListAr.length <= 0) {
			new thaw.gui.WarningWindow(mainWindow, I18n.getMessage("thaw.plugin.miniFrost.knownBoard.none"));
			return;
		}
		
		BoardAttachment selection = (BoardAttachment)JOptionPane.showInputDialog(mainWindow.getMainFrame(),
			      I18n.getMessage("thaw.plugin.miniFrost.knownBoard.select"),
			      I18n.getMessage("thaw.plugin.miniFrost.knownBoard.select"),
			      JOptionPane.QUESTION_MESSAGE,
			      null, /* icon */
			      boardListAr,
			      boardListAr[0]);
		
		if (selection == null)
			return;
		
		selection.addBoard(db, core.getQueueManager());
		
		miniFrost.getPanel().getBoardTree().refresh();
	}

	public Vector getAllKnownBoards() {
		return new Vector();
	}

	public Vector getAllMessages(String[] keywords, int orderBy, boolean desc,
			boolean archived, boolean read, boolean unsigned, int minTrustLevel) {

		return new Vector();
	}

	public Vector getBoards() {

		return new Vector();
	}

	public Vector getSentMessages() {
		return new Vector();
	}

	public String toString() {
		return "["+I18n.getMessage("thaw.plugin.miniFrost.knownBoard")+"]";
	}
}
