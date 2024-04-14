package hr.fer.zemris.java.webserver.workers;

import hr.fer.zemris.java.webserver.IWebWorker;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * An {@link IWebWorker} that sums the parameters a and b given through the
 * request context (in <code>parameters</code> map) if they are given correctly.
 * If they are not, a default value is used for calculation, which is 1 for a
 * and 2 for b.
 * <p>
 * The worker uses a smart script calc.smscr for rendering an html as a response
 * to the request.
 * 
 * @author Vedran Kolka
 *
 */
public class SumWorker implements IWebWorker {
	/** the default value for 'a' if it is not set correctly */
	public static final int DEFAULT_A = 1;
	/** the default value for 'b' if it is not set correctly */
	public static final int DEFAULT_B = 2;

	@Override
	public void processRequest(RequestContext context) throws Exception {

		int a = DEFAULT_A;
		int b = DEFAULT_B;

		String paramA = context.getParameter("a");
		String paramB = context.getParameter("b");

		try {
			a = paramA == null || paramA.length() == 0 ? DEFAULT_A : Integer.parseInt(paramA);
		} catch (NumberFormatException e) {
		}
		try {
			b = paramB == null || paramB.length() == 0 ? DEFAULT_B : Integer.parseInt(paramB);
		} catch (NumberFormatException e) {
		}

		context.setTemporaryParameter("varA", Integer.toString(a));
		context.setTemporaryParameter("varB", Integer.toString(b));
		context.setTemporaryParameter("zbroj", Integer.toString(a + b));

		String imgName = ((a + b) % 2 == 0 ? "jabuka.png" : "banana.png");
		context.setTemporaryParameter("imgName", imgName);

		context.getDispatcher().dispatchRequest("/private/pages/calc.smscr");
	}

}
