package hu.rgai.android.eventlogger.communication;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

import android.util.Log;

/**
 * The Class TrustManager.
 */
public class STUNnerSSLSocketFactory extends SSLSocketFactory {

	/**
	 * Keystore certificate alias.
	 */
	private static final String[] ALIASES = {"0", "wlab", "gremon-aensys-hu", "alarmmannen"};

	/** The ssl context. */
	private final SSLContext sslContext = SSLContext.getInstance(TLS);

	/**
	 * Instantiates a new trust manager.This class manage the https connection certificate.
	 * 
	 * @param truststore the truststore
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 * @throws KeyManagementException the key management exception
	 * @throws KeyStoreException the key store exception
	 * @throws UnrecoverableKeyException the unrecoverable key exception
	 * @throws CertificateException problem with certificate.
	 */
	public STUNnerSSLSocketFactory(final KeyStore truststore)
			throws NoSuchAlgorithmException, KeyManagementException,
			KeyStoreException, UnrecoverableKeyException, CertificateException {
		super(truststore);

		sslContext.init(null,
				new TrustManager[] {new STUNnerTrustManager(truststore)}, null);
	}

	@Override
	public Socket createSocket(final Socket socket, final String host,
			final int port, final boolean autoClose) throws IOException {
		return sslContext.getSocketFactory().createSocket(socket, host,
				port, autoClose);
	}

	@Override
	public Socket createSocket() throws IOException {
		return sslContext.getSocketFactory().createSocket();
	}

	/**
	 * @author rakz
	 */
	private final class STUNnerTrustManager implements X509TrustManager {

		/**
		 * Logtag for trust.
		 */
		private static final String TRUST = "TRUST";

		/**
		 * Keystore.
		 */
		private final KeyStore trustStore;

		/**
		 * Default trust manager.
		 */
		@SuppressWarnings("PMD.ImmutableField")
		private X509TrustManager defaultTrustManager = null;

		/**
		 * @param truststore keystore we trust.
		 * @throws CertificateException problem with certificate.
		 * @throws KeyStoreException problem with keystore.
		 */
		public STUNnerTrustManager(final KeyStore truststore) throws KeyStoreException, CertificateException {
			this.trustStore = truststore;
			if (this.trustStore == null) {
				throw new CertificateException();
			}

			try {
				final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init((KeyStore) null);
				final TrustManager[] trustManagers = tmf.getTrustManagers();
				if (trustManagers != null && trustManagers.length > 0) {
					defaultTrustManager = (X509TrustManager) trustManagers[0];
				} else {
					Log.e(TRUST, "Failed to load default trust manager, no turst manager for default alogirhtm!");
				}
			} catch (final NoSuchAlgorithmException e) {
				Log.e(TRUST, "Failed to load default trust manager!", e);
			}

		}

		/**
		 * @param chain Certificate chain.
		 * @param authType authentication type if cert based auth is used.
		 * @throws CertificateException if something is bad with certs.
		 */
		@Override
		public void checkServerTrusted(final X509Certificate[] chain,
				final String authType) throws CertificateException {

			try {
				// first try with default trust manager
				if (defaultTrustManager == null) {
					throw new CertificateException("Default trust manager is not initialized!");
				} else {
					defaultTrustManager.checkServerTrusted(chain, authType);
				}
			} catch (final CertificateException e) {
				// if there is a problem then try with local
				Log.i(TRUST, "Failed with default, trying local!");
				checkLocal(chain, authType);
			}
		}

		/**
		 * @param chain Certificate chain.
		 * @param authType authentication type if cert based auth is used.
		 * @throws CertificateException if something is bad with certs.
		 */
		private void checkLocal(final X509Certificate[] chain,
				final String authType) throws CertificateException {
			try {

				if (chain == null || chain.length == 0) {
					throw new CertificateException("Null or zero-length certificate chain");
				}

				if (authType == null || authType.length() == 0) {
					throw new CertificateException("Null or zero-length authentication type");
				}

				/*
				 * Try to find certificate in trusted ones.
				 */
				for (int i = 0; i < ALIASES.length; i++) {

					final Certificate certificate = this.trustStore.getCertificate(ALIASES[i]);
					if (certificate == null) {
						continue;
					}
					try {
						/* Check if it has been signed by your CA */
						chain[0].verify(certificate.getPublicKey());
						/* Check certificate validity */
						chain[0].checkValidity();
					} catch (final GeneralSecurityException e) {
						// TODO comment back if debugging.
						// Logger.logException("Certificate check failed.", e);
						continue;
					}

					/*
					 * If we reach this point then verification passed and certificate is valid.
					 */
					return;

				}

				throw new CertificateException("no matching certificate found in trusted store!");

			} catch (final KeyStoreException e) {
				throw new CertificateException("Problem with keystore.", e);
			}
		}

		/**
		 * @return accepted certificate issuers.
		 */
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		/**
		 * @param chain Certificate chain.
		 * @param authType authentication type if cert based auth is used.
		 * @throws CertificateException if something is bad with certs.
		 */
		@Override
		public void checkClientTrusted(final X509Certificate[] chain,
				final String authType) throws CertificateException {
			/*
			 * Check if this should be implemented.
			 */
		}
	};

}
