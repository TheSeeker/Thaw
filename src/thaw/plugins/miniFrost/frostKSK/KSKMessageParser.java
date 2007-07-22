package thaw.plugins.miniFrost.frostKSK;

import java.sql.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.text.SimpleDateFormat;
import java.io.File;

import frost.util.XMLTools;

import thaw.plugins.Hsqldb;
import thaw.core.Logger;


/**
 * Dirty parser reusing some Frost functions
 * (Note: dirty because of the XML)
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
	private String pubKey;
	private String recipient;
	private String board;
	private String body;


	public boolean insert(Hsqldb db,
			      int boardId, int rev,
			      String boardNameExpected) {
		if (boardNameExpected == null) {
			Logger.error(this, "Board name expected == null ?!");
			return false;
		}

		if (board == null
		    || !(boardNameExpected.toLowerCase().equals(board.toLowerCase())))
			return false;


		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.M.d HH:mm:ss");

		time = time.replaceAll("GMT", "").trim();
		date = date.trim();

		date += " "+time;

		java.util.Date dateUtil = dateFormat.parse(date, new java.text.ParsePosition(0));

		if (dateUtil == null) {
			if (date == null)
				date = "(null)";
			Logger.warning(this, "Unable to parse the date ?! : "+date);
		}

		java.sql.Timestamp dateSql = new java.sql.Timestamp(dateUtil.getTime());


		int replyToId = -1;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

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


				st = db.getConnection().prepareStatement("INSERT INTO frostKSKMessages ("+
									 "subject, nick, sigId, content, "+
									 "date, msgId, inReplyTo, inReplyToId, "+
									 "rev, read, archived, boardId) VALUES ("+
									 "?, ?, NULL, ?, "+
									 "?, ?, ?, ?, "+
									 "?, FALSE, FALSE, ?)");
				st.setString(1, subject);
				st.setString(2, from); /* nick */
				st.setString(3, body); /* content */
				st.setTimestamp(4, dateSql);
				st.setString(5, messageId);

				if (replyToId >= 0)
					st.setInt(6, replyToId);
				else
					st.setNull(6, Types.INTEGER);

				if (inReplyTo != null)
					st.setString(7, inReplyTo);
				else
					st.setNull(7, Types.VARCHAR);

				st.setInt(8, rev);
				st.setInt(9, boardId);

				st.execute();

			}
		} catch(SQLException e) {
			Logger.error(this, "Can't insert the message into the db because : "+e.toString());
			return false;
		}

		return true;
	}


	protected boolean loadXMLElements(Element root) {
		messageId     = XMLTools.getChildElementsCDATAValue(root, "MessageId");
		inReplyTo     = XMLTools.getChildElementsCDATAValue(root, "InReplyTo");
		from          = XMLTools.getChildElementsCDATAValue(root, "From");
		subject       = XMLTools.getChildElementsCDATAValue(root, "Subject");
		date          = XMLTools.getChildElementsCDATAValue(root, "Date");
		time          = XMLTools.getChildElementsCDATAValue(root, "Time");
		pubKey        = XMLTools.getChildElementsCDATAValue(root, "pubKey");
		recipient     = XMLTools.getChildElementsCDATAValue(root, "recipient");
		board         = XMLTools.getChildElementsCDATAValue(root, "Board");
		body          = XMLTools.getChildElementsCDATAValue(root, "Body");

		return true;
	}

	/**
	 * This function has been imported from FROST.
	 * Parses the XML file and passes the FrostMessage element to XMLize load method.
	 */
	public boolean loadFile(File file) {
		try {
			Document doc = null;
			try {
				doc = XMLTools.parseXmlFile(file, false);
			} catch(Exception ex) {  // xml format error
				Logger.warning(this, "Invalid Xml");
				return false;
			}

			if( doc == null ) {
				Logger.warning(this,
					       "Error: couldn't parse XML Document - " +
					       "File name: '" + file.getName() + "'");
				return false;
			}

			Element rootNode = doc.getDocumentElement();

			// load the message itself
			return loadXMLElements(rootNode);

		} catch(Exception e) {
			Logger.warning(this, "Unable to parse XML message because : "+e.toString());
			return false;
		}
	}



}