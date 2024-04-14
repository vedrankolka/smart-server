package hr.fer.zemris.java.webserver.workers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import hr.fer.zemris.java.custom.scripting.exec.SmartScriptEngine;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;
import hr.fer.zemris.java.webserver.IWebWorker;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * An {@link IWebWorker} which reads the argument in parameters under key
 * "bgcolor" and if it is a valid rbg color in hexadecimal notation it is set as
 * a persistent parameter under the same key. <br>
 * Then, the worker creates an html with a link to the home page and a message
 * if the color has been updated or not.
 * 
 * @author Vedran Kolka
 *
 */
public class BgColorWorker implements IWebWorker {
	/** Path to the private script used for generating the html document */
	private static final String BGCOLOR_SCRIPT_PATH = "./webroot/private/pages/bgcolor.smscr";

	@Override
	public void processRequest(RequestContext context) throws Exception {

		String bgcolor = context.getParameter("bgcolor");
		boolean valid = bgcolor.length() == 6 && bgcolor.matches("[0-9a-fA-F]+");
		if (valid) {
			context.setPersistentParameter("bgcolor", bgcolor);
			context.setTemporaryParameter("updated", "");
		} else {
			context.setTemporaryParameter("updated", "not ");
		}
		// delegate the rest to the script
		String documentBody = Files.readString(Paths.get(BGCOLOR_SCRIPT_PATH), StandardCharsets.UTF_8);
		SmartScriptParser parser = new SmartScriptParser(documentBody);
		new SmartScriptEngine(parser.getDocumentNode(), context).execute();
	}

}
