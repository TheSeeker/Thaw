package thaw.fcp;

import thaw.core.Logger;

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

		if (key.startsWith("CHK@")
		    || key.startsWith("SSK@")
		    || key.startsWith("USK@")) {
			return (key.length() > 20);
		}

		return key.startsWith("KSK@");
	}


	private final static String[] TEST_GOOD_KEYS = {
		"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
		"USK@p-uFAWUomLm37MCQLu3r67-B8e6yF1kS4q2v0liM1Vk,h0MWqM~lF0Bec-AIv445PLn06ams9-RFbnwO6Cm2Snc,AQACAAE/Thaw/7/Thaw.frdx",
		"KSK@gpl",
		"SSK@FoNrbtiJCeRUIorP01Vx5~Pn0aVp4tMeesVKObwbKXE,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQECAAE/"
	};

	private final static String[] TEST_BASIC_BAD_KEYS = {
		"CHK@mmH/Toto.jar",
		"BLEH"
	};



	private static boolean testIsAKey() {
		System.out.println("=> isAKey()");

		for (int i = 0 ; i < TEST_GOOD_KEYS.length ; i++) {
			System.out.print("==> Good key "+Integer.toString(i)+" : ");

			if (!isAKey(TEST_GOOD_KEYS[i])) {
				System.out.println("FAILED");
				System.out.println("Failed on: "+TEST_GOOD_KEYS[i]);
				return false;
			}

			System.out.println("Ok");
		}


		for (int i = 0 ; i < TEST_BASIC_BAD_KEYS.length ; i++) {
			System.out.print("==> Bad key "+Integer.toString(i)+" : ");

			if (isAKey(TEST_BASIC_BAD_KEYS[i])) {
				System.out.println("FAILED");
				System.out.println("Failed on: "+TEST_BASIC_BAD_KEYS[i]);
				return false;
			}

			System.out.println("Ok");
		}

		return true;
	}




	public static String cleanURI(String uri) {
		if (uri == null)
			return uri;

		uri = uri.trim();
		uri = uri.replaceFirst("^http://[^/]+/+(freenet:)*","");

		try {
			uri = java.net.URLDecoder.decode(uri, "UTF-8");
		} catch (final java.io.UnsupportedEncodingException e) {
			Logger.warning(new FreenetURIHelper(), "UnsupportedEncodingException (UTF-8): "+e.toString());
		}

		if (!isAKey(uri)) {
			Logger.notice(new FreenetURIHelper(), "Not a valid key: "+uri);
			return null;
		}

		return uri;
	}


	private final static String[][] TEST_CLEANABLE_KEYS = {
		/* { unclean key, expected result } */
		{
			"CHK%40mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar"
		},

		{
			"http://127.0.0.1:8888/CHK%40mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar"
		},

		{
			"http://192.168.100.1:8888/CHK%40mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar"
		},

		{
			"http://192.168.100.1:1234/CHK%40mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar"
		}
	};


	private static boolean testCleanURI() {
		System.out.println("=> cleanURI()");

		for (int i = 0; i < TEST_CLEANABLE_KEYS.length ; i++) {
			System.out.print("==> Clean "+Integer.toString(i)+": ");

			String cleaned = cleanURI(TEST_CLEANABLE_KEYS[i][0]);

			if (!TEST_CLEANABLE_KEYS[i][1].equals(cleaned)) {
				System.out.println("FAILED");
				System.out.println("Failed on: "+TEST_CLEANABLE_KEYS[i][0]);

				System.out.println("Got : "+cleaned);

				return false;
			}

			System.out.println("Ok");
		}

		return true;
	}



	public static String getFilenameFromKey(final String key) {
		String filename = null;
		final String cutcut[];

		if (key == null)
			return null;


		if (key.startsWith("KSK")) {
			filename = key.substring(4);
		} else {
			cutcut = key.split("/");

			if (key.startsWith("CHK")) {

				if (cutcut.length >= 2)
					filename = cutcut[1];

			} else if (key.startsWith("SSK")) {

				filename = cutcut[cutcut.length-1];

			} else if (key.startsWith("USK")) {

				if (cutcut.length >= 4 || cutcut.length == 2)
					filename = cutcut[cutcut.length-1];
				else if (cutcut.length == 3)
					filename = cutcut[cutcut.length-2];
			}

		}


		if (filename != null) {
			try {
				filename = java.net.URLDecoder.decode(filename, "UTF-8");
			} catch (final java.io.UnsupportedEncodingException e) {
				Logger.warning(filename, "UnsupportedEncodingException (UTF-8): "+e.toString());
			}
		}

		return filename;
	}


	public final static String[][] TEST_FILENAMED_KEYS = {
		{ /* 0 */
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"Thaw-0.7.10.jar"
		},
		{ /* 1 */
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/",
			null
		},
		{ /* 2 * / /* the '/' at the end was removed */
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8",
			null
		},
		{ /* 3 */
			"USK%4061m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44/Publicly%20writable%20index.frdx",
			"Publicly writable index.frdx"
		},
		{ /* 4 */
			"USK%4061m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44",
			"Publicly writable index"
		},
		{ /* 5 */
			"KSK@gpl.txt",
			"gpl.txt"
		},
		{ /* 6 */
			"SSK@FoNrbtiJCeRUIorP01Vx5~Pn0aVp4tMeesVKObwbKXE,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQECAAE/toto-5/toto.frdx",
			"toto.frdx"
		},
		{ /* 7 */
			"SSK@FoNrbtiJCeRUIorP01Vx5~Pn0aVp4tMeesVKObwbKXE,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQECAAE/toto-5/",
			"toto-5" /* yes, it's the wanted behavior */
		}
	};


	private static boolean testGetFilenameFromKey() {

		System.out.println("=> getFilenameFromKey()");

		for (int i = 0; i < TEST_FILENAMED_KEYS.length ; i++) {

			System.out.print("==> getFilenameFromKey "+Integer.toString(i)+": ");

			String filename = getFilenameFromKey(TEST_FILENAMED_KEYS[i][0]);

			if ( (TEST_FILENAMED_KEYS[i][1] == null && filename != null)
			     || (TEST_FILENAMED_KEYS[i][1] != null && !TEST_FILENAMED_KEYS[i][1].equals(filename)) ) {

				System.out.println("FAILED");
				System.out.println("Failed on: "+TEST_FILENAMED_KEYS[i][0]);

				System.out.println("Got : "+filename);

				return false;
			}

			System.out.println("Ok");
		}

		return true;
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


	private final static String[][] TEST_SSK_TO_USK = {
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index-44/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44/Publicly writable index.frdx" 
		}
	};


	private static boolean testConvertSSKtoUSK() {

		System.out.println("=> convertSSKtoUSK()");

		for (int i = 0; i < TEST_SSK_TO_USK.length ; i++) {

			System.out.print("==> convert "+Integer.toString(i)+": ");

			String usk = convertSSKtoUSK(TEST_SSK_TO_USK[i][0]);

			if ( (TEST_SSK_TO_USK[i][1] == null && usk != null)
			     || (TEST_SSK_TO_USK[i][1] != null && !TEST_SSK_TO_USK[i][1].equals(usk)) ) {

				System.out.println("FAILED");
				System.out.println("Failed on: "+TEST_SSK_TO_USK[i][0]);

				System.out.println("Got : "+usk);

				return false;
			}

			System.out.println("Ok");
		}

		return true;
	}



	private static String abs(final String val) {
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



	private final static String[][] TEST_USK_TO_SSK = {
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index-44/Publicly writable index.frdx"
		},
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index-44/Publicly writable index.frdx"
		}
	};


	private static boolean testConvertUSKtoSSK() {

		System.out.println("=> convertSSKtoUSK()");

		for (int i = 0; i < TEST_USK_TO_SSK.length ; i++) {

			System.out.print("==> convert "+Integer.toString(i)+": ");

			String ssk = convertUSKtoSSK(TEST_USK_TO_SSK[i][0]);

			if ( (TEST_USK_TO_SSK[i][1] == null && ssk != null)
			     || (TEST_USK_TO_SSK[i][1] != null && !TEST_USK_TO_SSK[i][1].equals(ssk)) ) {

				System.out.println("FAILED");
				System.out.println("Failed on: "+TEST_USK_TO_SSK[i][0]);

				System.out.println("Got : "+ssk);

				return false;
			}

			System.out.println("Ok");
		}

		return true;
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


	private static String changeRev(final String revStr, final int rev, final int offset) {
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


	private static final String[][] TEST_CHANGE_SSK_REV = {
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-48/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-12/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-0/Publicly writable index.frdx"
		}
	};

	private static final String[][] TEST_CHANGE_USK_REV = {
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/44/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/40/Publicly writable index.frdx"
		},
		{
			/* yep, it's the expected behavior */
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/-44/Publicly writable index.frdx",
			/* rev += 4 */
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/-40/Publicly writable index.frdx"
		},
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/44/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/12/Publicly writable index.frdx"
		},
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/44/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/0/Publicly writable index.frdx"
		}
	};

	private static final int[][] TEST_CHANGE_REV = {
		/* { rev, offset } */
		{ 0, -4 },
		{ 0,  4 },
		{ 12, 0},
		{ 0, 0}
	};


	private static boolean testRevisionChange(boolean ssk, String[][] TEST_SET) {
		if (ssk)
			System.out.println("=> changeSSKRevision()");
		else
			System.out.println("=> changeUSKRevision()");

		for (int i = 0 ; i < TEST_CHANGE_REV.length ; i++) {
			System.out.print("==> Key "+Integer.toString(i)+": ");

			String result = (ssk ?
					 changeSSKRevision(TEST_SET[i][0], TEST_CHANGE_REV[i][0], TEST_CHANGE_REV[i][1]) :
					 changeUSKRevision(TEST_SET[i][0], TEST_CHANGE_REV[i][0], TEST_CHANGE_REV[i][1]));

			if (!TEST_SET[i][1].equals(result)) {
				System.out.println("FAILED");

				System.out.println("Failed on : "+TEST_SET[i][0]);
				System.out.println("Get      : "+result);
				System.out.println("Expected : "+TEST_SET[i][1]);

				return false;
			}


			System.out.println("Ok");
		}

		return true;
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


	private final static String[] TEST_USK_REV = {
		"USK@p-uFAWUomLm37MCQLu3r67-B8e6yF1kS4q2v0liM1Vk,h0MWqM~lF0Bec-AIv445PLn06ams9-RFbnwO6Cm2Snc,AQACAAE/Thaw/7/Thaw.frdx",
		"USK%4061m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44/Publicly%20writable%20index.frdx",
		"USK%4061m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-44/Publicly%20writable%20index.frdx"
	};

	private final static int[] TEST_USK_REV_RESULTS = {
		7,
		44,
		-44
	};


	private static boolean testGetUSKRevision() {
		System.out.println("=> getUSKRevision()");

		for (int i = 0; i < TEST_USK_REV.length ; i++) {
			System.out.print("==> Key "+Integer.toString(i) + " : ");

			int result = getUSKRevision(TEST_USK_REV[i]);

			if (result != TEST_USK_REV_RESULTS[i]) {
				System.out.println("FAILED");
				System.out.println("Returned: "+Integer.toString(result));
				return false;
			}

			System.out.println("Ok");
		}

		return true;
	}



	/**
	 * will lower the case !
	 * will return the begining of the key.
	 */
	public static String getComparablePart(String key) {
		if (key == null)
			return null;

		if (key.startsWith("KSK@")) {
			return key.toLowerCase();
		}

		if (key.length() <= 70)
			return key.toLowerCase();

		return key.substring(0, 70).toLowerCase();
	}


	/**
	 * this process is not costless.
	 * Ignore the revisions
	 * @return true if they match
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

			String[] splitA = keyA.split("/");
			String[] splitB = keyB.split("/");

			if (splitA.length != splitB.length) {
				/* we shorten the keys because one has less elements than the other */
				keyA = splitA[0]+splitA[1];
				keyB = splitB[0]+splitB[1];
			}

			keyA = keyA.replaceAll(".frdx", ".xml"); /* we consider .frdx equivalent to .xml */
			keyB = keyB.replaceAll(".frdx", ".xml"); /* we consider .frdx equivalent to .xml */
		}

		if ( keyA.equals(keyB) )
			return true;

		return false;
	}


	private final static String[][] TEST_COMPARE_KEYS = {
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"SSK@p-uFAWUomLm37MCQLu3r67-B8e6yF1kS4q2v0liM1Vk,h0MWqM~lF0Bec-AIv445PLn06ams9-RFbnwO6Cm2Snc,AQACAAE/Thaw/7/Thaw.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/-3/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-3/Publicly%20writable%20index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-3"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-3/pouet.txt"
		}
	};

	private final static boolean[] TEST_COMPARE_KEYS_RESULTS = {
		false,
		true,
		true,
		true,
		true/* yes it's a wanted behavior because of the indexes */,
		false
	};

	private static boolean testCompareKeys() {
		System.out.println("=> compareKeys");

		for (int i = 0; i < TEST_COMPARE_KEYS.length ; i++) {
			System.out.print("==> Key "+Integer.toString(i) + " : ");

			boolean result = compareKeys(TEST_COMPARE_KEYS[i][0],
						     TEST_COMPARE_KEYS[i][1]);

			if (result != TEST_COMPARE_KEYS_RESULTS[i]) {
				System.out.println("FAILED");
				System.out.println("Failed on :");
				System.out.println(TEST_COMPARE_KEYS[i][0]);
				System.out.println(TEST_COMPARE_KEYS[i][1]);
				System.out.println("Returned: "+Boolean.toString(result));
				return false;
			}

			System.out.println("Ok");
		}

		return true;
	}



	public static boolean isObsolete(String key) {

		if (key.startsWith("KSK"))
			return false;

		if (key.startsWith("SSK") || key.startsWith("USK")) {
			if (key.indexOf("AQABAAE") > 0)
				return true;

			return false;
		}

		if (key.startsWith("CHK")) {
			if (key.indexOf(",AAE") > 0)
				return true;

			return false;
		}

		return true;
	}


	public static void main(String[] args) {
		System.out.println("FreenetURIHelper tests:");

		if (!testIsAKey()
		    || !testCleanURI()
		    || !testGetFilenameFromKey()
		    || !testConvertSSKtoUSK()
		    || !testConvertUSKtoSSK()
		    || !testRevisionChange(true, TEST_CHANGE_SSK_REV)
		    || !testRevisionChange(false, TEST_CHANGE_USK_REV)
		    || !testGetUSKRevision()
		    || !testCompareKeys()) {

			System.out.println("FAILURE");
			return;

		}

		System.out.println("Tests successful");
	}
}

