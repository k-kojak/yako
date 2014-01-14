/**
 * 
 */
package hu.rgai.android.eventlogger.communication;

import hu.rgai.android.test.R;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.util.Log;

/**
 * @author ddani
 */
public class HttpClient extends DefaultHttpClient {

	/**
	 * Http port.
	 */
	private static final int HTTP_PORT = 80;

	/**
	 * Https port.
	 */
	private static final int HTTPS_PORT = 443;

	/**
	 * Https scheme.
	 */
	private static final String HTTPS_SCHEME = "https";

	/**
	 * Http scheme.
	 */
	private static final String HTTP_SCHEME = "http";

	/**
	 * Keystore type.
	 */
	private static final String BKS_TYPE = "BKS";

	/**
	 * Android context.
	 */
	private final WeakReference<Context> context;

	/**
	 * Constructor.
	 * 
	 * @param ctx android context
	 * @param httpParams HttpParams
	 */
	public HttpClient(final Context ctx, final HttpParams httpParams) {
		super(httpParams);
		this.context = new WeakReference<Context>(ctx);
	}

	@Override
	protected ClientConnectionManager createClientConnectionManager() {
		final SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme(HTTP_SCHEME, PlainSocketFactory.getSocketFactory(), HTTP_PORT));
		registry.register(new Scheme(HTTPS_SCHEME, this.newSslSocketFactory(), HTTPS_PORT));
		return new ThreadSafeClientConnManager(this.getParams(), registry);
	}

	/**
	 * Create SSLSocketFactory.
	 * 
	 * @return SSLSocketFactory SSLSocketFactory
	 */
	private SSLSocketFactory newSslSocketFactory() {
		final InputStream inputStream = this.context.get().getResources().openRawResource( R.raw.trust);
		try {
			final KeyStore trustStore = KeyStore.getInstance(BKS_TYPE);
			trustStore.load(inputStream, "6c79be11ab17c202cec77a0ef0ee7ac49f741fb2".toCharArray());

			final SSLSocketFactory socketFactory = new STUNnerSSLSocketFactory(trustStore);
			socketFactory.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
			HttpsURLConnection.setDefaultHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
			return socketFactory;
		} catch (final KeyStoreException e) {
			Log.e("KeyStoreException", e.toString());
		} catch (final NoSuchAlgorithmException e) {
			Log.e("NoSuchAlgorithmException", e.toString());
		} catch (final KeyManagementException e) {
			Log.e("KeyManagementException", e.toString());
		} catch (final UnrecoverableKeyException e) {
			Log.e("UnrecoverableKeyException", e.toString());
		} catch (final CertificateException e) {
			Log.e("CertificateException", e.toString());
		} catch (final IOException e) {
			Log.e("IOException", e.toString());
		} finally {
			try {
				inputStream.close();
			} catch (final IOException e) {
				Log.e("IOException", e.toString());
			}
		}

		return null;
	}
}
