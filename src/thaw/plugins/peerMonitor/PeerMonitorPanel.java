package thaw.plugins.peerMonitor;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import thaw.core.Config;
import thaw.core.I18n;

public class PeerMonitorPanel implements ActionListener
{
	private JPanel panel;
	private JPanel mainPanel;

	private JLabel refLabel;
	private JTextArea refArea;
	private JButton refCopyButton;

	private JList peerList;
	private JProgressBar memBar;


	public PeerMonitorPanel(Config config) {

		panel = new JPanel(new BorderLayout());

		peerList = new JList();

		mainPanel = new JPanel(new GridLayout(2, 1));


		JPanel refPanel = new JPanel(new BorderLayout());
		JPanel refTitle = new JPanel(new BorderLayout());

		refLabel = new JLabel(I18n.getMessage("thaw.plugin.peerMonitor.yourReference"));
		refCopyButton = new JButton(I18n.getMessage("thaw.plugin.peerMonitor.copyReference"));
		refCopyButton.addActionListener(this);

		refTitle.add(refLabel, BorderLayout.CENTER);
		refTitle.add(refCopyButton, BorderLayout.EAST);

		refArea = new JTextArea("*Put the node ref here*");
		refArea.setEditable(false);

		refPanel.add(refTitle, BorderLayout.NORTH);
		refPanel.add(refArea, BorderLayout.CENTER);

		mainPanel.add(refPanel);
		mainPanel.add(new JLabel("*Put details about the peer here*"));

		memBar = new JProgressBar(0, 100);
		setMemBar(0, 134217728);
		memBar.setStringPainted(true);

		panel.add(mainPanel, BorderLayout.CENTER);
		panel.add(peerList, BorderLayout.EAST);
		panel.add(memBar, BorderLayout.SOUTH);
	}


	public void setMemBar(long used, long max) {
		int pourcent;

		pourcent = (int)((used * 100) / max);

		memBar.setString("Used memory : "
				 + thaw.gui.GUIHelper.getPrintableSize(used)
				 + " / "
				 + thaw.gui.GUIHelper.getPrintableSize(max)
				 + " ("+Integer.toString(pourcent)+"%)");

		memBar.setValue(pourcent);
	}

	public void setRef(String ref) {
		refArea.setText(ref);
	}


	public JPanel getPanel() {
		return panel;
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == refCopyButton) {
			thaw.gui.GUIHelper.copyToClipboard(refArea.getText());
		}

	}
}
