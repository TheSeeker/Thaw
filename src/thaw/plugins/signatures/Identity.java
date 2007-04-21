package thaw.plugins.signatures;

import java.awt.Color;


import freenet.crypt.DSAPrivateKey;
import freenet.crypt.DSAGroup;
import freenet.crypt.DSAPublicKey;
import freenet.crypt.DSASignature;
import freenet.crypt.Yarrow;

import thaw.plugins.Hsqldb;


public class Identity {

	public final static int[] trustLevelInt = {
		100,
		2,
		1,
		0,
		-1,
		-2
	};

	public final static String[] trustLevelStr = {
		"thaw.plugin.signature.trustLevel.dev",
		"thaw.plugin.signature.trustLevel.good",
		"thaw.plugin.signature.trustLevel.observe",
		"thaw.plugin.signature.trustLevel.check",
		"thaw.plugin.signature.trustLevel.bad",
		"thaw.plugin.signature.trustLevel.evil"
	};

	public final static Color[] trustLevelColor = {
		Color.BLUE,
		Color.GREEN,
		new java.awt.Color(0, 128, 0), /* light green */
		Color.ORANGE,
		new java.awt.Color(175, 0, 0), /* moderatly red */
		Color.RED
	};


	private Hsqldb db;

	private int id;

	private String nick;


	/* public key */
	private byte[] y;

	/* private key */
	private byte[] x;


	public Identity(Hsqldb db, int id, String nick,
			byte[] y, byte[] x) {

	}


	/**
	 * Generate a new identity
	 * you have to insert() it
	 */
	public static Identity generate(String nick) {
		Yarrow y = new Yarrow();

		return null;
	}


	public void insert() {

	}



	/**
	 * All the parameters are Base64 encoded, except text.
	 */
	public static boolean isValid(String text, /* signed text */
				      String r, /* sig */
				      String s, /* sig */
				      String p, /* publicKey */
				      String q, /* publicKey */
				      String g, /* publicKey */
				      String y) /* publicKey */ {
		return true;
	}


	/**
	 * we use q as a reference
	 */
	public static boolean isDuplicata(Hsqldb db, String nickName, String q) {
		return false;
	}


}

