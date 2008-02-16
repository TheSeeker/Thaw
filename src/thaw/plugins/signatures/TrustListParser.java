package thaw.plugins.signatures;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Vector;
import java.util.Iterator;

import thaw.core.Logger;

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

/* SAX */

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import java.io.FileInputStream;


public class TrustListParser {
	private TrustListParser() {
		
	}
	
	/*********************** EXPORT  ******************************/
	
	public static boolean exportTrustList(Vector identities, File outputFile) {
		try {
			FileOutputStream out = new FileOutputStream(outputFile);
			
			StreamResult streamResult;

			streamResult = new StreamResult(out);

			Document xmlDoc;

			final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlBuilder;

			try {
				xmlBuilder = xmlFactory.newDocumentBuilder();
			} catch(final javax.xml.parsers.ParserConfigurationException e) {
				Logger.error(new TrustListParser(), "Unable to generate the index because : "+e.toString());
				return false;
			}

			final DOMImplementation impl = xmlBuilder.getDOMImplementation();

			xmlDoc = impl.createDocument(null, "trustList", null);

			final Element rootEl = xmlDoc.getDocumentElement();

			/**** DOM Tree generation ****/
			fillInRootElement(identities, rootEl, xmlDoc);


			/* Serialization */
			final DOMSource domSource = new DOMSource(xmlDoc);
			final TransformerFactory transformFactory = TransformerFactory.newInstance();

			Transformer serializer;

			try {
				serializer = transformFactory.newTransformer();
			} catch(final javax.xml.transform.TransformerConfigurationException e) {
				Logger.error(new TrustListParser(), "Unable to save index because: "+e.toString());
				return false;
			}

			serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			serializer.setOutputProperty(OutputKeys.INDENT,"yes");

			/* final step */
			try {
				serializer.transform(domSource, streamResult);
			} catch(final javax.xml.transform.TransformerException e) {
				Logger.error(new TrustListParser(), "Unable to save index because: "+e.toString());
				return false;
			}
			
			out.close();
			
			return true;
		} catch(java.io.FileNotFoundException e) {
			Logger.error(new TrustListParser(), "File not found exception ?!");
		} catch(java.io.IOException e) {
			Logger.error(new TrustListParser(), "IOException while generating the index: "+e.toString());
		}
		
		return false;
	}
	
	/**
	 * Use it only if you know what you're doing
	 * @param identities
	 * @param rootEl
	 * @param xmlDoc
	 * @return
	 */
	public static boolean fillInRootElement(Vector identities, Element rootEl, Document xmlDoc) {
		//rootEl.appendChild(getXMLHeader(xmlDoc));
		
		for (Iterator it = identities.iterator();
			it.hasNext();) {
			Identity id = (Identity)it.next();
			
			if (id.getTrustLevel() != 0 /* no just 'SIGNED' */
					&& id.getTrustLevel() != Identity.trustLevelInt[0]) /* and no dev */
				rootEl.appendChild(getXMLIdentity(id, xmlDoc));
		}
		
		return true;
	}
	
	private static Element getXMLIdentity(Identity id, Document xmlDoc) {
		Element idEl = xmlDoc.createElement("identity");
		
		Element nickEl = xmlDoc.createElement("nick");
		nickEl.appendChild(xmlDoc.createTextNode(id.getNick()));
		idEl.appendChild(nickEl);
		
		Element publicKeyEl = xmlDoc.createElement("publicKey");
		publicKeyEl.appendChild(xmlDoc.createTextNode(id.getPublicKey()));
		idEl.appendChild(publicKeyEl);
		
		Element trustLevelEl = xmlDoc.createElement("trustLevel");
		trustLevelEl.appendChild(xmlDoc.createTextNode(Integer.toString(id.getTrustLevel()*10)));
		idEl.appendChild(trustLevelEl);
		
		return idEl;
	}
	
	
	
	/*********************** IMPORT ****************************************/
	
	public static interface TrustListContainer {
		public void start();
		
		/**
		 * Identity is used here just as a container.
		 * no ref to the db was provided to these identity
		 * @param i
		 */
		public void updateIdentity(Identity i);
		
		public void end();
	}


	/**
	 * public so you can override it if you want
	 * @author jflesch
	 */
	public static class TrustListHandler extends DefaultHandler {
		private TrustListContainer container;
		
		public TrustListHandler(TrustListContainer container) {
			setTrustListContainer(container);
		}
		
		protected void setTrustListContainer(TrustListContainer container) {
			this.container = container;
		}
		
		public void startDocument() throws SAXException {
			if (container != null)
				container.start();
		}
		
		private boolean nickTag = false;
		private boolean publicKeyTag = false;
		private boolean trustLevelTag = false;
		
		private String nick = null;
		private String publicKey = null;
		private String trustLevel = null;
		
		public void startElement(String nameSpaceURI, String localName,
								 String rawName, Attributes attrs) throws SAXException {
			if (rawName == null) {
				rawName = localName;
			}

			if (rawName == null)
				return;

			if ("identity".equals(rawName)) {
				nickTag = false;
				publicKeyTag = false;
				trustLevelTag = false;
				nick = null;
				publicKey = null;
				trustLevel = null;
			} else if ("nick".equals(rawName)) {
				nickTag = true;
			} else if ("publicKey".equals(rawName)) {
				publicKeyTag = true;
			} else if ("trustLevel".equals(rawName)) {
				trustLevelTag = true;
			}
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

			if ("identity".equals(rawName)) {
				if (nick != null && publicKey != null && trustLevel != null && container != null) {
					Identity i = new Identity(null, -1, nick, publicKey, null, false, Integer.parseInt(trustLevel)/10);
					container.updateIdentity(i);
				}
				publicKey = null;
			} else if ("nick".equals(rawName)) {
				nickTag = false;
			} else if ("publicKey".equals(rawName)) {
				publicKeyTag = false;
			} else if ("trustLevel".equals(rawName)) {
				trustLevelTag = false;
			}
		}
		
		public void characters(char[] ch, int start, int end) throws SAXException {
			String txt = new String(ch, start, end);
			
			if (nickTag)
				nick = txt;
			else if (publicKeyTag)
				publicKey = txt;
			else if (trustLevelTag)
				trustLevel = txt;
		}
		
		public void endDocument() throws SAXException {
			if (container != null)
				container.end();
		}
	}

	public static void importTrustList(TrustListContainer container, File inputFile) {
		importTrustList(new TrustListHandler(container), inputFile);
	}
	
	public static void importTrustList(TrustListHandler handler, File inputFile) {
		try {
			FileInputStream stream = new FileInputStream(inputFile);
			
			// Use the default (non-validating) parser
			SAXParserFactory factory = SAXParserFactory.newInstance();

			// Parse the input
			SAXParser saxParser = factory.newSAXParser();

			Logger.notice(handler, "Parsing index ...");
			saxParser.parse(stream, handler);
			Logger.notice(handler, "Parsing done");

			stream.close();
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(new TrustListParser(), "Unable to load XML: FileNotFoundException ('"+inputFile.getPath()+"') ! : "+e.toString());
		} catch(java.io.IOException e) {
			Logger.error(new TrustListParser(), "IOException while parsing the index: "+e.toString());
		} catch(javax.xml.parsers.ParserConfigurationException e) {
			Logger.notice(new TrustListParser(), "Error (1) while parsing index: "+e.toString());
		} catch(org.xml.sax.SAXException e) {
			Logger.notice(new TrustListParser(), "Error (2) while parsing index: "+e.toString());
		} catch(Exception e) {
			Logger.notice(new TrustListParser(), "Error (4) while parsing index: "+e.toString());
		}
	}

}
