package hr.fer.zemris.java.custom.scripting.exec;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A collection that offers a stack-like interface, but offers more stacks, each
 * accessed with a key of type String and values stored on stacks are
 * ValueWrappers.
 * 
 * @author Vedran Kolka
 *
 */
public class ObjectMultistack {

	private Map<String, MultistackEntry> stacks;

	/**
	 * A node of the stack, with a value ValueWrpper and a reference to the next
	 * node.
	 * 
	 * @author Vedran Kolka
	 *
	 */
	private static class MultistackEntry {
		/**
		 * The value of the node.
		 */
		private ValueWrapper value;
		/**
		 * A reference to the next node.
		 */
		private MultistackEntry next;

		/**
		 * Constructs a MultistackEntry with the given parameters.
		 * @param value
		 * @param next
		 * @throws NullPointerException if <code>value</code> is <code>null</code>
		 */
		private MultistackEntry(ValueWrapper value, MultistackEntry next) {
			this.value = Objects.requireNonNull(value);
			this.next = next;
		}

	}

	public ObjectMultistack() {
		stacks = new HashMap<>();
	}

	/**
	 * Pushes the given value on the stack with the key keyName.
	 * 
	 * @param keyName of the stack
	 * @param value   to push
	 * @throws NullPointerException if <code>value</code> is <code>null</code>
	 */
	public void push(String keyName, ValueWrapper value) {
		MultistackEntry newNode = new MultistackEntry(Objects.requireNonNull(value),
														stacks.get(keyName));
		stacks.put(keyName, newNode);
	}

	/**
	 * Returns the ValueWrapper on top of the stack with the key keyName and removes
	 * it from the stack.
	 * 
	 * @param keyName
	 * @return ValueWrapper
	 * @throws EmptyStackException if the referenced stack is empty
	 */
	public ValueWrapper pop(String keyName) {

		MultistackEntry top = peekNode(keyName);
		stacks.put(keyName, top.next);
		return top.value;
	}

	/**
	 * Returns the ValueWrapper from the top of the stack with key keyName
	 * but does not remove it from the top of the stack.
	 * @param keyName
	 * @return ValueWrapper on top of the referenced stack
	 * @throws EmptyStackException if the referenced stack is empty
	 */
	public ValueWrapper peek(String keyName) {

		MultistackEntry top = peekNode(keyName);
		return top.value;
	}

	/**
	 * Returns the StackNode on top of the stack
	 * with the key keyName.
	 * @param keyName
	 * @return StackNode on top of the stack
	 * @throws EmptyStackException if the referenced stack is empty
	 */
	private MultistackEntry peekNode(String keyName) {
		MultistackEntry top = stacks.get(keyName);

		if (top == null) {
			throw new EmptyStackException();
		}

		return top;
	}

	/**
	 * Checks if the stack with key keyName is empty.
	 * @param keyName
	 * @return true if it is, false otherwise
	 */
	public boolean isEmpty(String keyName) {
		return stacks.get(keyName) == null;
	}
	
}
