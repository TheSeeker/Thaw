package thaw.core;

/**
 * would be better called "FreenetKeyHelper" ... but too late :p
 */
public class FreenetURIHelper {

	private FreenetURIHelper() {

	}

	/**
	 * Quick test to see if the string could be a key
	 * only check the head, not the content (this property is used in FetchPlugin,
	 * please keep it)
	 */
	public static boolean isAKey(String key) {
		if (key == null)
		    return false;

		return (key.startsWith("CHK@")
			|| key.startsWith("SSK@")
			|| key.startsWith("USK@")
			|| key.startsWith("KSK@"));
	}

	public static String cleanURI(String uri) {
		if (uri == null)
			return uri;

		uri = uri.replaceFirst("http://127.0.0.1:8888/", "");
		uri = uri.replaceFirst("http://localhost/", "");
		uri = uri.replaceFirst("freenet:", "");

		try {
			uri = java.net.URLDecoder.decode(uri, "UTF-8");
		} catch (final java.io.UnsupportedEncodingException e) {
			Logger.warning(new FreenetURIHelper(), "UnsupportedEncodingException (UTF-8): "+e.toString());
		}

		if ((uri.indexOf("CHK@") < 0)
		    && (uri.indexOf("USK@") < 0)
		    && (uri.indexOf("KSK@") < 0)
		    && (uri.indexOf("SSK@") < 0)) {
			Logger.notice(new FreenetURIHelper(), "Not a valid key: "+uri);
			return null;
		}

		return uri;
	}


	public static String getFilenameFromKey(final String key) {
		String filename;
		final String cutcut[] = key.split("/");

		if (key == null)
			return null;

		if(!key.endsWith("/")) {
			filename = cutcut[cutcut.length-1];
		} else {
			filename = "index.html";
		}

		return filename;
	}


	public static String convertSSKtoUSK(String SSK) {
		if ((SSK == null) || SSK.startsWith("USK@"))
			return SSK;

		SSK = SSK.replaceFirst("SSK@", "USK@");

		final String[] split = SSK.split("/");

		SSK = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch (i) {
			case(0):
				SSK = split[i];
				break;
			case(1):
				final String subsplit[] = split[i].split("-");

				SSK = SSK + "/";

				for (int j = 0 ; j < subsplit.length-1 ; j++) {
					if (j == 0)
						SSK = SSK + subsplit[j];
					else
						SSK = SSK + "-" + subsplit[j];
				}

				SSK = SSK + "/" + subsplit[subsplit.length-1];

				break;
			default:
				SSK = SSK + "/" + split[i];
			}
		}

		return SSK;
	}


	public static String abs(final String val) {
		try {
			final java.math.BigDecimal bd = new java.math.BigDecimal(val);
			return bd.abs().toString();
		} catch(final java.lang.NumberFormatException e) {
			Logger.warning(new FreenetURIHelper(), "NumberFormatException while parsing '"+val+"'");
			return "0";
		}
	}


	public static String convertUSKtoSSK(String USK) {
		if ((USK == null) || USK.startsWith("SSK@"))
			return USK;

		USK = USK.replaceFirst("USK@", "SSK@");

		final String[] split = USK.split("/");

		USK = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch (i) {
			case(0):
				USK = split[i];
				break;
			case(2):
				USK += "-" + FreenetURIHelper.abs(split[i]);
				break;
			default:
				USK += "/" + split[i];
				break;
			}
		}

		return USK;
	}


	public static String getPublicInsertionSSK(String key) {
		key = FreenetURIHelper.convertUSKtoSSK(key);

		final String split[] = key.split("/");

		key = "";

		for (int i = 0 ; i < split.length-1 ; i++) {
			if (i == 0)
				key = key + split[i];
			else
				key = key + "/" + split[i];
		}

		return key;
	}


	protected static String changeRev(final String revStr, final int rev, final int offset) {
		if (offset == 0)
			return Integer.toString(rev);

		return Integer.toString(Integer.parseInt(revStr) + offset);
	}

	/**
	 * @param offset if == 0, then rev is changed according to the given offset
	 */
	public static String changeSSKRevision(String key, final int rev, final int offset) {

		if (key == null)
			return null;

		final String[] split = key.split("/");

		key = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch(i) {
			case(0):
				key = key + split[i];
				break;
			case(1):
				final String[] subsplit = split[i].split("-");

				for (int j = 0 ; j < subsplit.length-1 ; j++) {
					if (j == 0)
						key = key + "/" + subsplit[j];
					else
						key = key + "-" + subsplit[j];
				}

				key = key + "-" + FreenetURIHelper.changeRev(subsplit[subsplit.length-1], rev, offset);
				break;
			default:
				key = key + "/" + split[i];
			}
		}

		return key;
	}


	public static String changeUSKRevision(String key, int rev, int offset) {
		if (key == null)
			return null;

		final String[] split = key.split("/");

		key = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch(i) {
			case(0):
				key = key + split[i];
				break;
			case(2):
				key = key + "/" + FreenetURIHelper.changeRev(split[2], rev, offset);
				break;
			default:
				key = key + "/" + split[i];
			}
		}

		return key;

	}


	public static int getUSKRevision(final String key) {
		String[] split;

		if (key == null)
			return -1;

		split = key.split("/");

		if (split.length < 3)
			return -1;

		try {
			return Integer.parseInt(split[2]);
		} catch(NumberFormatException e) {
			Logger.warning(new FreenetURIHelper(), "Unable to parse '"+key +"'");
			return -1;
		}
	}


	/**
	 * will lower the case !
	 * will return the begining of the key.
	 */
	public static String getComparablePart(String key) {
		if (key == null)
			return null;

		if (key.startsWith("KSK@")) {
			return key;
		}

		if (key.length() <= 70)
			return key.toLowerCase();

		return key.substring(0, 70).toLowerCase();
	}


	/**
	 * this process is not costless.
	 */
	public static boolean compareKeys(String keyA, String keyB) {
		if (keyA == keyB)
			return true;

		if (keyA == null || keyB == null) {
			Logger.notice(new FreenetURIHelper(), "compareKeys : null argument ?!");
			return false;
		}

		keyA = cleanURI(keyA);
		keyB = cleanURI(keyB);

		if (keyA.startsWith("USK@"))
			keyA = convertUSKtoSSK(keyA);

		if (keyB.startsWith("USK@"))
			keyB = convertUSKtoSSK(keyB);

		if (!keyA.substring(0, 3).equals(keyB.substring(0, 3))) {
			Logger.notice(new FreenetURIHelper(), "Not the same kind of key : "+
				      keyA.substring(0, 3) + " vs " + keyB.substring(0, 3));
			return false;
		}

		if (keyA.startsWith("CHK@")) {
			return getComparablePart(keyA).equals(getComparablePart(keyB));
		}

		if (keyA.startsWith("SSK@")) {
			keyA = changeSSKRevision(keyA, 0, 0);
			keyB = changeSSKRevision(keyB, 0, 0);

			keyA = keyA.replaceAll(".frdx", ".xml"); /* we consider .frdx equivalent to .xml */
			keyB = keyB.replaceAll(".frdx", ".xml"); /* we consider .frdx equivalent to .xml */
		}

		if ( keyA.equals(keyB) )
			return true;

		return false;
	}
}

