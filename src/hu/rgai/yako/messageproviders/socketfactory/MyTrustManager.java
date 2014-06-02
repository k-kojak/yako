package hu.rgai.yako.messageproviders.socketfactory;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class MyTrustManager implements X509TrustManager {

  public boolean isClientTrusted(X509Certificate[] cert) {
    return true;
  }

  public boolean isServerTrusted(X509Certificate[] cert) {
    return true;
  }

  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }

  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    
  }

  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    
  }
}
