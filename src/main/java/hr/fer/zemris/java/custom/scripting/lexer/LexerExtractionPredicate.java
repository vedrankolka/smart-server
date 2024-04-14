package hr.fer.zemris.java.custom.scripting.lexer;

@FunctionalInterface
public interface LexerExtractionPredicate {

	/**
	 * Tests if the extraction of characters should continue.
	 *  @return <code>true</code> if it should
	 */
	boolean test();
	
}
