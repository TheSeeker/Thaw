package thaw.plugins.indexWebGrapher;

import java.util.Vector;
import java.util.Iterator;

import java.awt.Graphics;
import java.awt.Color;

import thaw.core.Logger;


public class Node implements Comparable {

	public final static int RADIUS = 4;

	private int id;
	private int indexId;
	private String indexName;
	private String indexKey;
	private GraphPanel graphPanel;

	private Vector linkTo;
	private Vector linkedFrom;

	/**
	 * @param indexId -1 if none
	 */
	public Node(int id,
		    int indexId,
		    String indexName,
		    String indexKey,
		    GraphPanel graphPanel) {
		this.id = id;
		this.indexId = indexId;
		this.indexName = indexName;
		this.indexKey = indexKey;
		this.graphPanel = graphPanel;

		linkTo = new Vector();
		linkedFrom = new Vector();

		graphPanel.registerNode(this);
	}


	public int getId() {
		return id;
	}

	public int getIndexId() {
		return indexId;
	}

	public String getIndexKey() {
		return indexKey;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setLinkTo(Node node) {
		linkTo.add(node);
		node.setLinkedFrom(this);
	}

	private void setLinkedFrom(Node node) {
		linkedFrom.add(node);
	}

	public Vector getLinkList() {
		return linkTo;
	}

	public int getLinkCount() {
		return linkTo.size() + linkedFrom.size();
	}

	/**
	 * we want the nodes with the higher number of
	 * links first => we consider them smaller
	 */
	public int compareTo(final Object o) {
		final Node node = (Node)o;

		if (node.getLinkCount() < getLinkCount())
			return -1;
		if (node.getLinkCount() > getLinkCount())
			return 1;

		return 0;
	}


	private boolean posSet = false;
	private double x = -1;
	private double y = -1;

	public boolean isPositionSet() {
		return posSet;
	}


	public void setPosition(double x, double y) {
		posSet = true;
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}


	private double velocityX = 0.0;
	private double velocityY = 0.0;

	public final static double TIMESTEP = 0.1;

	private double[] attraction(Node node) {
		return new double[] {0, 0};
	}

	private double[] repulsion(Node node) {
		return new double[] {0, 0};
	}

	/**
	 * see http://en.wikipedia.org/wiki/Force-based_algorithms
	 */
	public void computeVelocity(Vector nodeList) {
		double netForceX = 0.0;
		double netForceY = 0.0;

		/* repulsion */
		for (Iterator it = nodeList.iterator();
		     it.hasNext();) {
			Node node = (Node)it.next();

			if (node == this
			    || linkTo.indexOf(node) >= 0
			    || linkedFrom.indexOf(node) >= 0)
				continue;

			double[] repuls = repulsion(node);
			netForceX += repuls[0];
			netForceY += repuls[1];
		}

		/* attraction */
		for (Iterator it = linkTo.iterator();
		     it.hasNext();) {
			Node node = (Node)it.next();

			if (node == this)
				continue;

			double[] attr = attraction(node);
			netForceX += attr[0];
			netForceY += attr[1];
		}

		/* attraction */
		for (Iterator it = linkedFrom.iterator();
		     it.hasNext();) {
			Node node = (Node)it.next();

			if (node == this)
				continue;

			double[] attr = attraction(node);
			netForceX += attr[0];
			netForceY += attr[1];
		}

		velocityX += netForceX;
		velocityY += netForceY;
	}

	/**
	 * @return true if moved
	 */
	public boolean applyVelocity() {
		if (velocityX == 0 && velocityY == 0)
			return false;

		x += velocityX * TIMESTEP;
		y += velocityY * TIMESTEP;

		return true;
	}


	/**
	 * Recursivity : Dirty, but easier :P
	 */
	public void setInitialNeightboorPositions() {
		int unplaced = 0;

		for (Iterator it = linkTo.iterator();
		     it.hasNext(); ) {
			Node node = (Node)it.next();

			if (!node.isPositionSet())
				unplaced++;
		}

		if (unplaced == 0)
			return;

		double step = (2*Math.PI) / unplaced;

		double current = 0;

		for (Iterator it = linkTo.iterator();
		     it.hasNext();) {
			Node node = (Node)it.next();

			if (!node.isPositionSet()) {
				double diffX = Math.cos(current) * (unplaced+1);
				double diffY = Math.sin(current) * (unplaced+1);

				node.setPosition(x + diffX,
						 y + diffY);

				node.setInitialNeightboorPositions();

				current += step;
			}
		}

		return;
	}


	public boolean linkTo(Node node) {
		return (linkTo.indexOf(node) >= 0);
	}

	private boolean selected = false;

	public void setSelected(boolean sel) {
		this.selected = sel;
	}

	public boolean isSelected() {
		return selected;
	}

	public double getXPixel(double zoom, int zeroX) {
		return (double)((int)(x*zoom) + zeroX);
	}

	public double getYPixel(double zoom, int zeroY) {
		return (double)((int)(y*zoom) + zeroY);
	}


	public void paintTaNodeFaceDeNoeud(Graphics g, double zoom,
					   int zeroX, int zeroY) {

		int realX = (int)(x*zoom);
		int realY = (int)(y*zoom);

		if (selected)
			g.setColor(Color.ORANGE);
		else
			g.setColor(Color.GRAY);

		for (Iterator it = linkTo.iterator();
		     it.hasNext();) {
			Node target = (Node)it.next();

			int targetX = (int)(target.getX()*zoom);
			int targetY = (int)(target.getY()*zoom);

			if (target.isSelected()) {
				g.setColor(Color.CYAN);
			}

			if ( (target.isSelected() || selected)
			     && target.linkTo(this) ) {
				g.setColor(Color.PINK);
			}

			g.drawLine(realX+zeroX, realY+zeroY, targetX+zeroX, targetY+zeroY);

			if (target.isSelected())
				g.setColor(Color.GRAY);
		}


		if (selected)
			g.setColor(Color.RED);
		else
			g.setColor(Color.GREEN);

		g.fillOval( realX - RADIUS + zeroX,
			    realY - RADIUS + zeroY,
			    2*RADIUS,
			    2*RADIUS);


		if (selected) {
			g.setColor(Color.BLACK);

			g.drawString(indexName,
				     realX + zeroX,
				     realY + zeroY - 10);
		}
	}
}
