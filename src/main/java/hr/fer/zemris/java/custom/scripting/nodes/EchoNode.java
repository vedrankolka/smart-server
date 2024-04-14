package hr.fer.zemris.java.custom.scripting.nodes;

import java.util.Arrays;

import hr.fer.zemris.java.custom.scripting.elems.Element;

/**
 * A node representing a command which generates some textual output dynamically.
 * @author Vedran Kolka
 *
 */
public class EchoNode extends Node {

	/**
	 * array of Elements
	 */
	private Element[] elements;
	
	/**
	 * Creates an EchoNode with a variable number of Elements.
	 * @param elements
	 */
	public EchoNode(Element ...elements ) {
		this.elements = elements;
	}
	
	public Element[] getElements() {
		return elements;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(elements);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof EchoNode)) {
			return false;
		}
		EchoNode other = (EchoNode) obj;
		boolean equals = true;
		for(int i = 0 ; i < elements.length ; ++i) {
			equals = equals && elements[i].equals(other.elements[i]);
		}
		return equals;
	}

	@Override
	public void accept(INodeVisitor visitor) {
		visitor.visitEchoNode(this);
	}

}
