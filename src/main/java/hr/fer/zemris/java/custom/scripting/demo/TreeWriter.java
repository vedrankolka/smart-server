package hr.fer.zemris.java.custom.scripting.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import hr.fer.zemris.java.custom.scripting.lexer.SmartScriptLexer;
import hr.fer.zemris.java.custom.scripting.nodes.DocumentNode;
import hr.fer.zemris.java.custom.scripting.nodes.EchoNode;
import hr.fer.zemris.java.custom.scripting.nodes.ForLoopNode;
import hr.fer.zemris.java.custom.scripting.nodes.INodeVisitor;
import hr.fer.zemris.java.custom.scripting.nodes.TextNode;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParserException;

/**
 * A demo program that writes the document structure implementing a INodeVisitor
 * and using the visitor design pattern.
 * 
 * @author Vedran Kolka
 *
 */
public class TreeWriter {

	/**
	 * @param args try "src/main/resources/document2.txt"
	 */
	public static void main(String[] args) {

		if (args.length != 1) {
			System.out.println("The program expects exactly one argument - a path to a SmartScript to parse.");
			return;
		}

		try {
			String script = Files.readString(Paths.get(args[0]), StandardCharsets.UTF_8);
			SmartScriptParser p = new SmartScriptParser(script);
			WriterVisitor visitor = new WriterVisitor();
			p.getDocumentNode().accept(visitor);
			System.out.println("\nDone.");

		} catch (IOException e) {
			System.err.println("Reading failed.");
		} catch (SmartScriptParserException e) {
			System.err.println(e.getMessage());
		}

	}

	/**
	 * An implementation of an {@link INodeVisitor} that writes the document it is
	 * visiting to the standard output.
	 * 
	 * @author Vedran Kolka
	 *
	 */
	public static class WriterVisitor implements INodeVisitor {

		@Override
		public void visitTextNode(TextNode node) {
			System.out.print(node.getText());
		}

		@Override
		public void visitForLoopNode(ForLoopNode node) {

			StringBuilder sb = new StringBuilder();

			sb.append(SmartScriptLexer.OPEN_TAG).append("FOR ");
			sb.append(node.getVariable().asText()).append(' ');
			sb.append(node.getStartExpression().asText()).append(' ');
			sb.append(node.getEndExpression().asText()).append(' ');
			if (node.getStepExpression() != null) {
				sb.append(node.getStepExpression().asText());
			}
			sb.append(SmartScriptLexer.CLOSE_TAG);
			System.out.print(sb.toString());

			for (int i = 0; i < node.numberOfChildren(); ++i) {
				node.getChild(i).accept(this);
			}

			System.out.print(SmartScriptLexer.OPEN_TAG + "END" + SmartScriptLexer.CLOSE_TAG);
		}

		@Override
		public void visitEchoNode(EchoNode node) {
			System.out.print(SmartScriptParser.reconstructEchoTag(node));
		}

		@Override
		public void visitDocumentNode(DocumentNode node) {
			for (int i = 0; i < node.numberOfChildren(); ++i) {
				node.getChild(i).accept(this);
			}
		}

	}

}
