package hr.fer.zemris.java.custom.scripting.lexer;

import hr.fer.zemris.java.custom.scripting.lexer.Token;
import hr.fer.zemris.java.custom.scripting.lexer.TokenType;

/**
 * A Lexer that reads the given text and extracts tokens from it.
 * @author Vedran Kolka
 *
 */

public class SmartScriptLexer {
	public static final char TAG_BEGINING = '{';
	public static final char TAG_END = '}';
	public static final char TAG_CHAR = '$';
	public static final char ESCAPE_CHAR = '\\';
	public static final char FUNCTION_CHAR = '@';
	public static final String OPEN_TAG = "{$";
	public static final String CLOSE_TAG = "$}";
	public static final char[] VALID_ESCAPE_FOR_TEXT = {ESCAPE_CHAR, TAG_BEGINING};
	public static final char[] VALID_ESCAPE_FOR_STRING = {ESCAPE_CHAR, '"', 'n', 'r', 't'};
	public static final char[] OPERATORS = {'+', '-', '*', '/', '^'};
	/**
	 * The current state of the Lexer
	 */
	private LexerState state;
	/**
	 * Source text converted to an array of characters.
	 */
	private char[] data;
	/**
	 * Current Token.
	 */
	private Token token;
	/**
	 * Index of the next character to be processed.
	 */
	private int currentIndex;
	
	/**
	 * Creates a Lexer that read the given <code>text</code>.
	 * Initially in BASIC state.
	 * @param text
	 * @throws NullPointerException if <code>text</code> is <code>null</code>
	 */
	public SmartScriptLexer(String text) {
		if(text==null) {
			throw new NullPointerException("text cannot be null.");
		}
		this.data = text.toCharArray();
		this.state = LexerState.BASIC;
	}
	
	/**
	 * Generates the next token and returns it.
	 * @return next token
	 * @throws LexerException if a token cannot be generated
	 */
	public Token nextToken() {
		//if last token was EOF token, throw LexerException
		if(token != null && token.getType()==TokenType.EOF) {
			throw new SmartScriptLexerException("Cannot read token after EOF");
		}
		//the TAG state ignored whitespaces
		if(state==LexerState.TAG) {
			skipWhitespaces();
		}
		//if the end of the String is reached, return EOF token
		if(currentIndex>=data.length) {
			token = new Token(TokenType.EOF, null);
			return token;
		}
		
		if(state==LexerState.BASIC) {
			token = extractBasicToken();
		} else if(state==LexerState.TAG) {
			token = extractTagToken();
			
		}
		if(token==null) {
			throw new RuntimeException("Unexpected LexerState.");
		}
		return token;
	}
	
	/**
	 * Returns the last generated token. Does not generate a new token.
	 * @return last generated token
	 */
	public Token getToken() {
		return token;
	}
	
	public void setState(LexerState state) {
		if(state==null) {
			throw new NullPointerException("Lexer state cannot be null");
		}
		this.state = state;
	}
	
//private helper methods------------------------------------------------------------------------
	/**
	 * Skips all blanks in the text.
	 */
	private void skipWhitespaces() {
		while(currentIndex<data.length) {
			char character = data[currentIndex];
			if(character==' ' || character=='\t' || character=='\r' || character=='\n') {
				currentIndex++;
				continue;
			}
			break;
		}
	}
	/**
	 * Extracts a token in line with BASIC state rules.
	 * @return token
	 * @throws LexerException
	 */
	private Token extractBasicToken() {
		//if character is a letter, return a TEXT token
		if(!tagBeginning()) {
			return extractText();
		}
		if (tagBeginning()){
			currentIndex += 2;
			return new Token(TokenType.TAG_BEGINNING_SEQUENCE, OPEN_TAG);
		}
		//if it is not the beginning of a tag, it is undefined for this lexer
		throw new SmartScriptLexerException("Unexpected syntax in BASIC state.");
	}
	
