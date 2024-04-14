package hr.fer.zemris.java.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 
 * @author Vedran Kolka
 *
 */
public class RequestContext {

	public static class RCCookie {

		/** name of this cookie */
		private String name;
		/** value of this cookie */
		private String value;
		/** domain of this cookie */
		private String domain;
		/** path of this cookie */
		private String path;
		/** maximum age of this cookie's validity */
		private Integer maxAge;
		/** indicates if it is a HttpOnly cookie */
		private boolean httpOnly;
		

		/**
		 * Constructor for RCCookie
		 * 
		 * @param name
		 * @param value
		 * @param maxAge
		 * @param domain
		 * @param path
		 * @param httpOnly
		 */
		public RCCookie(String name, String value, Integer maxAge, String domain, String path, boolean httpOnly) {
			this.name = name;
			this.value = value;
			this.maxAge = maxAge;
			this.domain = domain;
			this.path = path;
			this.httpOnly = httpOnly;
		}

		/**
		 * Getter for <code>name</code>
		 * 
		 * @return name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Getter for <code>value</code>
		 * 
		 * @return value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Getter for <code>domain</code>
		 * 
		 * @return domain
		 */
		public String getDomain() {
			return domain;
		}

		/**
		 * Getter for <code>path</code>
		 * 
		 * @return path
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Getter for <code>maxAge</code>
		 * 
		 * @return maxAge
		 */
		public Integer getMaxAge() {
			return maxAge;
		}

	}

	/** Output stream on which the context writes the answer */
	private OutputStream outputStream;
	/** Charset to be used when encoding */
	@SuppressWarnings("unused")
	private Charset charset;
	/** Encoding of the answer body if it is of type 'text' */
	private String encoding = "UTF-8";
	/** Status code of the answer */
	private int statusCode = 200;
	/** Status message of the answer */
	private String statusText = "OK";
	/** Content type of the answer */
	private String mimeType = "text/html";
	/** Length of the answer (header not included) */
	private Long contentLength;
	/** parameters of this request */
	private Map<String, String> parameters;
	/** temporary parameters of this request, used for internal request dispatching */
	private Map<String, String> temporaryParameters;
	/** persistent parameters of this request */
	private Map<String, String> persistentParameters;
	/** A list of cookies in this request */
	private List<RCCookie> outputCookies;
	/** A flag indicating if the header was already generated */
	private boolean headerGenerated;
	/** A read-only dispatcher of this context */
	private IDispatcher dispatcher;
	/** A session ID to which this request belongs to */
	private String sid;

	/**
	 * Constructor for RequestContext.
	 * 
	 * @param outputStream         cannot be <code>null</code>
	 * @param parameters           if <code>null</code> it is treated as empty
	 * @param persistentParameters if <code>null</code> it is treated as empty
	 * @param outputCookies        if <code>null</code> it is treated as empty
	 * @throws NullPointerException if <code>outputStream</code> is
	 *                              <code>null</code>
	 */
	public RequestContext(OutputStream outputStream, Map<String, String> parameters,
			Map<String, String> persistentParameters, List<RCCookie> outputCookies, String sid) {
		this(outputStream, parameters, persistentParameters, outputCookies, null, null, sid);
	}

	/**
	 * Constructor for RequestContext.
	 * 
	 * @param outputStream         cannot be <code>null</code>
	 * @param parameters           if <code>null</code> it is treated as empty
	 * @param persistentParameters if <code>null</code> it is treated as empty
	 * @param outputCookies        if <code>null</code> it is treated as empty
	 * @param temporaryParameters  if <code>null</code> it is treated as empty
	 * @param dispatcher
	 * @throws NullPointerException if <code>outputStream</code> is <code>null</code> 
	 */
	public RequestContext(OutputStream outputStream, Map<String, String> parameters,
			Map<String, String> persistentParameters, List<RCCookie> outputCookies,
			Map<String, String> temporaryParameters, IDispatcher dispatcher, String sid) {
		
		this.outputStream = Objects.requireNonNull(outputStream);
		parameters = parameters == null ? new HashMap<>() : parameters;
		this.parameters = Collections.unmodifiableMap(parameters);
		this.persistentParameters = persistentParameters == null ? new HashMap<>() : persistentParameters;
		this.temporaryParameters = temporaryParameters == null ? new HashMap<>() : temporaryParameters;
		this.outputCookies = outputCookies == null ? new ArrayList<>() : outputCookies;
		this.dispatcher = dispatcher;
		this.sid = sid;
	}

