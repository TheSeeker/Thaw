package thaw.plugins.miniFrost;

import java.util.Enumeration;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;


public class BoardFolder implements MutableTreeNode {

	public Enumeration children() {
		return null;
	}

	public boolean getAllowsChildren() {
		return true;
	}

	public TreeNode getChildAt(int childIndex) {
		return null;
	}

	public int getChildCount() {
		return 0;
	}

	public int getIndex(TreeNode node) {
		return -1;
	}

	public TreeNode getParent() {
		return null;
	}

	public boolean isLeaf() {
		return false;
	}


	public void insert(MutableTreeNode child, int index) {

	}

	public void remove(int index) {

	}

	public void remove(MutableTreeNode node) {

	}

	public void removeFromParent() {

	}

	public void setParent(MutableTreeNode newParent) {

	}

	public void setUserObject(Object object) {

	}


	/**
	 * must store this state in the db
	 */
	public void setFolded(boolean folded) {

	}

	/**
	 * value from the bdd (can be cached)
	 */
	public boolean isFolded() {
		return true;
	}
}
