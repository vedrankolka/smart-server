package hr.fer.zemris.java.webserver;

/**
 * 
 * @author Vedran Kolka
 *
 */
public interface IDispatcher {
	/**
	 * Dispatches a request for the given <code>urlPath</code>.
	 * 
	 * @throws Exception
	 */
	void dispatchRequest(String urlPath) throws Exception;

}
