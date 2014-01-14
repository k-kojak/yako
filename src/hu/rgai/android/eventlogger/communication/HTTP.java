/**
 * 
 */
package hu.rgai.android.eventlogger.communication;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;

/**
 * Utility class to perform HTTP POST and GET requests.
 * 
 * @author ddani
 */
public final class HTTP {

	/**
	 * Inputstream buffer size.
	 */
	private static final int BUFFER_SIZE = 1024;

	/**
	 * Connection timeout.
	 */
	private static final int CONNECTION_TIMEOUT = 20000;

	/**
	 * String constant for response mapping preparation.
	 */
	private static final String EMPTY_STRING = "";

	/**
	 * String constant for response mapping preparation.
	 */
	private static final String HTML_CLOSE_TAG = "</html>";

	/**
	 * String constant for response mapping preparation.
	 */
	private static final String HTML_OPEN_TAG = "<html>";

	/**
	 * Hidden constructor.
	 */
	private HTTP() {
	}

	/**
	 * Convert HttpResponse to String.
	 * 
	 * @param response HttpResponse
	 * @return http response string
	 * @throws IOException general input output exception.
	 */
	private static String httpResponseToString(final HttpResponse response)
			throws IOException {

		/*
		 * If we have a response, see the HTTP response code first.
		 */
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode < HttpStatus.SC_OK
				|| statusCode >= HttpStatus.SC_MULTIPLE_CHOICES || response.getEntity() == null) {
			throw new HttpResponseException(statusCode, "Failed to get response!");
		}

		/*
		 * If response is fine, try to decode it.
		 */
		final String responseBody = new String(EntityUtils.toString(response.getEntity()).getBytes(),
				org.apache.http.protocol.HTTP.UTF_8);
		return responseBody.replace(HTML_OPEN_TAG, EMPTY_STRING).replace(HTML_CLOSE_TAG, EMPTY_STRING);

	}

	/**
	 * HTTP POST operation with custom timeout.
	 * 
	 * @param url URL for the operation
	 * @param body request body
	 * @param timeout custom timeout
	 * @param context Context
	 * @return HttpResponse response
	 * @throws IOException input/output exception
	 */
	public static String post(final Context context, final String url,
			final String body, final int timeout) throws IOException {

		final HttpPost httpPost = new HttpPost(url);
		final StringEntity httpEntity = new StringEntity(body, org.apache.http.protocol.HTTP.UTF_8);
		httpEntity.setContentType("application/json");
		httpPost.setEntity(httpEntity);

		/*
		 * Invoke Http client.
		 */
		return httpResponseToString(new HttpClient(context, createHttpParams(timeout)).execute(httpPost));
	}

	/**
	 * HTTP POST operation with custom timeout.
	 * 
	 * @param url URL for the operation
	 * @param timeout custom timeout
	 * @param context Context
	 * @return InputStream response
	 * @throws IOException input/output exception
	 */
	public static InputStream postWithIsResponse(final Context context, final String url, final int timeout) throws IOException {
		final HttpPost httpPost = new HttpPost(url);
		/*
		 * Invoke Http client.
		 */
		final HttpResponse httpResponse = new HttpClient(context, createHttpParams(timeout)).execute(httpPost);
		final HttpEntity httpEntity = httpResponse.getEntity();
		return httpEntity.getContent();
	}

	/**
	 * HTTP POST operation with custom timeout.
	 * 
	 * @param url URL for the operation
	 * @param timeout custom timeout
	 * @param context Context
	 * @return byte array
	 * @throws IOException input/output exception
	 */
	public static byte[] post(final Context context, final String url, final int timeout) throws IOException {
		final HttpPost httpPost = new HttpPost(url);
		return httpResponseToByteArray(new HttpClient(context, createHttpParams(timeout)).execute(httpPost));
	}

	/**
	 * Convert HttpResponse to byte array.
	 * 
	 * @param response HttpResponse
	 * @return http response string
	 * @throws IOException general input output exception.
	 */
	private static byte[] httpResponseToByteArray(final HttpResponse response) throws IOException {

		/*
		 * If we have a response, see the HTTP response code first.
		 */
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode < HttpStatus.SC_OK
				|| statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
			throw new HttpResponseException(statusCode, "Failed to get response!");
		}

		final InputStream inputStream = new BufferedInputStream(response.getEntity().getContent());
		final ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

		// this is storage overwritten on each iteration with bytes
		final byte[] buffer = new byte[BUFFER_SIZE];

		// we need to know how may bytes were read to write them to the byteBuffer
		int len = inputStream.read(buffer);
		while (len != -1) {
			byteBuffer.write(buffer, 0, len);
			len = inputStream.read(buffer);
		}
		inputStream.close();
		// and then we can return your byte array.
		return byteBuffer.toByteArray();
	}

	/**
	 * Create http params.
	 * 
	 * @param timeout custom timeout
	 * @return {@link HttpParams}
	 */
	public static HttpParams createHttpParams(final int timeout) {
		final HttpParams httpParameters = new BasicHttpParams();
		/*
		 * Connection timeout.
		 */
		HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIMEOUT);

		/*
		 * Socket timeout.
		 */
		HttpConnectionParams.setSoTimeout(httpParameters, timeout);

		/*
		 * Timeout for retreiving the manged connection from connection pool.
		 */
		ConnManagerParams.setTimeout(httpParameters, timeout);

		HttpProtocolParams.setContentCharset(httpParameters,
				org.apache.http.protocol.HTTP.UTF_8);
		HttpProtocolParams.setHttpElementCharset(httpParameters,
				org.apache.http.protocol.HTTP.UTF_8);
		return httpParameters;
	}
}
