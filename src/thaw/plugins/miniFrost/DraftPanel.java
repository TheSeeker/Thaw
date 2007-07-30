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
import javax.swing.JDialog;

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

	private JLabel boardLabel;
	private JComboBox authorBox;
	private JTextField subjectField;
	private JTextArea textArea;
	private JButton cancelButton;
	private JButton sendButton;

	private JButton extractButton;

	private JDialog dialog;

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
		textArea.setFont(textArea.getFont().deriveFont((float)13.5));

		boardLabel = new JLabel("");
		extractButton = new JButton(thaw.gui.IconBox.minWindowNew);
		extractButton.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.newWindow"));
		extractButton.addActionListener(this);

		JPanel northPanel = new JPanel(new BorderLayout(5, 5));

		JPanel headersPanel = new JPanel(new GridLayout(3, 1));
		headersPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.board")+": "));
		headersPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.author")+": "));
		headersPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.subject")+": "));

		JPanel valuesPanel = new JPanel(new GridLayout(3, 1));

		JPanel topPanel = new JPanel(new BorderLayout(5, 5));
		topPanel.add(boardLabel, BorderLayout.CENTER);
		topPanel.add(extractButton, BorderLayout.EAST);

		valuesPanel.add(topPanel);
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

	public DraftPanel(MiniFrostPanel mainPanel, JDialog dialog) {
		this(mainPanel);
		this.dialog = dialog;
		extractButton.setEnabled(false);
	}


	public void setDraft(Draft draft) {
		this.draft = draft;

		/* board */
		boardLabel.setText(draft.getBoard().toString());

		/* identity */
		Vector ids = new Vector();
		ids.add(I18n.getMessage("thaw.plugin.miniFrost.anonymous"));
		ids.addAll(Identity.getYourIdentities(mainPanel.getDb()));

		authorBox.removeAllItems();

		for (Iterator it = ids.iterator(); it.hasNext();)
			authorBox.addItem(it.next());

		if (draft.getAuthorIdentity() != null)
			authorBox.setSelectedItem(draft.getAuthorIdentity());
		else if (draft.getAuthorNick() != null)
			authorBox.setSelectedItem(draft.getAuthorNick());

		/* subject */
		subjectField.setText(draft.getSubject());

		/* text */
		String txt = draft.getText();

		textArea.setText(draft.getText());

		refresh();
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

	/**
	 * Don't do the replacements in the text.
	 * Don't call Draft.setDate()
	 */
	public void fillInDraft() {
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
		draft.setText(txt);
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == extractButton) {
			fillInDraft();

			JDialog newDialog = new JDialog(mainPanel.getPluginCore().getCore().getMainWindow().getMainFrame(),
						     I18n.getMessage("thaw.plugin.miniFrost.draft"));
			newDialog.getContentPane().setLayout(new GridLayout(1, 1));

			DraftPanel panel = new DraftPanel(mainPanel, newDialog);

			panel.setDraft(draft);

			newDialog.getContentPane().add(panel.getPanel());

			newDialog.setSize(500, 500);

			newDialog.setVisible(true);

		} else if (e.getSource() == sendButton) {
			fillInDraft();

			Date date = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd - HH:mm:ss");

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

		if (dialog == null)
			mainPanel.displayMessageTable();
		else
			dialog.setVisible(false);
	}
}
