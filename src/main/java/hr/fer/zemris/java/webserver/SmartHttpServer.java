package hr.fer.zemris.java.webserver;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hr.fer.zemris.java.custom.scripting.exec.SmartScriptEngine;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;
import hr.fer.zemris.java.webserver.RequestContext.RCCookie;

/**
 * A web server that uses HTTP 1.1 protocol. The server also serves HTTP 1.0
 * protocol requests.
 * <p>
 * The server only serves requests with method GET as the other methods are not
 * supported.<br>
 * The server offers a number of web workers and scripts it can run as well as
 * fetching locally stored html, image files and more.
 * <p>
 * The configurations of the server such as IP address, domain, port and other
 * relevant properties are read from a properties file to which a path is given
 * through the constructor.
 * 
 * @author Vedran Kolka
 * @version 1.0
 *
 */
public class SmartHttpServer {

	/**
	 * Expected key of the server address in properties file for configuration of
	 * the server
	 */
	public static final String ADDRESS_KEY = "server.address";
	/**
	 * Expected key of the server domain name in properties file for configuration
	 * of the server
	 */
	public static final String DOMAIN_KEY = "server.domainName";
	/**
	 * Expected key of the server port in properties file for configuration of the
	 * server
	 */
	public static final String PORT_KEY = "server.port";
	/**
	 * Expected key of the number of worker threads of this server in properties
	 * file for configuration of the server
	 */
	public static final String WORKER_THREADS_KEY = "server.workerThreads";
	/**
	 * Expected key of the path to the document root in properties file for
	 * configuration of the server
	 */
	public static final String DOC_ROOT_KEY = "server.documentRoot";
	/**
	 * The expected key for path to mimeType properties file in properties file for
	 * configuration of the server
	 */
	public static final String MIME_CONFIG_KEY = "server.mimeConfig";
	/**
	 * Expected key of the session timeout in properties file for configuration the
	 * server
	 */
	public static final String TIMEOUT_KEY = "session.timeout";
	/**
	 * Expected key of the path to the configuration file for the worker threads in
	 * properties file for configuration of the server
	 */
	public static final String WORKERS_KEY = "server.workers";
	/** the path that should be requested to request an IWebWorker by convention */
	public static final String CONVENTION_DIR = "/ext/";
	/** the package where an {@link IWebWorker} should be placed by convention */
	public static final String CONVENTION_PACKAGE = "hr.fer.zemris.java.webserver.workers";
	/** the length of a session id */
	private static final int SID_LENGTH = 20;
	/** the interval of collecting expired sessions (in seconds) */
	private static final int GARBAGE_COLLECTION_INTERVAL = 5 * 60;

	/** address of this server */
	private String address;
	/** domain name of this server */
	private String domainName;
	/** port of this server */
	private int port;
	/** number of worker threads on this server */
	private int workerThreads;
	/** how long (in seconds) is a session valid */
	private int sessionTimeout;
	/** a map of mime types that this server supports */
	private Map<String, String> mimeTypes = new HashMap<String, String>();
	/** The thread on which the server runs */
	private ServerThread serverThread;
	/** The threadpool of worker threads that process the requests */
	private ExecutorService threadPool;
	/** The root directory of this servers files available from the web */
	private Path documentRoot;
	/** A map of IWebWorkers on this server */
	private Map<String, IWebWorker> workersMap;
	/** A map of this severs active sessions */
	private Map<String, SessionMapEntry> sessions = new HashMap<>();
	/** A Random for generating session IDs */
	private Random sessionRandom = new Random();
	/**
	 * A thread that checks if there are expired sessions to remove from the
	 * sessions map
	 */
	private Thread expiredSessionsCollector;
	/** A flag to indicate that the server has been requested to stop */
	private volatile boolean stopRequested;

