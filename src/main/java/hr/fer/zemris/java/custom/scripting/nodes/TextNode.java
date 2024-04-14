package hr.fer.zemris.java.custom.scripting.nodes;

/**
 * A node representing a piece of textual data.
 * 
 * @author Vedran Kolka
 *
 */
public class TextNode extends Node {
	/**
	 * text of the node
	 */
	private String text;

	public TextNode(String text) {
		this.text = text;
	}

	/**
	 * @return text
	 */
	public String getText() {
		return reconstructEscapeSigns(text);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TextNode))
			return false;
		TextNode other = (TextNode) obj;
		if (text == null) {
			if (other.text == null) {
				return true;
			}
			return false;
		}
		return text.equals(other.text);
	}

	private static String reconstructEscapeSigns(String text) {

		StringBuilder sb = new StringBuilder();
		char[] oldText = text.toCharArray();

		for (int i = 0; i < oldText.length; ++i) {
			// if it is a backslash or a '{' followed by a '$', add an escape before it
			if (oldText[i] == '\\' || (i + 1 < oldText.length && oldText[i + 1] == '$'))
				sb.append('\\');

			sb.append(oldText[i]);
		}
		return sb.toString();
	}

	@Override
	public void accept(INodeVisitor visitor) {
		visitor.visitTextNode(this);
	}
}
