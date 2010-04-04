package thaw.plugins.queueWatcher;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import thaw.core.I18n;
import thaw.core.Logger;
import thaw.fcp.FCPTransferQuery;


/**
 * Right panel of queueWatcher plugin. Show details about a transfer.
 */
public class DetailPanel implements Observer {

	private JPanel subPanel;
	private JPanel panel;

	private final JTextField file                  = new JTextField();
	private final JTextField size                  = new JTextField();
	private final JProgressBar progress            = new JProgressBar(0, 100);
	private final JProgressBar withTheNodeProgress = new JProgressBar(0, 100);
	private final JTextField status                = new JTextField();
	private final JTextField key                   = new JTextField();
	private final JTextField path                  = new JTextField();
	private final JTextField priority              = new JTextField();
	private final JTextField identifier            = new JTextField();
	private final JTextField globalQueue           = new JTextField();

	private FCPTransferQuery query = null;
	
	private final static Dimension dim = new Dimension(thaw.plugins.QueueWatcher.DIVIDER_LOCATION-10, 400);

	private final static String unknownStr = I18n.getMessage("thaw.common.unknown");
	private final static String failedStr = I18n.getMessage("thaw.common.failed");
	private final static String estimationStr = I18n.getMessage("thaw.common.estimation");
	
	public final static String[] prioritiesStr = new String[QueuePanel.MIN_PRIORITY+1];
	static {
		for (int i = 0 ; i < prioritiesStr.length ; i++)
			prioritiesStr[i] = I18n.getMessage("thaw.plugin.priority.p"+Integer.toString(i));
	};


	public DetailPanel() {
		panel = new JPanel();
		subPanel = new JPanel();

		final String[] fieldNames = { I18n.getMessage("thaw.common.file"),
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

		subPanel.setLayout(new GridLayout(fieldNames.length*2, 1));

		for(int i=0; i < (fieldNames.length * 2) ; i++) {

			if(i%2 == 0) {
				final JLabel newLabel = new JLabel(fieldNames[i/2]);
				subPanel.add(newLabel);
			} else {
				JComponent field = null;

				switch((i/2)) {
				case(0): field = file; file.setEditable(false); break;
				case(1): field = size; size.setEditable(false); break;
				case(2):
					field = progress;
					progress.setString("");
					progress.setStringPainted(true);
					break;
				case(3):
					field = withTheNodeProgress;
					withTheNodeProgress.setString("");
					withTheNodeProgress.setStringPainted(true);
					break;
				case(4): field = status; status.setEditable(false); break;
				case(5): field = key; key.setEditable(false);break;
				case(6): field = path; path.setEditable(false); break;
				case(7): field = priority; priority.setEditable(false); break;
				case(8): field = identifier; identifier.setEditable(false); break;
				case(9): field = globalQueue; globalQueue.setEditable(false); break;
				default: Logger.error(this, "Gouli goula ? ... is going to crash :p"); break;
				}

				subPanel.add(field);
			}

		} /* for (i < fieldNames.length) */

		subPanel.setPreferredSize(DetailPanel.dim);

		panel.add(subPanel);

	}


	public JPanel getPanel() {
		return panel;
	}


	public void setQuery(final FCPTransferQuery query) {
		if(this.query != null)
			((Observable)this.query).deleteObserver(this);

		this.query = query;

		if(this.query != null)
			((Observable)this.query).addObserver(this);

		refreshAll();
	}

	public void update(final Observable o, final Object arg) {
		refresh();
	}


	public void refresh() {
		if(query != null) {
			withTheNodeProgress.setValue(query.getTransferWithTheNodeProgression());
			withTheNodeProgress.setString(Integer.toString(query.getTransferWithTheNodeProgression()) + "%");

			progress.setValue(query.getProgression());
			if(!query.isFinished() || query.isSuccessful()) {
				String progression = Integer.toString(query.getProgression()) + "%";

				if(!query.isProgressionReliable())
					progression = progression + " ("+estimationStr+")";

				progress.setString(progression);
			} else
				progress.setString(failedStr);

			if(query.getFileKey() != null)
				key.setText(query.getFileKey());
			else
				key.setText(unknownStr);

			if(query.getFileSize() == 0)
				size.setText(unknownStr);
			else
				size.setText((new Long(query.getFileSize())).toString()+" B");

			status.setText(query.getStatus());
			if(query.getIdentifier() != null)
				identifier.setText(query.getIdentifier());
			else
				identifier.setText("N/A");

			if(query.getThawPriority() != -1)
				priority.setText(prioritiesStr[query.getThawPriority()]);
			else
				priority.setText(unknownStr);

		} else {
			withTheNodeProgress.setValue(0);
			withTheNodeProgress.setString("");
			progress.setValue(0);
			progress.setString("");
			status.setText("");
			identifier.setText("");
			size.setText("");
			priority.setText("");
			key.setText("");
		}
	}

	public void refreshAll() {
		refresh();

		if(query != null) {

			file.setText(query.getFilename());

			if(query.getPath() != null)
				path.setText(query.getPath());
			else
				path.setText(I18n.getMessage("thaw.common.unspecified"));

			if(query.isGlobal())
				globalQueue.setText(I18n.getMessage("thaw.common.yes"));
			else
				globalQueue.setText(I18n.getMessage("thaw.common.no"));

		} else {
			file.setText("");
			key.setText("");
			path.setText("");
			globalQueue.setText("");
		}

	}

}