	/**
	 * Returns the parameter associated with the given <code>name</code> in the map
	 * <code>parameters</code>.
	 * 
	 * @param name of the parameter
	 * @return parameter associated with the <code>name</code>, or <code>null</code>
	 *         if no parameter is associated with the <code>name</code>
	 */
	public String getParameter(String name) {
		return parameters.get(name);
	}

	/**
	 * Returns a read-only set of parameter names.
	 * 
	 * @return read-only set of parameter names
	 */
	public Set<String> getParameterNames() {
		return Collections.unmodifiableSet(parameters.keySet());
	}

	/**
	 * Returns the parameter associated with the given <code>name</code> in the map
	 * <code>persistentParameters</code>.
	 * 
	 * @param name of the parameter
	 * @return parameter associated with the <code>name</code>, or <code>null</code>
	 *         if no parameter is associated with the <code>name</code>
	 */
	public String getPersistentParameter(String name) {
		return persistentParameters.get(name);
	}

	/**
	 * Returns a read-only set of persistentParameter names.
	 * 
	 * @return read-only set of persistentParameter names
	 */
	public Set<String> getPersistentParameterNames() {
		return Collections.unmodifiableSet(persistentParameters.keySet());
	}

	/**
	 * Stores the given <code>value</code> with the given <code>name</code> in the
	 * persistentParameters map.
	 * 
	 * @param name  name of the parameter
	 * @param value value of the parameter
	 */
	public void setPersistentParameter(String name, String value) {
		persistentParameters.put(name, value);
	}

	/**
	 * Removes the value associated with the given <code>name</code> from the
	 * persistentParameters.
	 * 
	 * @param name name of the parameter whose value is removed
	 */
	public void removePersistentParameter(String name) {
		persistentParameters.remove(name);
	}

	/**
	 * Returns the parameter associated with the given <code>name</code> in the map
	 * <code>temporaryParameters</code>.
	 * 
	 * @param name of the parameter
	 * @return parameter associated with the <code>name</code>, or <code>null</code>
	 *         if no parameter is associated with the <code>name</code>
	 */
	public String getTemporaryParameter(String name) {
		return temporaryParameters.get(name);
	}

	/**
	 * Returns a read-only set of temporaryParameter names.
	 * 
	 * @return read-only set of temporaryParameter names
	 */
	public Set<String> getTemporaryParameterNames() {
		return Collections.unmodifiableSet(temporaryParameters.keySet());
	}

	/**
	 * Returns an identifier which is unique for current user session.
	 * 
	 * @return an identifier which is unique for current user session.
	 */
	public String getSessionID() {
		return sid;
	}

	/**
	 * Stores the given <code>value</code> with the given <code>name</code> in the
	 * temporaryParameters map.
	 * 
	 * @param name  name of the parameter
	 * @param value value of the parameter
	 */
	public void setTemporaryParameter(String name, String value) {
		temporaryParameters.put(name, value);
	}

	/**
	 * Removes the value associated with the given <code>name</code> from the
	 * temporaryParameters.
	 * 
	 * @param name name of the parameter whose value is removed
	 */
	public void removeTemporaryParameter(String name) {
		temporaryParameters.remove(name);
	}

	/**
	 * Adds the given <code>cookie</code> to the list of cookies.
	 * 
	 * @param cookie to add to the list
	 */
	public void addRCCookie(RCCookie cookie) {
		outputCookies.add(cookie);
	}

	/**
	 * Getter for the dispatcher.
	 * 
	 * @return dispatcher
	 */
	public IDispatcher getDispatcher() {
		return dispatcher;
	}

	/**
	 * Writes the given <code>data</code> to the outputStream of this RequestContex
	 * but first generates and writes a header if it was not yet generated.
	 * <p>
	 * This method relies on method {@link RequestContext#write(byte[], int, int)}
	 * with arguments offset = 0 and len = data.length .
	 * 
	 * @param data to write to the output stream
	 * @return this RequestContext
	 * @throws IOException
	 */
	public RequestContext write(byte[] data) throws IOException {
		return write(data, 0, data.length);
	}

