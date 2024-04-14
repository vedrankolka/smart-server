package hr.fer.zemris.java.webserver.workers;

import hr.fer.zemris.java.webserver.IWebWorker;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * An {@link IWebWorker} which simply formats all the given parameters in a
 * table in an html document which is then written to the client.
 * 
 * @author Vedran Kolka
 *
 */
public class EchoParams implements IWebWorker {

	private static final String USAGE_MESSAGE = "<p>Pass in parameters to format them in a table.</p>";

	@Override
	public void processRequest(RequestContext context) throws Exception {
		StringBuilder sb = new StringBuilder();

		if (context.getParameterNames().isEmpty()) {
			context.write(USAGE_MESSAGE);
			return;
		}

		sb.append("<html><body><table><table border=\"1|0\">");

		for (String paramName : context.getParameterNames()) {
			String paramValue = context.getParameter(paramName);
			sb.append("<tr><td>").append(paramName).append("</td><td>").append(paramValue).append("</td></tr>");
		}

		sb.append("</table></body></html>");

		context.write(sb.toString());
	}

}
