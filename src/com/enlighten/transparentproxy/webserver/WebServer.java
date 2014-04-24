package com.enlighten.transparentproxy.webserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.enlighten.transparentproxy.app.AppLog;
import com.enlighten.transparentproxy.constants.Constants;

public class WebServer extends Thread {
	private static final String SERVER_NAME = "AndWebServer";
	private static final String ALL_PATTERN = "*";
	private static final String MESSAGE_PATTERN = "/message*";
	private static final String FOLDER_PATTERN = "/dir*";

	private boolean isRunning = false;
	private Context context = null;
	private int serverPort = 0;

	private BasicHttpProcessor httpproc = null;
	private BasicHttpContext httpContext = null;
	private HttpService httpService = null;
	private HttpRequestHandlerRegistry registry = null;
	private NotificationManager notifyManager = null;

	public WebServer(Context context, NotificationManager notifyManager) {
		super(SERVER_NAME);

		this.setContext(context);
		this.setNotifyManager(notifyManager);

		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		serverPort = Integer
				.parseInt(pref.getString(Constants.PREF_SERVER_PORT, ""
						+ Constants.DEFAULT_SERVER_PORT));
		httpproc = new BasicHttpProcessor();
		httpContext = new BasicHttpContext();

		httpproc.addInterceptor(new ResponseDate());
		httpproc.addInterceptor(new ResponseServer() {
			@Override
			public void process(HttpResponse response, HttpContext context)
					throws HttpException, IOException {
				super.process(response, context);
			}
		});
		httpproc.addInterceptor(new ResponseContent() {
			@Override
			public void process(HttpResponse response, HttpContext context)
					throws HttpException, IOException {
				super.process(response, context);
			}
		});
		httpproc.addInterceptor(new ResponseConnControl() {
			@Override
			public void process(HttpResponse response, HttpContext context)
					throws HttpException, IOException {
				super.process(response, context);
			}
		});

		httpService = new HttpService(httpproc,
				new DefaultConnectionReuseStrategy(),
				new DefaultHttpResponseFactory());

		registry = new HttpRequestHandlerRegistry();

		registry.register(ALL_PATTERN, new HomePageHandler(context));
		registry.register(MESSAGE_PATTERN, new MessageCommandHandler(context,
				notifyManager));
		registry.register(FOLDER_PATTERN, new FolderCommandHandler(context,
				serverPort));

		httpService.setHandlerResolver(registry);
	}

	@Override
	public void run() {
		super.run();

		try {

			SSLServerSocketFactory sf = null;
			// Initialize SSL context
			KeyStore keystore = KeyStore.getInstance("bks");
			try {
				keystore.load(context.getAssets().open("example_store.bks"),
						"kapilpass".toCharArray());

				KeyManagerFactory kmfactory = KeyManagerFactory
						.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmfactory.init(keystore, "kapilpass".toCharArray());
				KeyManager[] keymanagers = kmfactory.getKeyManagers();
				SSLContext sslcontext = SSLContext.getInstance("TLS");
				sslcontext.init(keymanagers, null, null);
				sf = sslcontext.getServerSocketFactory();
				ServerSocket serverSocket = sf.createServerSocket(serverPort,
						0, InetAddress.getLocalHost());
				/*
				 * ServerSocket serverSocket = new ServerSocket(serverPort, 1,
				 * InetAddress.getLocalHost());
				 */
				System.out.println("@@bound address "
						+ serverSocket.getInetAddress());
				// serverSocket.setReuseAddress(true);

				while (isRunning) {

					final Socket socket = serverSocket.accept();

					DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();

					serverConnection.bind(socket, new BasicHttpParams());
					try {
						if (serverConnection.isOpen()) {
							httpService.handleRequest(serverConnection,
									httpContext);

							// serverConnection.shutdown();
						}
					} catch (ConnectionClosedException e) {
						e.printStackTrace();
						AppLog.logDebug("Webserver",
								"eating up client closed connection error");
					} catch (SocketException e) {
						e.printStackTrace();
						AppLog.logDebug("Webserver",
								"eating up socket closed exception");
					}

				}

				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnrecoverableKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyManagementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (CertificateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (KeyStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public synchronized void startThread() {
		isRunning = true;

		super.start();
	}

	public synchronized void stopThread() {
		isRunning = false;
	}

	public void setNotifyManager(NotificationManager notifyManager) {
		this.notifyManager = notifyManager;
	}

	public NotificationManager getNotifyManager() {
		return notifyManager;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Context getContext() {
		return context;
	}
}
