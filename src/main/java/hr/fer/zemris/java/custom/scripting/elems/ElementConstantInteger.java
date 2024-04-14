package hr.fer.zemris.java.custom.scripting.elems;

/**
 * Class representation of a constant integer.
 * @author Vedran Kolka
 *
 */
public class ElementConstantInteger extends Element {
	/**
	 * value of the constant integer
	 */
	private int value;
	
	public ElementConstantInteger(int value) {
		this.value = value;
	}
	
	@Override
	public String asText() {
		return "" + value;
	}
	
	@Override
	public Integer getValue() {
		return value;
	}
	
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		}
		if(obj==null) {
			return false;
		}
		if(!(obj instanceof ElementConstantInteger)) {
			return false;
		}
		ElementConstantInteger other = (ElementConstantInteger) obj;
		return value==other.value;
	}
	
}
