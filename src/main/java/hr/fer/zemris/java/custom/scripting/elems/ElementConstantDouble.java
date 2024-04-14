package hr.fer.zemris.java.custom.scripting.elems;

/**
 * Class representation of a double constant.
 * @author Vedran Kolka
 *
 */
public class ElementConstantDouble extends Element {
	/**
	 * value of double constant
	 */
	private double value;
	
	public ElementConstantDouble(double value) {
		this.value = value;
	}
	
	@Override
	public String asText() {
		return "" + value;
	}
	/**
	 * @return value
	 */
	@Override
	public Double getValue() {
		return value;
	}
	
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		}
		if(obj==null) {
			return false;
		}
		if(!(obj instanceof ElementConstantDouble)) {
			return false;
		}
		ElementConstantDouble other = (ElementConstantDouble) obj;
		return Math.abs(value-other.value)<10e-6;
	}
}
