package hr.fer.zemris.java.custom.scripting.elems;

import java.util.Objects;

/**
 * Class representation of an operator.
 * @author Vedran Kolka
 *
 */
public class ElementOperator extends Element {
	/**
	 * symbol of the operator
	 */
	private String symbol;
	
	public ElementOperator(String symbol) {
		this.symbol = symbol;
	}
	
	@Override
	public String asText() {
		return symbol;
	}
	/**
	 * @return symbol
	 */
	public String getSymbol() {
		return symbol;
	}
	
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		}
		if(obj==null) {
			return false;
		}
		if(!(obj instanceof ElementOperator)) {
			return false;
		}
		ElementOperator other = (ElementOperator) obj;
		return Objects.equals(symbol, other.symbol);
	}

	@Override
	public Object getValue() {
		return symbol;
	}
	
}
