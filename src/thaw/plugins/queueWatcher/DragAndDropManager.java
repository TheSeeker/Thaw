package thaw.plugins.queueWatcher;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import thaw.core.Core;
import thaw.core.Logger;
import thaw.fcp.FCPTransferQuery;


public class DragAndDropManager implements DragGestureListener, DragSourceListener {
	private QueuePanel[] queuePanels;

	private DragSource dragSource;

	private final String tmpDir = System.getProperty("java.io.tmpdir");

	public DragAndDropManager(final Core core, final QueuePanel[] queuePanels) {
		this.queuePanels = queuePanels;

		dragSource = DragSource.getDefaultDragSource();

		for(int i = 0 ; i < queuePanels.length ; i++) {
			dragSource.createDefaultDragGestureRecognizer(queuePanels[i].getTable(),
									   DnDConstants.ACTION_COPY_OR_MOVE,
									   this);

			/* TODO: Finish DnD support */
			//queuePanels[i].getTable().setTransferHandler(new FileTransferHandler());
			//queuePanels[i].getTable().setDragEnabled(true);

		}
	}

	private class FileTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 1L;

		protected  Transferable createTransferable(final JComponent c) {
			if(c == queuePanels[0].getTable()) {
				queuePanels[0].reloadSelections();
				return new DragableFinishedTransfers(queuePanels[0].getSelectedQueries(), false);
			}

			if(c == queuePanels[1].getTable()) {
				queuePanels[1].reloadSelections();
				return new DragableFinishedTransfers(queuePanels[1].getSelectedQueries(), true);
			}

			return null;
		}

	}

	public void dragGestureRecognized(final DragGestureEvent dge) {
		try {
			Transferable transferable;

			transferable = getTransferableFor(dge.getComponent());

			dge.startDrag(DragSource.DefaultCopyDrop, transferable);


		} catch(final java.awt.dnd.InvalidDnDOperationException e) {
			Logger.warning(this, "InvalideDnDOperation !");
		}
	}



	private class DragableFinishedTransfers implements Transferable{
		public final DataFlavor[] FLAVORS = {
			DataFlavor.javaFileListFlavor,
			DataFlavor.stringFlavor,
		};

		private Vector queries; /* FCPTransferQuery */
		private boolean insert;

		public DragableFinishedTransfers(final Vector queries, final boolean insert) {
			if((queries == null) || (queries.size() <= 0)) {
				Logger.warning(this, "Selection null ?!");
			}

			this.queries = queries;
			this.insert = insert;
		}

		public Object getTransferData(final DataFlavor flavor) {
			if((flavor == DataFlavor.javaFileListFlavor)
			   || flavor.equals(DataFlavor.javaFileListFlavor) ) {

				final Vector fileList = new Vector();

				for(final Iterator queryIt = queries.iterator();
				    queryIt.hasNext();) {
					final FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

					if(!query.isFinished() || !query.isSuccessful())
						continue;

					if(query.getPath() == null) // We need a path !
						query.saveFileTo(tmpDir);

					fileList.add(new File(query.getPath()));
				}


				return fileList;
			}

			if((flavor == DataFlavor.stringFlavor)
			   || flavor.equals(DataFlavor.stringFlavor) ) {
				String result = "";

				for(final Iterator queryIt = queries.iterator();
				    queryIt.hasNext();) {
					final FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

					if(!query.isFinished() || !query.isSuccessful())
						continue;

					if(query.getPath() == null) // We need a path !
						continue;

					if(!insert)
						result = result +query.getPath()+"\n";
					else
						result = result + query.getFileKey() + "\n";
				}

				return result;
			}

			return null;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return FLAVORS;
		}

		public boolean isDataFlavorSupported(final DataFlavor flavor) {
			for(int i = 0 ; i < FLAVORS.length ; i++)
				if((FLAVORS[i] == flavor) || FLAVORS[i].equals(flavor))
					return true;

			return false;
		}
	}




	private Transferable getTransferableFor(final Component c) {

		if(c == queuePanels[0].getTable()) {
			queuePanels[0].reloadSelections();
			return new DragableFinishedTransfers(queuePanels[0].getSelectedQueries(), false);
		}

		if(c == queuePanels[1].getTable()) {
			queuePanels[1].reloadSelections();
			return new DragableFinishedTransfers(queuePanels[1].getSelectedQueries(), true);
		}

		return null;
	}

	public void dragEnter(final DragSourceDragEvent e) {

		final DragSourceContext context = e.getDragSourceContext();
		//intersection of the users selected action, and the source and target actions
		final int myaction = e.getDropAction();

		if( (myaction & DnDConstants.ACTION_COPY) != 0) {
			context.setCursor(DragSource.DefaultCopyDrop);
		} else {
			context.setCursor(DragSource.DefaultCopyNoDrop);
		}


	}

	public void dragOver(final DragSourceDragEvent e) { }
	public void dragExit(final DragSourceEvent e) { }

	public void dragDropEnd(final DragSourceDropEvent e) {
	}

	public void dropActionChanged (final DragSourceDragEvent e) { }

}
