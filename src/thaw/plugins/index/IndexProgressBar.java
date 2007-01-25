package thaw.plugins.index;

import javax.swing.JProgressBar;

import java.util.Vector;
import java.util.Iterator;

import java.util.Observable;
import java.util.Observer;

import thaw.core.Logger;
import thaw.fcp.FCPTransferQuery;

public class IndexProgressBar {

	private JProgressBar progressBar;

	private int total;
	private int current;

	private int nmbTransfers;

	public IndexProgressBar() {
		progressBar = new JProgressBar(0, 100);
		total = 0;
		current = 0;
		nmbTransfers = 0;
		refreshBar();
		progressBar.setStringPainted(true);
	}

	public void addTransfer(int nmb) {
		Logger.info(this, "Adding "+Integer.toString(nmb)+" transfers");

		total += nmb;
		current += 0;
		nmbTransfers += nmb;

		refreshBar();
	}


	public void removeTransfer(int nmb) {
		Logger.info(this, "Removing "+Integer.toString(nmb)+" transfers");

		nmbTransfers -= nmb;

		if (nmbTransfers <= 0) {
			total = 0;
			current = 0;
		}
		else {
			current++;
		}

		refreshBar();
	}

	public void refreshBar() {
		int pourcent;

		if (total == 0) {
			progressBar.setValue(0);
			progressBar.setString("");
		} else {
			pourcent = (current * 100) / total;
			progressBar.setValue(pourcent);
			progressBar.setString(Integer.toString(pourcent) + " %");
		}
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}
}


