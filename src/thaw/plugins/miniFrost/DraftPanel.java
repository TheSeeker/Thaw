package thaw.plugins.miniFrost;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JButton;

import java.util.Iterator;
import java.util.Vector;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

import thaw.core.I18n;

import thaw.plugins.signatures.Identity;

import thaw.plugins.miniFrost.interfaces.Draft;



public class DraftPanel implements ActionListener {

	private Draft draft;
	private JPanel panel;

	private MiniFrostPanel mainPanel;

	private JComboBox authorBox;
	private JTextField subjectField;
	private JTextArea textArea;
	private JButton cancelButton;
	private JButton sendButton;


	public DraftPanel(MiniFrostPanel mainPanel) {
		this.mainPanel = mainPanel;

		panel = new JPanel(new BorderLayout(5, 5));

		Vector ids = new Vector();
		ids.add(I18n.getMessage("thaw.plugin.miniFrost.anonymous"));
		ids.addAll(Identity.getYourIdentities(mainPanel.getDb()));

		authorBox = new JComboBox();
		authorBox.setEditable(true);

		subjectField = new JTextField("");
		subjectField.setEditable(true);

		textArea = new JTextArea("");
		textArea.setEditable(true);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);


		JPanel northPanel = new JPanel(new BorderLayout(5, 5));

		JPanel headersPanel = new JPanel(new GridLayout(2, 1));
		headersPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.author")+": "));
		headersPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.subject")+": "));

		JPanel valuesPanel = new JPanel(new GridLayout(2, 1));
		valuesPanel.add(authorBox);
		valuesPanel.add(subjectField);

		northPanel.add(headersPanel, BorderLayout.WEST);
		northPanel.add(valuesPanel, BorderLayout.CENTER);


		JPanel southPanel = new JPanel(new GridLayout(1, 2));

		cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
		sendButton = new JButton(I18n.getMessage("thaw.common.ok"));
		cancelButton.addActionListener(this);
		sendButton.addActionListener(this);

		southPanel.add(cancelButton);
		southPanel.add(sendButton);

		panel.add(northPanel,                BorderLayout.NORTH );
		panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
		panel.add(southPanel,                BorderLayout.SOUTH );
	}


	public void setDraft(Draft draft) {
		this.draft = draft;
		refresh();

		Vector ids = new Vector();
		ids.add(I18n.getMessage("thaw.plugin.miniFrost.anonymous"));
		ids.addAll(Identity.getYourIdentities(mainPanel.getDb()));

		authorBox.removeAllItems();

		for (Iterator it = ids.iterator(); it.hasNext();)
			authorBox.addItem(it.next());

		subjectField.setText(draft.getInitialSubject());

		String txt = draft.getInitialText();

		textArea.setText(draft.getInitialText());
	}


	public void refresh() {
		/* we don't want to erase by accident the current draft
		 * => we do nothing
		 */
	}

	public void hided() {

	}

	public void redisplayed() {
		textArea.requestFocus();
	}

	public JPanel getPanel() {
		return panel;
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == sendButton) {
			Date date = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd - HH:mm:ss");

			/* author */

			if (authorBox.getSelectedItem() instanceof Identity) {
				draft.setAuthor(authorBox.getSelectedItem().toString(),
						(Identity)authorBox.getSelectedItem());
			} else {
				String nick = authorBox.getSelectedItem().toString();
				nick = nick.replaceAll("@", "_");

				draft.setAuthor(nick, null);
			}

			/* subject */

			draft.setSubject(subjectField.getText());

			/* text */

			String txt = textArea.getText();

			txt = txt.replaceAll("\\$sender\\$", authorBox.getSelectedItem().toString());

			String dateStr = dateFormat.format(date).toString();
			txt = txt.replaceAll("\\$dateAndTime\\$", dateStr);

			draft.setText(txt);


			/* date */
			draft.setDate(date);


			/* POST */
			draft.post(mainPanel.getPluginCore().getCore().getQueueManager());

		} if (e.getSource() == cancelButton) {

		}

		mainPanel.displayMessageTable();
	}
}
