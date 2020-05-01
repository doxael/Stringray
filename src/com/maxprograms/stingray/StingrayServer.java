package com.maxprograms.stingray;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONObject;

public class StingrayServer implements HttpHandler {

    private static Logger logger = System.getLogger(StingrayServer.class.getName());
    private HttpServer server;
    private AlignmentService service;
    private boolean debug;

    public static void main(String[] args) {
		String port = "8040";
		boolean shouldDebug = false;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-version")) {
				logger.log(Level.INFO, () -> "Version: " + Constants.VERSION + " Build: " + Constants.BUILD);
				return;
			}
			if (arg.equals("-port") && (i + 1) < args.length) {
				port = args[i + 1];
			}
			if (arg.equals("-debug")) {
				shouldDebug = true;
			}
		}
		try {
			StingrayServer instance = new StingrayServer(Integer.valueOf(port));
			instance.setDebug(shouldDebug);
			instance.run();
		} catch (Exception e) {
			logger.log(Level.ERROR, "Server error", e);
		}
    }
    
    public StingrayServer(Integer port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/StingrayServer", this);
        server.setExecutor(new ThreadPoolExecutor(3, 10, 20, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));
        service = new AlignmentService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
			String request = "";
			try (InputStream is = exchange.getRequestBody()) {
				request = readRequestBody(is);
			}
			if (request.isBlank()) {
				throw new IOException("Empty request");
			}
			if (debug) {
				logger.log(Level.INFO, request);
			}
			String response = "";
			JSONObject json = new JSONObject(request);
			String command = json.getString("command");
			switch (command) {
				case "version":
					JSONObject obj = new JSONObject();
					obj.put("tool", "StingrayServer");
					obj.put("version", Constants.VERSION);
					obj.put("build", Constants.BUILD);
					response = obj.toString();
					break;
				case "stop":
					if (debug) {
						logger.log(Level.INFO, "Stopping server");
						break;
					}
				case "getLanguages":
					response = getLanguages();
					break;
				default:
					JSONObject unknown = new JSONObject();
					unknown.put(Constants.STATUS, Constants.ERROR);
					unknown.put(Constants.REASON, "Unknown command");
					unknown.put("received", json.toString());
					response = unknown.toString();
			}
			if (debug) {
				logger.log(Level.INFO, response);
			}
			exchange.getResponseHeaders().add("content-type", "application/json; charset=utf-8");
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, bytes.length);
			try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
				try (OutputStream os = exchange.getResponseBody()) {
					byte[] array = new byte[2048];
					int read;
					while ((read = stream.read(array)) != -1) {
						os.write(array, 0, read);
					}
				}
			}
			if ("stop".equals(command)) {
				logger.log(Level.INFO, "Stopping server");
				System.exit(0);
			}
		} catch (IOException e) {
			logger.log(Level.ERROR, e);
			String message = e.getMessage();
			exchange.sendResponseHeaders(500, message.length());
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(message.getBytes());
			}
		}
    }

    private void setDebug(boolean value) {
		debug = value;
	}

	private void run() {
		server.start();
		logger.log(Level.INFO, "StingrayServer started");
    }
    
    protected static String readRequestBody(InputStream is) throws IOException {
		StringBuilder request = new StringBuilder();
		try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = rd.readLine()) != null) {
				request.append(line);
			}
		}
		return request.toString();
	}

	private String getLanguages() {
		return service.getLanguages().toString();
	}
}