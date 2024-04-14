package hr.fer.zemris.java.custom.scripting.lexer;
/**
 * An exception thrown by the {@link SmartScriptLexer}.
 * @author Vedran Kolka
 *
 */
public class SmartScriptLexerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 */
	public SmartScriptLexerException() {
		super();
	}
	/**
	 * Constructor
	 * @param message to be added with the exception
	 */
	public SmartScriptLexerException(String message) {
		super(message);
	}
	
}
