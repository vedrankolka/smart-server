package hr.fer.zemris.java.webserver.workers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import hr.fer.zemris.java.custom.scripting.exec.SmartScriptEngine;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;
import hr.fer.zemris.java.webserver.IWebWorker;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * An {@link IWebWorker} that act as a home page, producing an html document
 * that offers links to scripts and some other workers<br>
 * Also, the page's background can be set by the user through the page.
 * 
 * @author Vedran Kolka
 *
 */
public class Home implements IWebWorker {
	/** Default background color that is set if a color is not set. */
	private static final String DEFAULT_COLOR = "7F7F7F";
	/** Path to the private script that creates the html document */
	private static final String HOME_SCRIPT_PATH = "./webroot/private/pages/home.smscr";

	@Override
	public void processRequest(RequestContext context) throws Exception {
		// see if background color is set, if not, set it to default
		String bgcolor = context.getPersistentParameter("bgcolor");
		String background = bgcolor == null ? DEFAULT_COLOR : bgcolor;
		context.setTemporaryParameter("background", background);
		// delegate the rest to the script
		String documentBody = Files.readString(Paths.get(HOME_SCRIPT_PATH), StandardCharsets.UTF_8);
		SmartScriptParser parser = new SmartScriptParser(documentBody);
		new SmartScriptEngine(parser.getDocumentNode(), context).execute();
	}

}
