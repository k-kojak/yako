package hu.rgai.android.eventlogger;

import static hu.rgai.android.test.Constants.SERVLET_URL;
import static hu.rgai.android.test.Constants.SPACE_STR;
import static hu.rgai.android.test.Constants.apiCodeToAI;
import static hu.rgai.android.test.Constants.appPackageName;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.test.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
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
import org.json.JSONException;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.util.Log;

public enum EventLogger {
  INSTANCE;

  private volatile BufferedWriter bufferedWriter;
  private boolean logFileOpen = false;
  private String logFilePath;
  private long logfileCreatedTime;
  LogToJsonConverter logToJsonConverter = new LogToJsonConverter( apiCodeToAI, appPackageName);

  public long getLogfileCreatedTime() {
    return logfileCreatedTime;
  }

  private EventLogger() {
  }

  private final ArrayList<String> tempBufferToUpload = new ArrayList<String>();
  private boolean lockedToUpload = false;
  private Context context = null;

  public synchronized boolean openLogFile( String logFilePath, boolean isFullPath) {

    File logfile;
    if (isFullPath) {
      logfile = new File( logFilePath);
    } else {
      if (isSdPresent()) {
        logfile = new File( Environment.getExternalStorageDirectory().getAbsoluteFile(), logFilePath);
      } else {
        logfile = new File( Environment.getDataDirectory().getAbsoluteFile(), logFilePath);
      }
    }
    this.logFilePath = logfile.getPath();
    if (logfile.exists()) {
      try {
        bufferedWriter = new BufferedWriter( new FileWriter( logfile, true));
        logfileCreatedTime = getLogFileCreateDate();
      } catch (NumberFormatException e) {
        try {
          bufferedWriter.close();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
        lockedToUpload = true;
        deleteLogFileAndCreateNew();
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      try {
        bufferedWriter = new BufferedWriter( new FileWriter( logfile));
        logfileCreatedTime = LogToJsonConverter.getCurrentTime();
        bufferedWriter.write( Long.toString( logfileCreatedTime));
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
    logFileOpen = true;
    return true;
  }

  public boolean isLogFileOpen() {
    return logFileOpen;
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
    if (logTimeStamp) {
      writeFormatedLogToLogFile( LogToJsonConverter.getCurrentTime() + SPACE_STR + log);
    } else {
      writeFormatedLogToLogFile( log);
    }

  }

  private void writeFormatedLogToLogFile( String log) {
    if (!lockedToUpload) {
      try {
        bufferedWriter.write( StringEscapeUtils.escapeJava( log));
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else {
      tempBufferToUpload.add( StringEscapeUtils.escapeJava( log));
    }
  }

  private static boolean isSdPresent() {
    return android.os.Environment.getExternalStorageState().equals( android.os.Environment.MEDIA_MOUNTED);
  }

  private synchronized long getLogFileCreateDate() throws NumberFormatException, IOException {
    FileReader logFileReader = new FileReader( logFilePath);
    BufferedReader br = new BufferedReader( logFileReader);
    String readLine = br.readLine();
    Long dateInMillis = Long.valueOf( readLine);
    br.close();
    return dateInMillis;
  }

  private synchronized void deleteLogFileAndCreateNew() {
    File logfile = new File( logFilePath);
    logfile.delete();
    openLogFile( logFilePath, true);
    saveTempBufferToLogFileAndClear();
  }

  private void saveTempBufferToLogFileAndClear() {
    lockedToUpload = false;
    for (String log : tempBufferToUpload) {
      writeToLogFile( log, false);
    }
    tempBufferToUpload.clear();
  }

  synchronized boolean uploadLogsAndCreateNewLogfile( Context context) {
    if (context == null) {
      Log.d("rgai", "CONTEXT IS NULL @ EventLogger.uploadLogsAndCreateNewLogfile");
      return false;
    }
    boolean uploadSucces = true;
    if (logToJsonConverter.getDeviceId() == null) {
      Log.d("rgai", "logToJsonConverter: " + logToJsonConverter.toString());
      Log.d("rgai", "context: " + context.toString()); // TODO: a context neha itt null!!!!
      
      logToJsonConverter.setDeviceId(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
      
    }
    lockedToUpload = true;
    try {
      uploadSucces = uploadSucces && uploadLogsToServer( context);
      if (uploadSucces) {
        deleteLogFileAndCreateNew();
      } else {
        saveTempBufferToLogFileAndClear();
      }
      lockedToUpload = false;
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
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    lockedToUpload = false;
    return false;
  }

  public synchronized boolean uploadLogsToServer( Context context) throws ClientProtocolException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
      KeyManagementException, UnrecoverableKeyException, ParseException, JSONException {

    boolean uploadSucces = true;

    List<String> logList = getLogListFromLogFile();
    String jsonEncodedLogs = logToJsonConverter.convertLogToJsonFormat( logList);
    Log.d( "willrgai", jsonEncodedLogs);
    if (jsonEncodedLogs != null)
      uploadSucces = uploadSucces && uploadJsonEncodedString( jsonEncodedLogs);
    else
      return false;
    return uploadSucces;
  }

  private boolean uploadJsonEncodedString( String jsonEncodedLogs) throws UnsupportedEncodingException, IOException, ClientProtocolException, ParseException, JSONException {
    final HttpPost httpPost = new HttpPost( SERVLET_URL);

    boolean upLoadSuccess = uploadLogs( jsonEncodedLogs, httpPost);
    if (!upLoadSuccess)
      return false;

    upLoadSuccess = uploadCallInformations( httpPost);
    if (!upLoadSuccess)
      return false;
    return upLoadSuccess;
  }

  private boolean uploadCallInformations( final HttpPost httpPost) throws UnsupportedEncodingException, IOException, ClientProtocolException {
    List<String> callInformations = getCallInformations();

    String encryptedContactInformations = logToJsonConverter.convertLogToJsonFormat( callInformations);
    final StringEntity httpContentListEntity = new StringEntity( encryptedContactInformations, org.apache.http.protocol.HTTP.UTF_8);

    httpContentListEntity.setContentType( "application/json");

    httpPost.setEntity( httpContentListEntity);
    HttpResponse response = getNewHttpClient().execute( httpPost);
    return isUploadSuccessFull( response);
  }

  List<String> getCallInformations() {
    List<String> callInformations = new ArrayList<String>();

    Uri allCalls = Uri.parse( "content://call_log/calls");

    Cursor c = context.getContentResolver().query( allCalls, null, null, null, null);

    while (c.moveToNext()) {
      StringBuilder callInformationBuilder = new StringBuilder();
      callInformationBuilder.append( String.valueOf( c.getLong( c.getColumnIndex( "date"))));
      callInformationBuilder.append( SPACE_STR);
      callInformationBuilder.append( "call");
      callInformationBuilder.append( SPACE_STR);
      callInformationBuilder.append( c.getString( c.getColumnIndex( "numbertype")));
      callInformationBuilder.append( SPACE_STR);
      callInformationBuilder.append( c.getString( c.getColumnIndex( "new")));
      callInformationBuilder.append( SPACE_STR);
      callInformationBuilder.append( c.getString( c.getColumnIndex( "duration")));
      callInformationBuilder.append( SPACE_STR);
      callInformationBuilder.append( c.getString( c.getColumnIndex( "_id")));
      callInformationBuilder.append( SPACE_STR);
      callInformationBuilder.append( c.getString( c.getColumnIndex( "numberlabel")));
      callInformationBuilder.append( SPACE_STR);
      callInformationBuilder.append( c.getString( c.getColumnIndex( "name")));
      callInformationBuilder.append( SPACE_STR);
      callInformationBuilder.append( c.getString( c.getColumnIndex( "type")));
      callInformationBuilder.append( SPACE_STR);
      callInformationBuilder.append( RSAENCODING.INSTANCE.encodingString( c.getString( c.getColumnIndex( "number"))));
      callInformations.add( callInformationBuilder.toString());
    }

    return callInformations;
  }

  private boolean uploadLogs( String jsonEncodedLogs, final HttpPost httpPost) throws UnsupportedEncodingException, IOException, ClientProtocolException {
    final StringEntity httpEntity = new StringEntity( jsonEncodedLogs, org.apache.http.protocol.HTTP.UTF_8);
    httpEntity.setContentType( "application/json");
    httpPost.setEntity( httpEntity);
    InputStream reader = httpPost.getEntity().getContent();
    BufferedReader br = null;
    StringBuilder sb = new StringBuilder();
    br = new BufferedReader( new InputStreamReader( reader));
    String line;
    while ((line = br.readLine()) != null) {
      sb.append( line);
    }
    Log.d( "willrgai", sb.toString());
    HttpResponse response = getNewHttpClient().execute( httpPost);
    Log.d( "willrgai", response.getStatusLine().toString());
    return isUploadSuccessFull( response);
  }

  private boolean isUploadSuccessFull( HttpResponse response) {
    if (response.getStatusLine().getStatusCode() != 200)
      return false;
    else
      return true;
  }

  private List<String> getLogListFromLogFile() throws FileNotFoundException, IOException {
    List<String> logList = new ArrayList<String>();
    FileReader logFileReader = new FileReader( logFilePath);
    BufferedReader br = new BufferedReader( logFileReader);

    String readedLine;
    br.readLine();
    while ((readedLine = br.readLine()) != null) {
      logList.add( readedLine);
    }
    br.close();
    return logList;
  }

  public Context getContext() {
    return context;
  }

  public void setContext( Context context) {
    this.context = context;
  }

  public HttpClient getNewHttpClient() {
    final InputStream inputStream = context.getResources().openRawResource( R.raw.trust);
    try {
      final KeyStore trustStore = KeyStore.getInstance( "BKS");
      trustStore.load( inputStream, "6c79be11ab17c202cec77a0ef0ee7ac49f741fb2".toCharArray());

      SSLSocketFactory sf = new MySSLSocketFactory( trustStore);
      sf.setHostnameVerifier( SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

      HttpParams params = new BasicHttpParams();
      HttpProtocolParams.setContentCharset( params, HTTP.UTF_8);

      SchemeRegistry registry = new SchemeRegistry();
      registry.register( new Scheme( "http", PlainSocketFactory.getSocketFactory(), 80));
      registry.register( new Scheme( "https", sf, 443));

      ClientConnectionManager ccm = new ThreadSafeClientConnManager( params, registry);

      return new DefaultHttpClient( ccm, params);
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

  public static class LOGGER_STRINGS {

    public static class OTHER {
      public static final String CLICK_TO_MESSAGEGROUP_STR = "click to messagegroup";
      public static final String SPACE_STR = " ";

    }

    public static class MAINPAGE {
      public static final String PAUSE_STR = "mainpage:pause";
      public static final String RESUME_STR = "mainpage:resume";
      public static final String BACKBUTTON_STR = "mainpage:backbutton";
      public static final String STR = "MainPage";
    }

    public static class EMAIL {
      public static final String EMAIL_BACKBUTTON_STR = "Email:backbutton";
    }

    public static class SCROLL {
      public static final String END_STR = "scroll:end";
      public static final String START_STR = "scroll:start";
    }

  }

}
