package thaw.plugins.miniFrost.frostKSK;

import java.sql.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.text.SimpleDateFormat;
import java.io.File;

import java.util.Vector;
import java.util.Iterator;
import java.util.List;
import java.util.Date;

import frost.util.XMLTools;
import org.bouncycastle.util.encoders.Base64;

import frost.crypt.FrostCrypt;

import thaw.plugins.signatures.Identity;

import thaw.plugins.Hsqldb;
import thaw.core.Logger;
import thaw.core.I18n;

import thaw.plugins.miniFrost.RegexpBlacklist;


/**
 * Dirty parser reusing some Frost functions
 * (Note: dirty mainly because of the Frost format :p)
 */
public class KSKMessageParser {

	public KSKMessageParser() {

	}

	private String messageId;
	private String inReplyTo;
	private String from;
	private String subject;
	private String date;
	private String time;
	private String recipient;
	private String board;
	private String body;
	private String publicKey;
	private String signature;
	private String idLinePos;
	private String idLineLen;

	private Vector attachments;

	private Identity identity;

	private boolean read = false;
	private boolean archived = false;

	private Identity encryptedFor = null;

	private static FrostCrypt frostCrypt;


	public KSKMessageParser(String inReplyTo,
				String from,
				String subject,
				java.util.Date dateUtil,
				String recipient,
				String board,
				String body,
				String publicKey,
				Vector attachments,
				Identity identity) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.M.d HH:mm:ss");
		dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

		this.messageId = ""; /* will be generated from the SHA1 of the content */
		this.inReplyTo = inReplyTo;
		this.from = from;
		this.subject = subject;

		String[] date = dateFormat.format(dateUtil).toString().split(" ");
		this.date = date[0];
		this.time = date[1];

		this.recipient = null;

		this.board = board;
		this.body = body;
		this.publicKey = publicKey;
		this.idLinePos = "0";
		this.idLineLen = "0";

		this.attachments = attachments;

		this.identity = identity;

		if (frostCrypt == null)
			frostCrypt = new FrostCrypt();

		/* frost wants a SHA256 hash, but can't check from what is comes :p */
		this.messageId = frostCrypt.computeChecksumSHA256(getSignedContent(false));

