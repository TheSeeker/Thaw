package thaw.plugins.webOfTrust;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import thaw.gui.GUIHelper;
import thaw.plugins.signatures.Identity;

public class WotGraphNode {
	public final static int MAX_DEPTH = 50; /* just a security */
	
	public final static int MAX_DISTANCE = 500;
	public final static int MIN_DISTANCE = 50;
	
	public final static int NODE_RADIUS = 4;
	
	public final static int MAX_OPTIMIZATION_CYCLES = 30;
	
	private WebOfTrustGraph graph;
	private WotIdentity identity;
	private int x = -1;
	private int y = -1;
	
	private WotGraphNode[] neighbours;
	private int[] trustLinks;
	
	private final static Random random = new Random();
	
	private boolean selected = false;
	
	public WotGraphNode(WebOfTrustGraph graph, WotIdentity i) {
		this.graph = graph;
		this.identity = i;
		x = -1;
		y = -1;
	}
	
	public Identity getIdentity() {
		return identity;
	}
	
	public void setSelected(boolean s) {
		this.selected = s;
	}
	
	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	private int getAValueNear(int x) {
		int diff = (random.nextInt(MAX_DISTANCE-MIN_DISTANCE))+MIN_DISTANCE;
		boolean pos = random.nextBoolean();
		
		if (!pos)
			return x + (-1 * diff);
		return x + diff;
	}
	
	public void setAlmostRandomPosition(WotGraphNode parent) {
		this.x = getAValueNear(parent.getX());
		this.y = getAValueNear(parent.getY());
	}
	
	public void generateNeighbours() {
		generateNeighbours(0);
	}
	
	public void generateNeighbours(int depth) {
		if (depth == MAX_DEPTH)
			return;
		
		/* search neighbours */
		Vector trustList = identity.getTrustList();
		
		neighbours = new WotGraphNode[trustList.size()];
		trustLinks = new int[trustList.size()];
		
		int i = 0;
		
		for (Iterator it = trustList.iterator() ; it.hasNext() ; ) {
			WotIdentity.TrustLink link = (WotIdentity.TrustLink)it.next();
			neighbours[i] = new WotGraphNode(graph, link.getDestination());
			trustLinks[i] = link.getLinkTrustLevel();

			i++;
		}
		
		/* synchronizing with the already existing nodes */
		for (i = 0 ; i < neighbours.length ; i++) {
			WotGraphNode node = graph.getNode(neighbours[i].getIdentity().getPublicKey());
			
			if (node != null) {
				neighbours[i] = node;
			} else {
				neighbours[i].setAlmostRandomPosition(this);
				neighbours[i].generateNeighbours(depth+1); /* recursivity */
				graph.addNode(neighbours[i]);
			}
		}
	}
	
	public int nmbNeighbours() {
		return neighbours.length;
	}
	
	public int getAverageX() {
		int sum = 0;
		
		for (int i = 0 ; i < neighbours.length ; i++)
			sum += neighbours[i].getX();
		
		return sum / neighbours.length;
	}
	
	public int getAverageY() {
		int sum = 0;
		
		for (int i = 0 ; i < neighbours.length ; i++)
			sum += neighbours[i].getY();
		
		return sum / neighbours.length;
	}
	
	public void switchPosition(WotGraphNode node) {
		int newX = node.getX();
		int newY = node.getY();
		
		node.setPosition(x, y);
		setPosition(newX, newY);
	}
	
	
	public void optimizePositions() {
		for (int i = 0 ; i < MAX_OPTIMIZATION_CYCLES ;i++) {
			boolean hasMove = false;
			
			for (int j = 0; j < neighbours.length ; j++) {
				if (neighbours[j].nmbNeighbours() <= 0)
					continue;
				
				int x = neighbours[j].getAverageX();
				int y = neighbours[j].getAverageY();
				
				WotGraphNode nearest = graph.getNearestNode(x, y);
				
				if (nearest != neighbours[j]) {
					switchPosition(nearest);
					hasMove = true;
				}
			}
			
			if (!hasMove)
				break;
		}
		
		for (int j = 0; j < neighbours.length ; j++) {
			neighbours[j].optimizePositions();
		}
	}
	
	
	public void paintNode(Graphics g, float zoom, int zeroX, int zeroY) {
		int realX = (int)(x * zoom);
		int realY = (int)(y * zoom);
		
		g.setColor(identity.getTrustLevelColor());
		
		if (selected) {
			g.drawOval( realX - NODE_RADIUS + zeroX,
					realY - NODE_RADIUS + zeroY,
					2*NODE_RADIUS,
					2*NODE_RADIUS);
		} else {
			g.fillOval( realX - NODE_RADIUS + zeroX,
					realY - NODE_RADIUS + zeroY,
					2*NODE_RADIUS,
					2*NODE_RADIUS);
		}

		g.drawString(identity.toString(),
				realX + zeroX,
				realY + zeroY - 10);
	}
	
	private void paintLink(Graphics g, WotGraphNode neighbour, int trustLink,
							int myX, int myY, 
							float zoom, int zeroX, int zeroY) {
		int targetX = (int)(neighbour.getX()*zoom);
		int targetY = (int)(neighbour.getY()*zoom);

		if (trustLink < 0)
			g.setColor(Color.RED);
		else if (trustLink > 0)
			g.setColor(Color.BLUE);

		GUIHelper.paintArrow(g, targetX+zeroX, targetY+zeroY, myX, myY);
	}
	
	
	public void paintLinks(Graphics g, float zoom, int zeroX, int zeroY) {
		int realX = (int)(x * zoom)+zeroX;
		int realY = (int)(y * zoom)+zeroY;
		
		for (int i = 0 ; i < neighbours.length ; i++) {
			paintLink(g, neighbours[i], trustLinks[i], realX, realY, zoom, zeroX, zeroY);
		}
	}

}
