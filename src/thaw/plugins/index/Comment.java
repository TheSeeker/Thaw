package thaw.plugins.index;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.util.Observer;
import java.util.Observable;

import java.util.Vector;

/* Swing */

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;

import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/* DOM */

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;


/* SAX */

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;



/* SQL */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



/* Thaw */

import thaw.core.Config;
import thaw.core.Logger;
import thaw.core.MainWindow; /* used for warning popups */
import thaw.core.I18n;

import thaw.fcp.FreenetURIHelper;
import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPQueueManager;

import thaw.plugins.Hsqldb;
import thaw.plugins.signatures.Identity;

/**
 * Will use, from the configuration:
 *  'userNickname' as the author name
 */
public class Comment extends Observable implements Observer, ActionListener {
	public final static int MAX_SIZE = 16384;

	private Identity author;
	private String comment;

	private Index index;
	private Hsqldb db;

	private int rev;

	private boolean newComment = false;
	private boolean valid = false;


	/* needed to check the signature */
	private String sig;


	private Comment() {
		newComment = false;
	}


	/**
	 * @param index parent index
	 * @param rev revision of the comment (-1) if not inserted at the moment
	 * @param comment comment inside the comment ... :)
	 */
	public Comment(Hsqldb db, Index index, int rev, Identity author, String comment) {
		this.db = db;
		this.author = author;
		this.comment = comment;
		this.index = index;
		this.rev = rev;
		newComment = false;
	}


	private CommentTab tab;
	private JComboBox trust;

	private JButton changeBlackListState;

	private boolean blackListed;


