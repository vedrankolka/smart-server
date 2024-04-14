package hr.fer.zemris.java.custom.scripting.elems;

/**
 * Class representation of expressions.
 * @author Vedran Kolka
 *
 */
public abstract class Element {
	/**
	 * Returns a string representation of the element.
	 * @return a string representation of the element
	 */
	 public abstract String asText();
	 
	 /**
	  * Getter for this elements value
	  * @return value
	  */
	 public abstract Object getValue();
}
