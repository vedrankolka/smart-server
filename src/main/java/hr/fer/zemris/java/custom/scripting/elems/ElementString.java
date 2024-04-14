package hr.fer.zemris.java.custom.scripting.elems;

import java.util.Objects;

/**
 * Class representation of a string
 * @author Vedran Kolka
 *
 */
public class ElementString extends Element {
	
	private static final char[] VALID_ESCAPE_FOR_STRING = {'\\', '"', '\n', '\r', '\t'};
	
	/**
	 * value of the string constant
	 */
	private String value;
	
	public ElementString(String value) {
		this.value = value;
	}
	
	@Override
	public String asText() {
		return "\"" + reconstructEscapeSigns(value) + "\"";
	}
	/**
	 * @return value
	 */
	public String getValue() {
		return value;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		}
		if(obj==null) {
			return false;
		}
		if(!(obj instanceof ElementString)) {
			return false;
		}
		ElementString other = (ElementString) obj;
		return Objects.equals(value, other.value);
	}
	
	/**
	 * Returns the escape signs to the String so that it can be parsed again.
	 * @param text
	 * @param escapedCharacters - an array of characters before which the escape sign is added
	 * @return String with added escape signs
	 */
	private static String reconstructEscapeSigns(String text) {
		StringBuilder sb = new StringBuilder();
		char[] oldText = text.toCharArray();
		for(int i = 0 ; i<oldText.length ; ++i) {
			if(checkNextCharacter(i, oldText, VALID_ESCAPE_FOR_STRING)) {
				switch(oldText[i]) {
				case '"':
				case '\\':
					sb.append('\\');
					sb.append(oldText[i]);
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					throw new RuntimeException("Unexpected escaped character.");
				}
			} else {
				sb.append(oldText[i]);
			}
		}
		return sb.toString();
	}
	
	/**
	 * Checks if the character at <code>index</code> is one of the <code>validCharacters</code>.
	 * @param validCharacters
	 * @param data
	 * @return <code>true</code> if the next character matches
	 * one of the <code>validCharacters</code>
	 */
	public static boolean checkNextCharacter(int index, char[] data, char ...validCharacters) {
		if (index == data.length) {
			return false;
		}
		for (char c : validCharacters) {
			if (data[index] == c) {
				return true;
			}
		}
		return false;
	}
}
