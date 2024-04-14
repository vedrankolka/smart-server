package hr.fer.zemris.java.custom.scripting.elems;

import java.util.Objects;

/**
 * Class representation of a function.
 * @author Vedran Kolka
 *
 */
public class ElementFunction extends Element {
	/**
	 * name of the function
	 */
	private String name;
	
	public ElementFunction(String name) {
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
		if(!(obj instanceof ElementFunction)) {
			return false;
		}
		ElementFunction other = (ElementFunction) obj;
		return Objects.equals(name, other.name);
	}

	@Override
	public Object getValue() {
		return name;
	}
	
}
