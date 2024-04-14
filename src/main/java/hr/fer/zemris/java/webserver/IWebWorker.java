package hr.fer.zemris.java.webserver;

/**
 * Models any object that can process current request: it gets RequestContext as
 * parameter and it is expected to create content for the client.
 * 
 * @author Vedran Kolka
 *
 */
public interface IWebWorker {
	/**
	 * Processes the request in the given <code>context</code>.
	 * 
	 * @param context
	 * @throws Exception
	 */
	public void processRequest(RequestContext context) throws Exception;

}
