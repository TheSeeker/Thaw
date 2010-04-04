package thaw.gui;

import javax.swing.JProgressBar;

import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPClientGet;

import thaw.core.I18n;

public class TransferProgressBar extends JProgressBar {
	private static final long serialVersionUID = -4726613087699822787L;
	private FCPTransferQuery query;
	private boolean statusInProgressBar;
	private boolean withBorder;
	
	private final static String failedStr = I18n.getMessage("thaw.common.failed");
	private final static String finishedStr = I18n.getMessage("thaw.common.finished");
	

	public TransferProgressBar(FCPTransferQuery q) {
		this(q, true);
	}

	public TransferProgressBar(FCPTransferQuery query, boolean statusInProgressBar,
				   				boolean withBorder) {
		super(0, 100);
		this.query = query;
		this.statusInProgressBar = statusInProgressBar;
		this.withBorder = withBorder;
	}

	public TransferProgressBar(FCPTransferQuery query, boolean statusInProgressBar) {
		this(query, statusInProgressBar, false);
	}

	public void setQuery(FCPTransferQuery query) {
		this.query = query;
	}

	public void showStatusInProgressBar(boolean v) {
		this.statusInProgressBar = v;
	}


	public void refresh() {

		int progress = query.getProgression();

		setStringPainted(true);
		setBorderPainted(withBorder);

		if ((query instanceof FCPClientPut && ( progress == 0))
		    || ((query instanceof FCPClientGet) && (progress == 100)))
			progress = query.getTransferWithTheNodeProgression();

		setValue(progress);

		if(!query.isFinished()) {
			String txt= "";
			if (statusInProgressBar) {
				txt = (query.getStatus() +
					      " [ "+Integer.toString(progress)+"% ]");
			} else {
				txt = (Integer.toString(progress)+"%");
			}
			if (!query.isProgressionReliable()) {
				txt += " [*]";
			}
			setString(txt);
		} else if(query.isSuccessful()) {
			setString(finishedStr);
		} else {
			setString(failedStr);
		}
	}
}
