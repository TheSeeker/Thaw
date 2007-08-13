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

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


import thaw.gui.IconBox;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.plugins.signatures.Identity;
import thaw.plugins.miniFrost.interfaces.Author;
import thaw.plugins.miniFrost.interfaces.Message;
import thaw.plugins.miniFrost.interfaces.SubMessage;
import thaw.plugins.miniFrost.interfaces.Attachment;
import thaw.plugins.miniFrost.interfaces.Draft;


public class MessagePanel
	implements ActionListener {


	public final static String[] ACTIONS = {
		I18n.getMessage("thaw.plugin.miniFrost.actions"),
		I18n.getMessage("thaw.plugin.miniFrost.reply"),
		I18n.getMessage("thaw.plugin.miniFrost.archivate"),
		I18n.getMessage("thaw.plugin.miniFrost.unarchivate"),
		I18n.getMessage("thaw.plugin.miniFrost.unfoldAll"),
		I18n.getMessage("thaw.plugin.miniFrost.foldAll")
	};

	public final static int DEFAULT_UNFOLDED = 2;

	private MiniFrostPanel mainPanel;

	private JPanel panel;

	private JPanel insidePanel;
	private JPanel msgsPanel;


	private Message msg;
	private Vector subMsgs;
	private Vector attachments;


	private JScrollPane scrollPane;


	private JComboBox actions;
	private JButton back;
	private JButton nextUnread;
	private JButton reply;

	private Vector subPanels;

	private JLabel subject;


	public MessagePanel(MiniFrostPanel mainPanel) {
		this.mainPanel = mainPanel;

		insidePanel = null;

		panel = new JPanel(new BorderLayout(15, 15));

		/* messages Panel */

		msgsPanel = new JPanel(new BorderLayout(0, 20));
		msgsPanel.add(new JLabel(""), BorderLayout.CENTER);

		scrollPane = new JScrollPane(msgsPanel);
		panel.add(scrollPane, BorderLayout.CENTER);


		/* actions */

		actions = new JComboBox(ACTIONS);
		actions.addActionListener(this);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 3));

		back = new JButton("", IconBox.minLeft);
		back.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.goBack"));
		back.addActionListener(this);
		buttonPanel.add(back);

		nextUnread = new JButton("", IconBox.minNextUnread);
		nextUnread.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.nextUnread"));
		nextUnread.addActionListener(this);
		buttonPanel.add(nextUnread);

		reply = new JButton("", IconBox.minMsgReply);
		reply.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.reply"));
		reply.addActionListener(this);
		buttonPanel.add(reply);

		subject = new JLabel("");
		subject.setIcon(IconBox.minMail);
		subject.setIconTextGap(15);

		JPanel northPanel = new JPanel(new BorderLayout(5, 5));
		JPanel northNorthPanel = new JPanel(new BorderLayout(5,5));

		northNorthPanel.add(new JLabel(""), BorderLayout.CENTER);
		northNorthPanel.add(actions, BorderLayout.EAST);
		northNorthPanel.add(buttonPanel, BorderLayout.WEST);

		northPanel.add(northNorthPanel, BorderLayout.CENTER);
		northPanel.add(new JScrollPane(subject), BorderLayout.SOUTH);


		panel.add(northPanel, BorderLayout.NORTH);
	}

	public void hided() {
		nextUnread.setMnemonic(KeyEvent.VK_Z);
	}

	public void redisplayed() {
		nextUnread.setMnemonic(KeyEvent.VK_N);
		nextUnread.requestFocus();
	}

	public void setMessage(Message msg) {
		this.msg = msg;
		subMsgs = msg.getSubMessages();
		attachments = msg.getAttachments();

		refresh();
	}

	public Message getMessage() {
		return msg;
	}


	protected class AuthorPanel extends JPanel implements ActionListener {
		private JComboBox box = null;
		private JLabel nick;
		private Author author;

		public AuthorPanel(Author author) {
			super(new BorderLayout(5, 5));

			this.author = author;

			nick = new JLabel(" "+author.toString(false));

			add(nick, BorderLayout.CENTER);

			if (author.getIdentity() != null
			    && author.getIdentity().getPrivateKey() == null) {

				if (author.getIdentity().getTrustLevel()
				    == Identity.trustLevelInt[0]) /* if dev */
					box = new JComboBox(Identity.trustLevelStr);
				else
					box = new JComboBox(Identity.trustLevelUserStr);

				nick.setForeground(author.getIdentity().getTrustLevelColor());

				box.setSelectedItem(author.getIdentity().getTrustLevelStr());
				box.setForeground(author.getIdentity().getTrustLevelColor());
				box.addActionListener(this);

				add(box, BorderLayout.EAST);

			} else if (author.getIdentity() != null) {

				JLabel status = new JLabel(I18n.getMessage("thaw.plugin.signature.trustLevel.me"));
				status.setForeground(author.getIdentity().getTrustLevelColor());
				add(status, BorderLayout.EAST);

			}
		}

		public void actionPerformed(ActionEvent e) {
			author.getIdentity().setTrustLevel((String)box.getSelectedItem());
			box.setForeground(author.getIdentity().getTrustLevelColor());
			nick.setForeground(author.getIdentity().getTrustLevelColor());

			/* we just refresh, because if now the trust level is below what must be
			 * displayed ... */
			mainPanel.getMessageTreeTable().refresh();
		}
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
			JPanel headPanel = new JPanel(new BorderLayout(40, 40));

			JLabel dateLabel = new JLabel(java.text.DateFormat.getDateTimeInstance().format(msg.getDate()));
			AuthorPanel authorLabel = new AuthorPanel(msg.getAuthor());
			//authorLabel.setPreferredSize(new java.awt.Dimension(400, 15));


			upDownButton = new JButton("", (retracted ? IconBox.minDown : IconBox.minUp));
			upDownButton.addActionListener(this);

			JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
			rightPanel.add(dateLabel, BorderLayout.CENTER);
			rightPanel.add(upDownButton,BorderLayout.EAST);

			headPanel.add(authorLabel, BorderLayout.CENTER);
			headPanel.add(rightPanel, BorderLayout.EAST);

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
			a.setWrapStyleWord(true);
			a.setFont(a.getFont().deriveFont((float)13.5));
			a.setEditable(false);

			return a;
		}


		public void forceDot() {
			area.getCaret().setDot(msg.getMessage().length());
		}

		public void setRetracted(boolean retracted) {
			if (!retracted) {
				area = getTextArea(msg.getMessage());
				this.add(area, BorderLayout.CENTER);

				upDownButton.setIcon(IconBox.minUp);
			} else {
				if (area != null)
					this.remove(area);
				area = null;
				upDownButton.setIcon(IconBox.minDown);
			}

			this.retracted = retracted;

			this.revalidate();
			panel.revalidate();
			msgsPanel.revalidate();
		}


		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == upDownButton) {
				setRetracted(!retracted);
			}
		}

	}


	protected class AttachmentAction extends JMenuItem
		implements ActionListener {

		private Attachment a;
		private String action;

		public AttachmentAction(Attachment a, String action) {
			super(action);

			this.a = a;
			this.action = action;

			super.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			a.apply(mainPanel.getDb(),
				mainPanel.getPluginCore().getCore().getQueueManager(),
				action);
		}
	}


	protected class AttachmentPanel extends JPanel
		implements ActionListener {

		private JButton button;
		private Vector attachments;

		public AttachmentPanel(Vector attachments) {
			super();
			this.attachments = attachments;
			button = new JButton(IconBox.attachment);
			button.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.attachments"));
			button.addActionListener(this);
			super.add(button);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == button) {
				JPopupMenu menu = new JPopupMenu();

				/* make the menu */

				for(Iterator it = attachments.iterator();
				    it.hasNext();) {
					Attachment a = (Attachment)it.next();
					JMenu subMenu = new JMenu(a.getPrintableType() + " : "+a.toString());

					String[] actions = a.getActions();

					for (int i = 0 ; i < actions.length ; i++) {
						subMenu.add(new AttachmentAction(a, actions[i]));
					}

					menu.add(subMenu);
				}


				/* and next display it */
				menu.show(this,
					  this.getWidth()/2,
					  this.getHeight()/2);
			}
		}
	}


	public JPanel getEncryptedForPanel(Identity id) {
		JPanel panel = new JPanel(new GridLayout(1, 1));
		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.encryptedBody").replaceAll("X", id.toString())));
		return panel;
	}


	public void refresh() {
		subPanels = new Vector();

		/* will imbricate BorderLayout */
		/* it's dirty, but it should work */

		JPanel iPanel = null;

		subject.setText(I18n.getMessage("thaw.plugin.miniFrost.subject")+": "+msg.getSubject()+
				"    [r"+Integer.toString(msg.getRev())+"]");

		Logger.info(this, "Displaying "+Integer.toString(subMsgs.size())+" sub-msgs");

		if (msg.encryptedFor() != null) {
			iPanel = getEncryptedForPanel(msg.encryptedFor());
			Logger.info(this, "(Encrypted message)");
		}

		int i = 0;

		/* sub messages */
		for (Iterator it = subMsgs.iterator();
		     it.hasNext();) {
			SubMessage subMsg = (SubMessage)it.next();
			//SubMessagePanel panel = new SubMessagePanel(subMsg,
			//					    (i + DEFAULT_UNFOLDED) < subMsgs.size());
			SubMessagePanel panel = new SubMessagePanel(subMsg, false);

			subPanels.add(panel);

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


		if (msg.encryptedFor() != null) {
			if (iPanel == null) {
				iPanel = getEncryptedForPanel(msg.encryptedFor());
			} else {
				JPanel newPanel = new JPanel(new BorderLayout(0, 20));
				newPanel.add(iPanel, BorderLayout.NORTH);
				newPanel.add(getEncryptedForPanel(msg.encryptedFor()), BorderLayout.CENTER);
				iPanel = newPanel;
			}
		}

		/* attachments */
		if (attachments != null) {
			AttachmentPanel panel = new AttachmentPanel(attachments);

			if (iPanel == null) {
				iPanel = panel;
			} else {
				JPanel newPanel = new JPanel(new BorderLayout(0, 20));
				newPanel.add(iPanel, BorderLayout.NORTH);
				newPanel.add(panel, BorderLayout.CENTER);
				iPanel = newPanel;
			}
		}


		if (insidePanel != null) {
			msgsPanel.remove(insidePanel);
			msgsPanel.revalidate();
			panel.revalidate();
		}

		if (iPanel != null) {
			msgsPanel.add(iPanel, BorderLayout.NORTH);
			msgsPanel.add(new JLabel(""), BorderLayout.CENTER);
		}

		insidePanel = iPanel;

		panel.revalidate();

		panel.repaint();

		putScrollBarAtBottom();
	}


	private void putScrollBarAtBottom() {
		int max = scrollPane.getVerticalScrollBar().getMaximum();
		scrollPane.getVerticalScrollBar().setValue(max);

		Runnable doScroll = new Runnable() {
				public void run() {
					scrollPane.getVerticalScrollBar().setUnitIncrement(15);

					int max = scrollPane.getVerticalScrollBar().getMaximum();
					int extent = scrollPane.getVerticalScrollBar().getVisibleAmount();
					int min = scrollPane.getVerticalScrollBar().getMinimum();
					int value = scrollPane.getVerticalScrollBar().getValue();

					Logger.debug(this, "ScrollBar: "
						    +"min : "+Integer.toString(min)
						    +" ; max : "+Integer.toString(max)
						    +" ; extent : "+Integer.toString(extent)
						    +" ; value : "+Integer.toString(value));

					scrollPane.getVerticalScrollBar().setValue(max);
				}
			};

		javax.swing.SwingUtilities.invokeLater(doScroll);
	}


	public JPanel getPanel() {
		return panel;
	}


	private boolean nextUnread() {
		/**
		 * because it knows the filter rules
		 */
		return mainPanel.getMessageTreeTable().nextUnread();
	}


	protected void reply() {
		Draft draft = msg.getBoard().getDraft(msg);
		mainPanel.getDraftPanel().setDraft(draft);
		mainPanel.displayDraftPanel();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == back) {

			mainPanel.displayMessageTable();

		} else if (e.getSource() == nextUnread) {

			if (!nextUnread())
				mainPanel.displayMessageTable();

		} else if (e.getSource() == reply) {

			reply();

		} else if (e.getSource() == actions) {

			int sel = actions.getSelectedIndex();

			if (sel == 2 || sel == 3) { /* (un)archive */
				boolean archive = (sel == 2);

				msg.setArchived(archive);
				mainPanel.getMessageTreeTable().refresh();

				if (archive && !nextUnread())
					mainPanel.displayMessageTable();

			} else if (sel == 1) { /* reply */

				reply();

			} else if (sel == 4 || sel == 5) { /* (un)fold */
				boolean retracted = (sel == 5);

				for (Iterator it = subPanels.iterator();
				     it.hasNext();) {

					((SubMessagePanel)it.next()).setRetracted(retracted);
				}
			}

			actions.setSelectedIndex(0);

		}
	}
}
