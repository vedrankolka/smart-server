package hr.fer.zemris.java.custom.scripting.lexer;

/**
 * Class representation of a token.
 * 
 * @author Vedran Kolka
 *
 */
public class Token {

	/**
	 * Type of token.
	 */
	private TokenType type;
	/**
	 * Value of token.
	 */
	private Object value;

	/**
	 * Constructor.
	 * 
	 * @param type  of this token
	 * @param value of this token
	 */
	public Token(TokenType type, Object value) {
		this.type = type;
		this.value = value;
	}

	/**
	 * Getter for type
	 * @return type of this token
	 */
	public TokenType getType() {
		return type;
	}

	/**
	 * Getter for value
	 * @return value of this token
	 */
	public Object getValue() {
		return value;
	}

}
