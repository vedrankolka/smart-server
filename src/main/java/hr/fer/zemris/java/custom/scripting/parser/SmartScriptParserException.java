package hr.fer.zemris.java.custom.scripting.parser;

/**
 * Exception that the SmartScriptPerser throws when the text it is parsing
 * is not written in line with the rules.
 * @author Vedran Kolka
 *
 */
public class SmartScriptParserException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public SmartScriptParserException() {
		
	}
	
	public SmartScriptParserException(String message) {
		super(message);
	}

}
