package thaw.plugins.indexWebGrapher;

import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Dimension;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Iterator;

import thaw.plugins.IndexWebGrapher;


public class GraphPanel extends JComponent implements MouseListener {

	public final static int BORDER = 50;

	private Hashtable nodeHashtable;
	private Vector nodeList;

	private int lastId;

	private double zoom;

	private IndexWebGrapher plugin;

	private Node lastSelectedNode;

	public GraphPanel(IndexWebGrapher plugin) {
		super();
		this.plugin = plugin;
		nodeHashtable = null;
		nodeList = null;
		lastId = 0;
		setPreferredSize(new Dimension(10, 10));
		this.addMouseListener(this);
	}

	public void reinit() {
		lastSelectedNode = null;
		nodeHashtable = new Hashtable();
		nodeList = new Vector();
		lastId = 0;
	}

	public void registerNode(Node node) {
		nodeHashtable.put(node.getIndexKey().substring(4, 40),
				  node);
		nodeList.add(node);

		if (node.getId() > lastId)
			lastId = node.getId();
	}

	public Node getNode(String key) {
		return (Node)nodeHashtable.get(key.substring(4, 40));
	}

	public Vector getNodeList() {
		return nodeList;
	}

	public int getLastUsedId() {
		return lastId;
	}


	public void setZoom(double zoom) {
		this.zoom = zoom;
	}

	private double minX = 0;
	private double maxX = 0;
	private double minY = 0;
	private double maxY = 0;


	public void guessZoom() {
		minX = 0;
		maxX = 0;
		minY = 0;
		maxY = 0;

		for (Iterator it = nodeList.iterator();
		     it.hasNext();) {
			Node node = (Node)it.next();

			if (node.getX() < minX) minX = node.getX();
			if (node.getX() > maxX) maxX = node.getX();
			if (node.getY() < minY) minY = node.getY();
			if (node.getY() > maxY) maxY = node.getY();
		}

		Dimension size = plugin.getScrollPane().getSize();

		double zoomX = (size.getWidth()-(2*BORDER)) / (maxX - minX);
		double zoomY = (size.getHeight()-(2*BORDER)) / (maxY - minY);

		zoom = ((zoomX > zoomY) ? zoomY : zoomX);
	}

	public void zoomIn() {
		zoom = zoom * 2.0;
		refresh();
	}

	public void zoomOut() {
		zoom = zoom / 2.0;
		refresh();
	}

	public void refresh() {
		Dimension dim = new Dimension((int)((maxX - minX) * zoom) + (2*BORDER),
					      (int)((maxY - minY) * zoom) + (2*BORDER));

		this.setPreferredSize(dim);
		this.setSize((int)((maxX - minX) * zoom) + (2*BORDER),
			     (int)((maxY - minY) * zoom) + (2*BORDER));
		plugin.getScrollPane().revalidate();
		repaint();
	}


	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Dimension d = getSize();
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(0, 0, (int)d.getWidth(), (int)d.getHeight());

		if (nodeList == null)
			return;

		int zeroX = (-1 * (int)(minX * zoom)) + BORDER;
		int zeroY = (-1 * (int)(minY * zoom)) + BORDER;

		for (Iterator it = nodeList.iterator();
		     it.hasNext();) {
			((Node)it.next()).paintTaNodeFaceDeNoeud(g, zoom, zeroX, zeroY);
		}
	}


	public void clicked(int x, int y) {
		int zeroX = (-1 * (int)(minX * zoom)) + BORDER;
		int zeroY = (-1 * (int)(minY * zoom)) + BORDER;

		if (lastSelectedNode != null)
			lastSelectedNode.setSelected(false);

		Node bestPlaced = null;
		double bestDist = 0.0;

		for (Iterator it = nodeList.iterator();
		     it.hasNext();) {
			Node node = (Node)it.next();

			if (bestPlaced == null) {
				bestPlaced = node;
				double diffBestX = Math.abs(bestPlaced.getXPixel(zoom, zeroX)-x);
				double diffBestY = Math.abs(bestPlaced.getYPixel(zoom, zeroY)-y);

				bestDist = Math.sqrt(Math.pow(diffBestX, 2)+Math.pow(diffBestY, 2));
			} else {
				double diffNodeX = Math.abs(node.getXPixel(zoom, zeroX)-x);
				double diffNodeY = Math.abs(node.getYPixel(zoom, zeroY)-y);

				double distNode = Math.sqrt(Math.pow(diffNodeX, 2) + Math.pow(diffNodeY, 2));

				if (distNode < bestDist) {
					bestPlaced = node;
					bestDist = distNode;
				}
			}
		}

		if (bestPlaced != null) {
			bestPlaced.setSelected(true);
			lastSelectedNode = bestPlaced;
		}

		repaint();
	}

	public void mouseClicked(final MouseEvent e) {
		clicked(e.getX(), e.getY());
	}

	public void mouseEntered(final MouseEvent e) { }
	public void mouseExited(final MouseEvent e) { }

	public void mousePressed(final MouseEvent e) {
		clicked(e.getX(), e.getY());
	}

	public void mouseReleased(final MouseEvent e) {
		clicked(e.getX(), e.getY());
	}
}
