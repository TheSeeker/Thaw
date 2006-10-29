package thaw.plugins.queueWatcher;

import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JProgressBar;
import javax.swing.JComponent;

import java.util.Observer;
import java.util.Observable;

import thaw.core.*;
import thaw.fcp.*;


/**
 * Right panel of queueWatcher plugin. Show details about a transfer.
 */
public class DetailPanel implements Observer {

	private Core core;

	private JPanel subPanel;
	private JPanel panel;

	private JTextField file       = new JTextField();
	private JTextField size       = new JTextField();
	private JProgressBar progress = new JProgressBar(0, 100);
	private JProgressBar withTheNodeProgress = new JProgressBar(0, 100);
	private JTextField status     = new JTextField();
	private JTextField key        = new JTextField();
	private JTextField path       = new JTextField();
	private JTextField priority   = new JTextField();
	private JTextField identifier = new JTextField();
	private JTextField globalQueue= new JTextField();

	private FCPTransferQuery query = null;


	private final static Dimension dim = new Dimension(300, 400);


	public DetailPanel(Core core) {
		this.core = core;

		this.panel = new JPanel();
		this.subPanel = new JPanel();

		String[] fieldNames = { I18n.getMessage("thaw.common.file"),
					I18n.getMessage("thaw.common.size"),
					I18n.getMessage("thaw.common.progress"),
					I18n.getMessage("thaw.common.withTheNodeProgress"),
					I18n.getMessage("thaw.common.status"),
					I18n.getMessage("thaw.common.key"),
					I18n.getMessage("thaw.common.localPath"),
					I18n.getMessage("thaw.common.priority"),
					I18n.getMessage("thaw.common.identifier"),
					I18n.getMessage("thaw.common.globalQueue")
		};

		this.subPanel.setLayout(new GridLayout(fieldNames.length*2, 1));

		for(int i=0; i < (fieldNames.length * 2) ; i++) {

			if(i%2 == 0) {
				JLabel newLabel = new JLabel(fieldNames[i/2]);
				this.subPanel.add(newLabel);
			} else {
				JComponent field = null;

				switch((i/2)) {
				case(0): field = this.file; this.file.setEditable(false); break;
				case(1): field = this.size; this.size.setEditable(false); break;
				case(2):
					field = this.progress;
					this.progress.setString("");
					this.progress.setStringPainted(true);
					break;
				case(3):
					field = this.withTheNodeProgress;
					this.withTheNodeProgress.setString("");
					this.withTheNodeProgress.setStringPainted(true);
					break;
				case(4): field = this.status; this.status.setEditable(false); break;
				case(5): field = this.key; this.key.setEditable(false);break;
				case(6): field = this.path; this.path.setEditable(false); break;
				case(7): field = this.priority; this.priority.setEditable(false); break;
				case(8): field = this.identifier; this.identifier.setEditable(false); break;
				case(9): field = this.globalQueue; this.globalQueue.setEditable(false); break;
				default: Logger.error(this, "Gouli goula ? ... is going to crash :p"); break;
				}

				this.subPanel.add(field);
			}

		} /* for (i < fieldNames.length) */

		this.subPanel.setPreferredSize(dim);

		this.panel.add(this.subPanel);

	}


	public JPanel getPanel() {
		return this.panel;
	}


	public void setQuery(FCPTransferQuery query) {
		if(this.query != null)
			((Observable)this.query).deleteObserver(this);

		this.query = query;

		if(this.query != null)
			((Observable)this.query).addObserver(this);

		this.refreshAll();
	}

	public void update(Observable o, Object arg) {
		this.refresh();
	}


	public void refresh() {
		if(this.query != null) {
			this.withTheNodeProgress.setValue(this.query.getTransferWithTheNodeProgression());
			this.withTheNodeProgress.setString(Integer.toString(this.query.getTransferWithTheNodeProgression()) + "%");

			this.progress.setValue(this.query.getProgression());
			if(!this.query.isFinished() || this.query.isSuccessful()) {
				String progression = Integer.toString(this.query.getProgression()) + "%";

				if(!this.query.isProgressionReliable())
					progression = progression + " ("+I18n.getMessage("thaw.common.estimation")+")";

				this.progress.setString(progression);
			} else
				this.progress.setString(I18n.getMessage("thaw.common.failed"));

			if(this.query.getFileKey() != null)
				this.key.setText(this.query.getFileKey());
			else
				this.key.setText(I18n.getMessage("thaw.common.unknown"));

			if(this.query.getFileSize() == 0)
				this.size.setText(I18n.getMessage("thaw.common.unknown"));
			else
				this.size.setText((new Long(this.query.getFileSize())).toString()+" B");

			this.status.setText(this.query.getStatus());
			if(this.query.getIdentifier() != null)
				this.identifier.setText(this.query.getIdentifier());
			else
				this.identifier.setText("N/A");

			if(this.query.getThawPriority() != -1)
				this.priority.setText(I18n.getMessage("thaw.plugin.priority.p"+Integer.toString(this.query.getThawPriority())));
			else
				this.priority.setText(I18n.getMessage("thaw.common.unknown"));

		} else {
			this.withTheNodeProgress.setValue(0);
			this.withTheNodeProgress.setString("");
			this.progress.setValue(0);
			this.progress.setString("");
			this.status.setText("");
			this.identifier.setText("");
			this.size.setText("");
			this.priority.setText("");
			this.key.setText("");
		}
	}

	public void refreshAll() {
		this.refresh();

		if(this.query != null) {

			this.file.setText(this.query.getFilename());

			if(this.query.getPath() != null)
				this.path.setText(this.query.getPath());
			else
				this.path.setText(I18n.getMessage("thaw.common.unspecified"));

			if(this.query.isGlobal())
				this.globalQueue.setText(I18n.getMessage("thaw.common.yes"));
			else
				this.globalQueue.setText(I18n.getMessage("thaw.common.no"));

		} else {
			this.file.setText("");
			this.key.setText("");
			this.path.setText("");
			this.globalQueue.setText("");
		}

	}

}
