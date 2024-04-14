package hr.fer.zemris.java.custom.scripting.parser;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import hr.fer.zemris.java.custom.scripting.elems.Element;
import hr.fer.zemris.java.custom.scripting.elems.ElementConstantDouble;
import hr.fer.zemris.java.custom.scripting.elems.ElementConstantInteger;
import hr.fer.zemris.java.custom.scripting.elems.ElementFunction;
import hr.fer.zemris.java.custom.scripting.elems.ElementOperator;
import hr.fer.zemris.java.custom.scripting.elems.ElementString;
import hr.fer.zemris.java.custom.scripting.elems.ElementVariable;
import hr.fer.zemris.java.custom.scripting.lexer.LexerState;
import hr.fer.zemris.java.custom.scripting.lexer.SmartScriptLexer;
import hr.fer.zemris.java.custom.scripting.lexer.SmartScriptLexerException;
import hr.fer.zemris.java.custom.scripting.lexer.Token;
import hr.fer.zemris.java.custom.scripting.lexer.TokenType;
import hr.fer.zemris.java.custom.scripting.nodes.DocumentNode;
import hr.fer.zemris.java.custom.scripting.nodes.EchoNode;
import hr.fer.zemris.java.custom.scripting.nodes.ForLoopNode;
import hr.fer.zemris.java.custom.scripting.nodes.Node;
import hr.fer.zemris.java.custom.scripting.nodes.TextNode;

/**
 * A program that parses the given text into a document that describes the given text.
 * @author Vedran Kolka
 *
 */
public class SmartScriptParser {
	
	private static final String OPEN_TAG = "{$";
	private static final String CLOSE_TAG = "$}";

	/**
	 * A lexer which the parser uses for getting tokens from the text.
	 */
	private SmartScriptLexer lexer;
	/**
	 * The root node of the document model.
	 */
	private DocumentNode documentNode;
	
	public SmartScriptParser(String text) {
		this.lexer = new SmartScriptLexer(text);
		this.documentNode = new DocumentNode();
		createDocumentTree();
	}
	
	/**
	 * Creates the document tree with the tokens provided by the lexer.
	 * @throws SmartScriptParserException if the text is written incorrectly
	 */
	private void createDocumentTree() {
		
		Stack<Object> stack = new Stack<>();
		stack.push(documentNode);
		int numberOfOpenedNonEmptyTags = 0;
		
		while(true) {
			Token token = null;
			try {
				token = lexer.nextToken();
			} catch(SmartScriptLexerException ex) {
				throw new SmartScriptParserException(ex.getMessage());
			}
			//if it is the end of file, the tree is done
			if(token.getType()==TokenType.EOF) {
				if(numberOfOpenedNonEmptyTags!=0) {
					throw new SmartScriptParserException("A non-empty tag was opened but never closed.");
				}
				return;
			}
			Node newNode = null;
			if(token.getType()==TokenType.TEXT) {
				try {
					newNode = createTextNode(token);
					((Node) stack.peek()).addChildNode(newNode);
					continue;
				} catch(EmptyStackException ex) {
					throw new SmartScriptParserException(ex.getMessage() + "\nInvalid text.");
				}
			}
			if(token.getType()==TokenType.TAG_BEGINNING_SEQUENCE) {
				lexer.setState(LexerState.TAG);
				try {
					token = lexer.nextToken();
				} catch(SmartScriptLexerException ex) {
					throw new SmartScriptParserException(ex.getMessage());
				}
				try {
					if(token.getType() != TokenType.VARIABLE_NAME) {
						throw new SmartScriptParserException("Tag must begin with an apropriate tag name.");
					}
					String tagName = (String) token.getValue();
					if("FOR".equals(tagName.toUpperCase())) {
						//a for tag is added to the list of children and then pushed on the stack
						newNode = createForLoopNode();
						((Node)stack.peek()).addChildNode(newNode);
						stack.push(newNode);
						numberOfOpenedNonEmptyTags++;
					} else if("=".equals(tagName)) {
						//an echo tag is pushed on the stack
						newNode = createEchoNode();
						((Node)stack.peek()).addChildNode(newNode);
					} else if("END".equals(tagName.toUpperCase())) {
						checkTagEndSequence();
						lexer.setState(LexerState.BASIC);
						stack.pop();
						numberOfOpenedNonEmptyTags--;
					} else {
					//if the tag was opened but there was not a recognizable tag name, throw hands
						throw new SmartScriptParserException("Unexpected tag in text.");
					}
				} catch(EmptyStackException ex) {
					throw new SmartScriptParserException("The text contains more END tags than"
							+ " opened non-empty tags.");
				}
			}
			
		}
		
	}
	
