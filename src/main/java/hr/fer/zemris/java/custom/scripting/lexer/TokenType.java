package hr.fer.zemris.java.custom.scripting.lexer;

public enum TokenType {
	//token types generated in BASIC state
	TEXT, TAG_BEGINNING_SEQUENCE,
	//token types generated in TAG state
	OPERATOR, FUNCTION, STRING, NUMBER, TAG_END_SEQUENCE, VARIABLE_NAME,
	//token types generated in both states
	EOF;
}
