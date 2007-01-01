package thaw.core;


public class FreenetURIHelper {

	public FreenetURIHelper() {

	}

	/**
	 * Quick test to see if the string could be a key
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

		SSK.replaceFirst("SSK@", "USK@");

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
				USK = USK + "-" + FreenetURIHelper.abs(split[i]);
				break;
			default:
				USK = USK + "/" + split[i];
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
		if (rev >= 0)
			return Integer.toString(rev);

		return Integer.toString(Integer.parseInt(revStr) + offset);
	}

	/**
	 * @param rev if < 0, then rev is changed according to the given offset
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
						key = key + subsplit[j];
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


	public static int getUSKRevision(final String key) {
		String[] split;

		if (key == null)
			return -1;

		split = key.split("/");

		if (split.length < 3)
			return -1;

		return Integer.parseInt(split[2]);
	}


	public static String getComparablePart(String key) {
		if (key.startsWith("KSK@"))
			return key;

		int maxLength = 0;

		if (key.length() <= 70)
			maxLength = key.length();
		else
			maxLength = 70;

		return key.substring(0, maxLength).toLowerCase();
	}
}

