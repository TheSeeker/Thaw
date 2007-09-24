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
import java.awt.event.MouseEvent;


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

import thaw.fcp.*;
import thaw.plugins.IndexBrowser;
import thaw.plugins.index.IndexManagementHelper;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;

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

	private KeyActionMenu keyActionMenu;

	public MessagePanel(MiniFrostPanel mainPanel) {
		this.mainPanel = mainPanel;


		/* keyActionMenu == Right click menu when you click on an highlighted key
		 */
		keyActionMenu = new KeyActionMenu(chkActions, indexActions);


		insidePanel = null;

		panel = new JPanel(new BorderLayout(15, 15));

		/* messages Panel */

		msgsPanel = new JPanel(new BorderLayout(0, 20));
		msgsPanel.add(new JLabel(""), BorderLayout.CENTER);

		/* actions */

		actions = new JComboBox(ACTIONS);
		actions.addActionListener(this);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 3));

		back = new JButton("", IconBox.minLeft);
		back.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.goBack"));
		back.addActionListener(this);

		if (mainPanel.isInGmailView())
			buttonPanel.add(back);

		nextUnread = new JButton("", IconBox.minNextUnread);
		nextUnread.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.nextUnread"));
		nextUnread.addActionListener(this);

		if (mainPanel.isInGmailView())
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

		boolean gmailView = mainPanel.isInGmailView();

		if (gmailView) {
			northNorthPanel.add(new JLabel(""), BorderLayout.CENTER);
			northNorthPanel.add(actions, BorderLayout.EAST);
			northNorthPanel.add(buttonPanel, BorderLayout.WEST);
			northPanel.add(northNorthPanel, BorderLayout.CENTER);
			northPanel.add(new JScrollPane(subject), BorderLayout.SOUTH);

			panel.add(northPanel, BorderLayout.NORTH);

			scrollPane = new JScrollPane(msgsPanel,
						     JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		} else { /* => if outlook layout */

			JPanel globalPanel = new JPanel(new BorderLayout(10, 10));
			globalPanel.add(subject, BorderLayout.NORTH);
			globalPanel.add(msgsPanel, BorderLayout.CENTER);

			scrollPane = new JScrollPane(globalPanel,
						     JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		}

		panel.add(scrollPane, BorderLayout.CENTER);
	}


	public MiniFrostPanel getMiniFrostPanel() {
		return mainPanel;
	}

	public void revalidate() {
		panel.revalidate();
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
			SubMessagePanel panel = new SubMessagePanel(this, subMsg, false);

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
		revalidate();
		panel.repaint();

		putScrollBarAtBottom();
	}


	public JScrollPane getScrollPane() {
		return scrollPane;
	}


	private class ScrollBarSetter implements ThawRunnable {
		public ScrollBarSetter() { }

		public void run() {
			try {
				/* dirty way to have the expected result */
				Thread.sleep(100);
			} catch(InterruptedException e) {

			}

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

		public void stop() {
			/* \_o< */
		}
	}


	private void putScrollBarAtBottom() {
		int max = scrollPane.getVerticalScrollBar().getMaximum();
		scrollPane.getVerticalScrollBar().setValue(max);

		Runnable doScroll = new Runnable() {
				public void run() {

					Thread th = new ThawThread(new ScrollBarSetter(),
								   "Scrollbar setter", this);
					th.start();
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
		if (msg == null)
			return;

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


	public final static JMenuItem[] chkActions = {
		new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.downloadThisKey")),
		new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.downloadAllKeys")),
		null,
		new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.copyThisKey")),
		new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.copyAllKeys"))
	};

	public final static JMenuItem[] indexActions = {
		new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.addThisIndex")),
		new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.addAllIndexes")),
		null,
		new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.copyThisKey")),
		new JMenuItem(I18n.getMessage("thaw.plugin.miniFrost.copyAllKeys"))
	};



	protected MiniFrostPanel getMainPanel() {
		return mainPanel;
	}

	public Vector getAllKeys() {
		Vector v = new Vector();

		for (Iterator it = subPanels.iterator();
		     it.hasNext();) {
			v.addAll( ((SubMessagePanel)it.next()).getKeys() );
		}

		return v;
	}

	public Vector getCHKKeys() {
		Vector v = new Vector();

		Vector keys = getAllKeys();

		for (Iterator it = keys.iterator();
		     it.hasNext();) {
			String key = (String)it.next();

			if (key.startsWith("CHK@"))
				v.add(key);
		}

		return v;
	}


	public Vector getIndexKeys() {
		Vector v = new Vector();

		Vector keys = getAllKeys();

		for (Iterator it = keys.iterator();
		     it.hasNext();) {
			String key = (String)it.next();

			if (key.endsWith(".frdx"))
				v.add(key);
		}

		return v;
	}


	protected class KeyActionMenu extends JPopupMenu implements ActionListener {
		private String key;
		private JMenuItem[] chkActions;
		private JMenuItem[] indexActions;
		private MessagePanel messagePanel;

		public KeyActionMenu(JMenuItem[] chkActions, JMenuItem[] indexActions) {
			super();

			this.chkActions = chkActions;
			this.indexActions = indexActions;

			for (int i = 0 ; i < indexActions.length;i++) {
				if (indexActions[i] != null)
					indexActions[i].addActionListener(this);
			}

			for (int i = 0 ; i < indexActions.length;i++) {
				if (chkActions[i] != null)
					chkActions[i].addActionListener(this);
			}
		}


		public void setKeys(MessagePanel panel, String key) {
			this.key = key;
			this.messagePanel = panel;

			removeAll();

			boolean index = key.endsWith(".frdx");

			JMenuItem[] items = (index ? indexActions : chkActions);

			for (int i = 0 ; i < items.length ;i++) {
				if (items[i] != null)
					add(items[i]);
				else
					addSeparator();
			}
		}

		public void download(String key, java.io.File destDir) {
			FCPQueueManager queueManager = messagePanel.getMainPanel().getPluginCore().getCore().getQueueManager();

			FCPClientGet get = new FCPClientGet(key, FCPClientGet.DEFAULT_PRIORITY,
							    FCPClientGet.PERSISTENCE_FOREVER,
							    true /* global queue */,
							    FCPClientGet.DEFAULT_MAX_RETRIES /* max retries */,
							    destDir.getPath());

			queueManager.addQueryToThePendingQueue(get);
		}


		public void addIndex(String key) {
			FCPQueueManager queueManager = messagePanel.getMainPanel().getPluginCore().getCore().getQueueManager();
			IndexBrowser browser = (IndexBrowser)messagePanel.getMainPanel().getPluginCore().getCore().getPluginManager().getPlugin("thaw.plugins.IndexBrowser");

			if (browser == null) {
				Logger.error(this, "The index browser is not running. Can't add an index");
				return;
			}

			IndexManagementHelper.addIndex(queueManager,
						       browser.getIndexBrowserPanel(),
						       null,
						       key,
						       true);
		}

		public void actionPerformed(ActionEvent e) {

			if (e.getSource() == chkActions[3]
			    || e.getSource() == indexActions[3]) { /* copy this key */

				thaw.gui.GUIHelper.copyToClipboard(key);

			} else if (e.getSource() == chkActions[4]
				   || e.getSource() == indexActions[4]) { /* copy all keys */

				Vector v = ( (e.getSource() == indexActions[4]) ?
					     messagePanel.getIndexKeys() :
					     messagePanel.getCHKKeys() );

				String str = "";

				for (Iterator it = v.iterator();
				     it.hasNext();) {
					str += ((String)it.next())+"\n";
				}

				thaw.gui.GUIHelper.copyToClipboard(str);

			} else if (e.getSource() == chkActions[0]) { /* download this key */

				String lastPath = mainPanel.getConfig().getValue("lastDestinationDirectory");

				thaw.gui.FileChooser chooser = (lastPath != null ?
								new thaw.gui.FileChooser(lastPath) :
								new thaw.gui.FileChooser());

				chooser.setTitle(I18n.getMessage("thaw.plugin.fetch.chooseDestination"));
				chooser.setDirectoryOnly(true);
				chooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);

				java.io.File file = chooser.askOneFile();

				if (file == null)
					return;

				mainPanel.getConfig().setValue("lastDestinationDirectory", file.getPath());

				download(key, file);

			} else if (e.getSource() == chkActions[1]) { /* download all the keys */

				String lastPath = mainPanel.getConfig().getValue("lastDestinationDirectory");

				thaw.gui.FileChooser chooser = (lastPath != null ?
								new thaw.gui.FileChooser(lastPath) :
								new thaw.gui.FileChooser());

				chooser.setTitle(I18n.getMessage("thaw.plugin.fetch.chooseDestination"));
				chooser.setDirectoryOnly(true);
				chooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);

				java.io.File file = chooser.askOneFile();

				if (file == null)
					return;

				mainPanel.getConfig().setValue("lastDestinationDirectory", file.getPath());

				Vector v = messagePanel.getCHKKeys();

				for (Iterator it = v.iterator();
				     it.hasNext();) {
					download((String)it.next(), file);
				}

			} else if (e.getSource() == indexActions[0]) { /* add this index */

				addIndex(key);

			} else if (e.getSource() == indexActions[1]) { /* add all the indexes */

				Vector v = messagePanel.getIndexKeys();

				for (Iterator it = v.iterator();
				     it.hasNext();) {
					addIndex((String)it.next());
				}

			}

		}
	}



	public void popMenuOnKey(MouseEvent e, String key) {
		keyActionMenu.setKeys(this, key);
		keyActionMenu.show(e.getComponent(), e.getX(), e.getY());
	}
}
