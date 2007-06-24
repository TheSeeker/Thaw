package thaw.gui;

import javax.swing.JProgressBar;

import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPClientGet;

import thaw.core.I18n;

public class TransferProgressBar extends JProgressBar {
	private FCPTransferQuery query;
	private boolean statusInProgressBar;

	public TransferProgressBar(FCPTransferQuery q) {
		this(q, true);
	}

	public TransferProgressBar(FCPTransferQuery query, boolean statusInProgressBar) {
		super(0, 100);
		this.query = query;
		this.statusInProgressBar = statusInProgressBar;

		refresh();
	}

	public void refresh() {

		int progress;

		setStringPainted(true);
		setBorderPainted(false);

		if ((query instanceof FCPClientPut && (query.getTransferWithTheNodeProgression() < 100))
		    || ((query instanceof FCPClientGet) && (query.getTransferWithTheNodeProgression() > 0)))
			progress = query.getTransferWithTheNodeProgression();
		else
			progress = query.getProgression();

		setValue(progress);

		if(query.isFinished() && !query.isSuccessful())
			setString(I18n.getMessage("thaw.common.failed"));

		if(query.isFinished() && query.isSuccessful())
			setString(I18n.getMessage("thaw.common.finished"));

		if(!query.isFinished()) {
			if (statusInProgressBar)
				setString(query.getStatus() +
					      " [ "+Integer.toString(progress)+"% ]");
			else
				setString(Integer.toString(progress)+"%");
		}
	}
}
