package thaw.plugins.miniFrost;

import java.util.Vector;
import java.util.Iterator;
import java.util.Random;

import thaw.plugins.miniFrost.interfaces.Board;

import thaw.core.Logger;
import thaw.core.Config;
import thaw.core.ThawThread;

public class AutoRefresh implements Runnable {

	public static final int DEFAULT_MAX_BOARDS_REFRESHING = 7;
	public static final int DEFAULT_INTERVAL = 10; /* s */

	private boolean run;

	private int maxBoardRefreshing;
	private int interval;

	private Config config;
	private BoardTree boardTree;

	private Random random;

	public AutoRefresh(Config config, BoardTree boardTree) {
		this.config = config;
		this.boardTree = boardTree;

		run = true;

		maxBoardRefreshing = DEFAULT_MAX_BOARDS_REFRESHING;
		interval = DEFAULT_INTERVAL;

		random = new Random();

		if (config.getValue("miniFrostAutoRefreshMaxBoards") != null)
			maxBoardRefreshing = Integer.parseInt(config.getValue("miniFrostAutoRefreshMaxBoards"));
		if (config.getValue("miniFrostAutoRefreshInterval") != null)
			interval = Integer.parseInt(config.getValue("miniFrostAutoRefreshInterval"));

		Thread th = new ThawThread(this, "Board autorefresh", this);
		th.start();
	}


	public boolean canRefreshAnotherOne() {
		int refreshing = 0;

		for (Iterator it = boardTree.getBoards().iterator();
		     it.hasNext();) {
			if (((Board)it.next()).isRefreshing())
				refreshing++;

			if (refreshing >= maxBoardRefreshing)
				return false;
		}

		return true;
	}

	public boolean refreshAnotherOne() {
		int notRefreshing = 0;

		for (Iterator it = boardTree.getBoards().iterator();
		     it.hasNext();) {
			if (!(((Board)it.next()).isRefreshing()))
				notRefreshing++;
		}

		if (notRefreshing == 0)
			return false;

		int sel = random.nextInt(notRefreshing);


		Board board = null;

		int i = 0;

		for (Iterator it = boardTree.getBoards().iterator();
		     it.hasNext() && i <= sel ;) {
			board = (Board)it.next();

			if (!board.isRefreshing())
				i++;
		}

		if (board == null) {
			Logger.error(this, "Hm, error while selecting the board to refresh : "+
				     Integer.toString(sel) + " ; "+
				     Integer.toString(notRefreshing) + " ; "+
				     Integer.toString(boardTree.getBoards().size()));
			return false;
		}

		board.refresh();

		boardTree.refresh(board);

		return true;
	}


	public void run() {
		while(run) {
			try {
				Thread.sleep(interval * 1000);
			} catch(InterruptedException e) {
				Logger.notice(this, "Autorefresher interrupted ?!");
			}

			if (!run)
				return;

			try {
				if (canRefreshAnotherOne())
					refreshAnotherOne();
			} catch(Exception e) {
				Logger.error(this, "Error while autorefreshing : "+e.toString());
			}
		}
	}

	public void stop() {
		run = false;
	}

}
