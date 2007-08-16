package thaw.plugins.miniFrost.frostKSK;

import java.sql.*;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.Vector;

import frost.util.XMLTools;

import thaw.plugins.Hsqldb;
import thaw.plugins.miniFrost.interfaces.Attachment;
import thaw.core.Logger;


public class KSKAttachmentFactory {

	/**
	 * one per message (not really used atm, but we never know)
	 */
	public KSKAttachmentFactory() {

	}


	public static Vector getAttachments(KSKMessage msg,
					    KSKBoardFactory boardFactory,
					    Hsqldb db) {
		Vector v = new Vector();

		Vector sub;

		if ((sub = KSKFileAttachment.select( msg, boardFactory,  db)) != null)
			v.addAll(sub);
		if ((sub = KSKBoardAttachment.select(msg, boardFactory,  db)) != null)
			v.addAll(sub);

		return (v.size() > 0 ? v : null);
	}


	public KSKAttachment getAttachment(Element attachmentEl) {
		if (attachmentEl.getAttribute("type").length() <= 0) {
			Logger.notice(this, "No type specified in the attachment ("+
				       attachmentEl.toString()+")");
			return null;
		}

		KSKAttachment a = null;

		if (attachmentEl.getAttribute("type").equals("file"))
			a = new KSKFileAttachment();
		else if (attachmentEl.getAttribute("type").equals("board"))
			a = new KSKBoardAttachment();

		if (a == null) {
			Logger.notice(this, "Unknown attachment type : "
				       +attachmentEl.getAttribute("type"));
		}
		else
			loadValues(a, attachmentEl);

		return a;
	}



	public void loadValues(KSKAttachment a, Element rootEl) {
		if (a.getContainer() != null)
			rootEl = (Element)XMLTools.getChildElementsByTagName(rootEl, a.getContainer()).iterator().next();

		if (rootEl == null) {
			Logger.warning(this, "no container ?!");
			return;
		}

		String[] properties = a.getProperties();

		for (int i = 0 ; i < properties.length ; i++) {
			String val;

			try {
				val = XMLTools.getChildElementsCDATAValue(rootEl, properties[i]);
			} catch(java.lang.ClassCastException e) {
				/* Dirty */
				val = XMLTools.getChildElementsTextValue(rootEl, properties[i]);
			}

			if (val != null)
				a.setValue(properties[i], val);
		}
	}
}
