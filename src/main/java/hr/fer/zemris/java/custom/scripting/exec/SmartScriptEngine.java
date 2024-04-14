package hr.fer.zemris.java.custom.scripting.exec;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.BinaryOperator;

import hr.fer.zemris.java.custom.scripting.elems.Element;
import hr.fer.zemris.java.custom.scripting.elems.ElementConstantDouble;
import hr.fer.zemris.java.custom.scripting.elems.ElementConstantInteger;
import hr.fer.zemris.java.custom.scripting.elems.ElementFunction;
import hr.fer.zemris.java.custom.scripting.elems.ElementOperator;
import hr.fer.zemris.java.custom.scripting.elems.ElementString;
import hr.fer.zemris.java.custom.scripting.elems.ElementVariable;
import hr.fer.zemris.java.custom.scripting.nodes.DocumentNode;
import hr.fer.zemris.java.custom.scripting.nodes.EchoNode;
import hr.fer.zemris.java.custom.scripting.nodes.ForLoopNode;
import hr.fer.zemris.java.custom.scripting.nodes.INodeVisitor;
import hr.fer.zemris.java.custom.scripting.nodes.TextNode;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * An engine that executes a smart script whose document node is given through
 * the constructor.
 * 
 * @author Vedran Kolka
 *
 */