	/**
	 * Returns the root of the document model.
	 * @return DocumentNode
	 */
	public DocumentNode getDocumentNode() {
		return documentNode;
	}
	/**
	 * Reconstructs the original document body from the tree which root is the given <code>document</code>.
	 * @param DocumentNode document from which the document body is reconstructed.
	 * @return String representation of the document body
	 */
	public static String createOriginalDocumentBody(DocumentNode document) {
		if(document==null) {
			throw new NullPointerException("Document cannot be reconstructed from null.");
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ; i<document.numberOfChildren() ; ++i) {
			sb.append(createDocumentBody(document.getChild(i)));
		}
		return sb.toString();
	}
	
	//private helper methods----------------------------------------------------------------------
	
	private static String createDocumentBody(Node node) {
		if(node instanceof TextNode) {
			return ((TextNode)node).getText();
		}
		if(node instanceof EchoNode) {
			return reconstructEchoTag((EchoNode)node);
		}
		if(node instanceof ForLoopNode) {
			return reconstructForLoopTag((ForLoopNode)node);
		}
		throw new SmartScriptParserException("Unexpected node type.");
	}
	
	/**
	 * Reconstructs the tag text from the given <code>node</code> in line with the rules of parsing.
	 * @param EchoNode to be reconstructed
	 * @return String that represents the whole echo tag
	 */
	public static String reconstructEchoTag(EchoNode node) {
		String text = OPEN_TAG + "= ";
		for(Element e : node.getElements()) {
			text += e.asText() + " ";
		}
		text += CLOSE_TAG;
		return text;
	}
	
	/**
	 * Reconstructs the tag text from the given <code>node</code> in line with the rules of parsing.
	 * @param ForLoopNode to be reconstructed
	 * @return String that represents the whole for loop tag, including the end tag
	 */
	public static String reconstructForLoopTag(ForLoopNode node) {
		String stepExpressionText = node.getStepExpression()==null ? "" : node.getStepExpression().asText();
		String text = OPEN_TAG + "FOR " +
				node.getVariable().asText() + " " +
				node.getStartExpression().asText() + " " +
				node.getEndExpression().asText() + " " +
				stepExpressionText + " " +
				CLOSE_TAG;
		for(int i = 0 ; i<node.numberOfChildren() ; ++i) {
			text += createDocumentBody(node.getChild(i));
		}
		text += OPEN_TAG + "END " + CLOSE_TAG;
		return text;
	}
	
	/**
	 * Creates a TextNode with the text from the given <code>token</code>.
	 * @param token - a token of TEXT type
	 * @return TextNode with text from the token
	 */
	private static TextNode createTextNode(Token token) {
		return new TextNode((String)token.getValue());
	}
	
	/**
	 * Creates a ForLoopNode with elements enclosed in the tag.
	 * @return ForLoopNode with elements enclosed in the tag
	 * @throws SmartScriptParserException if the for loop statement was invalid
	 */
	private ForLoopNode createForLoopNode() {
		
		try {
			Token token = lexer.nextToken();
			if(token.getType()!=TokenType.VARIABLE_NAME) {
				throw new SmartScriptParserException("FOR LOOP tag should start with a variable name.");
			}
			ElementVariable variable = (ElementVariable)createElement(token);
			Element startExpression = createExpression(lexer.nextToken());
			Element endExpression = createExpression(lexer.nextToken());
			Element stepExpression = createExpression(lexer.nextToken());
			//if the stepExpression was not null, then the TAG_END_SEQUENCE must be the next token
			if(stepExpression!=null) {
				checkTagEndSequence();
				//if it was, change the lexer state
				lexer.setState(LexerState.BASIC);
			}
			return new ForLoopNode(variable, startExpression, endExpression, stepExpression);
		/* The lexer can throw the exception, but also if the TAG_END_SEQUENCE was read
		 * before it was expected, one of the expressions will be null and the constructor
		 * will throw a NullPointerException
		 */
		} catch(SmartScriptLexerException | NullPointerException ex) {
			throw new SmartScriptParserException("Invalid FOR LOOP expression." +
					ex.getMessage());
		}
		
	}
	
