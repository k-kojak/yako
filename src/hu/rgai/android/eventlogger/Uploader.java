package hu.rgai.android.eventlogger;

import static hu.rgai.android.test.Constants.SERVLET_URL;
import static hu.rgai.android.test.Constants.SPACE_STR;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.test.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import android.provider.ContactsContract;
import android.provider.Settings.Secure;
import android.util.Log;

class Uploader implements Runnable {
  private static final String UPLOAD_FAILED_STR = "upload failed";

  public Uploader( LogToJsonConverter logToJsonConverter, Context context) {
    super();
    this.logToJsonConverter = logToJsonConverter;
    this.context = context;
  }

  private static final String CONTACTINFO_STR = "contactinfo";

  private static final String PHONE_NUMBER_STR = "PHONE_NUMBER";
  private static final String EMAILS_STR = "EMAILS";
  LogToJsonConverter logToJsonConverter;
  Context context;

  @Override
  public void run() {
    if ( context == null ) {
      Log.d("rgai", "CONTEXT IS NULL @ EventLogger.uploadLogsAndCreateNewLogfile");
      return;
    }

    if ( logToJsonConverter.getDeviceId() == null ) {
      Log.d("rgai", "logToJsonConverter: " + logToJsonConverter.toString());
      Log.d("rgai", "context: " + context.toString()); // TODO: a context neha itt null!!!!

      logToJsonConverter.setDeviceId(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
    }
    EventLogger.INSTANCE.lockedToUpload = true;
    try {
      if ( uploadLogsToServer(context) ) {
        EventLogger.INSTANCE.deleteLogFileAndCreateNew();
      } else {
        EventLogger.INSTANCE.writeToLogFile(UPLOAD_FAILED_STR, true);
      }
    } catch ( Exception e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    EventLogger.INSTANCE.saveTempBufferToLogFileAndClear();
  }

  public synchronized boolean uploadLogsToServer( Context context) throws ClientProtocolException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException, ParseException, JSONException {

    List<String> logList = getLogListFromLogFile();
    String jsonEncodedLogs = logToJsonConverter.convertLogToJsonFormat(logList);
    Log.d("willrgai", jsonEncodedLogs);
    if ( jsonEncodedLogs == null )
      return false;

    final HttpPost httpPost = new HttpPost(SERVLET_URL);

    if ( !uploadLogs(jsonEncodedLogs, httpPost) )
      return false;

    if ( !uploadCallInformations(httpPost) )
      return false;

    if ( !uploadContentInformations(httpPost) )
      return false;
    return true;
  }

  private boolean uploadContentInformations( final HttpPost httpPost) {
    List<String> contentInformations = getContentInformations();
    String encryptedContactInformations = logToJsonConverter.convertLogToJsonFormat(contentInformations);
    StringEntity httpContentListEntity;
    HttpResponse response = null;
    try {
      httpContentListEntity = new StringEntity(encryptedContactInformations, org.apache.http.protocol.HTTP.UTF_8);
      httpContentListEntity.setContentType("application/json");
      response = getNewHttpClient().execute(httpPost);
    } catch ( UnsupportedEncodingException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    } catch ( ClientProtocolException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    } catch ( IOException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    }

    return isUploadSuccessFull(response);

  }

  @SuppressWarnings("unused")
  List<String> getContentInformations() {
    List<String> contentInformations = new ArrayList<String>();
    Cursor cursor;
    String uploadTime = Long.toString(LogToJsonConverter.getCurrentTime());
    String imWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";

    cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
    while ( cursor.moveToNext() ) {
      String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

      // Get E-mails

      String emailsWhere = ContactsContract.Data.CONTACT_ID + " = ? ";
      String[] emailsWhereParams = new String[] { contactId };

      Cursor emails = context.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, emailsWhere, emailsWhereParams, null);
      String emailAddress;
      while ( emails.moveToNext() ) {
        addEmailContactInfos(contentInformations, uploadTime, contactId, emails);
      }
      emails.close();

      // Get Numbers

      String numbersWhere = ContactsContract.Data.CONTACT_ID + " = ? ";
      String[] numbersWhereParams = new String[] { contactId };

      Cursor numbers = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, numbersWhere, numbersWhereParams, null);

      while ( numbers.moveToNext() ) {
        addPhoneNumberContactInfos(contentInformations, uploadTime, contactId, numbers);
      }

      numbers.close();

      // Get Instant Messenger.........
      String[] imWhereParams = new String[] { contactId, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE };

      Cursor imCur = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, imWhere, imWhereParams, null);
      while ( imCur.moveToNext() ) {
        addInstantMessengerContactInfos(contentInformations, uploadTime, contactId, imCur);
      }
      imCur.close();
    }
    cursor.close();
    return contentInformations;

  }

  private void addInstantMessengerContactInfos( List<String> contentInformations, String uploadTime, String contactId, Cursor imCur) {
    String imName = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA));
    String imP = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL));
    String imCustomP = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL));

    // TODO ha a gtalk előjönne mint issue
    if ( false ) {
      if ( Integer.parseInt(imP) == -1 ) {
        System.out.println("Protocol: Custom ");
      } else if ( Integer.parseInt(imP) == 5 ) {
        System.out.println("Protocol: Google talk");
      }
    }

    if ( imCustomP != null ) {
      contentInformations.add(new StringBuilder().append(uploadTime).append(SPACE_STR).append(CONTACTINFO_STR).append(SPACE_STR).append(contactId).append(SPACE_STR).append(imCustomP).append(SPACE_STR).append(RSAENCODING.INSTANCE.encodingString(imName)).toString());
    }
  }

  private void addPhoneNumberContactInfos( List<String> contentInformations, String uploadTime, String contactId, Cursor numbers) {
    String number = numbers.getString(numbers.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
    contentInformations.add(new StringBuilder().append(uploadTime).append(SPACE_STR).append(CONTACTINFO_STR).append(SPACE_STR).append(contactId).append(SPACE_STR).append(PHONE_NUMBER_STR).append(SPACE_STR).append(RSAENCODING.INSTANCE.encodingString(number)).toString());
  }

  private void addEmailContactInfos( List<String> contentInformations, String uploadTime, String contactId, Cursor emails) {
    String emailAddress;
    emailAddress = emails.getString(emails.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
    contentInformations.add(new StringBuilder().append(uploadTime).append(SPACE_STR).append(CONTACTINFO_STR).append(SPACE_STR).append(contactId).append(SPACE_STR).append(EMAILS_STR).append(SPACE_STR).append(RSAENCODING.INSTANCE.encodingString(emailAddress)).toString());
  }

  private boolean uploadCallInformations( final HttpPost httpPost) throws UnsupportedEncodingException, IOException, ClientProtocolException {
    List<String> callInformations = getCallInformations();

    String encryptedContactInformations = logToJsonConverter.convertLogToJsonFormat(callInformations);
    final StringEntity httpContentListEntity = new StringEntity(encryptedContactInformations, org.apache.http.protocol.HTTP.UTF_8);

    httpContentListEntity.setContentType("application/json");

    httpPost.setEntity(httpContentListEntity);
    HttpResponse response = getNewHttpClient().execute(httpPost);
    return isUploadSuccessFull(response);
  }

  List<String> getCallInformations() {
    List<String> callInformations = new ArrayList<String>();

    Uri allCalls = Uri.parse("content://call_log/calls");

    Cursor c = context.getContentResolver().query(allCalls, null, "date >" + String.valueOf(EventLogger.INSTANCE.logfileCreatedTime), null, null);

    while ( c.moveToNext() ) {
      StringBuilder callInformationBuilder = new StringBuilder();
      callInformationBuilder.append(String.valueOf(c.getLong(c.getColumnIndex("date"))));
      callInformationBuilder.append(SPACE_STR);
      callInformationBuilder.append("call");
      callInformationBuilder.append(SPACE_STR);
      callInformationBuilder.append(c.getString(c.getColumnIndex("numbertype")));
      callInformationBuilder.append(SPACE_STR);
      callInformationBuilder.append(c.getString(c.getColumnIndex("new")));
      callInformationBuilder.append(SPACE_STR);
      callInformationBuilder.append(c.getString(c.getColumnIndex("duration")));
      callInformationBuilder.append(SPACE_STR);
      callInformationBuilder.append(c.getString(c.getColumnIndex("_id")));
      callInformationBuilder.append(SPACE_STR);
      callInformationBuilder.append(c.getString(c.getColumnIndex("numberlabel")));
      callInformationBuilder.append(SPACE_STR);
      callInformationBuilder.append(c.getString(c.getColumnIndex("type")));
      callInformationBuilder.append(SPACE_STR);
      callInformationBuilder.append(RSAENCODING.INSTANCE.encodingString(c.getString(c.getColumnIndex("number"))));
      callInformations.add(callInformationBuilder.toString());
    }

    return callInformations;
  }

  private boolean uploadLogs( String jsonEncodedLogs, final HttpPost httpPost) throws UnsupportedEncodingException, IOException, ClientProtocolException {
    final StringEntity httpEntity = new StringEntity(jsonEncodedLogs, org.apache.http.protocol.HTTP.UTF_8);
    httpEntity.setContentType("application/json");
    httpPost.setEntity(httpEntity);
    InputStream reader = httpPost.getEntity().getContent();
    BufferedReader br = null;
    StringBuilder sb = new StringBuilder();
    br = new BufferedReader(new InputStreamReader(reader));
    String line;
    while ( ( line = br.readLine()) != null ) {
      sb.append(line);
    }
    Log.d("willrgai", sb.toString());
    HttpResponse response = getNewHttpClient().execute(httpPost);
    Log.d("willrgai", response.getStatusLine().toString());
    return isUploadSuccessFull(response);
  }

  private boolean isUploadSuccessFull( HttpResponse response) {
    if ( response.getStatusLine().getStatusCode() != 200 )
      return false;
    else
      return true;
  }

  private List<String> getLogListFromLogFile() throws FileNotFoundException, IOException {
    List<String> logList = new ArrayList<String>();
    FileReader logFileReader = new FileReader(EventLogger.INSTANCE.logFilePath);
    BufferedReader br = new BufferedReader(logFileReader);

    String readedLine;
    br.readLine();
    while ( ( readedLine = br.readLine()) != null ) {
      logList.add(readedLine);
    }
    br.close();
    return logList;
  }

  public HttpClient getNewHttpClient() {
    final InputStream inputStream = context.getResources().openRawResource(R.raw.trust);
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
    } catch ( Exception e ) {
      return new DefaultHttpClient();
    } finally {
      try {
        inputStream.close();
      } catch ( IOException e ) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}