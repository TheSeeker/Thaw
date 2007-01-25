package thaw.plugins.insertPlugin;

import java.util.HashMap;
import java.util.Vector;

import thaw.core.Logger;

/**
 * Holds the default MIME types.
 */
public class DefaultMIMETypes {

	/** Default MIME type - what to set it to if we don't know any better */
	public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

	/** MIME types: number -> name */
	private static final Vector mimeTypesByNumber = new Vector();

	/** MIME types: name -> number */
	private static final HashMap mimeTypesByName = new HashMap();

	/** MIME types by extension. One extension maps to one MIME type, but not necessarily
	 * the other way around. */
	private static final HashMap mimeTypesByExtension = new HashMap();

	/** Primary extension by MIME type number. */
	private static final HashMap primaryExtensionByMimeNumber = new HashMap();

	/**
	 * Just to be able to use thaw.core.Logger.
	 */
	public DefaultMIMETypes() {

	}


	/**
	 * Add a MIME type, without any extensions.
	 * @param number The number of the MIME type for compression. This *must not change*
	 * for a given type, or the metadata format will be affected.
	 * @param type The actual MIME type string. Do not include ;charset= etc; these are
	 * parameters and there is a separate mechanism for them.
	 */
	protected static synchronized void addMIMEType(final short number, final String type) {
		if(DefaultMIMETypes.mimeTypesByNumber.size() > number) {
			final String s = (String) DefaultMIMETypes.mimeTypesByNumber.get(number);
			if(s != null) throw new IllegalArgumentException("Already used: "+number);
		} else {
			DefaultMIMETypes.mimeTypesByNumber.add(number, null);
		}
		DefaultMIMETypes.mimeTypesByNumber.set(number, type);
		DefaultMIMETypes.mimeTypesByName.put(type, new Short(number));
	}

	/**
	 * Add a MIME type.
	 * @param number The number of the MIME type for compression. This *must not change*
	 * for a given type, or the metadata format will be affected.
	 * @param type The actual MIME type string. Do not include ;charset= etc; these are
	 * parameters and there is a separate mechanism for them.
	 * @param extensions An array of common extensions for files of this type. Must be
	 * unique for the type.
	 */
	protected static synchronized void addMIMEType(final short number, final String type, final String[] extensions, final String outExtension) {
		DefaultMIMETypes.addMIMEType(number, type);
		final Short t = new Short(number);
		if(extensions != null) {
			for(int i=0;i<extensions.length;i++) {
				final String ext = extensions[i].toLowerCase();
				if(DefaultMIMETypes.mimeTypesByExtension.containsKey(ext)) {
					// No big deal
					final Short s = (Short) DefaultMIMETypes.mimeTypesByExtension.get(ext);
					Logger.notice(new DefaultMIMETypes(), "Extension "+ext+" assigned to "+DefaultMIMETypes.byNumber(s.shortValue())+" in preference to "+number+":"+type);
				} else {
					// If only one, make it primary
					if((outExtension == null) && (extensions.length == 1))
						DefaultMIMETypes.primaryExtensionByMimeNumber.put(t, ext);
					DefaultMIMETypes.mimeTypesByExtension.put(ext, t);
				}
			}
		}
		if(outExtension != null)
			DefaultMIMETypes.primaryExtensionByMimeNumber.put(t, outExtension);

	}

	/**
	 * Add a MIME type, with extensions separated by spaces. This is more or less
	 * the format in /etc/mime-types.
	 */
	protected static synchronized void addMIMEType(final short number, final String type, final String extensions) {
		DefaultMIMETypes.addMIMEType(number, type, extensions.split(" "), null);
	}

	/**
	 * Add a MIME type, with extensions separated by spaces. This is more or less
	 * the format in /etc/mime-types.
	 */
	protected static synchronized void addMIMEType(final short number, final String type, final String extensions, final String outExtension) {
		DefaultMIMETypes.addMIMEType(number, type, extensions.split(" "), outExtension);
	}

	/**
	 * Get a known MIME type by number.
	 */
	public synchronized static String byNumber(final short x) {
		if((x > DefaultMIMETypes.mimeTypesByNumber.size()) || (x < 0))
			return null;
		return (String) DefaultMIMETypes.mimeTypesByNumber.get(x);
	}

	/**
	 * Get the number of a MIME type, or -1 if it is not in the table of known MIME
	 * types, in which case it will have to be sent uncompressed.
	 */
	public synchronized static short byName(final String s) {
		final Short x = (Short) DefaultMIMETypes.mimeTypesByName.get(s);
		if(x != null) return x.shortValue();
		else return -1;
	}

	/* From toad's /etc/mime.types
	 * cat /etc/mime.types | sed "/^$/d;/#/d" | tr --squeeze '\t' ' ' |
	 * (y=0; while read x; do echo "$x" |
	 * sed -n "s/^\([^ ]*\)$/addMIMEType\($y, \"\1\"\);/p;s/^\([^ (),]\+\) \(.*\)$/addMIMEType\($y, \"\1\", \"\2\"\);/p;"; y=$((y+1)); done)
	 */

	// FIXME should we support aliases?

