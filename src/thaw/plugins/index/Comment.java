package thaw.plugins.index;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.util.Observer;
import java.util.Observable;

/* Swing */

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;

import java.awt.GridLayout;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


/* SAX */

import org.xml.sax.*;
import org.xml.sax.helpers.LocatorImpl;

import java.io.IOException;

import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;



/* SQL */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/* Thaw */

import thaw.core.Config;
import thaw.core.Logger;
import thaw.core.I18n;

import thaw.fcp.FreenetURIHelper;
import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPQueueManager;

import thaw.plugins.Hsqldb;


/**
 * Will use, from the configuration:
 *  'userNickname' as the author name
 */
public class Comment extends Observable implements Observer {
	public final static int MAX_SIZE = 16384;

	private String author;
	private String comment;

	private Index index;
	private Hsqldb db;

	private int rev;

	private boolean newComment = false;

	private Comment() {

	}


	/**
	 * @param index parent index
	 * @param rev revision of the comment (-1) if not inserted at the moment
	 * @param comment comment inside the comment ... :)
	 */
	public Comment(Hsqldb db, Index index, int rev, String author, String comment) {
		this.db = db;
		this.author = author;
		this.comment = comment;
		this.index = index;
		this.rev = rev;
	}


	public JPanel getPanel() {
		JPanel panel = new JPanel(new GridLayout(1, 1));
		JTextArea text = new JTextArea(comment.trim());

		panel.setBorder(BorderFactory.createTitledBorder(I18n.getMessage("thaw.plugin.index.comment.author")+" : "+author));

		text.setEditable(false);
		text.setBackground(panel.getBackground());

		//panel.setPreferredSize(new java.awt.Dimension(600, 150));

		panel.add(text);

		return panel;
	}


	/**
	 * Will write it in a temporary file
	 */
	public java.io.File writeCommentToFile() {

		java.io.File outputFile;

		try {
			outputFile = java.io.File.createTempFile("thaw-", "-comment.xml");
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

		Text authorTxt = xmlDoc.createTextNode(author);
		Text textTxt = xmlDoc.createTextNode(comment);

		authorTag.appendChild(authorTxt);
		textTag.appendChild(textTxt);

		rootEl.appendChild(authorTag);
		rootEl.appendChild(textTag);


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


	/**
	 * @param privateKey must be an SSK without anything useless
	 */
	public boolean insertComment(FCPQueueManager queueManager) {
		String privateKey = index.getCommentPrivateKey();

		this.queueManager = queueManager;

		java.io.File xmlFile = writeCommentToFile();

		if (xmlFile == null)
                        return false;

		FCPClientPut put = new FCPClientPut(xmlFile, 2, 0, "comment",
                                                    FreenetURIHelper.convertSSKtoUSK(privateKey)+"/", /* the convertion fonction forget the '/' */
						    2, false, 0);
                put.addObserver(this);

		return queueManager.addQueryToTheRunningQueue(put);
	}



	protected class CommentHandler extends DefaultHandler {
		private Locator locator = null;

		public CommentHandler() {

		}

		/**
		 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
		 */
		public void setDocumentLocator(Locator value) {
			locator =  value;
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

			if (authorTag) author = txt;
			if (textTag)   comment = txt;
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

		}
	}


	/**
	 * @return false if already in the bdd or if there is any error
	 */
	public boolean parseComment(java.io.File xmlFile) {
		newComment = false;

		Logger.info(this, "Parsing comment ...");

		FileInputStream in;

		try {
			in = new FileInputStream(xmlFile);
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(this, "Unable to load XML: FileNotFoundException ('"+xmlFile.getPath()+"') ! : "+e.toString());
			return false;
		}

		CommentHandler handler = new CommentHandler();

		try {
			// Use the default (non-validating) parser
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

		if (comment != null && author != null) {
			Logger.info(this, "Parsing done");

			try {
				synchronized(db.dbLock) {
					PreparedStatement st;

					st = db.getConnection().prepareStatement("SELECT id FROM indexComments "+
										 "WHERE indexId = ? AND author = ? "+
										 "AND text = ?");

					st.setInt(1, index.getId());
					st.setString(2, author);
					st.setString(3, comment);

					ResultSet set = st.executeQuery();

					if (set.next()) {
						Logger.debug(this, "Comment already in db");
						return false;
					}

					Logger.info(this, "New comment !");

					newComment = true;

					st = db.getConnection().prepareStatement("INSERT INTO indexComments "+
										 "(author, text, rev, indexId) "+
										 "VALUES (?, ?, ?, ?)");
					st.setString(1, author);
					st.setString(2, comment);
					st.setInt(3, rev);
					st.setInt(4, index.getId());

					st.execute();

					return true;
				}
			} catch(SQLException e) {
				Logger.error(this, "Unable to add comment in the db because: "+e.toString());
			}
		}

		return false;
	}



	/**
	 * On freenet
	 */
	public boolean exists() {
		return (comment != null && author != null);
	}

	public boolean isNew() {
		return newComment;
	}


	public boolean fetchComment(FCPQueueManager queueManager) {
		newComment = false;

		this.queueManager = queueManager;

		String publicKey = index.getCommentPublicKey(); /* should be an SSK */

		publicKey += "comment-"+Integer.toString(rev)+"/comment.xml";

		FCPClientGet get = new FCPClientGet(publicKey, 2 /* priority */, 2 /* persistence */,
						    false /* global queue */, 5 /* max retries */,
						    System.getProperty("java.io.tmpdir"),
						    MAX_SIZE, true /* no DDA */);

		get.addObserver(this);

		return get.start(queueManager);
	}



	public void update(Observable o, Object param) {
		if (o instanceof FCPTransferQuery) {
			if (((FCPTransferQuery)o).isFinished())
				((Observable)o).deleteObserver(this);

			if (o instanceof FCPClientPut) {
				FCPClientPut put = (FCPClientPut)o;

				if (put.isFinished() && put.isSuccessful()) {
					if (put.stop(queueManager))
						queueManager.remove(put);
					/* because the PersistentPut message sent by the node problably made it added to the queueManager  by the QueueLoader*/
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
