package hr.fer.zemris.java.custom.scripting.exec.demo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import hr.fer.zemris.java.custom.scripting.exec.SmartScriptEngine;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Demo of the SmartScriptEngine with the file brojPoziva.smscr
 * 
 * @author Vedran Kolka
 *
 */
public class BrojPozivaDemo {

	public static void main(String[] args) throws IOException {

		String documentBody = OsnovniDemo.readFromDisk("src/main/resources/brojPoziva.smscr");
		Map<String, String> persistentParameters = new HashMap<String, String>();
		persistentParameters.put("brojPoziva", "3");
		RequestContext rc = new RequestContext(System.out, null, persistentParameters, null, null);
		new SmartScriptEngine(new SmartScriptParser(documentBody).getDocumentNode(), rc).execute();
		System.out.println("Vrijednost u mapi: " + rc.getPersistentParameter("brojPoziva"));

	}

}
