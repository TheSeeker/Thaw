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

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import javax.swing.text.Document;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;

import thaw.plugins.miniFrost.interfaces.Author;
import thaw.plugins.miniFrost.interfaces.SubMessage;

import javax.swing.JComponent;
import javax.swing.AbstractAction;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;

import java.awt.Cursor;
import java.util.Vector;
import java.util.Iterator;


import thaw.gui.IconBox;
import thaw.plugins.signatures.Identity;
import thaw.core.I18n;
import thaw.core.Logger;

import java.util.regex.Pattern;


public class SubMessagePanel extends JPanel implements ActionListener {

	private JButton upDownButton;
	private boolean retracted;
	private SubMessage msg;

	private JComponent area;
	private MessagePanel messagePanel;

	public SubMessagePanel(MessagePanel messagePanel, SubMessage msg, boolean retracted) {

		super(new BorderLayout(5,5));

		this.messagePanel = messagePanel;
		this.retracted=retracted;
		this.msg = msg;

		setBorder(BorderFactory.createLineBorder(Color.BLACK));


		/* header */
		JPanel headPanel = new JPanel(new BorderLayout(10, 10));

		JLabel dateLabel = new JLabel(java.text.DateFormat.getDateTimeInstance().format(msg.getDate()));
		AuthorPanel authorLabel = new AuthorPanel(msg.getAuthor());
		//authorLabel.setPreferredSize(new java.awt.Dimension(400, 15));


		upDownButton = new JButton("", (retracted ? IconBox.minDown : IconBox.minUp));
		upDownButton.addActionListener(this);

		JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
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

	public final static String KEY_ATTRIBUTE = "key";


	private Vector keys = null;


	public Vector getKeys() {
		return keys;
	}


	/**
	 * inspired by frost code
	 */
	private StyledDocument parseText(String txt) {

		keys = new Vector();

		DefaultStyledDocument doc = new DefaultStyledDocument();

		String[] split = txt.trim().split("\n");

		SimpleAttributeSet noAttrs = new SimpleAttributeSet();
		SimpleAttributeSet keyAttrs = new SimpleAttributeSet();

		//keyAttrs.addAttribute(LinkEditorKit.LINK, s);
		keyAttrs.addAttribute(StyleConstants.Underline, Boolean.TRUE);
		keyAttrs.addAttribute(StyleConstants.Foreground, Color.BLUE);


		for (int i = 0 ; i < split.length ; i++) { /* foreach line */
			String startStr = null; /* before the key */
			String keyStr = null; /* the key */
			String endStr = null; /* after the key */

			for (int j = 0 ; j < RECOGNIZED_KEYS.length ; j++) { /* foreach key type */

				int startKeyInt = split[i].indexOf(RECOGNIZED_KEYS[j][0]);

				if (startKeyInt >= 0) {
					startStr = split[i].substring(0, startKeyInt);

					String keyAndEndStr = split[i].substring(startKeyInt);

					int keyEndInt = -1;

					if (RECOGNIZED_KEYS[j][1] != null) {
						keyEndInt = keyAndEndStr.indexOf(RECOGNIZED_KEYS[j][1]);

						if (keyEndInt >= 0) {
							keyEndInt += RECOGNIZED_KEYS[j][1].length();
						}

					}

					if (keyEndInt < 0
					    && RECOGNIZED_KEYS[j][1] != null
					    && !" ".equals(RECOGNIZED_KEYS[j][1])) {
						startStr = null;
						continue; /* will try to find another key */
					}

					if (keyEndInt >= 0) {
						keyStr = keyAndEndStr.substring(0, keyEndInt);
						endStr = keyAndEndStr.substring(keyEndInt);
					} else {
						keyStr = keyAndEndStr;
						endStr = null;
					}


					break; /* we got a key */
				}
			}


			try {
				/* insertString() will generate Elements and insert them in the document */

				if (startStr == null) {
					doc.insertString(doc.getLength(), split[i], noAttrs);
				} else {
					doc.insertString(doc.getLength(), startStr, noAttrs);
				}

				if (keyStr != null) {
					keys.add(keyStr);

					SimpleAttributeSet cloned = (SimpleAttributeSet)keyAttrs.clone();

					cloned.addAttribute(KEY_ATTRIBUTE, new KeyLinkAction(keyStr));

					doc.insertString(doc.getLength(), keyStr, cloned);
				}

				if (endStr != null)
					doc.insertString(doc.getLength(), endStr, noAttrs);

				if (i != split.length-1)
					doc.insertString(doc.getLength(), "\n", noAttrs);
			} catch(javax.swing.text.BadLocationException e) {
				Logger.error(this, "Error while parsing the text: "+e.toString()+" ; A line will be missing");
				e.printStackTrace();
			}
		}

		return doc;

	}


	protected class KeyLinkAction {
		private String key;

		public KeyLinkAction(String key) {
			this.key = key;
		}

		public void execute(MouseEvent e) {
			messagePanel.popMenuOnKey(e, key);
		}

	}


	protected class TextMouseMotionListener extends MouseInputAdapter {
		private TextPanel txtArea;
		private StyledDocument doc;

		public TextMouseMotionListener(TextPanel txtArea, StyledDocument doc) {
			this.txtArea = txtArea;
			this.doc = doc;
		}

                public void mouseMoved(MouseEvent e) {
                        Element elem = doc.getCharacterElement(txtArea.viewToModel(e.getPoint()));
                        AttributeSet as = elem.getAttributes();
                        if(StyleConstants.isUnderline(as))
                                txtArea.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        else
                                txtArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
        }


	protected class TextClickListener extends MouseAdapter {
		private TextPanel txtArea;
		private StyledDocument doc;

		public TextClickListener(TextPanel txtArea, StyledDocument doc) {
			this.txtArea = txtArea;
			this.doc = doc;
		}

		public void mouseClicked( MouseEvent e ) {
			Element elem = doc.getCharacterElement(txtArea.viewToModel(e.getPoint()));
			AttributeSet as = elem.getAttributes();
			KeyLinkAction fla = (KeyLinkAction)as.getAttribute(KEY_ATTRIBUTE);

			if(fla != null)
				fla.execute(e);
		}
	}

	private JComponent getEditorPane(String txt) {
		TextPanel a = new TextPanel();

		a.setFont(a.getFont().deriveFont((float)13.5));
		a.setEditable(false);
		a.setBackground(Color.WHITE);

		StyledDocument doc = parseText(txt);

		a.setStyledDocument(parseText(txt));

		a.firePropertyChange("lineWrap", false, true);
		a.firePropertyChange("wrapStyleWord", false, true);

		a.addMouseListener(new TextClickListener(a, doc));
		a.addMouseMotionListener(new TextMouseMotionListener(a, doc));

		/*
		JScrollPane pane = new JScrollPane(a,
						   JScrollPane.VERTICAL_SCROLLBAR_NEVER,
						   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		*/

		return a;
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
