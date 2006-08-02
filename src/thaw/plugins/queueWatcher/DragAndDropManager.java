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
import thaw.plugins.QueueWatcher;
import thaw.fcp.*;
import thaw.i18n.I18n;


public class DragAndDropManager implements DragGestureListener, DragSourceListener {

	private Core core;
	private QueuePanel[] queuePanels;

	private DragSource dragSource;
	private DragGestureListener dgListener;

	private String tmpDir = System.getProperty("java.io.tmpdir");

	public DragAndDropManager(Core core, QueuePanel[] queuePanels) {
		this.core = core;
		this.queuePanels = queuePanels;

		dragSource = DragSource.getDefaultDragSource();

		for(int i = 0 ; i < queuePanels.length ; i++) {
			this.dragSource.createDefaultDragGestureRecognizer(queuePanels[i].getTable(),
									   DnDConstants.ACTION_COPY_OR_MOVE,
									   this);
			
			//queuePanels[i].getTable().setTransferHandler(new FileTransferHandler());
			//queuePanels[i].getTable().setDragEnabled(true);

		}
	}


	private class FileTransferHandler extends TransferHandler {

		protected  Transferable createTransferable(JComponent c) {
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

	public void dragGestureRecognized(DragGestureEvent dge) {
		try {
			Transferable transferable;

			transferable = getTransferableFor(dge.getComponent());

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
			return queries;
		}

		public Object getTransferData(DataFlavor flavor) {
			if(flavor == DataFlavor.javaFileListFlavor
			   || flavor.equals(DataFlavor.javaFileListFlavor) ) {

				Vector fileList = new Vector();

				for(Iterator queryIt = queries.iterator();
				    queryIt.hasNext();) {
					FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

					if(!query.isFinished() || !query.isSuccessful())
						continue;

					if(query.getPath() == null) // We need a path !
						query.saveFileTo(tmpDir);

					fileList.add(new File(query.getPath()));
				}


				return fileList;
			}

			if(flavor == DataFlavor.stringFlavor
			   || flavor.equals(DataFlavor.stringFlavor) ) {
				String result = "";

				for(Iterator queryIt = queries.iterator();
				    queryIt.hasNext();) {
					FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

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

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			for(int i = 0 ; i < FLAVORS.length ; i++)
				if(FLAVORS[i] == flavor || FLAVORS[i].equals(flavor))
					return true;

			return false;
		}
	}




	private Transferable getTransferableFor(Component c) {

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
