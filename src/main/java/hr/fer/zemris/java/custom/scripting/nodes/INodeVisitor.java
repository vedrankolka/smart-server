package hr.fer.zemris.java.custom.scripting.nodes;
/**
 * Interface that models a node visitor.
 * @author Vedran Kolka
 *
 */
public interface INodeVisitor {
	/** Visit a {@link TextNode} */
	public void visitTextNode(TextNode node);
	/** Visit a {@link ForLoopNode} */
	public void visitForLoopNode(ForLoopNode node);
	/** Visit a {@link EchoNode} */
	public void visitEchoNode(EchoNode node);
	/** Visit a {@link DocumentNode} */
	public void visitDocumentNode(DocumentNode node);
}
