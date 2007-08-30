package thaw.plugins.miniFrost;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;

import javax.swing.BorderFactory;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.Rectangle;
import java.awt.Dimension;

import java.awt.Color;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import thaw.plugins.miniFrost.interfaces.Author;
import thaw.plugins.miniFrost.interfaces.SubMessage;

import thaw.gui.IconBox;
import thaw.plugins.signatures.Identity;
import thaw.core.I18n;

import java.util.regex.Pattern;


public class SubMessagePanel extends JPanel implements ActionListener {

	private JButton upDownButton;
	private boolean retracted;
	private SubMessage msg;

	private JScrollPane area;
	private MessagePanel messagePanel;

	public SubMessagePanel(MessagePanel messagePanel, SubMessage msg, boolean retracted) {

		super(new BorderLayout(5,5));

		this.messagePanel = messagePanel;
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
			area = getEditorPane(msg.getMessage().trim());

			this.add(area, BorderLayout.CENTER);
		}

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
			messagePanel.getMiniFrostPanel().getMessageTreeTable().refresh();
		}
	}



	private class TextPanel extends JTextPane {

		public TextPanel() {
			super();
		}

		private Rectangle rect(javax.swing.text.Position p)
			throws javax.swing.text.BadLocationException
			{
				int off = p.getOffset();
				Rectangle r = modelToView(off>0 ? off-1 : off);
				return r;
			}


		public Dimension getPreferredSize() {
			try {
				Rectangle start =
					rect(getDocument().getStartPosition());
				Rectangle end =
					rect(getDocument().getEndPosition());
				if (start==null || end==null) {
					return super.getPreferredSize();
				}
				int height = end.y + end.height - start.y + 4;

				return new Dimension(messagePanel.getScrollPane().getWidth()-30, height);

			} catch (javax.swing.text.BadLocationException e) {
				return super.getPreferredSize();
			}
		}

	}


	public final static String[][] RECOGNIZED_KEYS = {
		{ "KSK@", " " /* or eol */ },
		{ "CHK@", null /* eol */ },
		{ "USK@", ".frdx" },
	};



	private JScrollPane getEditorPane(String txt) {
		TextPanel a = new TextPanel();

		a.setContentType("text/html");

		a.setFont(a.getFont().deriveFont((float)13.5));
		a.setEditable(false);

		a.setText(txt);

		a.firePropertyChange("lineWrap", false, true);
		a.firePropertyChange("wrapStyleWord", false, true);

		JScrollPane pane = new JScrollPane(a,
						   JScrollPane.VERTICAL_SCROLLBAR_NEVER,
						   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		return pane;
	}

	public void setRetracted(boolean retracted) {
		if (!retracted) {
			area = getEditorPane(msg.getMessage());
			this.add(area, BorderLayout.CENTER);

			upDownButton.setIcon(IconBox.minUp);
		} else {
			if (area != null)
				this.remove(area);
			area = null;
			upDownButton.setIcon(IconBox.minDown);
		}

		this.retracted = retracted;

		messagePanel.revalidate();
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == upDownButton) {
			setRetracted(!retracted);
		}
	}

}