	/**
	 * Extracts a token in line with TAG state rules.
	 * @return token
	 * @throws LexerException
	 */
	private Token extractTagToken() {
		if(checkNextCharacter(currentIndex, '=')) {
			currentIndex++;
			return new Token(TokenType.VARIABLE_NAME, "=");
		}
		//Tag names are treated as variables, then the parser checks them
		if(Character.isLetter(data[currentIndex])) {
			return extractVariable();
		}
		//if it is a + or -
		if(checkNextCharacter(currentIndex, OPERATORS)) {
			//check if it is a number
			if(checkNextCharacter(currentIndex, '+', '-') &&
					currentIndex+1<data.length &&
					Character.isDigit(data[currentIndex+1])) {
				return extractNumber();
			}
			//then its an operator
			String operator = new String(data, currentIndex++, 1);
			return new Token(TokenType.OPERATOR, operator);
		}
		//if it is a number, return a NUMBER token
		if(Character.isDigit(data[currentIndex])) {
			return extractNumber();
		}
		if(checkNextCharacter(currentIndex, FUNCTION_CHAR) && 
				currentIndex + 1 < data.length &&
				Character.isLetter(data[currentIndex+1])) {
			return extractFunction();
		}
		if(checkNextCharacter(currentIndex, '"')) {
			return extractString();
		}
		if(tagEnding()) {
			currentIndex += 2;
			return new Token(TokenType.TAG_END_SEQUENCE, CLOSE_TAG);
		}
		
		throw new SmartScriptLexerException("Unexpected syntax in TAG state.");
	}
	/**
	 * Extracts the text as a token.
	 * @return token(TEXT, value)
	 * @throws LexerException if the escape character is used incorrectly
	 */
	private Token extractText() {
		LexerExtractionPredicate p = () -> {
			boolean flag = !tagBeginning();
			if(data[currentIndex]==ESCAPE_CHAR) {
				checkEscapeSequence(VALID_ESCAPE_FOR_TEXT);
				currentIndex++;
			}
			return flag;
		};
		return new Token(TokenType.TEXT, extractCharacters(p, 0));
	}
	
	/**
	 * Extracts a number in digits-dot-digits format as a token.
	 * @return token(NUMBER, value)
	 * @throws LexerException
	 */
	private Token extractNumber() {
		LexerExtractionPredicate p = () -> !tagEnding() &&
				(Character.isDigit(data[currentIndex]) || checkNextCharacter(currentIndex, '.'));
		String numberString = extractCharacters(p, 1);
		try {
			double number = Double.parseDouble(numberString);
			return new Token(TokenType.NUMBER, number);
		} catch(NumberFormatException e) {
			throw new SmartScriptLexerException("Wrong number format.");
		}
	}
	
	/**
	 * Extracts a variable name as a token.
	 * @return token(VARIABLE_NAME, value)
	 */
	private Token extractVariable() {
		LexerExtractionPredicate p = () -> 
			Character.isLetter(data[currentIndex]) ||
			Character.isDigit(data[currentIndex]) ||
			checkNextCharacter(currentIndex, '_');
		String variableName = extractCharacters(p, 0);
		return new Token(TokenType.VARIABLE_NAME, variableName);
		
	}
	
	/**
	 * Extracts a function name as a token.
	 * @return token(FUNCTION, value)
	 */
	private Token extractFunction() {
		LexerExtractionPredicate p = () ->
			Character.isLetter(data[currentIndex]) ||
			Character.isDigit(data[currentIndex]) ||
			checkNextCharacter(currentIndex, '_');
		String functionName = extractCharacters(p, 2);
		// to skip the '@'
		functionName = functionName.substring(1);
		return new Token(TokenType.FUNCTION, functionName);
	}
	