	/**
	 * Constructor.<br>
	 * Configures all the parameters from the configuration file whose path is
	 * <code>configFileName</code>.
	 * 
	 * @param configFileName name of the configuration file for this server
	 * @throws RuntimeException if configuration fails, due to invalid file name or
	 *                          invalid configurations
	 */
	public SmartHttpServer(String configFileName) {

		try {
			Map<String, String> serverProperties = loadProperties(null, Paths.get(configFileName));
			// read all the configuration properties
			address = serverProperties.getOrDefault(ADDRESS_KEY, "127.0.0.1");
			domainName = serverProperties.getOrDefault(DOMAIN_KEY, "www.localhost.com");
			port = Integer.parseInt(serverProperties.getOrDefault(PORT_KEY, "8080"));
			workerThreads = Integer.parseInt(serverProperties.getOrDefault(WORKER_THREADS_KEY, "1"));
			sessionTimeout = Integer.parseInt(serverProperties.getOrDefault(TIMEOUT_KEY, "6000"));
			Path mimeConfigPath = Paths.get(serverProperties.get(MIME_CONFIG_KEY));
			mimeTypes = loadProperties(mimeTypes, mimeConfigPath);
			documentRoot = Paths.get(serverProperties.getOrDefault(DOC_ROOT_KEY, "."));
			serverThread = new ServerThread();
			Path workersConfigPath = Paths.get(serverProperties.get(WORKERS_KEY));
			workersMap = loadWorkers(workersConfigPath);
			expiredSessionsCollector = createGarbageThread();

		} catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Configuration of the server failed. " + e.getMessage());
		}

	}

	/**
	 * Creates a simple "garbage collector" daemon thread which wakes up
	 * occasionally to remove expired sessions from the <code>sessions</code> map.
	 * 
	 * @return the created thread
	 */
	private Thread createGarbageThread() {
		Runnable r = () -> {
			while (!stopRequested) {
				try {
					Thread.sleep(GARBAGE_COLLECTION_INTERVAL * 1000);
				} catch (InterruptedException e) {
				}
				List<String> sessionsToRemove = new ArrayList<>();
				synchronized (SmartHttpServer.this) {
					long currentTime = System.currentTimeMillis();
					for (String sid : sessions.keySet()) {
						if (sessions.get(sid).validUntil < currentTime) {
							sessionsToRemove.add(sid);
						}
					}
					sessionsToRemove.forEach(sid -> {
						sessions.remove(sid);
					});
				}
			}
		};
		Thread t = new Thread(r);
		t.setName("Garbage Thread");
		t.setDaemon(true);
		return t;
	}

	/**
	 * Loads the properties from the file with the given <code>path</code> and maps
	 * them to a Map<String, String> which it returns
	 * 
	 * @param map  to be filled with loaded properties, or <code>null</code> if a
	 *             new map is requested
	 * @param path of the properties file
	 * @return Map<String, String> with properties
	 * @throws IOException
	 */
	private Map<String, String> loadProperties(Map<String, String> mapToFill, Path path) throws IOException {

		Map<String, String> map = mapToFill == null ? new HashMap<>() : mapToFill;
		List<String> lines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);

		for (String line : lines) {
			if (line.length() == 0 || line.startsWith("#"))
				continue;
			String[] l = line.split("=");
			String key = l[0].trim();
			String value = l[1].trim();
			map.put(key, value);
		}

		return map;
	}

	/**
	 * Loads the workers to a map of workers from the given path to a properties
	 * file where the key is a "path" and the value is a fully qualified name that
	 * is used to fill the map that is returned with instances of the workers from
	 * the file.
	 * 
	 * @param workersPath
	 * @return map of created workers
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private Map<String, IWebWorker> loadWorkers(Path workersPath)
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

		Map<String, IWebWorker> map = new HashMap<>();

		List<String> lines = Files.readAllLines(workersPath, StandardCharsets.ISO_8859_1);

		for (String line : lines) {
			if (line.length() == 0 || line.startsWith("#"))
				continue;
			String[] l = line.split("=");
			String path = l[0].trim();
			String fqcn = l[1].trim();

			Class<?> referenceToClass = this.getClass().getClassLoader().loadClass(fqcn);
			@SuppressWarnings("deprecation")
			Object newObject = referenceToClass.newInstance();
			IWebWorker iww = (IWebWorker) newObject;

			map.put(path, iww);

		}
		return map;
	}

	/**
	 * Starts the server if it is not already running.
	 */
	protected synchronized void start() {
		if (serverThread.isAlive())
			return;
		threadPool = Executors.newFixedThreadPool(workerThreads, r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});

		serverThread.start();
		expiredSessionsCollector.start();
		System.out.println("Server started.");
	}

	/**
	 * Stops the server if it is running.
	 */
	@SuppressWarnings("deprecation")
	protected synchronized void stop() {
		if (!serverThread.isAlive())
			return;
		threadPool.shutdown();
		stopRequested = true;

		expiredSessionsCollector.stop();
		serverThread.stop();
		System.out.println("Server stopped.");
	}

	/**
	 * Server thread that accepts all requests and delegates them to
	 * {@link ClientWorker}s.
	 * 
	 * @author Vedran Kolka
	 *
	 */
	protected class ServerThread extends Thread {
		
		protected ServerThread() {
			super();
			setName("Server Thread");
		}
		
		@Override
		public void run() {
			// open serverSocket on specified port
			try (ServerSocket serverSocket = new ServerSocket()) {

				serverSocket.bind(new InetSocketAddress(address, port));

				while (!stopRequested) {
					Socket client = serverSocket.accept();
					ClientWorker cw = new ClientWorker(client);
					threadPool.submit(cw);
				}

			} catch (IOException e) {
				System.err.println("Server broke down. " + e.getClass() + ": " + e.getMessage());
				System.err.println("Shuting down…");
				SmartHttpServer.this.stop();
			}

		}
	}

	/**
	 * A worker and dispatcher for clients to which the port is given through the
	 * constructor.
	 * 
	 * @author Vedran Kolka
	 *
	 */
	private class ClientWorker implements Runnable, IDispatcher {

		/** Socket of the connection with the client */
		private Socket csocket;
		/** Input stream of the socket */
		private PushbackInputStream istream;
		/** Output stream of the socket */
		private OutputStream ostream;
		/** version of the used protocol */
		private String version;
		/** requested method */
		private String method;
		/** host name of the client */
		private String host;
		/** parameters of the RequestContext */
		private Map<String, String> params = new HashMap<>();
		/** temporaryParameters of the RequestContext */
		private Map<String, String> tempParams = new HashMap<>();
		/** persistentParameters of the RequestContext */
		private Map<String, String> permPrams = new HashMap<>();
		/** output cookies of the RequestContext */
		private List<RCCookie> outputCookies = new ArrayList<>();
		/** Session ID */
		private String SID;
		/** Context of this request */
		private RequestContext rc;

		/**
		 * Constructor.
		 * 
		 * @param csocket - socket through which the request is obtained and read and
		 *                through which the worker responds.
		 */
		public ClientWorker(Socket csocket) {
			super();
			this.csocket = csocket;
		}

		@Override
		public void run() {

			try {

				istream = new PushbackInputStream(csocket.getInputStream());
				ostream = new BufferedOutputStream(csocket.getOutputStream());

				byte[] requestData = readRequest(istream);

				if (requestData == null) {
					sendError(400, "Bad request.");
					return;
				}

				String requestString = new String(requestData, StandardCharsets.US_ASCII);
				List<String> requestHeader = extractHeader(requestString);

				String[] firstLine = requestHeader.isEmpty() ? null : requestHeader.get(0).split(" ");

				if (firstLine == null || firstLine.length != 3) {
					sendError(400, "Bad request");
					return;
				}

				method = firstLine[0].toUpperCase();
				version = firstLine[2].toUpperCase();

				if (!method.equals("GET")) {
					sendError(400, "Method Not Allowed");
					return;
				}

				if (!(version.equals("HTTP/1.0") || version.equals("HTTP/1.1"))) {
					sendError(400, "HTTP Version Not Supported");
					return;
				}

				for (int i = 1; i < requestHeader.size(); ++i) {
					String line = requestHeader.get(i);
					if (line.startsWith("Host")) {
						// Host: someHostName:port -> someHostname
						host = line.split(":")[1].trim();
						break;
					}
				}
				// if the host was not named, set domain name as host
				host = host == null ? domainName : host;

				checkSession(requestHeader);

				String[] requestedPath = firstLine[1].split("\\?");

				if (requestedPath.length == 2) {
					String paramString = requestedPath[1];
					parseParameters(paramString);
				}

				internalDispatchRequest(requestedPath[0], true);

			} catch (NullPointerException | IndexOutOfBoundsException | IllegalArgumentException e) {
				try {
					System.err.println("Bad request. " + e.getClass() + ": " + e.getMessage());
					sendError(400, e.getMessage());
				} catch (IOException e1) {
					System.err.println("Comunication failed. " + e1.getMessage());
				}
			} catch (IOException e) {
				System.err.println("Socket with address  " + csocket.getInetAddress() + "broken.");
				e.printStackTrace();
			} catch (Exception e) {
				// if it is not one of the expected exception, 'log' it
				e.printStackTrace();
			} finally {
				try {
					csocket.close();
				} catch (IOException e) {
					System.err.println("Closing of the socket failed.");
				}
			}
		}

		public void internalDispatchRequest(String urlPath, boolean directCall) throws Exception {

			if (directCall && (urlPath.startsWith("/private/") || urlPath.equals("/private"))) {
				sendError(403, "forbidden");
				// log the attack attempt
				System.err.println("Someone tried to touch our privates!");
				return;
			}

			// first check if it is a path of a worker that is in the map
			IWebWorker iww = workersMap.get(urlPath);
			if (iww != null) {
				if (rc == null)
					rc = new RequestContext(ostream, params, permPrams, outputCookies, tempParams, this, SID);
				iww.processRequest(rc);
				return;
			}
			// then check if it is a worker called by convention
			if (urlPath.startsWith(CONVENTION_DIR)) {
				String fqcn = CONVENTION_PACKAGE + "." + urlPath.substring(5);
				Class<?> referenceToClass = this.getClass().getClassLoader().loadClass(fqcn);
				@SuppressWarnings("deprecation")
				Object newObject = referenceToClass.newInstance();
				iww = (IWebWorker) newObject;
				if (rc == null)
					rc = new RequestContext(ostream, params, permPrams, outputCookies, tempParams, this, SID);
				iww.processRequest(rc);
				return;
			}
			// then it is a normal request and we proceed as usual-> first strip the '/' and
			// resolve the path
			String pathInWebroot = urlPath.substring(1);
			Path path = documentRoot.resolve(pathInWebroot);
			// if it is not a direct/indirect child of the documentRoot, send forbidden
			// error
			if (!path.toAbsolutePath().startsWith(documentRoot.toAbsolutePath())) {
				sendError(403, "forbidden");
				return;
			}

			// determine the extension and execute if it is a smart script or look for the
			// mimeType in the map, if not found, set octet-stream
			String extension = extractExtension(path.getFileName().toString());

			if (extension != null && extension.equals("smscr")) {
				String documentBody = Files.readString(path, StandardCharsets.UTF_8);
				SmartScriptParser parser = new SmartScriptParser(documentBody);

				if (rc == null) {
					rc = new RequestContext(ostream, params, permPrams, outputCookies, tempParams, this, SID);
				}
				// create engine and execute it
				new SmartScriptEngine(parser.getDocumentNode(), rc).execute();
				return;
			}

			String mimeTypeFromMap = mimeTypes.get(extension);
			String mimeType = mimeTypeFromMap == null ? "application/octet-stream" : mimeTypeFromMap;

			// if it does not exist, or if it not a file or if it is not readable send error
			// message
			if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
				sendError(404, "file not found");
				return;
			}

			if (rc == null) {
				rc = new RequestContext(ostream, params, permPrams, outputCookies, SID);
			}

			rc.setMimeType(mimeType);
			rc.setStatusCode(200);
			rc.setStatusText("OK");

			// finally, read the requested file and write it to the context
			byte[] fileData = Files.readAllBytes(path);
			rc.write(fileData);

		}

		@Override
		public void dispatchRequest(String urlPath) throws Exception {
			internalDispatchRequest(urlPath, false);
		}

		private String extractExtension(String fileName) {
			int dot = fileName.lastIndexOf('.');

			if (dot == -1)
				return null;

			return fileName.substring(dot + 1, fileName.length());
		}

		/**
		 * Parses the given string and fills the params map.
		 * 
		 * @param paramString
		 */
		private void parseParameters(String paramString) {

			for (String entry : paramString.split("&")) {
				String[] e = entry.split("=");
				String paramName = e[0];
				String paramValue = e.length < 2 ? null : e[1];
				params.put(paramName, paramValue);
			}
		}

		/**
		 * Creates and sends a simple answer with given <code>statusCode</code> and
		 * <code>statusText</code> and no body.
		 * 
		 * @param statusCode to write in the answer header
		 * @param statusText to write in the answer header
		 * @throws IOException
		 */
		private void sendError(int statusCode, String statusText) throws IOException {

			if (rc == null) {
				rc = new RequestContext(ostream, params, permPrams, outputCookies, tempParams, this, SID);
			}

			rc.setContentLength(0L);
			rc.setMimeType("text/plain");
			rc.setStatusCode(statusCode);
			rc.setStatusText(statusText);

			rc.write(new byte[0]);

		}

		/**
		 * Checks if a request is from an active session.<br>
		 * If it is not from a session that exists or if it is from a session that has
		 * expired, it creates a new session and adds it to the <code>sessions</code>
		 * map.
		 * 
		 * @param requestHeader - list of lines of the request header
		 */
		private void checkSession(List<String> requestHeader) {

			long currentTime = System.currentTimeMillis();

			synchronized (SmartHttpServer.this) {

				String sidCandidate = null;

				l: for (String line : requestHeader) {
					if (!line.startsWith("Cookie:"))
						continue;
					// strip "Cookie:" and split the rest by ';'
					line = line.substring(7);
					String[] cookies = line.split(";");

					for (String cookie : cookies) {
						cookie = cookie.trim();
						String[] cookieParams = cookie.split("=");
						if (cookieParams[0].equals("sid")) {
							// remove the quotation marks from the session id
							sidCandidate = cookieParams[1].substring(1, cookieParams[1].length() - 1);
							break l;
						}
					}
				}
				SessionMapEntry validSession;
				// if a sid cookie was not found, create a new session
				if (sidCandidate == null) {
					validSession = createNewSession();
					// else check if the candidate is valid
				} else {
					validSession = sessions.get(sidCandidate);
					// if the host does not match, create new session
					if (validSession == null || !validSession.host.equals(host)) {
						validSession = createNewSession();
						// if the session expired, remove it and create new session
					} else if (validSession.validUntil < System.currentTimeMillis()) {
						sessions.remove(validSession.sid);
						validSession = createNewSession();
						// else update the session's validUntil since it is an active session
					} else {
						validSession.validUntil = currentTime + sessionTimeout * 1000;
					}
				}
				SID = validSession.sid;
				// set this client workers persistent parameters to the map of the session
				permPrams = validSession.map;
			}
		}

		/**
		 * Creates a new {@link SessionMapEntry} with a new unique session ID (sid or
		 * SID). Creates an appropriate cookie for the session <b>and adds it to
		 * <code>outputCookies</code></b>.
		 * 
		 * @return created SessionMapEntry
		 */
		private SessionMapEntry createNewSession() {

			SID = getNewSID(sessionRandom);

			long validUntil = System.currentTimeMillis() + sessionTimeout * 1000;
			Map<String, String> map = new ConcurrentHashMap<>();
			SessionMapEntry newSession = new SessionMapEntry(SID, host, validUntil, map);
			RCCookie c = new RCCookie("sid", SID, null, host, "/", true);

			sessions.put(SID, newSession);
			outputCookies.add(c);

			return newSession;

		}

	}

	/**
	 * Breaks the given string to lines by separating with '\n' and concatenates the
	 * lines which are lines belonging to the same attribute (the same attributes
	 * value written through more lines)
	 * 
	 * @param requestHeader to break
	 * @return List of lines of the header given in bytes
	 */
	private static List<String> extractHeader(String requestHeader) {

		List<String> headers = new ArrayList<String>();
		String currentLine = null;

		for (String s : requestHeader.split("\n")) {
			if (s.isEmpty())
				break;
			char c = s.charAt(0);
			if (c == 9 || c == 32) {
				currentLine += s;
			} else {
				if (currentLine != null) {
					headers.add(currentLine);
				}
				currentLine = s;
			}
		}

		if (!currentLine.isEmpty()) {
			headers.add(currentLine);
		}

		return headers;
	}

	/**
	 * Reads the bytes from the given <code>is</code> until a sequence "\r\n\r\n"
	 * occurs.
	 * 
	 * @param is InputStream from which the bytes are read
	 * @return array of read bytes
	 * @throws IOException
	 */
	private static byte[] readRequest(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int state = 0;
		l: while (true) {
			int b = is.read();
			if (b == -1)
				return null;
			if (b != 13) {
				bos.write(b);
			}
			switch (state) {
			case 0:
				if (b == 13) {
					state = 1;
				} else if (b == 10)
					state = 4;
				break;
			case 1:
				if (b == 10) {
					state = 2;
				} else
					state = 0;
				break;
			case 2:
				if (b == 13) {
					state = 3;
				} else
					state = 0;
				break;
			case 3:
				if (b == 10) {
					break l;
				} else
					state = 0;
				break;
			case 4:
				if (b == 10) {
					break l;
				} else
					state = 0;
				break;
			}
		}
		return bos.toByteArray();
	}

	/**
	 * Creates a random String of SID_LENGTH uppercase letters with the given Random
	 * object
	 * 
	 * @param r {@link Random} for generating the string
	 * @return randomly generated string of length SID_LENGTH
	 */
	private static String getNewSID(Random r) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < SID_LENGTH; ++i) {
			char c = (char) (r.nextInt(26) + 65);
			sb.append(c);
		}

		return sb.toString();
	}

	/**
	 * An entry for the sessions map, holding all relevant data for a session,
	 * mapped by a session's ID (sid / SID).
	 * 
	 * @author Vedran Kolka
	 *
	 */
	private static class SessionMapEntry {
		/** session ID */
		String sid;
		/** host of the session */
		String host;
		/** how long is the session valid */
		long validUntil;
		/** Map of the persistent parameters of this session */
		Map<String, String> map;

		/**
		 * Constructor.
		 * 
		 * @param sid        unique session ID
		 * @param host       name of the server of the session
		 * @param validUntil until when is the session valid
		 * @param map        of persistent parameters of this session
		 */
		public SessionMapEntry(String sid, String host, long validUntil, Map<String, String> map) {
			super();
			this.sid = sid;
			this.host = host;
			this.validUntil = validUntil;
			this.map = map;
		}

	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("A path to a configuration file was expected as an argument.");
			return;
		}
		SmartHttpServer server = new SmartHttpServer(args[0]);
		server.start();

		try (Scanner sc = new Scanner(System.in)) {
			System.out.println("To shutdown, type 'exit'");
			while (true) {
				String command = sc.next();
				if (command.equals("exit")) {
					server.stop();
					break;
				}
			}
		}
		System.exit(0);
	}
}
