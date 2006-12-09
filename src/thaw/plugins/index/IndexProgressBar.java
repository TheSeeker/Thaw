package thaw.plugins.index;

import javax.swing.JProgressBar;

import java.util.Vector;
import java.util.Iterator;

import java.util.Observable;
import java.util.Observer;

import thaw.fcp.FCPTransferQuery;

public class IndexProgressBar implements Observer {

	private JProgressBar progressBar;

	private int total;
	private int offset;
	private int current;

	private Vector transfers;
	private int nmbTransfers;

	public IndexProgressBar() {
		progressBar = new JProgressBar(0, 100);
		total = 0;
		current = 0;
		nmbTransfers = 0;
		offset = 0;
		transfers = new Vector();
		refreshBar();
		progressBar.setStringPainted(true);
	}

	public void addTransfer(FCPTransferQuery query) {
		if (!(query instanceof Observable))
			return;

		((Observable)query).addObserver(this);

		total += 100;
		current += query.getProgression();
		nmbTransfers++;
		transfers.add(query);

		refreshBar();
	}

	/**
	 * Is usually called automatically
	 */
	public void removeTransfer(FCPTransferQuery query) {
		if (!(query instanceof Observable))
			return;

		((Observable)query).deleteObserver(this);

		nmbTransfers--;

		if (nmbTransfers == 0) {
			total = 0;
			current = 0;
			offset = 0;
		}
		else {
			offset += 100;
		}

		transfers.remove(query);

		refreshBar();
	}

	public void refreshBar() {
		int pourcent;

		if (total == 0 || current == total) {
			progressBar.setValue(0);
			progressBar.setString("");
		} else {
			pourcent = (current * 100) / total;
			progressBar.setValue(pourcent);
			progressBar.setString(Integer.toString(pourcent) + " %");
		}
	}

	public void update(Observable o, Object arg) {
		if (o instanceof FCPTransferQuery) {
			FCPTransferQuery query = (FCPTransferQuery)o;

			current = offset;
			total = offset;

			for (Iterator it = transfers.iterator();
			     it.hasNext(); ) {
				FCPTransferQuery q = (FCPTransferQuery)it.next();
				current += q.getProgression();
				total += 100;
			}

			if (query.isFinished())
				removeTransfer(query);
		}
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}
}