	/**
	 * Creates an Element that is a valid expression for the FOR LOOP tag.
	 * Only <b>VARIABLE</b>, <b>NUMBER</b> and <b>STRING</b> are acceptable.
	 * @param token from which the expression element is created.
	 * @return Element if it is valid,
	 * <code>null</code> if <code>token</code> is type of <b>END_TAG</b>
	 * @throws SmartScriptParserException if <code>token</code> is type of <b>FUNCTION</b>
	 * or an Element cannot be created from the given <code>token</code>
	 */
	private Element createExpression(Token token) {
		if(token.getType()==TokenType.FUNCTION) {
			throw new SmartScriptParserException("FOR LOOP tag cannot contain a function.");
		}
		if(token.getType()==TokenType.TAG_END_SEQUENCE) {
			lexer.setState(LexerState.BASIC);
			return null;
		}
		return createElement(token);
	}
	
	/**
	 * Creates an EchoNode with elements enclosed in the tag.
	 * @return EchoNode with elements enclosed in the tag
	 * @throws SmartScriptParserException if the tag is never closed
	 */
	private EchoNode createEchoNode() {

		Token elementToken = null;
		List<Element> elementsCollection = new ArrayList<>();
		do {
			try {
				elementToken = lexer.nextToken();
				if(elementToken.getType()==TokenType.EOF) {
					throw new SmartScriptParserException("The tag is never closed.");
				}
			} catch (SmartScriptLexerException ex) {
				throw new SmartScriptParserException(ex.getMessage());
			}
			if(elementToken.getType()==TokenType.TAG_END_SEQUENCE) {
				lexer.setState(LexerState.BASIC);
				break;
			}
			elementsCollection.add(createElement(elementToken));
			
		} while(true);
		//create an array and cast all Object from elementsCollection to Elements and add them to the array
		Element[] elements = new Element[elementsCollection.size()];
		for(int i = 0 ; i<elementsCollection.size() ; ++i) {
			elements[i] = (Element)elementsCollection.get(i);
		}
		return new EchoNode(elements);
		
	}
	
	/**
	 * Creates an Element from the given <code>token</code>.
	 * @param token from which the Element is created
	 * @return Element with the information from the token
	 * @throws SmartScriptParserException if an element cannot be created
	 * from the given<code>token</code> type
	 */
	private Element createElement(Token token) {
		switch(token.getType()) {
		case VARIABLE_NAME:
			return new ElementVariable((String)token.getValue());
		case STRING:
			return new ElementString((String)token.getValue());
		case OPERATOR:
			return new ElementOperator((String)token.getValue());
		case FUNCTION:
			return new ElementFunction((String)token.getValue());
		case NUMBER:
			double number = (double)token.getValue();
			if(Math.floor(number)==number) {
				return new ElementConstantInteger((int)number);
			} else {
				return new ElementConstantDouble(number);
			}
		default:
			throw new SmartScriptParserException("An element cannot be created from token type "
					+ token.getType());
		}
	}
	
	/**
	 * Checks if the next token is a <b>TAG_END_SEQUENCE</b> token.
	 * @throws SmartScriptParserException if it was not
	 */
	private void checkTagEndSequence() {
		if(lexer.nextToken().getType()!=TokenType.TAG_END_SEQUENCE) {
			throw new SmartScriptParserException("A tag end sequence was expected, but token was type of "
					+ lexer.getToken().getType());
		}
	}
	
}
