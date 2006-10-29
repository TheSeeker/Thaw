/*
 JDragTree.java / Frost
 Copyright (C) 2003  Frost Project <jtcfrost.sourceforge.net>

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation; either version 2 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/
package thaw.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.tree.*;

public class JDragTree extends JTree implements DragGestureListener, DragSourceListener {

	private static final long serialVersionUID = 1L;

	//	DropTargetListener interface object...
	private class CDropTargetListener implements DropTargetListener
	{

		 // Fields...
		 private TreePath        _pathLast       = null;
		 private Rectangle2D     _raCueLine      = new Rectangle2D.Float();
		 private Rectangle2D     _raGhost        = new Rectangle2D.Float();
		 private Color           _colorCueLine;
		 private Point           _ptLast         = new Point();
		 private javax.swing.Timer           _timerHover;
		 private int             _nLeftRight     = 0;    // Cumulative left/right mouse movement
		 private int             _nShift         = 0;

		 // Constructor...
		 public CDropTargetListener()
		 {
			 this._colorCueLine = new Color(
					 SystemColor.controlShadow.getRed(),
					 SystemColor.controlShadow.getGreen(),
					 SystemColor.controlShadow.getBlue(),
					 64
				   );

			 // Set up a hover timer, so that a node will be automatically expanded or collapsed
			 // if the user lingers on it for more than a short time
			 this._timerHover = new javax.swing.Timer(1000, new ActionListener()
			 {
				 public void actionPerformed(ActionEvent e)
				 {
					 CDropTargetListener.this._nLeftRight = 0;    // Reset left/right movement trend
					 if (JDragTree.this.isRootPath(CDropTargetListener.this._pathLast))
						 return; // Do nothing if we are hovering over the root node
					 if (JDragTree.this.isExpanded(CDropTargetListener.this._pathLast))
						 JDragTree.this.collapsePath(CDropTargetListener.this._pathLast);
					 else
						 JDragTree.this.expandPath(CDropTargetListener.this._pathLast);
				 }
			 });
			 this._timerHover.setRepeats(false);  // Set timer to one-shot mode
		 }

		 // DropTargetListener interface
		 public void dragEnter(DropTargetDragEvent e)
		 {
			 if (!this.isDragAcceptable(e))
				 e.rejectDrag();
			 else
				 e.acceptDrag(e.getDropAction());
		 }

		 public void dragExit(DropTargetEvent e)
		 {
			 if (!DragSource.isDragImageSupported())
			 {
				 JDragTree.this.repaint(this._raGhost.getBounds());
			 }
		 }

		 /**
		 * This is where the ghost image is drawn
		 */
		 public void dragOver(DropTargetDragEvent e)
		 {
             if( e==null || this._raGhost == null || this._ptLast == null ||
                 JDragTree.this._ptOffset == null || JDragTree.this._imgGhost == null || this._raCueLine == null ) {
                 return;
             }
			 // Even if the mouse is not moving, this method is still invoked 10 times per second
			 Point pt = e.getLocation();
             if(pt==null) {
                 return;
             }
			 if (pt.equals(this._ptLast))
				 return;

			 // Try to determine whether the user is flicking the cursor right or left
			 int nDeltaLeftRight = pt.x - this._ptLast.x;
			 if ( (this._nLeftRight > 0 && nDeltaLeftRight < 0) || (this._nLeftRight < 0 && nDeltaLeftRight > 0) )
				 this._nLeftRight = 0;
			 this._nLeftRight += nDeltaLeftRight;
			 this._ptLast = pt;
			 Graphics2D g2 = (Graphics2D) JDragTree.this.getGraphics();
             if( g2 == null ) {
                 return;
             }

			 // If a drag image is not supported by the platform, then draw my own drag image
			 if (!DragSource.isDragImageSupported())
			 {
				 JDragTree.this.paintImmediately(this._raGhost.getBounds()); // Rub out the last ghost image and cue line
				 // And remember where we are about to draw the new ghost image
				 this._raGhost.setRect(pt.x - JDragTree.this._ptOffset.x, pt.y - JDragTree.this._ptOffset.y, JDragTree.this._imgGhost.getWidth(), JDragTree.this._imgGhost.getHeight());
				 g2.drawImage(JDragTree.this._imgGhost, AffineTransform.getTranslateInstance(this._raGhost.getX(), this._raGhost.getY()), null);
			 }
			 else    // Just rub out the last cue line
				 JDragTree.this.paintImmediately(this._raCueLine.getBounds());

			 TreePath path = JDragTree.this.getClosestPathForLocation(pt.x, pt.y);
			 if (!(path == this._pathLast))
			 {
				 this._nLeftRight = 0;    // We've moved up or down, so reset left/right movement trend
				 this._pathLast = path;
				 this._timerHover.restart();
			 }

			 // In any case draw (over the ghost image if necessary) a cue line indicating where a drop will occur
			 Rectangle raPath = JDragTree.this.getPathBounds(path);
			 this._raCueLine.setRect(0,  raPath.y+(int)raPath.getHeight(), JDragTree.this.getWidth(), 2);

			 g2.setColor(this._colorCueLine);
			 g2.fill(this._raCueLine);

			 this._nShift = 0;

			 // And include the cue line in the area to be rubbed out next time
			 this._raGhost = this._raGhost.createUnion(this._raCueLine);

			 // Do this if you want to prohibit dropping onto the drag source
			 if (path.equals(JDragTree.this._pathSource))
				 e.rejectDrag();
			 else
				 e.acceptDrag(e.getDropAction());
		 }

		 public void dropActionChanged(DropTargetDragEvent e)
		 {
			 if (!this.isDragAcceptable(e))
				 e.rejectDrag();
			 else
				 e.acceptDrag(e.getDropAction());
		 }

		 public void drop(DropTargetDropEvent e)
		 {
			 this._timerHover.stop(); // Prevent hover timer from doing an unwanted expandPath or collapsePath

			 if (!this.isDropAcceptable(e))
			 {
				 e.rejectDrop();
				 return;
			 }

			 e.acceptDrop(e.getDropAction());

			 Transferable transferable = e.getTransferable();

			 DataFlavor[] flavors = transferable.getTransferDataFlavors();

			 for (int i = 0; i < flavors.length; i++ )
			 {
				 DataFlavor flavor = flavors[i];
				 if (flavor.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType))
				 {
					 try
					 {
						 Point pt = e.getLocation();
						 TreePath pathTarget = JDragTree.this.getClosestPathForLocation(pt.x, pt.y);
						 TreePath pathSource = (TreePath) transferable.getTransferData(flavor);

						 if( pathTarget == null || pathSource == null )
						 {
							 e.dropComplete(false);
							 return;
						 }

						 DefaultMutableTreeNode sourceNode = (DefaultMutableTreeNode)pathSource.getLastPathComponent();
						 DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode)sourceNode.getParent();

						 DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode)pathTarget.getLastPathComponent();
						 DefaultMutableTreeNode newParent = (DefaultMutableTreeNode)targetNode.getParent();

						 if( !sourceNode.isLeaf() && targetNode.getParent() == sourceNode )
						 {
							 // trying to drag a folder into its own childs
							 e.dropComplete(false);
							 return;
						 }

						 DefaultTreeModel model = (DefaultTreeModel)JDragTree.this.getModel();
						 TreePath pathNewChild = null;

						 if( targetNode.isLeaf() || JDragTree.this.isCollapsed(pathTarget) )
						 {
							 // collapsed tree node or leaf
							 // dropped on a leaf, insert into leaf's parent AFTER leaf
							 int idx = newParent.getIndex(targetNode);
							 if( idx < 0 )
							 {
								 logger.warning("child not found in parent!!!");
								 e.dropComplete(false);
								 return;
							 }
							 else
							 {
								 idx++; // insert AFTER targetNode

								 // remove node from oldParent ...
								 Object[] removedChilds = { sourceNode };
								 int[] childIndices = { oldParent.getIndex(sourceNode) };
								 sourceNode.removeFromParent();
								 model.nodesWereRemoved( oldParent, childIndices, removedChilds );

								 // ... and insert into newParent
								 if( idx >= newParent.getChildCount() )
								 {
									 newParent.add( sourceNode );
									 int insertedIndex[] = { newParent.getChildCount()-1 };
									 model.nodesWereInserted( newParent, insertedIndex );
								 }
								 else
								 {
									 newParent.insert(sourceNode, idx);
									 int insertedIndex[] = { idx };
									 model.nodesWereInserted( newParent, insertedIndex );
								 }
							 }
						 }
						 else
						 {
							 // expanded node, insert UNDER the node (before first child)
							 // remove node from oldParent ...
							 Object[] removedChilds = { sourceNode };
							 int[] childIndices = { oldParent.getIndex(sourceNode) };
							 sourceNode.removeFromParent();
							 model.nodesWereRemoved( oldParent, childIndices, removedChilds );
							 // ... and add to newParent
							 targetNode.insert( sourceNode, 0 );
							 int insertedIndex[] = { 0 };
							 model.nodesWereInserted( targetNode, insertedIndex );
						 }

						 if (pathNewChild != null)
							 JDragTree.this.setSelectionPath(pathNewChild); // Mark this as the selected path in the tree
						 break; // No need to check remaining flavors
					 }
					 catch (UnsupportedFlavorException ufe)
					 {
						 logger.log(Level.SEVERE, "Exception thrown in drop(DropTargetDropEvent e)", ufe);
						 e.dropComplete(false);
						 return;
					 }
					 catch (IOException ioe)
					 {
						 logger.log(Level.SEVERE, "Exception thrown in drop(DropTargetDropEvent e)", ioe);
						 e.dropComplete(false);
						 return;
					 }
				 }
			 }
			 e.dropComplete(true);
		 }

		 // Helpers...
		 public boolean isDragAcceptable(DropTargetDragEvent e)
		 {
			 // Only accept COPY or MOVE gestures (ie LINK is not supported)
			 if ((e.getDropAction() & DnDConstants.ACTION_MOVE) == 0)
				 return false;

			 // Only accept this particular flavor
			 if (!e.isDataFlavorSupported(TREEPATH_FLAVOR))
				 return false;

			 // Do this if you want to prohibit dropping onto the drag source...
			 Point pt = e.getLocation();
			 TreePath path = JDragTree.this.getClosestPathForLocation(pt.x, pt.y);
			 if(path == null || path.equals(JDragTree.this._pathSource))
				 return false;

			 return true;
		 }

		 public boolean isDropAcceptable(DropTargetDropEvent e)
		 {
			 // Only accept COPY or MOVE gestures (ie LINK is not supported)
			 if ((e.getDropAction() & DnDConstants.ACTION_MOVE) == 0)
				 return false;

			 // Only accept this particular flavor
			 if (!e.isDataFlavorSupported(TREEPATH_FLAVOR))
				 return false;

			 // Do this if you want to prohibit dropping onto the drag source...
			 Point pt = e.getLocation();
			 TreePath path = JDragTree.this.getClosestPathForLocation(pt.x, pt.y);
			 if( path == null || path.equals(JDragTree.this._pathSource))
				 return false;

			 return true;
		 }
	}

	/**
	* This represents a TreePath (a node in a JTree) that can be transferred between a drag source and a drop target.
	*/
	private class CTransferableTreePath implements Transferable
	{
		private TreePath        _path;
		/**
		* Constructs a transferrable tree path object for the specified path.
		*/
		public CTransferableTreePath(TreePath path)
		{
			this._path = path;
		}

		// Transferable interface methods...
		public DataFlavor[] getTransferDataFlavors()
		{
			return JDragTree.this._flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			return java.util.Arrays.asList(JDragTree.this._flavors).contains(flavor);
		}

		public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
		{
			if (flavor.isMimeTypeEqual(TREEPATH_FLAVOR.getMimeType()))
				return this._path;
			else
				throw new UnsupportedFlavorException(flavor);
		}
	}

	private static Logger logger = Logger.getLogger(JDragTree.class.getName());

	private TreePath        _pathSource;                // The path being dragged
	private BufferedImage   _imgGhost;                  // The 'drag image'
	private Point           _ptOffset = new Point();    // Where, in the drag image, the mouse was clicked

	// The type of DnD object being dragged...
	public final static DataFlavor TREEPATH_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "TreePath");
	private DataFlavor[]    _flavors = { TREEPATH_FLAVOR };

	private DragSource dragSource = null;
	private DragGestureRecognizer dgRecognizer = null;
	private DropTarget dropTarget = null;

	/**
	 * @param root
	 */
	public JDragTree(TreeNode root) {
		super(root);
		this.initialize();
	}

	/**
	 * @param root
	 */
	public JDragTree(TreeModel model) {
		super(model);
		this.initialize();
	}

	private void initialize() {
		// install drag n drop support
		this.dragSource = DragSource.getDefaultDragSource();
		this.dgRecognizer = this.dragSource.createDefaultDragGestureRecognizer(this,
																	 DnDConstants.ACTION_MOVE,
																	 this);
		// don't act on right mouse button
		this.dgRecognizer.setSourceActions(this.dgRecognizer.getSourceActions() & ~InputEvent.BUTTON3_MASK & ~InputEvent.BUTTON2_MASK);
		this.dropTarget = new DropTarget(this, new CDropTargetListener());
	}

	/**
	 * @param path
	 * @return
	 */
	private boolean isRootPath(TreePath path)
	{
		return this.isRootVisible() && this.getRowForPath(path) == 0;
	}

	// DragSourceListener interface methods
	public void dragDropEnd(DragSourceDropEvent e)
	{
		this.repaint();
	}

	public void dragEnter(DragSourceDragEvent e) {}

	public void dragExit(DragSourceEvent e) {}

	// DragGestureListener interface method
	public void dragGestureRecognized(DragGestureEvent e)
	{
		//we should make sure we aren't in edit mode
		InputEvent ievent=e.getTriggerEvent();
		if( ievent instanceof MouseEvent )
		{
			//even though I tell dgRecognizer to ignore the the right mouse button,
			// it thinks the RMB starts a drag event...argh
			if( (((MouseEvent)ievent).getModifiers() & InputEvent.BUTTON3_MASK) != 0 )
			{
				return;
			}
		}

		// begin dnd
		Point ptDragOrigin = e.getDragOrigin();
		TreePath path = this.getPathForLocation(ptDragOrigin.x, ptDragOrigin.y);
		if (path == null)
			return;
		if (this.isRootPath(path))
			return; // Ignore user trying to drag the root node

		// Work out the offset of the drag point from the TreePath bounding rectangle origin
		Rectangle raPath = this.getPathBounds(path);
		this._ptOffset.setLocation(ptDragOrigin.x-raPath.x, ptDragOrigin.y-raPath.y);

		// Get the cell renderer (which is a JLabel) for the path being dragged
		JLabel lbl = (JLabel) this.getCellRenderer().getTreeCellRendererComponent
								(
									this,                                           // tree
									path.getLastPathComponent(),                    // value
									false,                                          // isSelected   (dont want a colored background)
									this.isExpanded(path),                               // isExpanded
									this.getModel().isLeaf(path.getLastPathComponent()), // isLeaf
									0,                                              // row          (not important for rendering)
									false                                           // hasFocus     (dont want a focus rectangle)
								);
		lbl.setSize((int)raPath.getWidth(), (int)raPath.getHeight()); // <-- The layout manager would normally do this

		// Get a buffered image of the selection for dragging a ghost image
		this._imgGhost = new BufferedImage((int)raPath.getWidth(), (int)raPath.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D g2 = this._imgGhost.createGraphics();

		// Ask the cell renderer to paint itself into the BufferedImage
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));      // Make the image ghostlike
		lbl.paint(g2);

		// Now paint a gradient UNDER the ghosted JLabel text (but not under the icon if any)
		// Note: this will need tweaking if your icon is not positioned to the left of the text
		Icon icon = lbl.getIcon();
		int nStartOfText = (icon == null) ? 0 : icon.getIconWidth()+lbl.getIconTextGap();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, 0.5f)); // Make the gradient ghostlike
		g2.setPaint(new GradientPaint(nStartOfText, 0, SystemColor.controlShadow,
									  this.getWidth(),   0, new Color(255,255,255,0)));
		g2.fillRect(nStartOfText, 0, this.getWidth(), this._imgGhost.getHeight());
		g2.dispose();

		this.setSelectionPath(path); // Select this path in the tree

		// Wrap the path being transferred into a Transferable object
		Transferable transferable = new CTransferableTreePath(path);

		// Remember the path being dragged (because if it is being moved, we will have to delete it later)
		this._pathSource = path;

		// We pass our drag image just in case it IS supported by the platform
		e.startDrag(null, this._imgGhost, new Point(5,5), transferable, this);
	}

	public void dragOver(DragSourceDragEvent e) {}

	public void dropActionChanged(DragSourceDragEvent e) {}

}
