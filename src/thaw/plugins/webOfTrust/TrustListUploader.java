package thaw.plugins.webOfTrust;

import thaw.plugins.Hsqldb;
import thaw.plugins.Signatures;
import thaw.plugins.WebOfTrust;
import thaw.plugins.signatures.Identity;
import thaw.plugins.signatures.TrustListParser;


import thaw.core.Config;
import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPGenerateSSK;
import thaw.fcp.FreenetURIHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Observer;
import java.util.Observable;
import java.util.Vector;

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

public class TrustListUploader implements Signatures.SignaturesObserver, Observer {
	private final FCPQueueManager queueManager;
	private final Config config;
	private final Hsqldb db;
	
	private Date lastUpload = null;
	private Date lastTrustChange = null;
	
	private Identity identity;
	
	private String privateKey;
	private String publicKey;
	
	private FCPClientPut upload;

	public TrustListUploader(Hsqldb db, FCPQueueManager queueManager, Config config) {
		this.queueManager = queueManager;
		this.config = config;
		this.db = db;
		
		lastUpload = null;
		lastTrustChange = null;
		
		identity = null;
		
		try {
			if (config.getValue("wotIdentityUsed") != null)
				identity = Identity.getIdentity(db, Integer.parseInt(config.getValue("wotIdentityUsed")));
		} catch(Exception e) {
			Logger.error(this, "Error in the config : can't find the identity to use to upload the trust list (or its keys) => won't insert the trust list ; Exception throwed: "+e.toString());
		}
		
		publicKey = null;
		privateKey = null;
	}
	
	public Identity getIdentityUsed() {
		return identity;
	}
	
	public void init() {
		lastUpload = null;
		
		if (config.getValue("wotLastUpload") != null)
			lastUpload = new Date(Long.parseLong(config.getValue("wotLastUpload")));
		
		lastTrustChange = null;
		
		if (config.getValue("wotLastTrustChange") != null)
			lastTrustChange = new Date(Long.parseLong(config.getValue("wotLastTrustChange")));
		
		if (config.getValue("wotPublicKey") != null)
			publicKey = config.getValue("wotPublicKey");
		if (config.getValue("wotPrivateKey") != null)
			privateKey = config.getValue("wotPrivateKey");
		
		if (publicKey == null || privateKey == null) {
			regenerateKeys();
		}
		
		Signatures.addObserver(this);
	}
	
	public void regenerateKeys() {
		publicKey = null;
		privateKey = null;
		lastUpload = null;
		
		Logger.notice(this, "Regenerating a key pair for the wot of trust");

		FCPGenerateSSK sskGenerator = new FCPGenerateSSK();
		sskGenerator.addObserver(this);
		sskGenerator.start(queueManager);
	}
	
	/**
	 * @return true if a trust change was done more than some times ago and no
	 *  trust list upload has been done between this change and now
	 */
	private boolean mustUpload() {
		/* never uploaded => we must do it for the first time */
		if (lastUpload == null)
			return true;
		
		/* no change => no upload */
		if (lastTrustChange == null)
			return false;
		
		/* if the last upload is older than the last trust change */
		if (lastUpload.compareTo(lastTrustChange) < 0) {
		
			/* last change was done more than UPLOAD_AFTER_MS ms (for example 30min) */
			if (new Date().getTime() - lastTrustChange.getTime() >= WebOfTrust.UPLOAD_AFTER_MS)
				return true;
			
		}
		
		return false;
	}
	
	/**
	 * @return true if the list was already uploaded once or if we have more
	 *   than 1 identity with a trust != 0 and != dev trust
	 */
	private boolean hasSomethingToUpload() {
		/* if no identity selected, can't insert */
		if (identity == null)
			return false;
		
		/* if a trust was changed at a given time */
		/* then we must upload, even if it's to say we trust nobody anymore */
		if (lastUpload != null || lastTrustChange != null)
			return true;
		
		return Identity.hasAtLeastATrustDefined(db);
	}
	
	private boolean addHeaders(Element rootEl, Document xmlDoc) {
		Element trustListOwnerEl = xmlDoc.createElement("trustListOwner");

		Element nickEl = xmlDoc.createElement("nick");
		nickEl.appendChild(xmlDoc.createTextNode(identity.getNick()));
		
		Element publicKeyEl = xmlDoc.createElement("publicKey");
		publicKeyEl.appendChild(xmlDoc.createTextNode(identity.getPublicKey()));
		
		trustListOwnerEl.appendChild(nickEl);
		trustListOwnerEl.appendChild(publicKeyEl);
		
		rootEl.appendChild(trustListOwnerEl);
		
		return true;
	}
	
