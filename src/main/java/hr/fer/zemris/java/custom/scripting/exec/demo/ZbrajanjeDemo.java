package hr.fer.zemris.java.custom.scripting.exec.demo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import hr.fer.zemris.java.custom.scripting.exec.SmartScriptEngine;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Demo of the SmartScriptEngine with the file zbrajanje.smscr
 * 
 * @author Vedran Kolka
 *
 */
public class ZbrajanjeDemo {

	public static void main(String[] args) throws IOException {

		String documentBody = OsnovniDemo.readFromDisk("src/main/resources/zbrajanje.smscr");
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("a", "4");
		parameters.put("b", "2");
		// create engine and execute it
		new SmartScriptEngine(new SmartScriptParser(documentBody).getDocumentNode(),
				new RequestContext(System.out, parameters, null, null, null)).execute();

	}

}