	static {
		DefaultMIMETypes.addMIMEType((short)0, "application/activemessage");
		DefaultMIMETypes.addMIMEType((short)1, "application/andrew-inset", "ez");
		DefaultMIMETypes.addMIMEType((short)2, "application/applefile");
		DefaultMIMETypes.addMIMEType((short)3, "application/atomicmail");
		DefaultMIMETypes.addMIMEType((short)4, "application/batch-SMTP");
		DefaultMIMETypes.addMIMEType((short)5, "application/beep+xml");
		DefaultMIMETypes.addMIMEType((short)6, "application/cals-1840");
		DefaultMIMETypes.addMIMEType((short)7, "application/commonground");
		DefaultMIMETypes.addMIMEType((short)8, "application/cu-seeme", "csm cu");
		DefaultMIMETypes.addMIMEType((short)9, "application/cybercash");
		DefaultMIMETypes.addMIMEType((short)10, "application/dca-rft");
		DefaultMIMETypes.addMIMEType((short)11, "application/dec-dx");
		DefaultMIMETypes.addMIMEType((short)12, "application/docbook+xml");
		DefaultMIMETypes.addMIMEType((short)13, "application/dsptype", "tsp");
		DefaultMIMETypes.addMIMEType((short)14, "application/dvcs");
		DefaultMIMETypes.addMIMEType((short)15, "application/edi-consent");
		DefaultMIMETypes.addMIMEType((short)16, "application/edifact");
		DefaultMIMETypes.addMIMEType((short)17, "application/edi-x12");
		DefaultMIMETypes.addMIMEType((short)18, "application/eshop");
		DefaultMIMETypes.addMIMEType((short)19, "application/font-tdpfr");
		DefaultMIMETypes.addMIMEType((short)20, "application/futuresplash", "spl");
		DefaultMIMETypes.addMIMEType((short)21, "application/ghostview");
		DefaultMIMETypes.addMIMEType((short)22, "application/hta", "hta");
		DefaultMIMETypes.addMIMEType((short)23, "application/http");
		DefaultMIMETypes.addMIMEType((short)24, "application/hyperstudio");
		DefaultMIMETypes.addMIMEType((short)25, "application/iges");
		DefaultMIMETypes.addMIMEType((short)26, "application/index");
		DefaultMIMETypes.addMIMEType((short)27, "application/index.cmd");
		DefaultMIMETypes.addMIMEType((short)28, "application/index.obj");
		DefaultMIMETypes.addMIMEType((short)29, "application/index.response");
		DefaultMIMETypes.addMIMEType((short)30, "application/index.vnd");
		DefaultMIMETypes.addMIMEType((short)31, "application/iotp");
		DefaultMIMETypes.addMIMEType((short)32, "application/ipp");
		DefaultMIMETypes.addMIMEType((short)33, "application/isup");
		DefaultMIMETypes.addMIMEType((short)34, "application/mac-compactpro", "cpt");
		DefaultMIMETypes.addMIMEType((short)35, "application/marc");
		DefaultMIMETypes.addMIMEType((short)36, "application/mac-binhex40", "hqx");
		DefaultMIMETypes.addMIMEType((short)37, "application/macwriteii");
		DefaultMIMETypes.addMIMEType((short)38, "application/mathematica", "nb");
		DefaultMIMETypes.addMIMEType((short)39, "application/mathematica-old");
		DefaultMIMETypes.addMIMEType((short)40, "application/msaccess", "mdb");
		DefaultMIMETypes.addMIMEType((short)41, "application/msword", "doc dot", "doc");
		DefaultMIMETypes.addMIMEType((short)42, "application/news-message-id");
		DefaultMIMETypes.addMIMEType((short)43, "application/news-transmission");
		DefaultMIMETypes.addMIMEType((short)44, "application/octet-stream", "bin");
		DefaultMIMETypes.addMIMEType((short)45, "application/ocsp-request");
		DefaultMIMETypes.addMIMEType((short)46, "application/ocsp-response");
		DefaultMIMETypes.addMIMEType((short)47, "application/oda", "oda");
		DefaultMIMETypes.addMIMEType((short)48, "application/ogg", "ogg");
		DefaultMIMETypes.addMIMEType((short)49, "application/parityfec");
		DefaultMIMETypes.addMIMEType((short)50, "application/pics-rules", "prf");
		DefaultMIMETypes.addMIMEType((short)51, "application/pgp-encrypted");
		DefaultMIMETypes.addMIMEType((short)52, "application/pgp-keys", "key");
		DefaultMIMETypes.addMIMEType((short)53, "application/pdf", "pdf");
		DefaultMIMETypes.addMIMEType((short)54, "application/pgp-signature", "pgp");
		DefaultMIMETypes.addMIMEType((short)55, "application/pkcs10");
		DefaultMIMETypes.addMIMEType((short)56, "application/pkcs7-mime");
		DefaultMIMETypes.addMIMEType((short)57, "application/pkcs7-signature");
		DefaultMIMETypes.addMIMEType((short)58, "application/pkix-cert");
		DefaultMIMETypes.addMIMEType((short)59, "application/pkixcmp");
		DefaultMIMETypes.addMIMEType((short)60, "application/pkix-crl");
		DefaultMIMETypes.addMIMEType((short)61, "application/postscript", "ps ai eps", "ps");
		DefaultMIMETypes.addMIMEType((short)62, "application/prs.alvestrand.titrax-sheet");
		DefaultMIMETypes.addMIMEType((short)63, "application/prs.cww");
		DefaultMIMETypes.addMIMEType((short)64, "application/prs.nprend");
		DefaultMIMETypes.addMIMEType((short)65, "application/qsig");
		DefaultMIMETypes.addMIMEType((short)66, "application/rar", "rar");
		DefaultMIMETypes.addMIMEType((short)67, "application/rdf+xml", "rdf");
		DefaultMIMETypes.addMIMEType((short)68, "application/remote-printing");
		DefaultMIMETypes.addMIMEType((short)69, "application/riscos");
		DefaultMIMETypes.addMIMEType((short)70, "application/rss+xml", "rss");
		DefaultMIMETypes.addMIMEType((short)71, "application/rtf", "rtf");
		DefaultMIMETypes.addMIMEType((short)72, "application/sdp");
		DefaultMIMETypes.addMIMEType((short)73, "application/set-payment");
		DefaultMIMETypes.addMIMEType((short)74, "application/set-payment-initiation");
		DefaultMIMETypes.addMIMEType((short)75, "application/set-registration");
		DefaultMIMETypes.addMIMEType((short)76, "application/set-registration-initiation");
		DefaultMIMETypes.addMIMEType((short)77, "application/sgml");
		DefaultMIMETypes.addMIMEType((short)78, "application/sgml-open-catalog");
		DefaultMIMETypes.addMIMEType((short)79, "application/sieve");
		DefaultMIMETypes.addMIMEType((short)80, "application/slate");
		DefaultMIMETypes.addMIMEType((short)81, "application/smil", "smi smil", "smil");
		DefaultMIMETypes.addMIMEType((short)82, "application/timestamp-query");
		DefaultMIMETypes.addMIMEType((short)83, "application/timestamp-reply");
		DefaultMIMETypes.addMIMEType((short)84, "application/vemmi");
		DefaultMIMETypes.addMIMEType((short)85, "application/whoispp-query");
		DefaultMIMETypes.addMIMEType((short)86, "application/whoispp-response");
		DefaultMIMETypes.addMIMEType((short)87, "application/wita");
		DefaultMIMETypes.addMIMEType((short)88, "application/wordperfect5.1", "wp5");
		DefaultMIMETypes.addMIMEType((short)89, "application/x400-bp");
		DefaultMIMETypes.addMIMEType((short)90, "application/xhtml+xml", "xht xhtml", "xhtml");
		DefaultMIMETypes.addMIMEType((short)91, "application/xml", "xml xsl", "xml");
		DefaultMIMETypes.addMIMEType((short)92, "application/xml-dtd");
		DefaultMIMETypes.addMIMEType((short)93, "application/xml-external-parsed-entity");
		DefaultMIMETypes.addMIMEType((short)94, "application/zip", "zip");
		DefaultMIMETypes.addMIMEType((short)95, "application/vnd.3M.Post-it-Notes");
		DefaultMIMETypes.addMIMEType((short)96, "application/vnd.accpac.simply.aso");
		DefaultMIMETypes.addMIMEType((short)97, "application/vnd.accpac.simply.imp");
		DefaultMIMETypes.addMIMEType((short)98, "application/vnd.acucobol");
		DefaultMIMETypes.addMIMEType((short)99, "application/vnd.aether.imp");
		DefaultMIMETypes.addMIMEType((short)100, "application/vnd.anser-web-certificate-issue-initiation");
		DefaultMIMETypes.addMIMEType((short)101, "application/vnd.anser-web-funds-transfer-initiation");
		DefaultMIMETypes.addMIMEType((short)102, "application/vnd.audiograph");
		DefaultMIMETypes.addMIMEType((short)103, "application/vnd.bmi");
		DefaultMIMETypes.addMIMEType((short)104, "application/vnd.businessobjects");
		DefaultMIMETypes.addMIMEType((short)105, "application/vnd.canon-cpdl");
		DefaultMIMETypes.addMIMEType((short)106, "application/vnd.canon-lips");
		DefaultMIMETypes.addMIMEType((short)107, "application/vnd.cinderella", "cdy");
		DefaultMIMETypes.addMIMEType((short)108, "application/vnd.claymore");
		DefaultMIMETypes.addMIMEType((short)109, "application/vnd.commerce-battelle");
		DefaultMIMETypes.addMIMEType((short)110, "application/vnd.commonspace");
		DefaultMIMETypes.addMIMEType((short)111, "application/vnd.comsocaller");
		DefaultMIMETypes.addMIMEType((short)112, "application/vnd.contact.cmsg");
		DefaultMIMETypes.addMIMEType((short)113, "application/vnd.cosmocaller");
		DefaultMIMETypes.addMIMEType((short)114, "application/vnd.ctc-posml");
		DefaultMIMETypes.addMIMEType((short)115, "application/vnd.cups-postscript");
		DefaultMIMETypes.addMIMEType((short)116, "application/vnd.cups-raster");
		DefaultMIMETypes.addMIMEType((short)117, "application/vnd.cups-raw");
		DefaultMIMETypes.addMIMEType((short)118, "application/vnd.cybank");
		DefaultMIMETypes.addMIMEType((short)119, "application/vnd.dna");
		DefaultMIMETypes.addMIMEType((short)120, "application/vnd.dpgraph");
		DefaultMIMETypes.addMIMEType((short)121, "application/vnd.dxr");
		DefaultMIMETypes.addMIMEType((short)122, "application/vnd.ecdis-update");
		DefaultMIMETypes.addMIMEType((short)123, "application/vnd.ecowin.chart");
		DefaultMIMETypes.addMIMEType((short)124, "application/vnd.ecowin.filerequest");
		DefaultMIMETypes.addMIMEType((short)125, "application/vnd.ecowin.fileupdate");
		DefaultMIMETypes.addMIMEType((short)126, "application/vnd.ecowin.series");
		DefaultMIMETypes.addMIMEType((short)127, "application/vnd.ecowin.seriesrequest");
		DefaultMIMETypes.addMIMEType((short)128, "application/vnd.ecowin.seriesupdate");
		DefaultMIMETypes.addMIMEType((short)129, "application/vnd.enliven");
		DefaultMIMETypes.addMIMEType((short)130, "application/vnd.epson.esf");
		DefaultMIMETypes.addMIMEType((short)131, "application/vnd.epson.msf");
		DefaultMIMETypes.addMIMEType((short)132, "application/vnd.epson.quickanime");
		DefaultMIMETypes.addMIMEType((short)133, "application/vnd.epson.salt");
		DefaultMIMETypes.addMIMEType((short)134, "application/vnd.epson.ssf");
		DefaultMIMETypes.addMIMEType((short)135, "application/vnd.ericsson.quickcall");
		DefaultMIMETypes.addMIMEType((short)136, "application/vnd.eudora.data");
		DefaultMIMETypes.addMIMEType((short)137, "application/vnd.fdf");
		DefaultMIMETypes.addMIMEType((short)138, "application/vnd.ffsns");
		DefaultMIMETypes.addMIMEType((short)139, "application/vnd.flographit");
		DefaultMIMETypes.addMIMEType((short)140, "application/vnd.framemaker");
		DefaultMIMETypes.addMIMEType((short)141, "application/vnd.fsc.weblaunch");
		DefaultMIMETypes.addMIMEType((short)142, "application/vnd.fujitsu.oasys");
		DefaultMIMETypes.addMIMEType((short)143, "application/vnd.fujitsu.oasys2");
		DefaultMIMETypes.addMIMEType((short)144, "application/vnd.fujitsu.oasys3");
		DefaultMIMETypes.addMIMEType((short)145, "application/vnd.fujitsu.oasysgp");
		DefaultMIMETypes.addMIMEType((short)146, "application/vnd.fujitsu.oasysprs");
		DefaultMIMETypes.addMIMEType((short)147, "application/vnd.fujixerox.ddd");
		DefaultMIMETypes.addMIMEType((short)148, "application/vnd.fujixerox.docuworks");
		DefaultMIMETypes.addMIMEType((short)149, "application/vnd.fujixerox.docuworks.binder");
		DefaultMIMETypes.addMIMEType((short)150, "application/vnd.fut-misnet");
		DefaultMIMETypes.addMIMEType((short)151, "application/vnd.grafeq");
		DefaultMIMETypes.addMIMEType((short)152, "application/vnd.groove-account");
		DefaultMIMETypes.addMIMEType((short)153, "application/vnd.groove-identity-message");
		DefaultMIMETypes.addMIMEType((short)154, "application/vnd.groove-injector");
		DefaultMIMETypes.addMIMEType((short)155, "application/vnd.groove-tool-message");
		DefaultMIMETypes.addMIMEType((short)156, "application/vnd.groove-tool-template");
		DefaultMIMETypes.addMIMEType((short)157, "application/vnd.groove-vcard");
		DefaultMIMETypes.addMIMEType((short)158, "application/vnd.hhe.lesson-player");
		DefaultMIMETypes.addMIMEType((short)159, "application/vnd.hp-HPGL");
		DefaultMIMETypes.addMIMEType((short)160, "application/vnd.hp-PCL");
		DefaultMIMETypes.addMIMEType((short)161, "application/vnd.hp-PCLXL");
		DefaultMIMETypes.addMIMEType((short)162, "application/vnd.hp-hpid");
		DefaultMIMETypes.addMIMEType((short)163, "application/vnd.hp-hps");
		DefaultMIMETypes.addMIMEType((short)164, "application/vnd.httphone");
		DefaultMIMETypes.addMIMEType((short)165, "application/vnd.hzn-3d-crossword");
		DefaultMIMETypes.addMIMEType((short)166, "application/vnd.ibm.MiniPay");
		DefaultMIMETypes.addMIMEType((short)167, "application/vnd.ibm.afplinedata");
		DefaultMIMETypes.addMIMEType((short)168, "application/vnd.ibm.modcap");
		DefaultMIMETypes.addMIMEType((short)169, "application/vnd.informix-visionary");
		DefaultMIMETypes.addMIMEType((short)170, "application/vnd.intercon.formnet");
		DefaultMIMETypes.addMIMEType((short)171, "application/vnd.intertrust.digibox");
		DefaultMIMETypes.addMIMEType((short)172, "application/vnd.intertrust.nncp");
		DefaultMIMETypes.addMIMEType((short)173, "application/vnd.intu.qbo");
		DefaultMIMETypes.addMIMEType((short)174, "application/vnd.intu.qfx");
		DefaultMIMETypes.addMIMEType((short)175, "application/vnd.irepository.package+xml");
		DefaultMIMETypes.addMIMEType((short)176, "application/vnd.is-xpr");
		DefaultMIMETypes.addMIMEType((short)177, "application/vnd.japannet-directory-service");
		DefaultMIMETypes.addMIMEType((short)178, "application/vnd.japannet-jpnstore-wakeup");
		DefaultMIMETypes.addMIMEType((short)179, "application/vnd.japannet-payment-wakeup");
		DefaultMIMETypes.addMIMEType((short)180, "application/vnd.japannet-registration");
		DefaultMIMETypes.addMIMEType((short)181, "application/vnd.japannet-registration-wakeup");
		DefaultMIMETypes.addMIMEType((short)182, "application/vnd.japannet-setstore-wakeup");
		DefaultMIMETypes.addMIMEType((short)183, "application/vnd.japannet-verification");
		DefaultMIMETypes.addMIMEType((short)184, "application/vnd.japannet-verification-wakeup");
		DefaultMIMETypes.addMIMEType((short)185, "application/vnd.koan");
		DefaultMIMETypes.addMIMEType((short)186, "application/vnd.lotus-1-2-3");
		DefaultMIMETypes.addMIMEType((short)187, "application/vnd.lotus-approach");
		DefaultMIMETypes.addMIMEType((short)188, "application/vnd.lotus-freelance");
		DefaultMIMETypes.addMIMEType((short)189, "application/vnd.lotus-notes");
		DefaultMIMETypes.addMIMEType((short)190, "application/vnd.lotus-organizer");
		DefaultMIMETypes.addMIMEType((short)191, "application/vnd.lotus-screencam");
		DefaultMIMETypes.addMIMEType((short)192, "application/vnd.lotus-wordpro");
		DefaultMIMETypes.addMIMEType((short)193, "application/vnd.mcd");
		DefaultMIMETypes.addMIMEType((short)194, "application/vnd.mediastation.cdkey");
		DefaultMIMETypes.addMIMEType((short)195, "application/vnd.meridian-slingshot");
		DefaultMIMETypes.addMIMEType((short)196, "application/vnd.mif", "mif");
		DefaultMIMETypes.addMIMEType((short)197, "application/vnd.minisoft-hp3000-save");
		DefaultMIMETypes.addMIMEType((short)198, "application/vnd.mitsubishi.misty-guard.trustweb");
		DefaultMIMETypes.addMIMEType((short)199, "application/vnd.mobius.daf");
		DefaultMIMETypes.addMIMEType((short)200, "application/vnd.mobius.dis");
		DefaultMIMETypes.addMIMEType((short)201, "application/vnd.mobius.msl");
		DefaultMIMETypes.addMIMEType((short)202, "application/vnd.mobius.plc");
		DefaultMIMETypes.addMIMEType((short)203, "application/vnd.mobius.txf");
		DefaultMIMETypes.addMIMEType((short)204, "application/vnd.motorola.flexsuite");
		DefaultMIMETypes.addMIMEType((short)205, "application/vnd.motorola.flexsuite.adsi");
		DefaultMIMETypes.addMIMEType((short)206, "application/vnd.motorola.flexsuite.fis");
		DefaultMIMETypes.addMIMEType((short)207, "application/vnd.motorola.flexsuite.gotap");
		DefaultMIMETypes.addMIMEType((short)208, "application/vnd.motorola.flexsuite.kmr");
		DefaultMIMETypes.addMIMEType((short)209, "application/vnd.motorola.flexsuite.ttc");
		DefaultMIMETypes.addMIMEType((short)210, "application/vnd.motorola.flexsuite.wem");
		DefaultMIMETypes.addMIMEType((short)211, "application/vnd.mozilla.xul+xml", "xul");
		DefaultMIMETypes.addMIMEType((short)212, "application/vnd.ms-artgalry");
		DefaultMIMETypes.addMIMEType((short)213, "application/vnd.ms-asf");
		DefaultMIMETypes.addMIMEType((short)214, "application/vnd.ms-excel", "xls xlb xlt", "xls");
		DefaultMIMETypes.addMIMEType((short)215, "application/vnd.ms-lrm");
		DefaultMIMETypes.addMIMEType((short)216, "application/vnd.ms-pki.seccat", "cat");
		DefaultMIMETypes.addMIMEType((short)217, "application/vnd.ms-pki.stl", "stl");
		DefaultMIMETypes.addMIMEType((short)218, "application/vnd.ms-powerpoint", "ppt pps", "pps");
		DefaultMIMETypes.addMIMEType((short)219, "application/vnd.ms-project");
		DefaultMIMETypes.addMIMEType((short)220, "application/vnd.ms-tnef");
		DefaultMIMETypes.addMIMEType((short)221, "application/vnd.ms-works");
		DefaultMIMETypes.addMIMEType((short)222, "application/vnd.mseq");
		DefaultMIMETypes.addMIMEType((short)223, "application/vnd.msign");
		DefaultMIMETypes.addMIMEType((short)224, "application/vnd.music-niff");
		DefaultMIMETypes.addMIMEType((short)225, "application/vnd.musician");
		DefaultMIMETypes.addMIMEType((short)226, "application/vnd.netfpx");
		DefaultMIMETypes.addMIMEType((short)227, "application/vnd.noblenet-directory");
		DefaultMIMETypes.addMIMEType((short)228, "application/vnd.noblenet-sealer");
		DefaultMIMETypes.addMIMEType((short)229, "application/vnd.noblenet-web");
		DefaultMIMETypes.addMIMEType((short)230, "application/vnd.novadigm.EDM");
		DefaultMIMETypes.addMIMEType((short)231, "application/vnd.novadigm.EDX");
		DefaultMIMETypes.addMIMEType((short)232, "application/vnd.novadigm.EXT");
		DefaultMIMETypes.addMIMEType((short)233, "application/vnd.osa.netdeploy");
		DefaultMIMETypes.addMIMEType((short)234, "application/vnd.palm");
		DefaultMIMETypes.addMIMEType((short)235, "application/vnd.pg.format");
		DefaultMIMETypes.addMIMEType((short)236, "application/vnd.pg.osasli");
		DefaultMIMETypes.addMIMEType((short)237, "application/vnd.powerbuilder6");
		DefaultMIMETypes.addMIMEType((short)238, "application/vnd.powerbuilder6-s");
		DefaultMIMETypes.addMIMEType((short)239, "application/vnd.powerbuilder7");
		DefaultMIMETypes.addMIMEType((short)240, "application/vnd.powerbuilder7-s");
		DefaultMIMETypes.addMIMEType((short)241, "application/vnd.powerbuilder75");
		DefaultMIMETypes.addMIMEType((short)242, "application/vnd.powerbuilder75-s");
		DefaultMIMETypes.addMIMEType((short)243, "application/vnd.previewsystems.box");
		DefaultMIMETypes.addMIMEType((short)244, "application/vnd.publishare-delta-tree");
		DefaultMIMETypes.addMIMEType((short)245, "application/vnd.pvi.ptid1");
		DefaultMIMETypes.addMIMEType((short)246, "application/vnd.pwg-xhtml-print+xml");
		DefaultMIMETypes.addMIMEType((short)247, "application/vnd.rapid");
		DefaultMIMETypes.addMIMEType((short)248, "application/vnd.s3sms");
		DefaultMIMETypes.addMIMEType((short)249, "application/vnd.seemail");
		DefaultMIMETypes.addMIMEType((short)250, "application/vnd.shana.informed.formdata");
		DefaultMIMETypes.addMIMEType((short)251, "application/vnd.shana.informed.formtemplate");
		DefaultMIMETypes.addMIMEType((short)252, "application/vnd.shana.informed.interchange");
		DefaultMIMETypes.addMIMEType((short)253, "application/vnd.shana.informed.package");
		DefaultMIMETypes.addMIMEType((short)254, "application/vnd.smaf", "mmf");
		DefaultMIMETypes.addMIMEType((short)255, "application/vnd.sss-cod");
		DefaultMIMETypes.addMIMEType((short)256, "application/vnd.sss-dtf");
		DefaultMIMETypes.addMIMEType((short)257, "application/vnd.sss-ntf");
		DefaultMIMETypes.addMIMEType((short)258, "application/vnd.stardivision.calc", "sdc");
		DefaultMIMETypes.addMIMEType((short)259, "application/vnd.stardivision.draw", "sda");
		DefaultMIMETypes.addMIMEType((short)260, "application/vnd.stardivision.impress", "sdd sdp");
		DefaultMIMETypes.addMIMEType((short)261, "application/vnd.stardivision.math", "smf");
		DefaultMIMETypes.addMIMEType((short)262, "application/vnd.stardivision.writer", "sdw vor");
		DefaultMIMETypes.addMIMEType((short)263, "application/vnd.stardivision.writer-global", "sgl");
		DefaultMIMETypes.addMIMEType((short)264, "application/vnd.street-stream");
		DefaultMIMETypes.addMIMEType((short)265, "application/vnd.sun.xml.calc", "sxc");
		DefaultMIMETypes.addMIMEType((short)266, "application/vnd.sun.xml.calc.template", "stc");
		DefaultMIMETypes.addMIMEType((short)267, "application/vnd.sun.xml.draw", "sxd");
		DefaultMIMETypes.addMIMEType((short)268, "application/vnd.sun.xml.draw.template", "std");
		DefaultMIMETypes.addMIMEType((short)269, "application/vnd.sun.xml.impress", "sxi");
		DefaultMIMETypes.addMIMEType((short)270, "application/vnd.sun.xml.impress.template", "sti");
		DefaultMIMETypes.addMIMEType((short)271, "application/vnd.sun.xml.math", "sxm");
		DefaultMIMETypes.addMIMEType((short)272, "application/vnd.sun.xml.writer", "sxw");
		DefaultMIMETypes.addMIMEType((short)273, "application/vnd.sun.xml.writer.global", "sxg");
		DefaultMIMETypes.addMIMEType((short)274, "application/vnd.sun.xml.writer.template", "stw");
		DefaultMIMETypes.addMIMEType((short)275, "application/vnd.svd");
		DefaultMIMETypes.addMIMEType((short)276, "application/vnd.swiftview-ics");
		DefaultMIMETypes.addMIMEType((short)277, "application/vnd.symbian.install", "sis");
		DefaultMIMETypes.addMIMEType((short)278, "application/vnd.triscape.mxs");
		DefaultMIMETypes.addMIMEType((short)279, "application/vnd.trueapp");
		DefaultMIMETypes.addMIMEType((short)280, "application/vnd.truedoc");
		DefaultMIMETypes.addMIMEType((short)281, "application/vnd.tve-trigger");
		DefaultMIMETypes.addMIMEType((short)282, "application/vnd.ufdl");
		DefaultMIMETypes.addMIMEType((short)283, "application/vnd.uplanet.alert");
		DefaultMIMETypes.addMIMEType((short)284, "application/vnd.uplanet.alert-wbxml");
		DefaultMIMETypes.addMIMEType((short)285, "application/vnd.uplanet.bearer-choice");
		DefaultMIMETypes.addMIMEType((short)286, "application/vnd.uplanet.bearer-choice-wbxml");
		DefaultMIMETypes.addMIMEType((short)287, "application/vnd.uplanet.cacheop");
		DefaultMIMETypes.addMIMEType((short)288, "application/vnd.uplanet.cacheop-wbxml");
		DefaultMIMETypes.addMIMEType((short)289, "application/vnd.uplanet.channel");
		DefaultMIMETypes.addMIMEType((short)290, "application/vnd.uplanet.channel-wbxml");
		DefaultMIMETypes.addMIMEType((short)291, "application/vnd.uplanet.list");
		DefaultMIMETypes.addMIMEType((short)292, "application/vnd.uplanet.list-wbxml");
		DefaultMIMETypes.addMIMEType((short)293, "application/vnd.uplanet.listcmd");
		DefaultMIMETypes.addMIMEType((short)294, "application/vnd.uplanet.listcmd-wbxml");
		DefaultMIMETypes.addMIMEType((short)295, "application/vnd.uplanet.signal");
		DefaultMIMETypes.addMIMEType((short)296, "application/vnd.vcx");
		DefaultMIMETypes.addMIMEType((short)297, "application/vnd.vectorworks");
		DefaultMIMETypes.addMIMEType((short)298, "application/vnd.vidsoft.vidconference");
		DefaultMIMETypes.addMIMEType((short)299, "application/vnd.visio", "vsd");
		DefaultMIMETypes.addMIMEType((short)300, "application/vnd.vividence.scriptfile");
		DefaultMIMETypes.addMIMEType((short)301, "application/vnd.wap.sic");
		DefaultMIMETypes.addMIMEType((short)302, "application/vnd.wap.slc");
		DefaultMIMETypes.addMIMEType((short)303, "application/vnd.wap.wbxml", "wbxml");
		DefaultMIMETypes.addMIMEType((short)304, "application/vnd.wap.wmlc", "wmlc");
		DefaultMIMETypes.addMIMEType((short)305, "application/vnd.wap.wmlscriptc", "wmlsc");
		DefaultMIMETypes.addMIMEType((short)306, "application/vnd.webturbo");
		DefaultMIMETypes.addMIMEType((short)307, "application/vnd.wrq-hp3000-labelled");
		DefaultMIMETypes.addMIMEType((short)308, "application/vnd.wt.stf");
		DefaultMIMETypes.addMIMEType((short)309, "application/vnd.xara");
		DefaultMIMETypes.addMIMEType((short)310, "application/vnd.xfdl");
		DefaultMIMETypes.addMIMEType((short)311, "application/vnd.yellowriver-custom-menu");
		DefaultMIMETypes.addMIMEType((short)312, "application/x-123", "wk");
		DefaultMIMETypes.addMIMEType((short)313, "application/x-apple-diskimage", "dmg");
		DefaultMIMETypes.addMIMEType((short)314, "application/x-bcpio", "bcpio");
		DefaultMIMETypes.addMIMEType((short)315, "application/x-bittorrent", "torrent");
		DefaultMIMETypes.addMIMEType((short)316, "application/x-cdf", "cdf");
		DefaultMIMETypes.addMIMEType((short)317, "application/x-cdlink", "vcd");
		DefaultMIMETypes.addMIMEType((short)318, "application/x-chess-pgn", "pgn");
		DefaultMIMETypes.addMIMEType((short)319, "application/x-chm", "chm");
		DefaultMIMETypes.addMIMEType((short)320, "application/x-core");
		DefaultMIMETypes.addMIMEType((short)321, "application/x-cpio", "cpio");
		DefaultMIMETypes.addMIMEType((short)322, "application/x-csh", "csh");
		DefaultMIMETypes.addMIMEType((short)323, "application/x-debian-package", "deb");
		DefaultMIMETypes.addMIMEType((short)324, "application/x-director", "dcr dir dxr");
		DefaultMIMETypes.addMIMEType((short)325, "application/x-doom", "wad");
		DefaultMIMETypes.addMIMEType((short)326, "application/x-dms", "dms");
		DefaultMIMETypes.addMIMEType((short)327, "application/x-dvi", "dvi");
		DefaultMIMETypes.addMIMEType((short)328, "application/x-executable");
		DefaultMIMETypes.addMIMEType((short)329, "application/x-flac", "flac");
		DefaultMIMETypes.addMIMEType((short)330, "application/x-font", "pfa pfb gsf pcf pcf.Z", "unknown-font-type");
		DefaultMIMETypes.addMIMEType((short)331, "application/x-futuresplash", "spl");
		DefaultMIMETypes.addMIMEType((short)332, "application/x-gnumeric", "gnumeric");
		DefaultMIMETypes.addMIMEType((short)333, "application/x-go-sgf", "sgf");
		DefaultMIMETypes.addMIMEType((short)334, "application/x-graphing-calculator", "gcf");
		DefaultMIMETypes.addMIMEType((short)335, "application/x-gtar", "gtar tgz taz", "tgz");
		DefaultMIMETypes.addMIMEType((short)336, "application/x-hdf", "hdf");
		DefaultMIMETypes.addMIMEType((short)337, "application/x-httpd-php", "phtml pht php", "php");
		DefaultMIMETypes.addMIMEType((short)338, "application/x-httpd-php-source", "phps");
		DefaultMIMETypes.addMIMEType((short)339, "application/x-httpd-php3", "php3");
		DefaultMIMETypes.addMIMEType((short)340, "application/x-httpd-php3-preprocessed", "php3p");
		DefaultMIMETypes.addMIMEType((short)341, "application/x-httpd-php4", "php4");
		DefaultMIMETypes.addMIMEType((short)342, "application/x-ica", "ica");
		DefaultMIMETypes.addMIMEType((short)343, "application/x-internet-signup", "ins isp");
		DefaultMIMETypes.addMIMEType((short)344, "application/x-iphone", "iii");
		DefaultMIMETypes.addMIMEType((short)345, "application/x-java-applet");
		DefaultMIMETypes.addMIMEType((short)346, "application/x-java-archive", "jar");
		DefaultMIMETypes.addMIMEType((short)347, "application/x-java-bean");
		DefaultMIMETypes.addMIMEType((short)348, "application/x-java-jnlp-file", "jnlp");
		DefaultMIMETypes.addMIMEType((short)349, "application/x-java-serialized-object", "ser");
		DefaultMIMETypes.addMIMEType((short)350, "application/x-java-vm", "class");
		DefaultMIMETypes.addMIMEType((short)351, "application/x-javascript", "js");
		DefaultMIMETypes.addMIMEType((short)352, "application/x-kdelnk");
		DefaultMIMETypes.addMIMEType((short)353, "application/x-kchart", "chrt");
		DefaultMIMETypes.addMIMEType((short)354, "application/x-killustrator", "kil");
		DefaultMIMETypes.addMIMEType((short)355, "application/x-kpresenter", "kpr kpt");
		DefaultMIMETypes.addMIMEType((short)356, "application/x-koan", "skp skd skt skm");
		DefaultMIMETypes.addMIMEType((short)357, "application/x-kspread", "ksp");
		DefaultMIMETypes.addMIMEType((short)358, "application/x-kword", "kwd kwt", "kwd");
		DefaultMIMETypes.addMIMEType((short)359, "application/x-latex", "latex");
		DefaultMIMETypes.addMIMEType((short)360, "application/x-lha", "lha");
		DefaultMIMETypes.addMIMEType((short)361, "application/x-lzh", "lzh");
		DefaultMIMETypes.addMIMEType((short)362, "application/x-lzx", "lzx");
		DefaultMIMETypes.addMIMEType((short)363, "application/x-maker", "frm maker frame fm fb book fbdoc");
		DefaultMIMETypes.addMIMEType((short)364, "application/x-mif", "mif");
		DefaultMIMETypes.addMIMEType((short)365, "application/x-ms-wmz", "wmz");
		DefaultMIMETypes.addMIMEType((short)366, "application/x-ms-wmd", "wmd");
		DefaultMIMETypes.addMIMEType((short)367, "application/x-msdos-program", "com exe bat dll", "exe");
		DefaultMIMETypes.addMIMEType((short)368, "application/x-msi", "msi");
		DefaultMIMETypes.addMIMEType((short)369, "application/x-netcdf", "nc");
		DefaultMIMETypes.addMIMEType((short)370, "application/x-ns-proxy-autoconfig", "pac");
		DefaultMIMETypes.addMIMEType((short)371, "application/x-nwc", "nwc");
		DefaultMIMETypes.addMIMEType((short)372, "application/x-object", "o");
		DefaultMIMETypes.addMIMEType((short)373, "application/x-oz-application", "oza");
		DefaultMIMETypes.addMIMEType((short)374, "application/x-pkcs7-certreqresp", "p7r");
		DefaultMIMETypes.addMIMEType((short)375, "application/x-pkcs7-crl", "crl");
		DefaultMIMETypes.addMIMEType((short)376, "application/x-python-code", "pyc pyo", "unknown-pyc-pyo");
		DefaultMIMETypes.addMIMEType((short)377, "application/x-quicktimeplayer", "qtl");
		DefaultMIMETypes.addMIMEType((short)378, "application/x-redhat-package-manager", "rpm");
		DefaultMIMETypes.addMIMEType((short)379, "application/x-rx");
		DefaultMIMETypes.addMIMEType((short)380, "application/x-sh");
		DefaultMIMETypes.addMIMEType((short)381, "application/x-shar", "shar");
		DefaultMIMETypes.addMIMEType((short)382, "application/x-shellscript");
		DefaultMIMETypes.addMIMEType((short)383, "application/x-shockwave-flash", "swf swfl", "swf");
		DefaultMIMETypes.addMIMEType((short)384, "application/x-sh", "sh");
		DefaultMIMETypes.addMIMEType((short)385, "application/x-stuffit", "sit");
		DefaultMIMETypes.addMIMEType((short)386, "application/x-sv4cpio", "sv4cpio");
		DefaultMIMETypes.addMIMEType((short)387, "application/x-sv4crc", "sv4crc");
		DefaultMIMETypes.addMIMEType((short)388, "application/x-tar", "tar");
		DefaultMIMETypes.addMIMEType((short)389, "application/x-tcl", "tcl");
		DefaultMIMETypes.addMIMEType((short)390, "application/x-tex-gf", "gf");
		DefaultMIMETypes.addMIMEType((short)391, "application/x-tex-pk", "pk");
		DefaultMIMETypes.addMIMEType((short)392, "application/x-texinfo", "texinfo texi", "texi");
		DefaultMIMETypes.addMIMEType((short)393, "application/x-trash", "~ % bak old sik");
		DefaultMIMETypes.addMIMEType((short)394, "application/x-troff", "t tr roff");
		DefaultMIMETypes.addMIMEType((short)395, "application/x-troff-man", "man");
		DefaultMIMETypes.addMIMEType((short)396, "application/x-troff-me", "me");
		DefaultMIMETypes.addMIMEType((short)397, "application/x-troff-ms", "ms");
		DefaultMIMETypes.addMIMEType((short)398, "application/x-ustar", "ustar");
		DefaultMIMETypes.addMIMEType((short)399, "application/x-videolan");
		DefaultMIMETypes.addMIMEType((short)400, "application/x-wais-source", "src");
		DefaultMIMETypes.addMIMEType((short)401, "application/x-wingz", "wz");
		DefaultMIMETypes.addMIMEType((short)402, "application/x-x509-ca-cert", "crt");
		DefaultMIMETypes.addMIMEType((short)403, "application/x-xcf", "xcf");
		DefaultMIMETypes.addMIMEType((short)404, "application/x-xfig", "fig");
		DefaultMIMETypes.addMIMEType((short)405, "audio/32kadpcm");
		DefaultMIMETypes.addMIMEType((short)406, "audio/basic", "au snd", "au");
		DefaultMIMETypes.addMIMEType((short)407, "audio/g.722.1");
		DefaultMIMETypes.addMIMEType((short)408, "audio/l16");
		DefaultMIMETypes.addMIMEType((short)409, "audio/midi", "mid midi kar", "mid");
		DefaultMIMETypes.addMIMEType((short)410, "audio/mp4a-latm");
		DefaultMIMETypes.addMIMEType((short)411, "audio/mpa-robust");
		DefaultMIMETypes.addMIMEType((short)412, "audio/mpeg", "mpga mpega mp2 mp3 m4a", "mp3");
		DefaultMIMETypes.addMIMEType((short)413, "audio/mpegurl", "m3u");
		DefaultMIMETypes.addMIMEType((short)414, "audio/parityfec");
		DefaultMIMETypes.addMIMEType((short)415, "audio/prs.sid", "sid");
		DefaultMIMETypes.addMIMEType((short)416, "audio/telephone-event");
		DefaultMIMETypes.addMIMEType((short)417, "audio/tone");
		DefaultMIMETypes.addMIMEType((short)418, "audio/vnd.cisco.nse");
		DefaultMIMETypes.addMIMEType((short)419, "audio/vnd.cns.anp1");
		DefaultMIMETypes.addMIMEType((short)420, "audio/vnd.cns.inf1");
		DefaultMIMETypes.addMIMEType((short)421, "audio/vnd.digital-winds");
		DefaultMIMETypes.addMIMEType((short)422, "audio/vnd.everad.plj");
		DefaultMIMETypes.addMIMEType((short)423, "audio/vnd.lucent.voice");
		DefaultMIMETypes.addMIMEType((short)424, "audio/vnd.nortel.vbk");
		DefaultMIMETypes.addMIMEType((short)425, "audio/vnd.nuera.ecelp4800");
		DefaultMIMETypes.addMIMEType((short)426, "audio/vnd.nuera.ecelp7470");
		DefaultMIMETypes.addMIMEType((short)427, "audio/vnd.nuera.ecelp9600");
		DefaultMIMETypes.addMIMEType((short)428, "audio/vnd.octel.sbc");
		DefaultMIMETypes.addMIMEType((short)429, "audio/vnd.qcelp");
		DefaultMIMETypes.addMIMEType((short)430, "audio/vnd.rhetorex.32kadpcm");
		DefaultMIMETypes.addMIMEType((short)431, "audio/vnd.vmx.cvsd");
		DefaultMIMETypes.addMIMEType((short)432, "audio/x-aiff", "aif aiff aifc", "aiff");
		DefaultMIMETypes.addMIMEType((short)433, "audio/x-gsm", "gsm");
		DefaultMIMETypes.addMIMEType((short)434, "audio/x-mpegurl", "m3u");
		DefaultMIMETypes.addMIMEType((short)435, "audio/x-ms-wma", "wma");
		DefaultMIMETypes.addMIMEType((short)436, "audio/x-ms-wax", "wax");
		DefaultMIMETypes.addMIMEType((short)437, "audio/x-pn-realaudio-plugin");
		DefaultMIMETypes.addMIMEType((short)438, "audio/x-pn-realaudio", "ra rm ram", "ra");
		DefaultMIMETypes.addMIMEType((short)439, "audio/x-realaudio", "ra");
		DefaultMIMETypes.addMIMEType((short)440, "audio/x-scpls", "pls");
		DefaultMIMETypes.addMIMEType((short)441, "audio/x-sd2", "sd2");
		DefaultMIMETypes.addMIMEType((short)442, "audio/x-wav", "wav");
		DefaultMIMETypes.addMIMEType((short)443, "chemical/x-pdb", "pdb");
		DefaultMIMETypes.addMIMEType((short)444, "chemical/x-xyz", "xyz");
		DefaultMIMETypes.addMIMEType((short)445, "image/cgm");
		DefaultMIMETypes.addMIMEType((short)446, "image/g3fax");
		DefaultMIMETypes.addMIMEType((short)447, "image/gif", "gif");
		DefaultMIMETypes.addMIMEType((short)448, "image/ief", "ief");
		DefaultMIMETypes.addMIMEType((short)449, "image/jpeg", "jpeg jpg jpe", "jpeg");
		DefaultMIMETypes.addMIMEType((short)450, "image/naplps");
		DefaultMIMETypes.addMIMEType((short)451, "image/pcx", "pcx");
		DefaultMIMETypes.addMIMEType((short)452, "image/png", "png");
		DefaultMIMETypes.addMIMEType((short)453, "image/prs.btif");
		DefaultMIMETypes.addMIMEType((short)454, "image/prs.pti");
		DefaultMIMETypes.addMIMEType((short)455, "image/svg+xml", "svg svgz", "svg");
		DefaultMIMETypes.addMIMEType((short)456, "image/tiff", "tiff tif", "tiff");
		DefaultMIMETypes.addMIMEType((short)457, "image/vnd.cns.inf2");
		DefaultMIMETypes.addMIMEType((short)458, "image/vnd.djvu", "djvu djv");
		DefaultMIMETypes.addMIMEType((short)459, "image/vnd.dwg");
		DefaultMIMETypes.addMIMEType((short)460, "image/vnd.dxf");
		DefaultMIMETypes.addMIMEType((short)461, "image/vnd.fastbidsheet");
		DefaultMIMETypes.addMIMEType((short)462, "image/vnd.fpx");
		DefaultMIMETypes.addMIMEType((short)463, "image/vnd.fst");
		DefaultMIMETypes.addMIMEType((short)464, "image/vnd.fujixerox.edmics-mmr");
		DefaultMIMETypes.addMIMEType((short)465, "image/vnd.fujixerox.edmics-rlc");
		DefaultMIMETypes.addMIMEType((short)466, "image/vnd.mix");
		DefaultMIMETypes.addMIMEType((short)467, "image/vnd.net-fpx");
		DefaultMIMETypes.addMIMEType((short)468, "image/vnd.svf");
		DefaultMIMETypes.addMIMEType((short)469, "image/vnd.wap.wbmp", "wbmp");
		DefaultMIMETypes.addMIMEType((short)470, "image/vnd.xiff");
		DefaultMIMETypes.addMIMEType((short)471, "image/x-cmu-raster", "ras");
		DefaultMIMETypes.addMIMEType((short)472, "image/x-coreldraw", "cdr");
		DefaultMIMETypes.addMIMEType((short)473, "image/x-coreldrawpattern", "pat");
		DefaultMIMETypes.addMIMEType((short)474, "image/x-coreldrawtemplate", "cdt");
		DefaultMIMETypes.addMIMEType((short)475, "image/x-corelphotopaint", "cpt");
		DefaultMIMETypes.addMIMEType((short)476, "image/x-icon", "ico");
		DefaultMIMETypes.addMIMEType((short)477, "image/x-jg", "art");
		DefaultMIMETypes.addMIMEType((short)478, "image/x-jng", "jng");
		DefaultMIMETypes.addMIMEType((short)479, "image/x-ms-bmp", "bmp");
		DefaultMIMETypes.addMIMEType((short)480, "image/x-photoshop", "psd");
		DefaultMIMETypes.addMIMEType((short)481, "image/x-portable-anymap", "pnm");
		DefaultMIMETypes.addMIMEType((short)482, "image/x-portable-bitmap", "pbm");
		DefaultMIMETypes.addMIMEType((short)483, "image/x-portable-graymap", "pgm");
		DefaultMIMETypes.addMIMEType((short)484, "image/x-portable-pixmap", "ppm");
		DefaultMIMETypes.addMIMEType((short)485, "image/x-rgb", "rgb");
		DefaultMIMETypes.addMIMEType((short)486, "image/x-xbitmap", "xbm");
		DefaultMIMETypes.addMIMEType((short)487, "image/x-xpixmap", "xpm");
		DefaultMIMETypes.addMIMEType((short)488, "image/x-xwindowdump", "xwd");
		DefaultMIMETypes.addMIMEType((short)489, "inode/chardevice");
		DefaultMIMETypes.addMIMEType((short)490, "inode/blockdevice");
		DefaultMIMETypes.addMIMEType((short)491, "inode/directory-locked");
		DefaultMIMETypes.addMIMEType((short)492, "inode/directory");
		DefaultMIMETypes.addMIMEType((short)493, "inode/fifo");
		DefaultMIMETypes.addMIMEType((short)494, "inode/socket");
		DefaultMIMETypes.addMIMEType((short)495, "message/delivery-status");
		DefaultMIMETypes.addMIMEType((short)496, "message/disposition-notification");
		DefaultMIMETypes.addMIMEType((short)497, "message/external-body");
		DefaultMIMETypes.addMIMEType((short)498, "message/http");
		DefaultMIMETypes.addMIMEType((short)499, "message/s-http");
		DefaultMIMETypes.addMIMEType((short)500, "message/news");
		DefaultMIMETypes.addMIMEType((short)501, "message/partial");
		DefaultMIMETypes.addMIMEType((short)502, "message/rfc822");
		DefaultMIMETypes.addMIMEType((short)503, "model/iges", "igs iges");
		DefaultMIMETypes.addMIMEType((short)504, "model/mesh", "msh mesh silo");
		DefaultMIMETypes.addMIMEType((short)505, "model/vnd.dwf");
		DefaultMIMETypes.addMIMEType((short)506, "model/vnd.flatland.3dml");
		DefaultMIMETypes.addMIMEType((short)507, "model/vnd.gdl");
		DefaultMIMETypes.addMIMEType((short)508, "model/vnd.gs-gdl");
		DefaultMIMETypes.addMIMEType((short)509, "model/vnd.gtw");
		DefaultMIMETypes.addMIMEType((short)510, "model/vnd.mts");
		DefaultMIMETypes.addMIMEType((short)511, "model/vnd.vtu");
		DefaultMIMETypes.addMIMEType((short)512, "model/vrml", "wrl vrml", "vrml");
		DefaultMIMETypes.addMIMEType((short)513, "multipart/alternative");
		DefaultMIMETypes.addMIMEType((short)514, "multipart/appledouble");
		DefaultMIMETypes.addMIMEType((short)515, "multipart/byteranges");
		DefaultMIMETypes.addMIMEType((short)516, "multipart/digest");
		DefaultMIMETypes.addMIMEType((short)517, "multipart/encrypted");
		DefaultMIMETypes.addMIMEType((short)518, "multipart/form-data");
		DefaultMIMETypes.addMIMEType((short)519, "multipart/header-set");
		DefaultMIMETypes.addMIMEType((short)520, "multipart/mixed");
		DefaultMIMETypes.addMIMEType((short)521, "multipart/parallel");
		DefaultMIMETypes.addMIMEType((short)522, "multipart/related");
		DefaultMIMETypes.addMIMEType((short)523, "multipart/report");
		DefaultMIMETypes.addMIMEType((short)524, "multipart/signed");
		DefaultMIMETypes.addMIMEType((short)525, "multipart/voice-message");
		DefaultMIMETypes.addMIMEType((short)526, "text/calendar", "ics icz", "ics");
		DefaultMIMETypes.addMIMEType((short)527, "text/comma-separated-values", "csv");
		DefaultMIMETypes.addMIMEType((short)528, "text/css", "css");
		DefaultMIMETypes.addMIMEType((short)529, "text/directory");
		DefaultMIMETypes.addMIMEType((short)530, "text/english");
		DefaultMIMETypes.addMIMEType((short)531, "text/enriched");
		DefaultMIMETypes.addMIMEType((short)532, "text/h323", "323");
		DefaultMIMETypes.addMIMEType((short)533, "text/html", "htm html shtml", "html");
		DefaultMIMETypes.addMIMEType((short)534, "text/iuls", "uls");
		DefaultMIMETypes.addMIMEType((short)535, "text/mathml", "mml");
		DefaultMIMETypes.addMIMEType((short)536, "text/parityfec");
		DefaultMIMETypes.addMIMEType((short)537, "text/plain", "asc txt text diff pot", "txt");
		DefaultMIMETypes.addMIMEType((short)538, "text/prs.lines.tag");
		DefaultMIMETypes.addMIMEType((short)539, "text/rfc822-headers");
		DefaultMIMETypes.addMIMEType((short)540, "text/richtext", "rtx");
		DefaultMIMETypes.addMIMEType((short)541, "text/rtf", "rtf");
		DefaultMIMETypes.addMIMEType((short)542, "text/scriptlet", "sct wsc");
		DefaultMIMETypes.addMIMEType((short)543, "text/t140");
		DefaultMIMETypes.addMIMEType((short)544, "text/texmacs", "tm ts");
		DefaultMIMETypes.addMIMEType((short)545, "text/tab-separated-values", "tsv");
		DefaultMIMETypes.addMIMEType((short)546, "text/uri-list");
		DefaultMIMETypes.addMIMEType((short)547, "text/vnd.abc");
		DefaultMIMETypes.addMIMEType((short)548, "text/vnd.curl");
		DefaultMIMETypes.addMIMEType((short)549, "text/vnd.DMClientScript");
		DefaultMIMETypes.addMIMEType((short)550, "text/vnd.flatland.3dml");
		DefaultMIMETypes.addMIMEType((short)551, "text/vnd.fly");
		DefaultMIMETypes.addMIMEType((short)552, "text/vnd.fmi.flexstor");
		DefaultMIMETypes.addMIMEType((short)553, "text/vnd.in3d.3dml");
		DefaultMIMETypes.addMIMEType((short)554, "text/vnd.in3d.spot");
		DefaultMIMETypes.addMIMEType((short)555, "text/vnd.IPTC.NewsML");
		DefaultMIMETypes.addMIMEType((short)556, "text/vnd.IPTC.NITF");
		DefaultMIMETypes.addMIMEType((short)557, "text/vnd.latex-z");
		DefaultMIMETypes.addMIMEType((short)558, "text/vnd.motorola.reflex");
		DefaultMIMETypes.addMIMEType((short)559, "text/vnd.ms-mediapackage");
		DefaultMIMETypes.addMIMEType((short)560, "text/vnd.sun.j2me.app-descriptor", "jad");
		DefaultMIMETypes.addMIMEType((short)561, "text/vnd.wap.si");
		DefaultMIMETypes.addMIMEType((short)562, "text/vnd.wap.sl");
		DefaultMIMETypes.addMIMEType((short)563, "text/vnd.wap.wml", "wml");
		DefaultMIMETypes.addMIMEType((short)564, "text/vnd.wap.wmlscript", "wmls");
		DefaultMIMETypes.addMIMEType((short)565, "text/x-c++hdr", "h++ hpp hxx hh", "hh");
		DefaultMIMETypes.addMIMEType((short)566, "text/x-c++src", "c++ cpp cxx cc", "cc");
		DefaultMIMETypes.addMIMEType((short)567, "text/x-chdr", "h");
		DefaultMIMETypes.addMIMEType((short)568, "text/x-crontab");
		DefaultMIMETypes.addMIMEType((short)569, "text/x-csh", "csh");
		DefaultMIMETypes.addMIMEType((short)570, "text/x-csrc", "c");
		DefaultMIMETypes.addMIMEType((short)571, "text/x-java", "java");
		DefaultMIMETypes.addMIMEType((short)572, "text/x-makefile");
		DefaultMIMETypes.addMIMEType((short)573, "text/x-moc", "moc");
		DefaultMIMETypes.addMIMEType((short)574, "text/x-pascal", "p pas", "pas");
		DefaultMIMETypes.addMIMEType((short)575, "text/x-pcs-gcd", "gcd");
		DefaultMIMETypes.addMIMEType((short)576, "text/x-perl", "pl pm", "pl");
		DefaultMIMETypes.addMIMEType((short)577, "text/x-python", "py");
		DefaultMIMETypes.addMIMEType((short)578, "text/x-server-parsed-html", "shmtl", "shtml");
		DefaultMIMETypes.addMIMEType((short)579, "text/x-setext", "etx");
		DefaultMIMETypes.addMIMEType((short)580, "text/x-sh", "sh");
		DefaultMIMETypes.addMIMEType((short)581, "text/x-tcl", "tcl tk", "tcl");
		DefaultMIMETypes.addMIMEType((short)582, "text/x-tex", "tex ltx sty cls", "tex");
		DefaultMIMETypes.addMIMEType((short)583, "text/x-vcalendar", "vcs");
		DefaultMIMETypes.addMIMEType((short)584, "text/x-vcard", "vcf");
		DefaultMIMETypes.addMIMEType((short)585, "video/dl", "dl");
		DefaultMIMETypes.addMIMEType((short)586, "video/fli", "fli");
		DefaultMIMETypes.addMIMEType((short)587, "video/gl", "gl");
		DefaultMIMETypes.addMIMEType((short)588, "video/mpeg", "mpeg mpg mpe", "mpeg");
		DefaultMIMETypes.addMIMEType((short)589, "video/mp4", "mp4");
		DefaultMIMETypes.addMIMEType((short)590, "video/quicktime", "qt mov", "mov");
		DefaultMIMETypes.addMIMEType((short)591, "video/mp4v-es");
		DefaultMIMETypes.addMIMEType((short)592, "video/parityfec");
		DefaultMIMETypes.addMIMEType((short)593, "video/pointer");
		DefaultMIMETypes.addMIMEType((short)594, "video/vnd.fvt");
		DefaultMIMETypes.addMIMEType((short)595, "video/vnd.motorola.video");
		DefaultMIMETypes.addMIMEType((short)596, "video/vnd.motorola.videop");
		DefaultMIMETypes.addMIMEType((short)597, "video/vnd.mpegurl", "mxu");
		DefaultMIMETypes.addMIMEType((short)598, "video/vnd.mts");
		DefaultMIMETypes.addMIMEType((short)599, "video/vnd.nokia.interleaved-multimedia");
		DefaultMIMETypes.addMIMEType((short)600, "video/vnd.vivo");
		DefaultMIMETypes.addMIMEType((short)601, "video/x-dv", "dif dv");
		DefaultMIMETypes.addMIMEType((short)602, "video/x-la-asf", "lsf lsx", "lsf");
		DefaultMIMETypes.addMIMEType((short)603, "video/x-mng", "mng");
		DefaultMIMETypes.addMIMEType((short)604, "video/x-ms-asf", "asf asx", "asf");
		DefaultMIMETypes.addMIMEType((short)605, "video/x-ms-wm", "wm");
		DefaultMIMETypes.addMIMEType((short)606, "video/x-ms-wmv", "wmv");
		DefaultMIMETypes.addMIMEType((short)607, "video/x-ms-wmx", "wmx");
		DefaultMIMETypes.addMIMEType((short)608, "video/x-ms-wvx", "wvx");
		DefaultMIMETypes.addMIMEType((short)609, "video/x-msvideo", "avi");
		DefaultMIMETypes.addMIMEType((short)610, "video/x-sgi-movie", "movie");
		DefaultMIMETypes.addMIMEType((short)611, "x-conference/x-cooltalk", "ice");
		DefaultMIMETypes.addMIMEType((short)612, "x-world/x-vrml", "vrm vrml wrl", "vrml");
		DefaultMIMETypes.addMIMEType((short)613, "application/x-freenet-reference", "fref");
		DefaultMIMETypes.addMIMEType((short)614, "application/x-freenet-index", "fidx");
		DefaultMIMETypes.addMIMEType((short)615, "application/x-freenet-blob", "flob");
	}