public class SmartScriptEngine {
	/** DocumentNode of the smart script to execute */
	private DocumentNode documentNode;
	/** The request context of the execution */
	private RequestContext requestContext;
	/** A multistack used for the algorithms of execution */
	private ObjectMultistack multistack = new ObjectMultistack();
	/** A visitor used for visiting the document tree */
	private INodeVisitor visitor = new INodeVisitor() {

		/**
		 * A map of operations that this engine supports.
		 */
		private Map<String, BinaryOperator<ValueWrapper>> operations;

		@Override
		public void visitTextNode(TextNode node) {
			try {
				requestContext.write(node.getText());
			} catch (IOException e) {
				System.err.println("Writing failed.");
			}
		}

		@Override
		public void visitForLoopNode(ForLoopNode node) {
			// create a stack for this variable mapped with the variable name and initialize
			// the value
			String varName = node.getVariable().getName();
			ValueWrapper value = new ValueWrapper(node.getStartExpression().asText());
			multistack.push(varName, value);
			// initialize step and end values
			String stepExpression = node.getStepExpression() == null ? "1" : node.getStepExpression().asText();
			ValueWrapper step = new ValueWrapper(stepExpression);
			ValueWrapper end = new ValueWrapper(node.getEndExpression().asText());
			// iterate with a step while value is <= end
			while (value.numCompare(end.getValue()) <= 0) {
				for (int i = 0; i < node.numberOfChildren(); ++i) {
					node.getChild(i).accept(visitor);
				}
				value = multistack.pop(varName);
				value.add(step.getValue());
				multistack.push(varName, value);
			}
			// remove this value of the variable from the stack
			multistack.pop(varName);

		}

		@Override
		public void visitEchoNode(EchoNode node) {
			Stack<Object> temp = new Stack<>();
			for (Element e : node.getElements()) {

				if (isConstant(e)) {
					temp.push(e.getValue());

				} else if (isVariable(e)) {
					ElementVariable ev = (ElementVariable) e;
					ValueWrapper mostRecentValue = multistack.peek(ev.getName());
					temp.push(mostRecentValue.getValue());

				} else if (e instanceof ElementOperator) {
					ValueWrapper second = new ValueWrapper(temp.pop());
					ValueWrapper first = new ValueWrapper(temp.pop());
					ValueWrapper result = operate(first, second, ((ElementOperator) e).getSymbol());
					temp.push(result.getValue());
				} else if (e instanceof ElementFunction) {
					function((ElementFunction) e, temp);
				} else {
					throw new RuntimeException("Unexpected element");
				}

			}
			// everything left on the stack is for the output stream in the right order
			Stack<Object> helper = new Stack<>();
			while (!temp.isEmpty()) {
				helper.push(temp.pop());
			}

			while (!helper.isEmpty()) {
				try {
					requestContext.write(helper.pop().toString());
				} catch (IOException e1) {
					System.err.println("Writing failed.");
				}
			}

		}

		/**
		 * Performs one of the defined functions where the arguments for the functions
		 * are expected to be at the top of the given stack <code>stack</code>
		 * 
		 * @param e
		 * @param stack
		 */
		private void function(ElementFunction e, Stack<Object> stack) {

			switch (e.getName()) {
			case "sin":
				sin(stack);
				break;
			case "decfmt":
				decfmt(stack);
				break;
			case "dup":
				dup(stack);
				break;
			case "swap":
				swap(stack);
				break;
			case "setMimeType":
				requestContext.setMimeType(stack.pop().toString());
				break;
			case "paramGet":
				paramGet(stack);
				break;
			case "pparamGet":
				pparamGet(stack);
				break;
			case "pparamSet":
				pparamSet(stack);
				break;
			case "pparamDel":
				pparamDel(stack);
				break;
			case "tparamGet":
				tparamGet(stack);
				break;
			case "tparamSet":
				tparamSet(stack);
				break;
			case "tparamDel":
				tparamDel(stack);
				break;
			default:
				throw new RuntimeException("Unsupported function: " + e.getName());
			}
		}

		/**
		 * Takes the object from top of the given <code>stack</code>, which is expected
		 * to be a string parsable to a double, a Double or Integer.<br>
		 * A sinus is calculated from the number and pushed on the <code>stack</code>.
		 * 
		 * @param stack on which is the expected argument
		 * @throws IllegalArgumentException if the object on top is not a string
		 *                                  parsable to a double, a Double or an Integer
		 */
		private void sin(Stack<Object> stack) {
			Number number = getAsNumber(stack.pop());
			Double xInDegrees = number.doubleValue();
			Double x = Math.toRadians(xInDegrees);
			stack.push(Math.sin(x));
		}

		/**
		 * Formats the number that is the second from the top of the <code>stack</code>
		 * to a decimal format represented by a string on top of the given
		 * <code>stack</code> and pushes the result on the <code>stack</code>.
		 * 
		 * @param stack on which are the expected arguments: string representing the
		 *              decimal format and a number
		 */
		private void decfmt(Stack<Object> stack) {
			DecimalFormat format = new DecimalFormat((String) stack.pop());
			double unformatted = getAsNumber(stack.pop()).doubleValue();
			String formatted = format.format(unformatted);
			stack.push(formatted);
		}

		/**
		 * Duplicates the object on top of the stack.<br>
		 * <p>
		 * It means it pops the object, then pushes it twice.
		 * 
		 * @param stack
		 */
		private void dup(Stack<Object> stack) {
			Object x = stack.pop();
			stack.push(x);
			stack.push(x);
		}

		/**
		 * Swaps the top two objects on the given <code>stack</code>.
		 * 
		 * @param stack on which to swap the objects
		 */
		private void swap(Stack<Object> stack) {
			Object a = stack.pop();
			Object b = stack.pop();
			stack.push(a);
			stack.push(b);
		}

		/**
		 * The method pops two arguments from the top of the <code>stack</code>. The one
		 * on top is expected to be a default value for the parameter whose name is
		 * expected to be the second to top object on the stack.<br>
		 * It gets the value associated with read name in the map
		 * <code>parameters</code> and pushes the value on the stack if it is not
		 * <code>null</code>. If the value is <code>null</code>, the read default value
		 * is pushed.
		 * 
		 * @param stack on which are the expected arguments: default value for the
		 *              parameter and the parameter name
		 */
		private void paramGet(Stack<Object> stack) {
			String defValue = stack.pop().toString();
			String paramName = stack.pop().toString();
			String paramValue = requestContext.getParameter(paramName);
			stack.push(paramValue == null ? defValue : paramValue);
		}

		/**
		 * The method pops two arguments from the top of the <code>stack</code>. The one
		 * on top is expected to be a default value for the parameter whose name is
		 * expected to be the second to top object on the stack.<br>
		 * It gets the value associated with read name in the map
		 * <code>persistentParameters</code> and pushes the value on the stack if it is
		 * not <code>null</code>. If the value is <code>null</code>, the read default
		 * value is pushed.
		 * 
		 * @param stack on which are the expected arguments: default value for the
		 *              parameter and the parameter name
		 */
		private void pparamGet(Stack<Object> stack) {
			String defValue = stack.pop().toString();
			String paramName = stack.pop().toString();
			String paramValue = requestContext.getPersistentParameter(paramName);
			stack.push(paramValue == null ? defValue : paramValue);
		}

		/**
		 * The method pops two arguments from the top of the <code>stack</code>. The one
		 * on top is expected to be a parameter name to set to the value expected to be
		 * the second to top object on the stack.<br>
		 * It sets the value associated with read name in the map
		 * <code>persistentParameters</code>.
		 * 
		 * @param stack on which are the expected arguments: parameter name and value to
		 *              set
		 */
		private void pparamSet(Stack<Object> stack) {
			String paramName = stack.pop().toString();
			String paramValue = stack.pop().toString();
			requestContext.setPersistentParameter(paramName, paramValue);
		}

		/**
		 * The method pops the object that is expected to be a parameter name.<br>
		 * It removes the value associated with the read name from the map
		 * <code>persistentParameters</code>.
		 * 
		 * @param stack on which is expected a string: name of the parameter to remove
		 */
		private void pparamDel(Stack<Object> stack) {
			String paramName = stack.pop().toString();
			requestContext.removePersistentParameter(paramName);
		}

		/**
		 * The method pops two arguments from the top of the <code>stack</code>. The one
		 * on top is expected to be a default value for the parameter whose name is
		 * expected to be the second to top object on the stack.<br>
		 * It gets the value associated with read name in the map
		 * <code>temporaryParameters</code> and pushes the value on the stack if it is
		 * not <code>null</code>. If the value is <code>null</code>, the read default
		 * value is pushed.
		 * 
		 * @param stack on which are the expected arguments: default value for the
		 *              parameter and the parameter name
		 */
		private void tparamGet(Stack<Object> stack) {
			String defValue = stack.pop().toString();
			String paramName = stack.pop().toString();
			String paramValue = requestContext.getTemporaryParameter(paramName);
			stack.push(paramValue == null ? defValue : paramValue);
		}

		/**
		 * The method pops two arguments from the top of the <code>stack</code>. The one
		 * on top is expected to be a parameter name to set to the value expected to be
		 * the second to top object on the stack.<br>
		 * It sets the value associated with read name in the map
		 * <code>temporaryParameters</code>.
		 * 
		 * @param stack on which are the expected arguments: parameter name and value to
		 *              set
		 */
		private void tparamSet(Stack<Object> stack) {
			String paramName = stack.pop().toString();
			String paramvalue = stack.pop().toString();
			requestContext.setTemporaryParameter(paramName, paramvalue);
		}

		/**
		 * The method pops the object that is expected to be a parameter name.<br>
		 * It removes the value associated with the read name from the map
		 * <code>temporaryParameters</code>.
		 * 
		 * @param stack on which is expected a string: name of the parameter to remove
		 */
		private void tparamDel(Stack<Object> stack) {
			String paramName = stack.pop().toString();
			requestContext.removeTemporaryParameter(paramName);
		}

		/**
		 * If map with operations has not been initialized it is now initialized.<br>
		 * Then the given <code>operation</code> is executed among the given operands
		 * <code>first</code> and <code>second</code>.
		 * 
		 * @param first     operand
		 * @param second    operand
		 * @param operation symbol of the operation (+, -, *, /)
		 * @return result of the operation
		 * @throws NullPointerException if the operation is not supported
		 */
		private ValueWrapper operate(ValueWrapper first, ValueWrapper second, String operation) {
			if (operations == null) {
				initOperations();
			}
			return operations.get(operation).apply(first, second);
		}

		/**
		 * Creates a map and adds supported operations to it.<br>
		 * Supported operations are:
		 * <ul>
		 * <li>+ addition
		 * <li>- subtraction
		 * <li>* multiplication
		 * <li>/ division
		 * </ul>
		 */
		private void initOperations() {
			operations = new HashMap<>();

			operations.put("+", (v1, v2) -> {
				v1.add(v2.getValue());
				return v1;
			});

			operations.put("-", (v1, v2) -> {
				v1.subtract(v2.getValue());
				return v1;
			});

			operations.put("*", (v1, v2) -> {
				v1.multiply(v2.getValue());
				return v1;
			});

			operations.put("/", (v1, v2) -> {
				v1.divide(v2.getValue());
				return v1;
			});
		}

		@Override
		public void visitDocumentNode(DocumentNode node) {
			for (int i = 0; i < node.numberOfChildren(); ++i) {
				node.getChild(i).accept(this);
			}
		}

	};