	/**
	 * Extracts a string as a token.
	 * @return token(STRING, value)
	 * @throws LexerException if the quotation marks are opened but not closed
	 */
	private Token extractString() {
		//first skip the opening quotation mark
		currentIndex++; 
		StringBuilder sb = new StringBuilder();
		while(currentIndex<data.length && !checkNextCharacter(currentIndex, '"')) {
			//if the next characters are a valid escape sequence, replace the characters as follows
			if(checkNextCharacter(currentIndex, ESCAPE_CHAR)) {
				checkEscapeSequence(VALID_ESCAPE_FOR_STRING);
				currentIndex++;
				switch(data[currentIndex]) {
				case '"':
				case '\\':
					sb.append(data[currentIndex]);
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'r':
					sb.append('\r');
					break;
				case 't':
					sb.append('\t');
					break;
				default:
					//will throw if an escape sequence is added but not implemented here
					throw new SmartScriptLexerException("Unexpected escape sequence.");
				}
				currentIndex++;
			} else {
				//if it is a regular character in the string, append it and move on
				sb.append(data[currentIndex++]);
			}
		}
		//if the loop ended because the end was reached, the quotation marks were not closed
		if(currentIndex>=data.length) {
			throw new SmartScriptLexerException("The quotation marks are never closed!");
		}
		//if they were closed, skip them and return the built string
		currentIndex++;
		String string = sb.toString();
		return new Token(TokenType.STRING, string);
	}
	
	/**
	 * Extracts characters from <code>data</code> while the
	 * predicate <code>p</code> allows it and returns it as a String.
	 * @param p -predicate which tells if the extraction should continue
	 * @param offset -how many characters not to check, but include in the string
	 * @return string made from all the extracted characters
	 * @throws IllegalArgumentException if offset is less than 0
	 * or greater than <code>data.length-currentIndex</code>
	 * @throws NullPointerException if <code>p</code> is <code>null</code>
	 */
	private String extractCharacters(LexerExtractionPredicate p, int offset) {
		if(offset<0) {
			throw new IllegalArgumentException("Offset cannot be negative.");
		}
		if(currentIndex+offset>data.length) {
			throw new IllegalArgumentException("data length was " + data.length +
					" and offset+currentIndex was " + (offset+currentIndex));
		}
		if(p==null) {
			throw new NullPointerException("Predicate cannot be null.");
		}
		int count;
		char[] characters = new char[data.length-currentIndex];
		//add the first *offset* characters without checking with p
		for(count = 0 ; count<offset ; ++count) {
			characters[count] = data[currentIndex++];
		}
		
		while(currentIndex<data.length && p.test()) {
			characters[count++] = data[currentIndex++];
		}
		String token = new String(characters, 0, count);
		return token;
	}
	
	/**
	 * Checks if the next characters mark the beginning of a tag.
	 * @return <code>true</code> if the next 2 characters match the tag beginning sequence,
	 * <code>false</code> otherwise
	 */
	private boolean tagBeginning() {
		//check if there is space for the whole tag sequence
		if(currentIndex==data.length-1) {
			return false;
		}
		//check if it is a tag beginning sequence
		return checkNextCharacter(currentIndex, TAG_BEGINING) &&
				checkNextCharacter(currentIndex+1, TAG_CHAR);
	}
	
	/**
	 * Checks if the next characters mark the ending of a tag.
	 * @return <code>true</code> if the next 2 characters match the tag ending sequence,
	 * <code>false</code> otherwise
	 */
	private boolean tagEnding() {
		//check if there is space for the whole tag sequence
		if(currentIndex==data.length-1) {
			return false;
		}
		//check if it is a tag ending sequence
		return checkNextCharacter(currentIndex, TAG_CHAR) &&
				checkNextCharacter(currentIndex+1, TAG_END);
	}
	
	/**
	 * Checks if the escape sequence is correct by matching the next character
	 * to one of the <code>validCharacters</code>.
	 * @param validCharacters
	 * @throws LexerException if the escape sequence is incorrect
	 */
	private void checkEscapeSequence(char ...validCharacters) {
		if(!checkNextCharacter(currentIndex+1, validCharacters)) {
			throw new SmartScriptLexerException("Invalid use of escape character.");
		}
	}
	
	/**
	 * Checks if the character at <code>index</code> is one of the <code>validCharacters</code>.
	 * @param validCharacters
	 * @return <code>true</code> if the next character matches
	 * one of the <code>validCharacters</code>
	 */
	private boolean checkNextCharacter(int index, char ...validCharacters) {
		if(index>=data.length) {
			return false;
		}
		for(char c : validCharacters) {
			if(data[index]==c) {
				return true;
			}
		}
		return false;
	}
	
}