	/**
	 * Writes the given <code>data</code> from <code>offset</code> to
	 * <code>offset + len</code> (excluded) to the ouputStream of this
	 * RequestContext but first generates and writes a header if it was not yet
	 * generated.
	 * 
	 * @param data   to write
	 * @param offset starting position from which the data should be written
	 * @param len    of the data to be written
	 * @return this RequestContext
	 * @throws IOException
	 */
	public RequestContext write(byte[] data, int offset, int len) throws IOException {

		if (!headerGenerated) {
			byte[] header = generateHeader();
			outputStream.write(header);
			headerGenerated = true;
		}

		outputStream.write(data, offset, len);
		outputStream.flush();

		return this;
	}

	/**
	 * Writes the given <code>text</code> encoded in <code>encoding</code> to the
	 * outputStream of this RequestContext but first generates and writes a header
	 * if it was not yet generated.
	 * <p>
	 * The method relies on the method {@link RequestContext#write(byte[])} giving
	 * it the bytes of the given <code>text</code>
	 * 
	 * @param text to write
	 * @return this RequestContext
	 * @throws IOException
	 */
	public RequestContext write(String text) throws IOException {
		byte[] data = text.getBytes(encoding);
		return write(data);
	}

	private byte[] generateHeader() {
		charset = Charset.forName(encoding);
		StringBuilder sb = new StringBuilder();

		String separator = "\r\n";
		// append the protocol and status message
		sb.append("HTTP/1.1 ").append(statusCode).append(' ').append(statusText).append(separator);
		// append content type and the set charset if it is a text type
		sb.append("Content-Type: ").append(mimeType);
		if (mimeType.startsWith("text/")) {
			sb.append("; charset=").append(encoding);
		}
		sb.append(separator);
		// append content length only if it is specified
		if (contentLength != null) {
			sb.append("Content-Length: ").append(contentLength).append(separator);
		}
		// append a line for each cookie only with specified values
		for (RCCookie c : outputCookies) {
			sb.append("Set-Cookie: ").append(c.name).append('=').append('"').append(c.value).append('"');
			if (c.domain != null) {
				sb.append("; Domain=").append(c.domain);
			}
			if (c.path != null) {
				sb.append("; Path=").append(c.path);
			}
			if (c.maxAge != null) {
				sb.append("; Max-Age=").append(c.maxAge);
			}
			if (c.httpOnly) {
				sb.append("; HttpOnly");
			}
			sb.append(separator);
		}
		// to indicate header ending
		sb.append(separator);

		return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
	}

	/**
	 * Setter for <code>encoding</code>
	 * 
	 * @param encoding to set
	 * @throws RuntimeException if called after the header was generated
	 */
	public void setEncoding(String encoding) {
		checkHeader();
		this.encoding = encoding;
	}

	/**
	 * Setter for <code>statusCode</code>
	 * 
	 * @param statusCode to set
	 * @throws RuntimeException if called after the header was generated
	 */
	public void setStatusCode(int statusCode) {
		checkHeader();
		this.statusCode = statusCode;
	}

	/**
	 * Setter for <code>statusText</code>
	 * 
	 * @param statusText to set
	 * @throws RuntimeException if called after the header was generated
	 */
	public void setStatusText(String statusText) {
		checkHeader();
		this.statusText = statusText;
	}

	/**
	 * Setter for <code>mimeType</code>
	 * 
	 * @param mimeType to set
	 * @throws RuntimeException if called after the header was generated
	 */
	public void setMimeType(String mimeType) {
		checkHeader();
		this.mimeType = mimeType;
	}

	/**
	 * Setter for <code>contentLength</code>
	 * 
	 * @param contentLength to set
	 * @throws RuntimeException if called after the header was generated
	 */
	public void setContentLength(Long contentLength) {
		checkHeader();
		this.contentLength = contentLength;
	}

	/**
	 * Checks if the header was already generated.
	 * 
	 * @throws RuntimeException if the header was already generated
	 */
	private void checkHeader() {
		if (headerGenerated) {
			throw new RuntimeException("Header already generated.");
		}
	}

}
