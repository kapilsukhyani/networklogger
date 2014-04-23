package com.enlighten.transparentproxy.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
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
			httpResponse = httpClient.execute(
					new HttpHost(request.getHeaders("HOST")[0].getValue(),443),
					request);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			httpClient.close();
		}

		return httpResponse;
	}

}
