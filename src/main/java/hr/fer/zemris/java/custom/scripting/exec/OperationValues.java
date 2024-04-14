package hr.fer.zemris.java.custom.scripting.exec;

/**
 * A class used to determine which values to use in an arithmetic operation of
 * the Objects given through the constructor.
 * 
 * @author Vedran Kolka
 *
 */
public class OperationValues {

	private Number firstValue;
	private Number secondValue;
	private Class<? extends Number> resultType;

	public OperationValues(Object first, Object second) {
		firstValue = determineArithmeticValue(first);
		secondValue = determineArithmeticValue(second);
		resultType = determineResultType(firstValue, secondValue);
	}

	public static Class<? extends Number> determineResultType(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return Double.class;
		else if(a instanceof Integer && b instanceof Integer)
			return Integer.class;
		throw new RuntimeException("Unexpected types for arithmetic operations.");
	}

	public static Number determineArithmeticValue(Object o) {
		if (o == null)
			return Integer.valueOf(0);

		if (o instanceof Integer || o instanceof Double)
			return (Number)o;

		if (o instanceof String) {
			String value = (String) o;
			try {
				if (value.contains(".") || value.contains("e") || value.contains("E")) {
					return Double.valueOf(value);
				} else {
					return Integer.valueOf(value);
				}
			} catch (NumberFormatException e) {
				throw new RuntimeException("The given string '" + value +
						"' is not valid for arithmetic operations.");
			}
		}
		// if it is none of the above, it is not a valid type of value
		throw new RuntimeException("Arithmetic operation is not supported with type " + o.getClass());
	}
	
	public Number getFirstValue() {
		return firstValue;
	}
	
	public Number getSecondValue() {
		return secondValue;
	}
	
	public Class<? extends Number> getResultType() {
		return resultType;
	}
}
