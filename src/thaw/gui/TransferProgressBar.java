package thaw.gui;

import javax.swing.JProgressBar;

import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPClientGet;

import thaw.core.I18n;

public class TransferProgressBar extends JProgressBar {
	private FCPTransferQuery query;
	private boolean statusInProgressBar;
	private boolean withBorder;

	public TransferProgressBar(FCPTransferQuery q) {
		this(q, true);
	}

	public TransferProgressBar(FCPTransferQuery query, boolean statusInProgressBar,
				   boolean withBorder) {
		super(0, 100);
		this.query = query;
		this.statusInProgressBar = statusInProgressBar;
		this.withBorder = withBorder;

		refresh();
	}

	public TransferProgressBar(FCPTransferQuery query, boolean statusInProgressBar) {
		this(query, statusInProgressBar, false);
	}

	public void refresh() {

		int progress;

		setStringPainted(true);
		setBorderPainted(withBorder);

		if ((query instanceof FCPClientPut && (query.getTransferWithTheNodeProgression() < 100))
		    || ((query instanceof FCPClientGet) && (query.getTransferWithTheNodeProgression() > 0)))
			progress = query.getTransferWithTheNodeProgression();
		else
			progress = query.getProgression();

		setValue(progress);

		if(query.isFinished() && !query.isSuccessful())
			setString(I18n.getMessage("thaw.common.failed"));
		else if(query.isFinished() && query.isSuccessful())
			setString(I18n.getMessage("thaw.common.finished"));
		else if(!query.isFinished()) {
			String txt= "";

			if (statusInProgressBar)
				txt = (query.getStatus() +
					      " [ "+Integer.toString(progress)+"% ]");
			else
				txt = (Integer.toString(progress)+"%");

			if (!query.isProgressionReliable())
				txt += " [*]";

			setString(txt);
		}

	}
}