	private boolean exportTrustList(Vector identities, File file) {
		try {
			FileOutputStream out = new FileOutputStream(file);
			
			StreamResult streamResult;

			streamResult = new StreamResult(out);

			Document xmlDoc;

			final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlBuilder;

			try {
				xmlBuilder = xmlFactory.newDocumentBuilder();
			} catch(final javax.xml.parsers.ParserConfigurationException e) {
				Logger.error(this, "Unable to generate the index because : "+e.toString());
				return false;
			}

			final DOMImplementation impl = xmlBuilder.getDOMImplementation();

			xmlDoc = impl.createDocument(null, "trustList", null);

			final Element rootEl = xmlDoc.getDocumentElement();

			/**** DOM Tree generation ****/
			addHeaders(rootEl, xmlDoc);
			TrustListParser.fillInRootElement(identities, rootEl, xmlDoc);


			/* Serialization */
			final DOMSource domSource = new DOMSource(xmlDoc);
			final TransformerFactory transformFactory = TransformerFactory.newInstance();

			Transformer serializer;

			try {
				serializer = transformFactory.newTransformer();
			} catch(final javax.xml.transform.TransformerConfigurationException e) {
				Logger.error(this, "Unable to save index because: "+e.toString());
				return false;
			}

			serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			serializer.setOutputProperty(OutputKeys.INDENT,"yes");

			/* final step */
			try {
				serializer.transform(domSource, streamResult);
			} catch(final javax.xml.transform.TransformerException e) {
				Logger.error(this, "Unable to save index because: "+e.toString());
				return false;
			}
			
			out.close();
			
			return true;
		} catch(java.io.FileNotFoundException e) {
			Logger.error(this, "File not found exception ?!");
		} catch(java.io.IOException e) {
			Logger.error(this, "IOException while generating the index: "+e.toString());
		}
		
		return false;
	}
	
	
	/**
	 * called ~ each second
	 */
	public synchronized void process() {
		/* an upload is already running => can't upload */
		if (upload != null)
			return;		
		
		/* no private key => can't upload anyway */
		if (privateKey == null || identity == null)
			return;

		
		if (mustUpload() && hasSomethingToUpload()) {
			
			Logger.notice(this, "Uploading your trust list ...");
			
			try {
				File file = File.createTempFile("thaw-", "-trustList.xml");
				Vector ids = Identity.getOtherIdentities(db);

				if (!exportTrustList(ids, file))
					return;
				
				upload = new FCPClientPut(file, FCPClientPut.KEY_TYPE_SSK, 0,
											"trustList", FreenetURIHelper.convertSSKtoUSK(privateKey)+"/", /* the convertion fonction forget the '/' */
											2, /* priority */
										    false, /* global */
										    FCPClientPut.PERSISTENCE_FOREVER); /* persistence */
				upload.addObserver(this);

				queueManager.addQueryToTheRunningQueue(upload);

			} catch(java.io.IOException e) {
				Logger.error(this, "Can't upload your trust list because : "+e.toString());
				e.printStackTrace();
			}
		}
	}
	
	
	public void stop() {
		Signatures.deleteObserver(this);
		
		if (lastUpload == null)
			config.setValue("wotLastUpload", null);
		else
			config.setValue("wotLastUpload", Long.toString(lastUpload.getTime()));
		
		if (lastTrustChange == null)
			config.setValue("wotLastTrustChange", null);
		else
			config.setValue("wotLastTrustChange", Long.toString(lastTrustChange.getTime()));
	}

	public void identityUpdated(Identity i) {
		/* a dev was added => ignore */
		if (i.getTrustLevel() == Identity.trustLevelInt[0])
			return;
		
		lastTrustChange = new Date();
	}

	public void privateIdentityAdded(Identity i) {
		/* a dev was added => ignore */
		if (i.getTrustLevel() == Identity.trustLevelInt[0])
			return;
		
		/* by default, new private identity have no trust, so we don't care */
		identityUpdated(i);
	}

	public void publicIdentityAdded(Identity i) {
		/* by default, new public identity have no trust, so we don't care */
	}

	public void update(Observable o, Object param) {
		if (o instanceof FCPGenerateSSK) {
			Logger.notice(this, "Key pair generated");
			FCPGenerateSSK sskGenerator = (FCPGenerateSSK)o;
			publicKey = sskGenerator.getPublicKey();
			privateKey = sskGenerator.getPrivateKey();

			config.setValue("wotPrivateKey", privateKey);
			config.setValue("wotPublicKey", publicKey);

		} else if (o instanceof FCPClientPut) {

			if (upload.isFinished()) {
		
				upload.deleteObserver(this);
				
				queueManager.remove(upload);
				
				if (upload.isSuccessful()) {
					Logger.notice(this, "Trust list inserted");
					lastUpload = new Date();
				} else
					Logger.warning(this, "Unable to insert trust list !");
				
				if (upload.getPath() != null)
					new File(upload.getPath()).delete();
				
				upload = null;
			}
		}
	}
}
