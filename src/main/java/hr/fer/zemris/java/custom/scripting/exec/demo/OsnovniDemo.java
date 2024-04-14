package hr.fer.zemris.java.custom.scripting.exec.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import hr.fer.zemris.java.custom.scripting.exec.SmartScriptEngine;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Demo of the SmartScriptEngine with the file osnovni.smscr
 * 
 * @author Vedran Kolka
 *
 */
public class OsnovniDemo {

	public static void main(String[] args) throws IOException {

		String documentBody = readFromDisk("src/main/resources/osnovni.smscr");
		// create engine and execute it
		new SmartScriptEngine(new SmartScriptParser(documentBody).getDocumentNode(),
				new RequestContext(System.out, null, null, null, null)).execute();

	}

	/**
	 * Reads the file with the given path from the disk as a string using UTF-8
	 * encoding.
	 * 
	 * @param path of the file to read
	 * @return read string
	 * @throws IOException
	 */
	public static String readFromDisk(String path) throws IOException {
		Path p = Paths.get(path);
		return Files.readString(p, StandardCharsets.UTF_8);
	}

}
