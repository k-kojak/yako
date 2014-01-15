package hu.rgai.android.eventlogger;

import static hu.rgai.android.test.Constants.SERVLET_URL;
import static hu.rgai.android.test.Constants.SPACE_STR;
import static hu.rgai.android.test.Constants.apiCodeToAI;
import static hu.rgai.android.test.Constants.appPackageName;
import hu.rgai.android.test.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public enum EventLogger {
  INSTANCE;

  private static final String TEST_MSG_STR = "{\"apiKey\": \"bb6ae46aa833d6faadc8758d07cf00d234984f33\", \"deviceId\": \"10122423432532\", \"package\": \"hu.rgai.android\", \"records\": [ { \"timestamp\": \"102123123213213\", \"data\": \"" + "[{\"eventname\":\"start\"}]".replaceAll( "\"", "\u0020") + "\"   }]}";
  //static Context context;
  private volatile BufferedWriter bufferedWriter;
  private String logFilePath;
  private long logfileCreatedTime;
  LogToJsonConverter logToJsonConverter = new LogToJsonConverter( apiCodeToAI, appPackageName);
  
  public long getLogfileCreatedTime() {
    return logfileCreatedTime;
  }

  private EventLogger(){}
  private ArrayList<String> tempBufferToUpload = new ArrayList<String>();
  private boolean lockedToUpload = false;
  private Context context;
  
  public synchronized boolean openLogFile( String logFilePath, boolean isFullPath ) {

    File logfile;
    if ( isFullPath ) {
      logfile = new File( logFilePath );
    } else {
      if ( isSdPresent() ) {
        logfile =new File( Environment.getExternalStorageDirectory().getAbsoluteFile(), logFilePath );
      } else {
        logfile = new File( logFilePath );
      }
    }

    this.logFilePath = logfile.getPath();
    if ( logfile.exists()){
      try {
        bufferedWriter = new BufferedWriter( new FileWriter(logfile, true) );
        logfileCreatedTime = getLogFileCreateDate();
      } catch (NumberFormatException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      try {
        bufferedWriter = new BufferedWriter( new FileWriter(logfile) );
        logfileCreatedTime = LogToJsonConverter.getCurrentTime();
        bufferedWriter.write( Long.toString( logfileCreatedTime ) );
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
    }
    
    return true;
  }
  

  
  public synchronized boolean closeLogFile() {
    try {
      bufferedWriter.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return true;
  }
  
  public synchronized void writeToLogFile( String log) {
    Log.d( "willrgai", "writeToLogFile " + log);
    if ( !lockedToUpload ) {
      try {
        bufferedWriter.write( LogToJsonConverter.getCurrentTime() + SPACE_STR + log);
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      tempBufferToUpload.add( log );
    }
  }

  private static boolean isSdPresent() {
    return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
  }
  
  private synchronized long getLogFileCreateDate() throws NumberFormatException, IOException {
    FileReader logFileReader = new FileReader( logFilePath );
    BufferedReader br = new BufferedReader( logFileReader );
    long dateInMillis = Long.valueOf( br.readLine() );
    return dateInMillis;
  }
  
  private synchronized void deleteLogFileAndCreateNew()  {
    File logfile = new File( logFilePath );
    logfile.delete();
    openLogFile( logFilePath, true);
    lockedToUpload = false;
    for ( String log : tempBufferToUpload) {
      writeToLogFile(log);
    }
    tempBufferToUpload.clear();    
  }
  
  synchronized boolean uploadLogsAndCreateNewLogfile( Context context ) {
    boolean uploadSucces = true;
    lockedToUpload = true;
    try {
      uploadSucces = uploadSucces && uploadLogsToServer( context );
    } catch (ClientProtocolException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (KeyManagementException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnrecoverableKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (KeyStoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (CertificateException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if ( uploadSucces ) {
      deleteLogFileAndCreateNew();
    } else {
      lockedToUpload = false;
      for ( String log : tempBufferToUpload) {
        writeToLogFile(log);
      }
      tempBufferToUpload.clear();
    }      
    return uploadSucces;
  }
  
  public synchronized boolean uploadLogsToServer( Context context ) throws ClientProtocolException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException{
    
    boolean uploadSucces = true;
    final HttpPost httpPost = new HttpPost(SERVLET_URL);

    final StringEntity httpEntity = new StringEntity( TEST_MSG_STR, org.apache.http.protocol.HTTP.UTF_8);

    httpEntity.setContentType("application/json");

    httpPost.setEntity(httpEntity); 
    HttpResponse response = getNewHttpClient().execute(httpPost);
    Log.d( "willrgai", "response " + EntityUtils.toString(response.getEntity()));
    
    return uploadSucces;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  class MySSLSocketFactory extends SSLSocketFactory {
    SSLContext sslContext = SSLContext.getInstance("TLS");

    public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        super(truststore);

        TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sslContext.init(null, new TrustManager[] { tm }, null);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
        return sslContext.getSocketFactory().createSocket();
    }
  }
  
  public HttpClient getNewHttpClient() {
    final InputStream inputStream = context.getResources().openRawResource( R.raw.trust);
    try {
      final KeyStore trustStore = KeyStore.getInstance("BKS");
      trustStore.load(inputStream, "6c79be11ab17c202cec77a0ef0ee7ac49f741fb2".toCharArray());

      SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
      sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

      HttpParams params = new BasicHttpParams();
      HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

      SchemeRegistry registry = new SchemeRegistry();
      registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
      registry.register(new Scheme("https", sf, 443));

      ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

      return new DefaultHttpClient(ccm, params);
    } catch (Exception e) {
        return new DefaultHttpClient();
    } finally {
      try {
        inputStream.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }


}
