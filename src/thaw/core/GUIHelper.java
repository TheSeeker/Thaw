package thaw.core;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.text.JTextComponent;

public class GUIHelper {

	public GUIHelper() {

	}

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

}
