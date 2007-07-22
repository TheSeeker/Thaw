package thaw.plugins.miniFrost;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;

import java.util.Vector;
import java.util.Iterator;
import java.awt.Color;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


import thaw.gui.IconBox;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.plugins.miniFrost.interfaces.Message;
import thaw.plugins.miniFrost.interfaces.SubMessage;


public class MessagePanel
	implements ActionListener {


	public final static String[] ACTIONS = {
		"",
		I18n.getMessage("thaw.plugin.miniFrost.archivate"),
		I18n.getMessage("thaw.plugin.miniFrost.reply"),
		I18n.getMessage("thaw.plugin.miniFrost.foldAll"),
		I18n.getMessage("thaw.plugin.miniFrost.unfoldAll")
	};

	//public final static int DEFAULT_UNFOLDED = 2;

	private MiniFrostPanel mainPanel;

	private JPanel panel;

	private JPanel insidePanel;
	private JPanel msgsPanel;


	private Message msg;
	private Vector subMsgs;

	private JScrollPane scrollPane;


	private JComboBox actions;
	private JButton back;
	private JButton nextUnread;


	public MessagePanel(MiniFrostPanel mainPanel) {
		this.mainPanel = mainPanel;

		insidePanel = null;

		panel = new JPanel(new BorderLayout(5, 5));


		/* messages Panel */

		msgsPanel = new JPanel(new BorderLayout(0, 20));
		msgsPanel.add(new JLabel(""), BorderLayout.CENTER);

		scrollPane = new JScrollPane(msgsPanel);

		panel.add(scrollPane, BorderLayout.CENTER);


		/* actions */

		actions = new JComboBox(ACTIONS);
		actions.addActionListener(this);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));

		back = new JButton("", IconBox.minLeft);
		back.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.goBack"));
		back.addActionListener(this);
		buttonPanel.add(back);

		nextUnread = new JButton("", IconBox.minNextUnread);
		nextUnread.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.nextUnread"));
		nextUnread.addActionListener(this);
		buttonPanel.add(nextUnread);

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(new JLabel(""), BorderLayout.CENTER);
		northPanel.add(actions, BorderLayout.EAST);
		northPanel.add(buttonPanel, BorderLayout.WEST);


		panel.add(northPanel, BorderLayout.NORTH);

	}

	public void setMessage(Message msg) {
		this.msg = msg;
		subMsgs = msg.getSubMessages();

		refresh();
	}

	public Message getMessage() {
		return msg;
	}


	protected class SubMessagePanel extends JPanel implements ActionListener {
		private JButton upDownButton;
		private boolean retracted;
		private SubMessage msg;

		private JTextArea area;


		public SubMessagePanel(SubMessage msg, boolean retracted) {

			super(new BorderLayout(5,5));

			this.retracted=retracted;
			this.msg = msg;

			setBorder(BorderFactory.createLineBorder(Color.BLACK));


			/* header */
			JPanel headPanel = new JPanel(new BorderLayout(20, 0));

			JLabel dateLabel = new JLabel(msg.getDate().toString());
			JLabel authorLabel = new JLabel(msg.getAuthor().toString());
			authorLabel.setPreferredSize(new java.awt.Dimension(400, 15));


			upDownButton = new JButton("", (retracted ? IconBox.minDown : IconBox.minUp));
			upDownButton.addActionListener(this);

			headPanel.add(authorLabel, BorderLayout.WEST);
			headPanel.add(dateLabel, BorderLayout.CENTER);
			headPanel.add(upDownButton, BorderLayout.EAST);

			this.add(headPanel, BorderLayout.NORTH);


			/* text */

			if (!retracted) {
				area = getTextArea(msg.getMessage());

				this.add(area, BorderLayout.CENTER);
			}

		}

		private JTextArea getTextArea(String txt) {
			JTextArea a = new JTextArea(txt);

			a.setLineWrap(true);
			a.setFont(a.getFont().deriveFont((float)13.5));
			a.setEditable(false);

			return a;
		}


		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == upDownButton) {
				if (retracted) {
					area = getTextArea(msg.getMessage());
					this.add(area, BorderLayout.CENTER);

					upDownButton.setIcon(IconBox.minUp);
				} else {
					this.remove(area);
					area = null;
					upDownButton.setIcon(IconBox.minDown);
				}

				retracted = !retracted;

				panel.validate();
			}
		}

	}


	public void refresh() {

		/* will imbricate BorderLayout */
		/* it's dirty, but it should work */

		JPanel iPanel = null;

		Logger.info(this, "Displaying "+Integer.toString(subMsgs.size())+" sub-msgs");

		int i = 0;

		for (Iterator it = subMsgs.iterator();
		     it.hasNext();) {
			SubMessage subMsg = (SubMessage)it.next();
			SubMessagePanel panel = new SubMessagePanel(subMsg,
								    false);

			if (iPanel == null) {
				iPanel = panel;
			} else {
				JPanel newPanel = new JPanel(new BorderLayout(0, 20));
				newPanel.add(iPanel, BorderLayout.NORTH);
				newPanel.add(panel, BorderLayout.CENTER);
				iPanel = newPanel;
			}

			i++;
		}

		if (insidePanel != null)
			msgsPanel.remove(insidePanel);

		if (iPanel != null) {
			msgsPanel.add(iPanel, BorderLayout.NORTH);
			msgsPanel.add(new JLabel(""), BorderLayout.CENTER);
		}

		insidePanel = msgsPanel;

		msgsPanel.validate();
		panel.validate();

		putScrollBarAtBottom();
	}


	private void putScrollBarAtBottom() {
		int max = scrollPane.getVerticalScrollBar().getMaximum();
		scrollPane.getVerticalScrollBar().setValue(max);
	}


	public JPanel getPanel() {
		return panel;
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == back) {
			mainPanel.displayMessageTable();
		}
	}
}