	public JPanel getPanel(CommentTab tab) {
		this.tab= tab;

		blackListed = isBlackListed();
		boolean hasPrivateKey = (index.getPrivateKey() != null);
		boolean isPrivateKeyPublished = index.publishPrivateKey();

		/**
		 * we don't display if it is blacklisted and we don't have the private key
		 */
		if (blackListed && !hasPrivateKey)
			return null;

		JPanel panel = new JPanel(new BorderLayout(10, 10));

		JTextArea text;

		if (!blackListed)
			text = new JTextArea(comment.trim());
		else
			text = new JTextArea(I18n.getMessage("thaw.plugin.index.comment.moderated"));

		text.setLineWrap(true);
		text.setWrapStyleWord(true);

		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
								 "--- "+author.toString()+" ---",
								 javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
								 javax.swing.border.TitledBorder.DEFAULT_POSITION,
								 new java.awt.Font("Dialog", java.awt.Font.BOLD, 14) ));

		JLabel sigLabel = new JLabel(I18n.getMessage("thaw.plugin.signature.trustLevel.trustLevel")+ " : ");
		JTextField sigLevel = new JTextField(author.getTrustLevelStr()
						     + (author.isDup() ? " - " + I18n.getMessage("thaw.plugin.signature.duplicata") : ""));


		sigLevel.setForeground(author.getTrustLevelColor());
		sigLevel.setEditable(false);
		sigLevel.setBackground(panel.getBackground());


		JPanel sigPanel = new JPanel(new BorderLayout());
		sigPanel.add(sigLabel, BorderLayout.WEST);
		sigPanel.add(sigLevel, BorderLayout.CENTER);


		Vector trustLevels = new Vector();

		for (int i = 0 ; i < Identity.trustLevelInt.length ; i++) {
			if (Identity.trustLevelInt[i] < 100)
				trustLevels.add(Identity.trustLevelStr[i]);
		}


		trust = new JComboBox(trustLevels);
		trust.setSelectedItem(author.getTrustLevelStr());
		trust.addActionListener(this);

		JPanel trustPanel = new JPanel(new BorderLayout(5, 5));

		if (author.getPrivateKey() == null) {
			trustPanel.add(trust, BorderLayout.CENTER);
		}

		JPanel bottomRightPanel = new JPanel(new BorderLayout(5, 5));

		bottomRightPanel.add(trustPanel, BorderLayout.CENTER);

		if ( (hasPrivateKey && (author.getPrivateKey() == null || blackListed) )
			&& (!isPrivateKeyPublished) ) {
			changeBlackListState = new JButton(blackListed ?
							   I18n.getMessage("thaw.plugin.index.comment.unmoderate") :
							   I18n.getMessage("thaw.plugin.index.comment.moderate"));
			changeBlackListState.addActionListener(this);
			bottomRightPanel.add(changeBlackListState, BorderLayout.EAST);
		}


		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(sigPanel, BorderLayout.WEST);
		topPanel.add(new JLabel(""), BorderLayout.CENTER);
		topPanel.add(bottomRightPanel, BorderLayout.EAST);


		text.setEditable(false);
		text.setBackground(panel.getBackground());


		panel.add(text, BorderLayout.CENTER);
		panel.add(topPanel, BorderLayout.NORTH);

		return panel;
	}


	public boolean mustBeIgnored(Config config) {
		if (author == null) /* fix nextgens weird case */
			return true;

		return author.mustBeIgnored(config);
	}


	public boolean isBlackListed() {
		return isBlackListed(db, index.getId(), rev);
	}

	public void unBlackList() {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st = db.getConnection().prepareStatement("DELETE FROM indexCommentBlackList WHERE rev = ? AND indexId = ?");
				st.setInt(1, rev);
				st.setInt(2, index.getId());
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to un-blacklist comment because: "+e.toString());
		}
	}


	public void blackList() {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st = db.getConnection().prepareStatement("INSERT INTO indexCommentBlackList (rev, indexId) VALUES (?, ?)");
				st.setInt(1, rev);
				st.setInt(2, index.getId());
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to blacklist comment because: "+e.toString());
		}
	}


	/**
	 * Only index owner(s) must be able to see black listed comments
	 */
	public static boolean isBlackListed(Hsqldb db, int indexId, int rev) {
		try {
			synchronized(db.dbLock) {

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id FROM indexCommentBlackList WHERE indexId = ? AND rev = ? LIMIT 1");
				st.setInt(1, indexId);
				st.setInt(2, rev);

				ResultSet set = st.executeQuery();

				return set.next();
			}
		} catch(SQLException e) {
			Logger.error(db, "thaw.plugins.index.Comment : Error while checking if the message is in the blacklist :"+e.toString());
		}

		return false;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == trust) {
			if (author == null)
				return;
			author.setTrustLevel((String)trust.getSelectedItem());
			tab.updateCommentList();
			return;
		}

		if (e.getSource() == changeBlackListState) {
			if (blackListed) {
				unBlackList();
			} else {
				blackList();
			}

			tab.updateCommentList();

			return;
		}
	}


	/**
	 * Will write it in a temporary file
	 */
	public java.io.File writeCommentToFile() {

		java.io.File outputFile;

		try {
			outputFile = java.io.File.createTempFile("thaw-", "-comment.xml");
			outputFile.deleteOnExit();
		} catch(java.io.IOException e) {
			Logger.error(new Comment(), "Unable to write comment in a temporary file because: "+e.toString());
			return null;
		}

		OutputStream out;

		try {
			out = new FileOutputStream(outputFile);
		} catch(java.io.FileNotFoundException e) {
			Logger.error(new Comment(), "File not found exception ?!");
			return null;
		}

		StreamResult streamResult;

		streamResult = new StreamResult(out);

		Document xmlDoc;

		final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder xmlBuilder;

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(final javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(new Comment(), "Unable to generate the comment xml file because : "+e.toString());
			return null;
		}

		final DOMImplementation impl = xmlBuilder.getDOMImplementation();

		xmlDoc = impl.createDocument(null, "comment", null);

		final Element rootEl = xmlDoc.getDocumentElement();


		/** START FILLING THE XML TREE HERE **/

		Element authorTag = xmlDoc.createElement("author");
		Element textTag = xmlDoc.createElement("text");

		Text authorTxt = xmlDoc.createTextNode(author.getNick());
		Text textTxt = xmlDoc.createTextNode(comment);

		authorTag.appendChild(authorTxt);
		textTag.appendChild(textTxt);

		Element signatureTag = xmlDoc.createElement("signature");

		Element sigTag = xmlDoc.createElement("sig");

		String sig = author.sign(index.getCommentPublicKey()+"-"+
					 author.getNick()+"-"+
					 comment);

		Text sigTxt = xmlDoc.createTextNode(sig);

		sigTag.appendChild(sigTxt);


		Element publicKeyTag = xmlDoc.createElement("publicKey");

		Text publicKeyTxt = xmlDoc.createTextNode(author.getPublicKey());

		publicKeyTag.appendChild(publicKeyTxt);

		signatureTag.appendChild(sigTag);
		signatureTag.appendChild(publicKeyTag);

		rootEl.appendChild(authorTag);
		rootEl.appendChild(textTag);
		rootEl.appendChild(signatureTag);


		/** GENERATE THE FILE **/


		/* Serialization */
		final DOMSource domSource = new DOMSource(xmlDoc);
		final TransformerFactory transformFactory = TransformerFactory.newInstance();

		Transformer serializer;

		try {
			serializer = transformFactory.newTransformer();
		} catch(final javax.xml.transform.TransformerConfigurationException e) {
			Logger.error(new Comment(), "Unable to write comment in an XML file because: "+e.toString());
			return null;
		}

		serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");

		/* final step */
		try {
			serializer.transform(domSource, streamResult);
		} catch(final javax.xml.transform.TransformerException e) {
			Logger.error(new Comment(), "Unable to save comment in an XML file (2) because: "+e.toString());
			return null;
		}


		return outputFile;
	}


	private FCPQueueManager queueManager;
	private MainWindow      mainWindow;

	/**
	 * @param privateKey must be an SSK without anything useless
	 */
	public boolean insertComment(FCPQueueManager queueManager, MainWindow mainWindow) {
		String privateKey = index.getCommentPrivateKey();

		this.queueManager = queueManager;
		this.mainWindow   = mainWindow;

		java.io.File xmlFile = writeCommentToFile();

		if (xmlFile == null)
                        return false;

		FCPClientPut put = new FCPClientPut(xmlFile,
						    FCPClientPut.KEY_TYPE_SSK,
						    0, /* rev : as we insert as USK => EDONTCARE */
						    "comment", /* filename (not really used anymore) */
                                                    FreenetURIHelper.convertSSKtoUSK(privateKey)+"/", /* the convertion fonction forget the '/' */
						    2, /* priority */
						    false, /* global */
						    FCPClientPut.PERSISTENCE_FOREVER ); /* persistence */
                put.addObserver(this);

		return queueManager.addQueryToTheRunningQueue(put);
	}



	protected class CommentHandler extends DefaultHandler {

		public CommentHandler() {

		}

		/**
		 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
		 */
		public void setDocumentLocator(Locator value) {

		}


		/**
		 * Called when parsing is started
		 * @see org.xml.sax.ContentHandler#startDocument()
		 */
		public void startDocument() throws SAXException {

		}


		/**
		 * Called when starting to parse in a specific name space
		 * @param prefix name space prefix
		 * @param URI name space URI
		 * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
		 */
		public void startPrefixMapping(String prefix, String URI) throws SAXException {
			/* \_o< */
		}

		/**
		 * @param prefix name space prefix
		 * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
		 */
		public void endPrefixMapping(String prefix) throws SAXException {
			/* \_o< */
		}


		private boolean authorTag;
		private boolean textTag;

		private boolean publicKeyTag;
		private boolean sigTag;

		/* needed to create / get the corresponding identity */
		private String authorTxt;
		private String publicKey;


		/**
		 * Called when the parsed find an opening tag
		 * @param localName local tag name
		 * @param rawName rawName (the one used here)
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(String nameSpaceURI, String localName,
					 String rawName, Attributes attrs) throws SAXException {
			if (rawName == null) {
				rawName = localName;
			}

			if (rawName == null)
				return;

			if ("author".equals(rawName))
				authorTag = true;

			if ("text".equals(rawName))
				textTag = true;

			if ("publicKey".equals(rawName))
				publicKeyTag = true;

			if ("sig".equals(rawName))
				sigTag = true;
		}


		/**
		 * Called when a closing tag is met
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		public void endElement(String nameSpaceURI, String localName,
				       String rawName) throws SAXException {
			if (rawName == null) {
				rawName = localName;
			}

			if (rawName == null)
				return;

			if ("author".equals(rawName))
				authorTag = false;

			if ("text".equals(rawName))
				textTag = false;

			if ("publicKey".equals(rawName))
				publicKeyTag = false;

			if ("sig".equals(rawName))
				sigTag = false;
		}


		/**
		 * Called when a text between two tag is met
		 * @param ch text
		 * @param start position
		 * @param end position
		 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
		 */
		public void characters(char[] ch, int start, int end) throws SAXException {
			String txt = new String(ch, start, end);

			if (authorTag)
				authorTxt = txt;

			if (textTag)
				comment = txt;

			if (publicKeyTag)
				publicKey = txt;

			if (sigTag)
				sig = txt;
		}

		public void ignorableWhitespace(char[] ch, int start, int end) throws SAXException {

		}

		public void processingInstruction(String target, String data) throws SAXException {

		}

		/**
		 * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
		 */
		public void skippedEntity(String arg0) throws SAXException {

		}

		/**
		 * Called when parsing is finished
		 * @see org.xml.sax.ContentHandler#endDocument()
		 */
		public void endDocument() throws SAXException {
			valid = false;

			try {
				if (comment != null && authorTxt != null
				    && publicKey != null && sig != null
				    && index != null) {

					author = Identity.getIdentity(db, authorTxt, publicKey);

					if (author == null) {
						Logger.warning(this, "Can't find the identity in the DB ?! WTF ?!");
						valid = false;
						return;
					}

					valid = author.check(index.getCommentPublicKey()+"-"+
							     author.getNick()+"-"+
							     comment,
							     sig);

					if (!valid) {
						Logger.notice(this, "Signature validation failed !");
					}
				} else {
					Logger.notice(this, "Signature validation failed ! (missing elements)");
					valid = false;
				}
			} catch(Exception e) { /* we must not failed ! */
				Logger.error(this, "Error while checking signature: "+e.toString());
				e.printStackTrace();
				valid = false;
			}
		}
	}


	/**
	 * @return false if already in the bdd or if there is any error
	 */
	public boolean parseComment(java.io.File xmlFile) {
		newComment = false;

		Logger.info(this, "Parsing comment : "+index.getCommentPublicKey() + " : "+Integer.toString(rev));

		FileInputStream in;

		try {
			in = new FileInputStream(xmlFile);
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(this, "Unable to load XML: FileNotFoundException ('"+xmlFile.getPath()+"') ! : "+e.toString());
			return false;
		}

		CommentHandler handler = new CommentHandler();

		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();

			// Parse the input
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(in, handler);
		} catch(javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(this, "Error (1) while parsing index: "+e.toString());
		} catch(org.xml.sax.SAXException e) {
			Logger.error(this, "Error (2) while parsing index: "+e.toString());
		} catch(java.io.IOException e) {
			Logger.error(this, "Error (3) while parsing index: "+e.toString());
		}

		if (comment != null && author != null && valid) {
			Logger.info(this, "Parsing done");

			try {
				synchronized(db.dbLock) {

					if (existsInTheBdd()) {
						Logger.debug(this, "Comment already in db");
						newComment = false;
						valid = false;
						return false;
					}

					Logger.info(this, "New comment !");

					newComment = true;

					PreparedStatement st;

					st = db.getConnection().prepareStatement("INSERT INTO indexComments "+
										 "(authorId, text, rev, indexId, sig) "+
										 "VALUES (?, ?, ?, ?, ?)");
					st.setInt(1, author.getId());
					st.setString(2, comment);
					st.setInt(3, rev);
					st.setInt(4, index.getId());
					st.setString(5, sig);

					st.execute();

					return true;
				}
			} catch(SQLException e) {
				Logger.error(this, "Unable to add comment in the db because: "+e.toString());
			}
		}
		else
			Logger.notice(this, "Parsing failed !");

		return false;
	}



	/**
	 * On freenet
	 */
	public boolean exists() {
		return (comment != null && author != null);
	}

	public boolean isValid() {
		return valid;
	}


	/**
	 * r and s must be set
	 * You have to do the synchronized(db.dbLock) !
	 */
	private boolean existsInTheBdd() {
		if (sig == null) {
			Logger.notice(this, "No sig, can't say if it's already in the bdd");
			return true;
		}

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT id FROM indexComments "+
								 "WHERE sig = ?");

			st.setString(1, sig);

			ResultSet set = st.executeQuery();

			return (set.next());
		} catch(SQLException e) {
			Logger.error(this, "Unable to check if the comment is already in the bdd, because: "+e.toString());
		}

		return true;
	}


	public boolean isNew() {
		return newComment;
	}


	public boolean fetchComment(FCPQueueManager queueManager) {
		newComment = false;

		this.queueManager = queueManager;

		String publicKey = index.getCommentPublicKey(); /* should be an SSK */

		if (publicKey == null)
			return false;

		publicKey += "comment-"+Integer.toString(rev)+"/comment.xml";

		FCPClientGet get = new FCPClientGet(publicKey, 2 /* priority */,
						    FCPClientGet.PERSISTENCE_UNTIL_DISCONNECT /* persistence */,
						    false /* global queue */, 0 /* maxretries */,
						    System.getProperty("java.io.tmpdir"),
						    MAX_SIZE, true /* no DDA */);

		get.addObserver(this);

		return get.start(queueManager);
	}



	public void update(Observable o, Object param) {
		if (o instanceof FCPTransferQuery) {
			if (((FCPTransferQuery)o).isFinished()) {
				o.deleteObserver(this);
				((FCPTransferQuery)o).stop(queueManager);
				queueManager.remove((FCPTransferQuery)o);
			}

			if (o instanceof FCPClientPut) {
				FCPClientPut put = (FCPClientPut)o;

				if (put.isFinished() && put.isSuccessful()) {
					if (put.stop(queueManager))
						queueManager.remove(put);
					/* because the PersistentPut message sent by the node problably made it
					 * added to the queueManager  by the QueueLoader*/
				} else if (put.isFinished() && !put.isSuccessful()) {
					int ret = JOptionPane.showOptionDialog(mainWindow.getMainFrame(),
									       I18n.getMessage("thaw.plugin.index.comment.failed"),
									       I18n.getMessage("thaw.error.title"),
									       JOptionPane.YES_NO_OPTION,
									       JOptionPane.ERROR_MESSAGE,
									       null,
									       null,
									       null);
					if (ret == JOptionPane.YES_OPTION) {
						/* we stop */
						if (put.stop(queueManager))
							queueManager.remove(put);
						/* and we restart */
						insertComment(queueManager, mainWindow);
					}
				}
			}

			if (o instanceof FCPClientGet) {
				FCPClientGet get = (FCPClientGet)o;

				if (get.isFinished() && get.isSuccessful()) {
					parseComment(new java.io.File(get.getPath()));
				}

			}

			FCPTransferQuery q = ((FCPTransferQuery)o);

			if (q.isFinished() && q.isSuccessful()) {
				java.io.File file = new java.io.File(q.getPath());

				file.delete();
			}

			if (q.isFinished()) {
				setChanged();
				notifyObservers();
			}

		}

		if (o instanceof FCPTransferQuery) {
			FCPTransferQuery q = (FCPTransferQuery)o;

			if (q.isFinished() && q.isSuccessful() && q instanceof Observable)
				((Observable)q).deleteObserver(this);
		}
	}


}
