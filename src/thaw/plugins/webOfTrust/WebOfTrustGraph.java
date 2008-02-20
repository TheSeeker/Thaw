package thaw.plugins.webOfTrust;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import thaw.plugins.Hsqldb;
import thaw.plugins.signatures.Identity;

public class WebOfTrustGraph extends JComponent implements MouseListener {
	public final static int BORDER = 40;
	
	private static final long serialVersionUID = 7654372155349615901L;
	
	private JScrollPane myScrollPane;
	
	private boolean visible;
	
	private Hashtable nodes;
	private WotGraphNode center;
	private WotGraphNode selectedNode;

	
	private float zoom = 1;
	private int minX = 0;
	private int maxX = 1;
	private int minY = 0;
	private int maxY = 1;
	private int zeroX = 200;
	private int zeroY = 200;
	

	public WebOfTrustGraph(Hsqldb db) {
		nodes = new Hashtable();
		
		visible = false;
		
		addMouseListener(this);
	}

	public void setScrollPane(JScrollPane pane) {
		this.myScrollPane = pane;
	}

	public void redraw(Identity c) {
		this.center = new WotGraphNode(this, new WotIdentity(c));
		
		if (!visible)
			return;

		nodes = new Hashtable();
		
		this.center.setPosition(0, 0);
		this.center.generateNeighbours();
		this.center.optimizePositions();
		addNode(this.center);
		
		if (myScrollPane != null)
			myScrollPane.revalidate();
		
		computeMinMax();
		computeOptimalZoom();
		recomputeSize();

		repaint();
	}
	
	private void computeMinMax() {
		minX = 0;
		minY = 0;
		maxX = 1;
		maxY = 1;
		
		synchronized(nodes) {
			for (Iterator it = nodes.values().iterator() ; it.hasNext() ; ) {
				WotGraphNode n = (WotGraphNode)it.next();
				
				if (n.getX() < minX) minX = n.getX();
				if (n.getX() > maxX) maxX = n.getX();
				if (n.getY() < minY) minY = n.getY();
				if (n.getY() > maxY) maxY = n.getY();
			}
		}
	}
	
	private void recomputeSize() {
		Dimension dim = new Dimension((int)((maxX - minX) * zoom) + (2*BORDER),
				(int)((maxY - minY) * zoom) + (2*BORDER));

		this.setPreferredSize(dim);
		this.setSize((int)((maxX - minX) * zoom) + (2*BORDER),
				(int)((maxY - minY) * zoom) + (2*BORDER));
		myScrollPane.revalidate();
	}
	
	public void computeOptimalZoom() {
		Dimension size = myScrollPane.getSize();
		double zoomX = (size.getWidth()-(2*BORDER)) / (maxX - minX);
		double zoomY = (size.getHeight()-(2*BORDER)) / (maxY - minY);

		zoom = (float)(((zoomX > zoomY) ? zoomY : zoomX));
		
		if (zoom == 0)
			zoom = 1;
		
		recomputeZero();
	}
	
	private void recomputeZero() {
		zeroX = (-1 * (int)(minX * zoom)) + BORDER;
		zeroY = (-1 * (int)(minY * zoom)) + BORDER;
	}
	
	public float getZoom() {
		return zoom;
	}
	
	public void setZoom(float z) {
		this.zoom = z;
		recomputeSize();
		recomputeZero();
		repaint();		
	}
	
	public WotGraphNode getNode(String publicKey) {
		synchronized(nodes) {
			return (WotGraphNode)nodes.get(publicKey);
		}
	}
	
	public WotGraphNode getNearestNode(int x, int y) {
		WotGraphNode selected = null;
		float lastDist = 999999999;
		
		synchronized(nodes) {
			for (Iterator it = nodes.values().iterator() ; it.hasNext() ; ) {
				WotGraphNode n = (WotGraphNode)it.next();
				float dist = (float)new Point2D.Float(n.getX(), n.getY()).distance(x, y);
				
				if (dist < lastDist) {
					selected = n;
					lastDist = dist;
				}
			}
		}
		
		return selected;
	}
	
	public void addNode(WotGraphNode node) {
		synchronized(nodes) {
			nodes.put(node.getIdentity().getPublicKey(), node);
		}
	}
	
	public void paintComponent(Graphics g) {
		/* background */
		Dimension d = getSize();
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(0, 0, (int)d.getWidth(), (int)d.getHeight());
		
		synchronized(nodes) {
			for (Iterator it = nodes.values().iterator() ; it.hasNext() ; ) {
				((WotGraphNode)it.next()).paintLinks(g, zoom, zeroX, zeroY);
			}
			
			for (Iterator it = nodes.values().iterator() ; it.hasNext() ; ) {
				((WotGraphNode)it.next()).paintNode(g, zoom, zeroX, zeroY);
			}
		}
	}

	public void mouseClicked(MouseEvent e) {
		int x = (int)((e.getX() - zeroX) / zoom);
		int y = (int)((e.getY() - zeroY) / zoom);
		
		WotGraphNode node = getNearestNode(x, y);
		
		synchronized(nodes) {
			for (Iterator it = nodes.values().iterator() ; it.hasNext() ; ) {
				WotGraphNode n = (WotGraphNode)it.next();
				n.setSelected(n == node);
			}
		}
		
		selectedNode = node;
		
		repaint();
	}

	public void mouseEntered(MouseEvent arg0) {	}

	public void mouseExited(MouseEvent arg0) { }

	public void mousePressed(MouseEvent arg0) {
		mouseClicked(arg0);
	}

	public void mouseReleased(MouseEvent e) {
		int x = (int)((e.getX() - zeroX) / zoom);
		int y = (int)((e.getY() - zeroY) / zoom);

		selectedNode.setPosition(x, y);
		repaint();
	}
	
	public void setVisible(boolean v) {
		visible = v;
		
		if (v && center != null) {
			myScrollPane.revalidate();
			redraw(center.getIdentity());
		}
	}
}
