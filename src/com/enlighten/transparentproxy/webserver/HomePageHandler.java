package com.enlighten.transparentproxy.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.Context;

import com.enlighten.transparentproxy.R;
import com.enlighten.transparentproxy.utils.Utility;

public class HomePageHandler implements HttpRequestHandler {
	private Context context = null;

	public HomePageHandler(Context context) {
		this.context = context;
	}

	@Override
	public void handle(HttpRequest request, HttpResponse response,
			HttpContext httpContext) throws HttpException, IOException {

		HttpResponse httpResponse = forwardRequest(request);

		if (null != httpResponse) {

			Header[] headers = httpResponse.getAllHeaders();

			for (Header header : headers) {
				Header[] internalHeaders = response
						.getHeaders(header.getName());
				if (null == internalHeaders || internalHeaders.length == 0) {
					if (!header.getName().equalsIgnoreCase("content-length")
							&& !header.getName().equalsIgnoreCase(
									"Transfer-encoding")) {
						response.addHeader(header);
					}
				}

			}
			// response.setHeaders(httpResponse.getAllHeaders());
			response.setParams(httpResponse.getParams());
			response.setLocale(httpResponse.getLocale());
			response.setStatusLine(httpResponse.getStatusLine());
			response.setEntity(httpResponse.getEntity());

		} else {

			String contentType = "text/html";
			HttpEntity entity = new EntityTemplate(new ContentProducer() {
				public void writeTo(final OutputStream outstream)
						throws IOException {
					OutputStreamWriter writer = new OutputStreamWriter(
							outstream, "UTF-8");
					String resp = Utility.openHTMLString(context, R.raw.home);

					writer.write(resp);
					writer.flush();
				}
			});

			((EntityTemplate) entity).setContentType(contentType);

			response.setEntity(entity);
		}

	}

	private HttpResponse forwardRequest(HttpRequest request) {
		HttpResponse httpResponse = null;
		HttpClient httpClient = getNewHttpClient();
		/*
		 * AndroidHttpClient httpClient = AndroidHttpClient.newInstance(request
		 * .getHeaders("User-Agent")[0].getValue());
		 */
		try {

			HttpUriRequest uriRequest = null;
			if (request.getRequestLine().getMethod().equals("GET")) {

				uriRequest = new HttpGet("https://"
						+ request.getHeaders("HOST")[0].getValue()
						+ request.getRequestLine().getUri());

			} else if ((request.getRequestLine().getMethod().equals("POST"))) {
				HttpPost post = new HttpPost("https://"
						+ request.getHeaders("HOST")[0].getValue()
						+ request.getRequestLine().getUri());

				post.setEntity(((HttpEntityEnclosingRequest) request)
						.getEntity());
				uriRequest = post;

			}

			for (Header header : request.getAllHeaders()) {
				if (!header.getName().equalsIgnoreCase("content-length")) {
					uriRequest.addHeader(header);
				}
			}

			httpResponse = httpClient.execute(uriRequest);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// TODO need to identify why SocketExcetion : socket closed is
			// thrown when client is closed
			// httpClient.close();
		}

		return httpResponse;
	}

	private String streamToString(InputStream stream) {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		StringBuilder builder = new StringBuilder();
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line + "\n");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return builder.toString();
	}

	public HttpClient getNewHttpClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new CustomSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return new DefaultHttpClient();
		}
	}

	public class CustomSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public CustomSocketFactory(KeyStore truststore)
				throws NoSuchAlgorithmException, KeyManagementException,
				KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
					System.out.println("@@  client trusted @@");
				}

				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
					System.out.println("@@ server trusted @@");
				}

				public X509Certificate[] getAcceptedIssuers() {
					System.out.println("@@ getAcceptedIssuers @@");
					return null;
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port,
				boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host,
					port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

}