	/**
	 * Constructor.
	 * 
	 * @param documentNode of the parsed script to execute
	 * @param requestContext which is used in executing the script
	 */
	public SmartScriptEngine(DocumentNode documentNode, RequestContext requestContext) {
		this.documentNode = documentNode;
		this.requestContext = requestContext;
	}

	public void execute() {
		documentNode.accept(visitor);
	}

	/**
	 * Expects a String that is parsable to a double, a Double or an Integer
	 * 
	 * @param obj
	 * @return Number
	 * @throws IllegalArgumentException if <code>obj</code> is not a string parsable
	 *                                  to a double, a Double or an Integer
	 */
	private Number getAsNumber(Object obj) {
		if (obj instanceof String) {
			return Double.parseDouble((String) obj);
		} else if (obj instanceof Double || obj instanceof Integer) {
			return (Number) obj;
		} else {
			throw new IllegalArgumentException("The given object cannot be interpreted as a number: " + obj);
		}
	}

	/**
	 * Checks if the given element is an instance of an element representing a
	 * constant.
	 * 
	 * @param e Element to check
	 * @return <code>true</code> if it is, <code>false</code> otherwise
	 */
	private boolean isConstant(Element e) {
		return e instanceof ElementConstantInteger || e instanceof ElementConstantDouble || e instanceof ElementString;
	}

	/**
	 * Checks if the given element is an instance of a {@link ElementVariable}
	 * 
	 * @param e Element to check
	 * @return <code>true</code> if it is, <code>false</code> otherwise
	 */
	private boolean isVariable(Element e) {
		return e instanceof ElementVariable;
	}

}
