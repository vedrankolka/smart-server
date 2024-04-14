package hr.fer.zemris.java.custom.scripting.exec;

import java.util.function.BinaryOperator;

/**
 * A wrapper of a value that offers arithmetic operations amongst
 * other ValueWrappers. 
 * @author Vedran Kolka
 *
 */

public class ValueWrapper {
	/**
	 * Value stored in this wrapper.
	 */
	private Object value;
	/**
	 * Constructor that sets the initial value of the wrapper to the given iniitalValue.
	 * @param initialValue
	 */
	public ValueWrapper(Object initialValue) {
		this.value = initialValue;
	}
	
	/**
	 * Adds the given incValue to this.value .
	 * @param incValue to add
	 */
	public void add(Object incValue) {
		OperationValues values = new OperationValues(this.value, incValue);
		this.value = calculate(values, Integer::sum, Double::sum);
	}
	
	public void subtract(Object decValue) {
		OperationValues values = new OperationValues(this.value, decValue);
		this.value = calculate(values, (i1, i2) -> i1 - i2 , (d1, d2) -> d1 - d2);
	}
	
	public void multiply(Object mulValue) {
		OperationValues values = new OperationValues(this.value, mulValue);
		this.value = calculate(values, (i1, i2) -> i1 * i2 , (d1, d2) -> d1 * d2);
	}
	
	public void divide(Object divValue) {
		OperationValues values = new OperationValues(this.value, divValue);
		this.value = calculate(values, (i1, i2) -> i1 / i2 , (d1, d2) -> d1 / d2);
	}

	public int numCompare(Object withValue) {
		OperationValues values = new OperationValues(this.value, withValue);
		return calculate(values, (Integer::compare),
				(d1, d2) -> Double.valueOf(d1.compareTo(d2))).intValue();
	}
	
	/**
	 * Getter for value.
	 * @return value
	 */
	public Object getValue() {
		return value;
	}
	/**
	 * Setter for value.
	 * @param value to set
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	
	private static Number calculate(OperationValues values, BinaryOperator<Integer> opInt, BinaryOperator<Double> opDouble) {
		
		if(values.getResultType() == Integer.class) {
			return opInt.apply(values.getFirstValue().intValue(), values.getSecondValue().intValue());
		} else if (values.getResultType() == Double.class){
			return opDouble.apply(values.getFirstValue().doubleValue(), values.getSecondValue().doubleValue());
		}
		throw new RuntimeException("Unexpected result type: " + values.getResultType());
	}
	
}
