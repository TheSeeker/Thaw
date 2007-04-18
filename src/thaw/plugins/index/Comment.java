package thaw.plugins.index;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.util.Observer;
import java.util.Observable;


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


/* Thaw */

import thaw.core.Config;
import thaw.core.Logger;

import thaw.fcp.FreenetURIHelper;
import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPQueueManager;


/**
 * Will use, from the configuration:
 *  'userNickname' as the author name
 */
public class Comment implements Observer {
	public final static int MAX_SIZE = 16384;

	private String author;
	private String comment;
	private Index index;
	private int rev;

	private Comment() {

	}


	/**
	 * @param index parent index
	 * @param rev revision of the comment (-1) if not inserted at the moment
	 * @param comment comment inside the comment ... :)
	 */
	public Comment(Index index, int rev, String author, String comment) {
		this.author = author;
		this.comment = comment;
		this.index = index;
		this.rev = rev;
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

		boolean res = put.start(queueManager);

		if (res)
			queueManager.addQueryToTheRunningQueue(put, false);

		return res;
	}


	public boolean parseComment(java.io.File xmlFile) {

		return false;
	}



	public boolean fetchComment(FCPQueueManager queueManager) {
		this.queueManager = queueManager;

		String publicKey = index.getCommentPublicKey(); /* should be an SSK */

		publicKey += "comment-"+Integer.toString(rev)+"/comment.xml";

		FCPClientGet get = new FCPClientGet(publicKey, 2 /* priority */, 2 /* persistence */,
						    false /* global queue */, 3 /* max retries */,
						    System.getProperty("java.io.tmpdir"),
						    MAX_SIZE, true /* no DDA */);

		get.addObserver(this);

		return get.start(queueManager);
	}



	public void update(Observable o, Object param) {
		if (o instanceof FCPTransferQuery) {

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

		}

		if (o instanceof FCPTransferQuery) {
			FCPTransferQuery q = (FCPTransferQuery)o;

			if (q.isFinished() && q.isSuccessful() && q instanceof Observable)
				((Observable)q).deleteObserver(this);
		}
	}


}
