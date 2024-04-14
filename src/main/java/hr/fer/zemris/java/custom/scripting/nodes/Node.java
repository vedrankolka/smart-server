package hr.fer.zemris.java.custom.scripting.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class representation of structured documents. Base class for all graph nodes.
 * @author Vedran Kolka
 *
 */
public abstract class Node {

	/**
	 * collection of children of this node
	 */
	private List<Node> children;
	
	/**
	 * Adds the given <code>child</code> to <code>children</code> collection.
	 * @param child
	 * @throws NullPointerException if <code>child</code> is <code>null</code>
	 */
	public void addChildNode(Node child) {
		Objects.requireNonNull(child);
		if(children==null) {
			children = new ArrayList<>();
		}
		children.add(child);
	}
	
	/**
	 * Returns the number of (direct) children.
	 * @return number of children
	 */
	public int numberOfChildren() {
		return children.size();
	}
	
	/**
	 * Returns the child on position <code>index</code>.
	 * @param index
	 * @return {@link IndexOutOfBoundsException} if <code>index</code>
	 * is less than 0 or greater than number of children-1
	 */
	public Node getChild(int index) {
		return (Node)children.get(index);
	}

	@Override
	public int hashCode() {
		return Objects.hash(children);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Node))
			return false;
		Node other = (Node) obj;
		if(children==null) {
			if(other.children==null) {
				return true;
			}
			return false;
		}
		boolean equals = true;
		for(int i = 0 ; i<children.size() ; ++i) {
			equals = equals && getChild(i).equals(other.getChild(i));
		}
		return equals;
	}
	
	/**
	 * Calls the visitor to visit this node.
	 * @param visitor to accept
	 */
	public abstract void accept(INodeVisitor visitor);
	
}
