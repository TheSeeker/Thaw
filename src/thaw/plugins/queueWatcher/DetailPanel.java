package thaw.plugins.queueWatcher;

import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JProgressBar;
import javax.swing.JComponent;

import thaw.core.*;
import thaw.i18n.I18n;


public class DetailPanel {

	private Core core;

	private JPanel subPanel;
	private JPanel panel;

	private JTextField file     = new JTextField();
	private JTextField size     = new JTextField();
	private JProgressBar progress = new JProgressBar(0, 100);
	private JTextField key      = new JTextField();
	private JTextField path     = new JTextField();
	private JTextField priority = new JTextField();
	private JTextField attempt  = new JTextField();

	private final static Dimension dim = new Dimension(300, 275);

	public DetailPanel(Core core) {
		this.core = core;
		
		panel = new JPanel();
		subPanel = new JPanel();

		String[] fieldNames = { I18n.getMessage("thaw.common.file"),
					I18n.getMessage("thaw.common.size"),
					I18n.getMessage("thaw.common.progress"),
					I18n.getMessage("thaw.common.key"),
					I18n.getMessage("thaw.common.localPath"),
					I18n.getMessage("thaw.common.priority"),
					I18n.getMessage("thaw.common.try")+" #" };

		subPanel.setLayout(new GridLayout(fieldNames.length*2, 1));

		for(int i=0; i < (fieldNames.length * 2) ; i++) {

			if(i%2 == 0) {
				JLabel newLabel = new JLabel(fieldNames[i/2]);
				subPanel.add(newLabel);
			} else {
				JComponent field = null;

				switch(((int)i/2)) {
				case(0): field = file; file.setEditable(false); break;
				case(1): field = size; size.setEditable(false); break;
				case(2):
					field = progress; 
					progress.setString("");
					progress.setStringPainted(true);
					break;
				case(3): field = key; key.setEditable(false);break;
				case(4): field = path; path.setEditable(false); break;
				case(5): field = priority; priority.setEditable(false); break;
				case(6): field = attempt; attempt.setEditable(false); break;
				default: Logger.warning(this, "Gouli goula ? ... is going to crash :p"); break;
				}

				subPanel.add(field);
			}

		} /* for (i < fieldNames.length) */

		subPanel.setPreferredSize(dim);

		panel.add(subPanel);

	}


	public JPanel getPanel() {
		return panel;
	}
}
