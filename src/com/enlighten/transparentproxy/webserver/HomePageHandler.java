package com.enlighten.transparentproxy.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.Context;
import android.net.http.AndroidHttpClient;

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
		AndroidHttpClient httpClient = AndroidHttpClient.newInstance(request
				.getHeaders("User-Agent")[0].getValue());
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
				
				post.setEntity(((HttpEntityEnclosingRequest) request).getEntity());
				uriRequest =post;

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
			//TODO need to identify why SocketExcetion : socket closed is thrown when client is closed 
//			httpClient.close();
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

}
