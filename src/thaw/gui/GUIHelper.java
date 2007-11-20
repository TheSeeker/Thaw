package thaw.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.text.JTextComponent;

import thaw.core.Logger;
import thaw.core.I18n;

public class GUIHelper {

	public GUIHelper() {

	}

	/**
	 * when actionPerformed() is called, it will fill in the specified
	 * text component with what is in the clipboard
	 */
	public static class PasteHelper implements ActionListener {
		JTextComponent txtComp;

		public PasteHelper(final AbstractButton src, final JTextComponent txtComp) {
			if (src != null)
				src.addActionListener(this);
			this.txtComp = txtComp;
		}

		public void actionPerformed(final ActionEvent evt) {
			GUIHelper.pasteToComponent(txtComp);
		}
	}

	public static void pasteToComponent(final JTextComponent txtComp) {
		final Toolkit tk = Toolkit.getDefaultToolkit();
		final Clipboard cp = tk.getSystemClipboard();

		String result;
		final Transferable contents = cp.getContents(null);

		final boolean hasTransferableText = ((contents != null) &&
					       contents.isDataFlavorSupported(DataFlavor.stringFlavor));

		try {
			if ( hasTransferableText ) {
				result = (String)contents.getTransferData(DataFlavor.stringFlavor);
				txtComp.setText(txtComp.getText() + result);
			} else {
				Logger.notice(new GUIHelper(), "Nothing to get from clipboard");
			}
		} catch(final java.awt.datatransfer.UnsupportedFlavorException e) {
			Logger.error(new GUIHelper(), "Error while pasting: UnsupportedFlavorException: "+e.toString());
		} catch(final java.io.IOException e) {
			Logger.error(new GUIHelper(), "Error while pasting: IOException: "+e.toString());
		}
	}


	public static void copyToClipboard(final String str) {
		final Toolkit tk = Toolkit.getDefaultToolkit();
		final StringSelection st = new StringSelection(str);
		final Clipboard cp = tk.getSystemClipboard();
		cp.setContents(st, null);
	}


	public static String getPrintableTime(final long seconds) {
		if (seconds == 0)
			return I18n.getMessage("thaw.common.unknown");

		if (seconds < 60)
			return (new Long(seconds)).toString() + " s";

		if (seconds < 3600) {
			final long min = seconds / 60;
			return ((new Long(min)).toString() + " min");
		}

		if (seconds < 86400) {
			final long hour = seconds / 3600;
			return ((new Long(hour)).toString() + " h");
		}

		final long day = seconds / 86400;
		return ((new Long(day)).toString()) + " day(s)";

	}


	public static String getPrintableSize(final long size) {
		if(size == 0)
			return I18n.getMessage("thaw.common.unknown");

		if(size < 1024) /* < 1KB */
			return ((new Long(size)).toString() + " B");

		if(size < 1048576) { /* < 1MB */
			final long kb = size / 1024;
			return ((new Long(kb)).toString() + " KB");
		}

		if(size < 1073741824) { /* < 1GB */
			final long mb = size / 1048576;
			return ((new Long(mb)).toString() + " MB");
		}

		final long gb = size / 1073741824;

		return ((new Long(gb)).toString() +" GB");
	}
}