	/** Guess a MIME type from a filename */
	public synchronized static String guessMIMEType(final String arg) {
		if (arg == null) {
			Logger.error(new DefaultMIMETypes(), "Unable to guess mime type for file 'null' ");
			return null;
		}

		final int x = arg.lastIndexOf('.');
		if((x == -1) || (x == arg.length()-1))
			return DefaultMIMETypes.DEFAULT_MIME_TYPE;
		final String ext = arg.substring(x+1).toLowerCase();
		final Short mimeIndexOb = (Short) DefaultMIMETypes.mimeTypesByExtension.get(ext);
		if(mimeIndexOb != null)
			return (String) DefaultMIMETypes.mimeTypesByNumber.get(mimeIndexOb.intValue());
		else return DefaultMIMETypes.DEFAULT_MIME_TYPE;
	}

	public synchronized static String getExtension(final String type) {
		final short typeNumber = DefaultMIMETypes.byName(type);
		if(typeNumber < 0) return null;
		return (String) DefaultMIMETypes.primaryExtensionByMimeNumber.get(new Short(typeNumber));
	}

	public synchronized static boolean isValidExt(final String expectedMimeType, final String oldExt) {
		final Short s = (Short) DefaultMIMETypes.mimeTypesByExtension.get(oldExt);
		if(s == null) return false;
		final String type = DefaultMIMETypes.byNumber(s.shortValue());
		return type.equals(expectedMimeType);
	}

	public synchronized static Vector getAllMIMETypes() {
		return DefaultMIMETypes.mimeTypesByNumber;
	}
}
