/*
 * Thaw - A tool to insert and fetch files on freenet
 * by Jerome Flesch
 * 2006(c) Freenet project
 * ==================================================
 *
 * This file was originally created by David Roden and next was adapted to Thaw by Jerome Flesch.
 *
 * Copyright (C) 2006 David Roden
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package thaw.core;

import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author David Roden &lt;droden@gmail.com&gt;
 * @version $Id: I18n.java 355 2006-03-24 15:04:11Z bombe $
 */
public class I18n {

	private static Locale currentLocale;

	public I18n() {

	}

	public static Locale getLocale() {
		if (currentLocale == null)
			currentLocale = Locale.getDefault();
		return currentLocale;
	}

	public static void setLocale(Locale locale) {
		currentLocale = locale;
		Locale.setDefault(locale);
	}

	public static ResourceBundle getResourceBundle() {
		return getResourceBundle(getLocale());
	}

	public static ResourceBundle getResourceBundle(Locale locale) {
		return ResourceBundle.getBundle("thaw.i18n.thaw", getLocale());
	}

	public static String getMessage(String key) {
		try {
			return getResourceBundle().getString(key);
		} catch(Exception e) {
			Logger.warning(new I18n(),/* we need a ref -> random ref -> this is *bad* */
				       "Unable to find translation for '"+key+"'");
			return key;
		}
	}

}