		if (identity == null)
			signature = null;
		else {
			signature = identity.sign(getSignedContent(true));
		}
	}


	private boolean alreadyInTheDb(Hsqldb db, String msgId) {
		if (msgId == null) {
			Logger.notice(this, "no message id => ignoring this message by supposing it "
				      +"already in the db");
			return true;
		}

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id FROM frostKSKMessages "+
									 "WHERE msgId = ? LIMIT 1");
				st.setString(1, msgId);

				ResultSet res = st.executeQuery();

				return (res.next());
			}
		} catch(SQLException e) {
			Logger.error(this,
				     "Exception while checking if the message was already in the db: "+
				     e.toString());
			return false;
		}
	}



	public boolean insert(Hsqldb db,
			      int boardId, java.util.Date boardDate, int rev,
			      String boardNameExpected) {

		if (boardNameExpected == null) {
			Logger.notice(this, "Board name expected == null ?!");
			return false;
		}

		if (board != null
		    && !(boardNameExpected.toLowerCase().equals(board.toLowerCase()))) {
			Logger.notice(this, "Board name doesn't match");
			return false;
		}

		if (alreadyInTheDb(db, messageId)) {
			Logger.notice(this, "We have already this id in the db ?!");
			archived = true;
			read = true;
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.M.d HH:mm:ss");

		date = date.trim();
		time = time.trim();

		date += " "+time;

		java.util.Date dateUtil = dateFormat.parse(date, new java.text.ParsePosition(0));

		if (dateUtil != null) {
			long dateDiff = KSKBoard.getMidnight(dateUtil).getTime() - KSKBoard.getMidnight(boardDate).getTime();
			/* we accept between X days before and X days after */

			if (dateDiff < (KSKBoard.MAX_DAYS_IN_THE_PAST+1)*(-1)*28*60*60*1000
			    || dateDiff > (KSKBoard.MAX_DAYS_IN_THE_FUTURE+1)*24*60*60*1000)
				dateUtil = null;
		}


		java.sql.Timestamp timestampSql;

		if (dateUtil != null)
			timestampSql = new java.sql.Timestamp(dateUtil.getTime());
		else
			timestampSql = new java.sql.Timestamp(boardDate.getTime());

		java.sql.Date dateSql = new java.sql.Date(boardDate.getTime());


		int replyToId = -1;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				/* we search the message to this one answer */
				if (inReplyTo != null) {
					String[] split = inReplyTo.split(",");
					inReplyTo = split[split.length-1];

					st = db.getConnection().prepareStatement("SELECT id FROM frostKSKMessages "+
										 "WHERE msgId = ? LIMIT 1");
					st.setString(1, inReplyTo);

					ResultSet res = st.executeQuery();

					if (res.next())
						replyToId = res.getInt("id");

				}

				/* we insert the message */

				st = db.getConnection().prepareStatement("INSERT INTO frostKSKMessages ("+
									 "subject, nick, sigId, content, "+
									 "date, msgId, inReplyTo, inReplyToId, "+
									 "rev, keyDate, read, archived, "+
									 "encryptedFor, boardId) VALUES ("+
									 "?, ?, ?, ?, "+
									 "?, ?, ?, ?, "+
									 "?, ?, ?, ?, "+
									 "?, ?)");
				st.setString(1, subject);
				st.setString(2, from); /* nick */
				if (identity != null)
					st.setInt(3, identity.getId());
				else
					st.setNull(3, Types.INTEGER);
				st.setString(4, body); /* content */
				st.setTimestamp(5, timestampSql);
				st.setString(6, messageId);

				if (replyToId >= 0)
					st.setInt(7, replyToId);
				else
					st.setNull(7, Types.INTEGER);

				if (inReplyTo != null)
					st.setString(8, inReplyTo);
				else
					st.setNull(8, Types.VARCHAR);

				st.setInt(9, rev);

				st.setDate(10, dateSql);

				st.setBoolean(11, read);
				st.setBoolean(12, archived);

				if (encryptedFor == null)
					st.setNull(13, Types.INTEGER);
				else
					st.setInt(13, encryptedFor.getId());

				st.setInt(14, boardId);

				st.execute();


				/* we need the id of the message */

				st = db.getConnection().prepareStatement("SELECT id FROM frostKSKmessages "+
									 "WHERE msgId = ? LIMIT 1");
				st.setString(1, messageId);

				ResultSet set = st.executeQuery();

				set.next();

				int id = set.getInt("id");

				/* we insert the attachments */

				if (attachments != null) {
					for(Iterator it = attachments.iterator();
					    it.hasNext();) {
						KSKAttachment a = (KSKAttachment)it.next();
						a.insert(db, id);
					}
				}
			}
		} catch(SQLException e) {
			Logger.warning(this, "Can't insert the message into the db because : "+e.toString());
			return false;
		}

		return true;
	}


	public boolean filter(RegexpBlacklist blacklist) {
		if (blacklist.isBlacklisted(subject)
		    || blacklist.isBlacklisted(from)
		    || blacklist.isBlacklisted(body)) {
			read = true;
			archived = true;
		}

		return true;
	}



	public final static char SIGNATURE_ELEMENTS_SEPARATOR = '|';

	/**
	 * @param withMsgId require to check the signature
	 */
	private String getSignedContent(boolean withMsgId) {
		final StringBuffer allContent = new StringBuffer();

		allContent.append(date).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(time+"GMT").append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(board).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(from).append(SIGNATURE_ELEMENTS_SEPARATOR);
		if (withMsgId)
			allContent.append(messageId).append(SIGNATURE_ELEMENTS_SEPARATOR);
		if( inReplyTo != null && inReplyTo.length() > 0 ) {
			allContent.append(inReplyTo).append(SIGNATURE_ELEMENTS_SEPARATOR);
		}
		if( recipient != null && recipient.length() > 0 ) {
			allContent.append(recipient).append(SIGNATURE_ELEMENTS_SEPARATOR);
		}
		allContent.append(idLinePos).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(idLineLen).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(subject).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(body).append(SIGNATURE_ELEMENTS_SEPARATOR);

		if (attachments != null) {
			for (Iterator it = attachments.iterator();
			     it.hasNext();) {
				KSKAttachment a = (KSKAttachment)it.next();
				allContent.append(a.getSignedStr());
			}
		}

		return allContent.toString();

	}


	public boolean checkSignature(Hsqldb db) {
		if (publicKey == null || signature == null) {
			from = from.replaceAll("@", "_");
			return true;
		}

		String[] split = from.split("@");
		if (split.length < 2 || "".equals(split[0].trim())) {
			Logger.notice(this, "Unable to extract the nick name from the message");
			return false;
		}

		String nick = split[0].trim();

		identity = Identity.getIdentity(db, nick, publicKey);

		boolean ret = identity.check(getSignedContent(true), signature);

		if (!ret) {
			Logger.warning(this, "Invalid signature !");
		}

		return ret;
	}



	protected boolean loadXMLElements(Element root) {
		messageId     = XMLTools.getChildElementsCDATAValue(root, "MessageId");
		inReplyTo     = XMLTools.getChildElementsCDATAValue(root, "InReplyTo");
		from          = XMLTools.getChildElementsCDATAValue(root, "From");
		subject       = XMLTools.getChildElementsCDATAValue(root, "Subject");
		date          = XMLTools.getChildElementsCDATAValue(root, "Date");
		time          = XMLTools.getChildElementsCDATAValue(root, "Time");
		if (time == null) time = "00:00:00GMT";
		time          = time.replaceAll("GMT", "");
		recipient     = XMLTools.getChildElementsCDATAValue(root, "recipient");
		board         = XMLTools.getChildElementsCDATAValue(root, "Board");
		if (board == null) board = ""; /* won't validate a check in insert() :p */
		body          = XMLTools.getChildElementsCDATAValue(root, "Body");
		signature     = XMLTools.getChildElementsCDATAValue(root, "SignatureV2");
		publicKey     = XMLTools.getChildElementsCDATAValue(root, "pubKey");
		idLinePos     = XMLTools.getChildElementsTextValue(root, "IdLinePos");
		idLineLen     = XMLTools.getChildElementsTextValue(root, "IdLineLen");

		List l = XMLTools.getChildElementsByTagName(root, "AttachmentList");
		if (l.size() == 1) {
			attachments = new Vector();

			KSKAttachmentFactory factory = new KSKAttachmentFactory();

			Element attachmentsEl = (Element) l.get(0);
			Iterator i = XMLTools.getChildElementsByTagName(attachmentsEl,"Attachment").iterator();
			while (i.hasNext()){
				Element el = (Element)i.next();
				KSKAttachment attachment = factory.getAttachment(el);
				if (attachment != null)
					attachments.add(attachment);
			}
		}

		if (from == null || subject == null || body == null) {
			Logger.notice(this, "Field missing");
			return false;
		}

		return true;
	}



	protected boolean decrypt(Hsqldb db, Element rootNode) {
		Vector identities = Identity.getYourIdentities(db);

		/**
		 * I prefer to not trust the recipient field ...
		 */

		byte[] content;
		String recipient = XMLTools.getChildElementsCDATAValue(rootNode, "recipient");

		try {
			content = Base64.decode(XMLTools.getChildElementsCDATAValue(rootNode,
										    "content").getBytes("UTF-8"));
		} catch(Exception e) {
			Logger.notice(this, "Unable to decode encrypted message because : "+e.toString());
			return false;
		}

		Identity identity = null;

		for (Iterator it = identities.iterator();
		     it.hasNext();) {
			Identity id = (Identity)it.next();
			if (id.toString().equals(recipient)) {
				identity = id;
				break;
			}
		}


		if (identity == null) {
			Logger.info(this, "Not for us but for '"+recipient+"'");
		}

		byte[] decoded = null;

		if (identity != null)
			decoded = identity.decode(content);

		if (decoded != null) {
			/*** we are able to decrypt it ***/

			/* Hm, there should be a better way (all in RAM) */

			encryptedFor = identity;

			File tmp = null;
			boolean ret = false;

			try {

				tmp = File.createTempFile("thaw-", "-decrypted-msg.xml");
				tmp.deleteOnExit();

				frost.util.FileAccess.writeFile(decoded, tmp);

				/* recursivity (bad bad bad, but I'm lazy :) */
				ret = loadFile(tmp, db);
			} catch(Exception e) {
				Logger.warning(this, "Unable to read the decrypted message because: "+e.toString());
			}

			if (tmp != null)
				tmp.delete();

			return ret;
		}


		/*** Unable to decrypt the message, but we will store what we know anyway ***
		 * to not fetch this message again
		 */
		/* (I'm still thinking that mixing up Boards & private messages is a BAD idea) */

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.M.d HH:mm:ss");
		dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

		inReplyTo = null;
		from = "["+I18n.getMessage("thaw.plugin.miniFrost.encrypted")+"]";
		subject = "["+I18n.getMessage("thaw.plugin.miniFrost.encryptedBody").replaceAll("X", recipient)+"]";

		String[] date = dateFormat.format(new Date()).toString().split(" ");
		this.date = date[0];
		this.time = date[1];

		/* not really used */
		this.recipient = recipient;
		/* will be ignored by the checks */
		this.board = null;

		this.body = I18n.getMessage("thaw.plugin.miniFrost.encryptedBody").replaceAll("X", recipient);
		this.publicKey = null;
		this.signature = null;
		this.idLinePos = "0";
		this.idLineLen = "0";
		attachments = null;

		identity = null;

		if (frostCrypt == null)
			frostCrypt = new FrostCrypt();
		this.messageId = frostCrypt.computeChecksumSHA256(date[0] + date[1]);

		read = true;
		archived = true;

		/* because we have date to store: */
		return true;
	}


	/**
	 * This function has been imported from FROST.
	 * Parses the XML file and passes the FrostMessage element to XMLize load method.
	 * @param db require if the message is encrypted
	 */
	public boolean loadFile(File file, Hsqldb db) {
		try {
			Document doc = null;
			try {
				doc = XMLTools.parseXmlFile(file, false);
			} catch(Exception ex) {  // xml format error
				Logger.notice(this, "Invalid Xml");
				return false;
			}

			if( doc == null ) {
				Logger.notice(this,
					       "Error: couldn't parse XML Document - " +
					       "File name: '" + file.getName() + "'");
				return false;
			}

			Element rootNode = doc.getDocumentElement();

			if(rootNode.getTagName().equals("EncryptedFrostMessage")) {
				if (db != null) {
					return decrypt(db, rootNode);
				} else {
					Logger.error(this, "Can't decrypt the message (no connection to the db available)");
					return false;
				}
			}

			// load the message itself
			return loadXMLElements(rootNode);

		} catch(Exception e) {
			/* XMLTools throws runtime exception sometimes ... */
			Logger.notice(this, "Unable to parse XML message because : "+e.toString());
			e.printStackTrace();
			return false;
		}
	}

	public Element makeText(Document doc, String tagName, String content) {
		if (content == null || tagName == null)
			return null;

		Text txt;
		Element current;

		current = doc.createElement(tagName);
		txt = doc.createTextNode(content);
		current.appendChild(txt);

		return current;
	}


	public Element makeCDATA(Document doc, String tagName, String content) {
		if (content == null || tagName == null)
			return null;

		CDATASection cdata;
		Element current;

		current = doc.createElement(tagName);
		cdata = doc.createCDATASection(content);
		current.appendChild(cdata);

		return current;
	}


	public Element getXMLTree(Document doc) {
		Element root = doc.createElement("FrostMessage");

		Element el;

		if ((el = makeText( doc, "client",    "Thaw "+thaw.core.Main.VERSION)) != null)
			root.appendChild(el);
		if ((el = makeCDATA(doc, "MessageId", messageId)) != null)   root.appendChild(el);
		if ((el = makeCDATA(doc, "InReplyTo", inReplyTo)) != null)   root.appendChild(el);
		if ((el = makeText( doc, "IdLinePos", idLinePos)) != null)   root.appendChild(el);
		if ((el = makeText( doc, "IdLineLen", idLineLen)) != null)   root.appendChild(el);
		if ((el = makeCDATA(doc, "From", from)) != null)             root.appendChild(el);
		if ((el = makeCDATA(doc, "Subject", subject)) != null)       root.appendChild(el);
		if ((el = makeCDATA(doc, "Date", date)) != null)             root.appendChild(el);
		if ((el = makeCDATA(doc, "Time", time+"GMT")) != null)       root.appendChild(el);
		if ((el = makeCDATA(doc, "Body", body)) != null)             root.appendChild(el);
		if ((el = makeCDATA(doc, "Board", board)) != null)           root.appendChild(el);
		if ((el = makeCDATA(doc, "pubKey", publicKey)) != null)      root.appendChild(el);
		if ((el = makeCDATA(doc, "recipient", recipient)) != null)   root.appendChild(el);
		if ((el = makeCDATA(doc, "SignatureV2", signature)) != null) root.appendChild(el);

		if (attachments != null) {
			el = doc.createElement("AttachmentList");

			for (Iterator it = attachments.iterator();
			     it.hasNext();) {
				el.appendChild(((KSKAttachment)it.next()).getXML(doc));
			}

			root.appendChild(el);
		}

		return root;
	}


	public File generateXML() {
		File tmpFile;

		try {
			tmpFile = File.createTempFile("thaw-", "-message.xml");
			tmpFile.deleteOnExit();
		} catch(java.io.IOException e) {
			Logger.error(this, "Can't create temporary file because : "+e.toString());
			return null;
		}

		Document doc = XMLTools.createDomDocument();

		doc.appendChild(getXMLTree(doc));

		return (XMLTools.writeXmlFile(doc, tmpFile.getPath()) ? tmpFile : null);
	}

	protected boolean mustBeDisplayedAsRead() {
		return read;
	}
}
