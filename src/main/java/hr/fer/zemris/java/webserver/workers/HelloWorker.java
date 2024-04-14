package hr.fer.zemris.java.webserver.workers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import hr.fer.zemris.java.webserver.IWebWorker;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * IWebWorker that will create a HTML page with current time displayed and it will give a
 * different message depending if a parameter called “name” was provided in URL
 * that started this worker
 * 
 * @author Vedran Kolka
 *
 */
public class HelloWorker implements IWebWorker {

	@Override
	public void processRequest(RequestContext context) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		context.setMimeType("text/html");
		String name = context.getParameter("name");
		boolean invalid = name == null || name.trim().isEmpty();

		try {
			context.write("<html><body>");
			context.write("<h1>Hello " + (invalid ? "" : name )+ " !!!</h1>");
			context.write("<p>Now is: " + sdf.format(now) + "</p>");
			if (invalid) {
				context.write("<p>You did not send me your name!</p>");
			} else {
				context.write("<p>Your name has " + name.trim().length() + " letters.</p>");
			}
			context.write("</body></html>");

		} catch (IOException ex) {
			// Log exception to servers log...
			ex.printStackTrace();
		}
	}
}
