package hu.rgai.android.eventlogger;

import static hu.rgai.android.test.Constants.SERVLET_URL;
import static hu.rgai.android.test.Constants.SPACE_STR;
import static hu.rgai.android.test.Constants.apiCodeToAI;
import static hu.rgai.android.test.Constants.appPackageName;
import hu.rgai.android.test.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.util.Log;

public enum EventLogger {
  INSTANCE;

  private static final String SUCCESS_STR = "success";
  private static final String RESULT_STR = "result";
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
  
  public synchronized void writeToLogFile( String log, boolean logTimeStamp) {
    Log.d( "willrgai", "writeToLogFile " + log);
    if ( logTimeStamp) {
      writeFormatedLogToLogFile( LogToJsonConverter.getCurrentTime() + SPACE_STR + log);
    } else {
      writeFormatedLogToLogFile( log);
    }

  }

  private void writeFormatedLogToLogFile(String log) {
    if ( !lockedToUpload ) {
      try {
        bufferedWriter.write(  log);
        bufferedWriter.newLine();
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
    br.close();
    return dateInMillis;
  }
  
  private synchronized void deleteLogFileAndCreateNew()  {
    File logfile = new File( logFilePath );
    logfile.delete();
    openLogFile( logFilePath, true);
    saveTempBufferToLogFileAndClear();
  }

  private void saveTempBufferToLogFileAndClear() {
    lockedToUpload = false;
    for ( String log : tempBufferToUpload) {
      writeToLogFile( log, false);
    }
    tempBufferToUpload.clear();
  }
  
  synchronized boolean uploadLogsAndCreateNewLogfile( Context context ) {
    boolean uploadSucces = true;
    if ( logToJsonConverter.getDeviceId() == null)
      logToJsonConverter.setDeviceId( Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
    lockedToUpload = true;
    try {
      uploadSucces = uploadSucces && uploadLogsToServer( context );
      if ( uploadSucces ) {
        deleteLogFileAndCreateNew();
      } else {
        saveTempBufferToLogFileAndClear();
      }
      return uploadSucces;
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
    } catch ( Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return false;
  }
  
  public synchronized boolean uploadLogsToServer( Context context ) throws ClientProtocolException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException, ParseException, JSONException{
    
    boolean uploadSucces = true;
    
    List<String> logList = getLogListFromLogFile();
    String jsonEncodedLogs = logToJsonConverter.convertLogToJsonFormat(logList);
    if (jsonEncodedLogs != null)
      uploadSucces = uploadSucces && uploadJsonEncodedString(jsonEncodedLogs);
    else
      return false;
    return uploadSucces;
  }

  private boolean uploadJsonEncodedString(String jsonEncodedLogs)
      throws UnsupportedEncodingException, IOException, ClientProtocolException, ParseException, JSONException {
    final HttpPost httpPost = new HttpPost(SERVLET_URL);

    final StringEntity httpEntity = new StringEntity( jsonEncodedLogs, org.apache.http.protocol.HTTP.UTF_8);

    httpEntity.setContentType("application/json");

    httpPost.setEntity(httpEntity); 

    HttpResponse response = getNewHttpClient().execute(httpPost);

    Log.d( "willrgai", "response " + EntityUtils.toString(response.getEntity()));
    Log.d( "willrgai", "response statuscode " + response.getStatusLine().getStatusCode());

    if ( response.getStatusLine().getStatusCode() == 200)
      return true;
    return false;
  }

  private List<String> getLogListFromLogFile() throws FileNotFoundException, IOException {
    List<String> logList = new ArrayList<String>();
    FileReader logFileReader = new FileReader( logFilePath );
    BufferedReader br = new BufferedReader( logFileReader );
    
    String readedLine;
    br.readLine();
    while ( (readedLine = br.readLine()) != null) {
      logList.add(readedLine);
    }
    br.close();
    return logList;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
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
