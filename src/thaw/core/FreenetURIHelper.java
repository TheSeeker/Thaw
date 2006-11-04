package thaw.core;

import thaw.core.Logger;

public class FreenetURIHelper {

	public FreenetURIHelper() {

	}

	public static String cleanURI(String uri) {
		if (uri == null)
			return uri;

		uri = uri.replaceFirst("http://127.0.0.1:8888/", "");
		uri = uri.replaceFirst("http://localhost/", "");
		uri = uri.replaceFirst("freenet:", "");

		try {
				uri = java.net.URLDecoder.decode(uri, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			Logger.warning(new FreenetURIHelper(), "UnsupportedEncodingException (UTF-8): "+e.toString());
		}

		if (uri.indexOf("CHK@") < 0
		    && uri.indexOf("USK@") < 0
		    && uri.indexOf("KSK@") < 0
		    && uri.indexOf("SSK@") < 0) {
			Logger.notice(new FreenetURIHelper(), "Not a valid key: "+uri);
			return null;
		}

		return uri;
	}


	public static String getFilenameFromKey(String key) {
		String filename;
		String cutcut[] = key.split("/");

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
		if (SSK == null || SSK.startsWith("USK@"))
			return SSK;

		SSK.replaceFirst("SSK@", "USK@");

		String[] split = SSK.split("/");

		SSK = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch (i) {
			case(0):
				SSK = split[i];
				break;
			case(1):
				String subsplit[] = split[i].split("-");

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


	public static String abs(String val) {
		java.math.BigDecimal bd = new java.math.BigDecimal(val);

		return bd.abs().toString();
	}


	public static String convertUSKtoSSK(String USK) {
		if (USK == null || USK.startsWith("SSK@"))
			return USK;

		USK = USK.replaceFirst("USK@", "SSK@");

		String[] split = USK.split("/");

		USK = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch (i) {
			case(0):
				USK = split[i];
				break;
			case(2):
				USK = USK + "-" + abs(split[i]);
				break;
			default:
				USK = USK + "/" + split[i];
				break;
			}
		}

		return USK;
	}


	public static String getPublicInsertionSSK(String key) {
		key = convertUSKtoSSK(key);

		String split[] = key.split("/");

		key = "";

		for (int i = 0 ; i < split.length-1 ; i++) {
			if (i == 0)
				key = key + split[i];
			else
				key = key + "/" + split[i];
		}

		return key;
	}


	protected static String changeRev(String revStr, int rev, int offset) {
		if (rev >= 0)
			return Integer.toString(rev);

		return Integer.toString(Integer.parseInt(revStr) + offset);
	}

	/**
	 * @param rev if < 0, then rev is changed according to the given offset
	 */
	public static String changeSSKRevision(String key, int rev, int offset) {

		if (key == null)
			return null;

		String[] split = key.split("/");

		key = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch(i) {
			case(0):
				key = key + split[i];
				break;
			case(1):
				String[] subsplit = split[i].split("-");

				for (int j = 0 ; j < subsplit.length-1 ; j++) {
					if (j == 0)
						key = key + subsplit[j];
					else
						key = key + "-" + subsplit[j];
				}

				key = key + "-" + changeRev(subsplit[subsplit.length-1], rev, offset);
				break;
			default:
				key = key + "/" + split[i];
			}
		}

		return key;
	}


	public static int getUSKRevision(String key) {
		String[] split;

		if (key == null)
			return -1;

		split = key.split("/");

		if (split.length < 3)
			return -1;

		return Integer.parseInt(split[2]);
	}

}

