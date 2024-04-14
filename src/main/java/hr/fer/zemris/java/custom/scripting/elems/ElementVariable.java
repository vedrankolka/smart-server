package hr.fer.zemris.java.custom.scripting.elems;

import java.util.Objects;

/**
 * Class representation of a variable element
 * @author Vedran Kolka
 *
 */
public class ElementVariable extends Element {
	/**
	 * variable name
	 */
	private String name;
	
	public ElementVariable(String name) {
		this.name = name;
	}

	@Override
	public String asText() {
		return name;
	}
	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}
	
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		}
		if(obj==null) {
			return false;
		}
		if(!(obj instanceof ElementVariable)) {
			return false;
		}
		ElementVariable other = (ElementVariable) obj;
		return Objects.equals(name, other.name);
	}

	@Override
	public Object getValue() {
		return name;
	}
	
}
