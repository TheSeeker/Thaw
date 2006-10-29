package thaw.plugins.queueWatcher;

import java.awt.dnd.DragSource;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DnDConstants;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceContext;
import javax.swing.TransferHandler;

import java.awt.Component;
import javax.swing.JComponent;

import java.util.Vector;
import java.util.Iterator;
import java.io.File;

import thaw.core.*;
import thaw.fcp.*;


public class DragAndDropManager implements DragGestureListener, DragSourceListener {

	private Core core;
	private QueuePanel[] queuePanels;

	private DragSource dragSource;
	private DragGestureListener dgListener;

	private String tmpDir = System.getProperty("java.io.tmpdir");

	public DragAndDropManager(Core core, QueuePanel[] queuePanels) {
		this.core = core;
		this.queuePanels = queuePanels;

		this.dragSource = DragSource.getDefaultDragSource();

		for(int i = 0 ; i < queuePanels.length ; i++) {
			this.dragSource.createDefaultDragGestureRecognizer(queuePanels[i].getTable(),
									   DnDConstants.ACTION_COPY_OR_MOVE,
									   this);

			/* TODO: Finish DnD support */
			//queuePanels[i].getTable().setTransferHandler(new FileTransferHandler());
			//queuePanels[i].getTable().setDragEnabled(true);

		}
	}


	private class FileTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 1L;

		protected  Transferable createTransferable(JComponent c) {
			if(c == DragAndDropManager.this.queuePanels[0].getTable()) {
				DragAndDropManager.this.queuePanels[0].reloadSelections();
				return new DragableFinishedTransfers(DragAndDropManager.this.queuePanels[0].getSelectedQueries(), false);
			}

			if(c == DragAndDropManager.this.queuePanels[1].getTable()) {
				DragAndDropManager.this.queuePanels[1].reloadSelections();
				return new DragableFinishedTransfers(DragAndDropManager.this.queuePanels[1].getSelectedQueries(), true);
			}

			return null;
		}

	}

	public void dragGestureRecognized(DragGestureEvent dge) {
		try {
			Transferable transferable;

			transferable = this.getTransferableFor(dge.getComponent());

			dge.startDrag(DragSource.DefaultCopyDrop, transferable);


		} catch(java.awt.dnd.InvalidDnDOperationException e) {
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

		public DragableFinishedTransfers(Vector queries, boolean insert) {
			if(queries == null || queries.size() <= 0) {
				Logger.warning(this, "Selection null ?!");
			}

			this.queries = queries;
			this.insert = insert;
		}


		private Vector getQueries() {
			return this.queries;
		}

		public Object getTransferData(DataFlavor flavor) {
			if(flavor == DataFlavor.javaFileListFlavor
			   || flavor.equals(DataFlavor.javaFileListFlavor) ) {

				Vector fileList = new Vector();

				for(Iterator queryIt = this.queries.iterator();
				    queryIt.hasNext();) {
					FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

					if(!query.isFinished() || !query.isSuccessful())
						continue;

					if(query.getPath() == null) // We need a path !
						query.saveFileTo(DragAndDropManager.this.tmpDir);

					fileList.add(new File(query.getPath()));
				}


				return fileList;
			}

			if(flavor == DataFlavor.stringFlavor
			   || flavor.equals(DataFlavor.stringFlavor) ) {
				String result = "";

				for(Iterator queryIt = this.queries.iterator();
				    queryIt.hasNext();) {
					FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

					if(!query.isFinished() || !query.isSuccessful())
						continue;

					if(query.getPath() == null) // We need a path !
						continue;

					if(!this.insert)
						result = result +query.getPath()+"\n";
					else
						result = result + query.getFileKey() + "\n";
				}

				return result;
			}

			return null;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return this.FLAVORS;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			for(int i = 0 ; i < this.FLAVORS.length ; i++)
				if(this.FLAVORS[i] == flavor || this.FLAVORS[i].equals(flavor))
					return true;

			return false;
		}
	}




	private Transferable getTransferableFor(Component c) {

		if(c == this.queuePanels[0].getTable()) {
			this.queuePanels[0].reloadSelections();
			return new DragableFinishedTransfers(this.queuePanels[0].getSelectedQueries(), false);
		}

		if(c == this.queuePanels[1].getTable()) {
			this.queuePanels[1].reloadSelections();
			return new DragableFinishedTransfers(this.queuePanels[1].getSelectedQueries(), true);
		}

		return null;
	}

	public void dragEnter(DragSourceDragEvent e) {

		DragSourceContext context = e.getDragSourceContext();
		//intersection of the users selected action, and the source and target actions
		int myaction = e.getDropAction();

		if( (myaction & DnDConstants.ACTION_COPY) != 0) {
			context.setCursor(DragSource.DefaultCopyDrop);
		} else {
			context.setCursor(DragSource.DefaultCopyNoDrop);
		}


	}

	public void dragOver(DragSourceDragEvent e) { }
	public void dragExit(DragSourceEvent e) { }

	public void dragDropEnd(DragSourceDropEvent e) {
	}

	public void dropActionChanged (DragSourceDragEvent e) { }

}
