package hu.rgai.android.messageproviders;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.FullMessage;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.FullThreadMessage;
import hu.rgai.android.beens.HtmlContent;
import hu.rgai.android.beens.MainServiceExtraParams;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.MessageListResult;
import hu.rgai.android.beens.MessageRecipient;
import hu.rgai.android.beens.Person;
import hu.rgai.android.beens.SmsAccount;
import hu.rgai.android.beens.SmsMessageRecipient;
import hu.rgai.android.config.Settings;
import hu.rgai.android.services.MainService;
import hu.rgai.android.services.schedulestarters.MainScheduler;
import hu.rgai.android.test.YakoApp;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

public class SmsMessageProvider extends BroadcastReceiver  implements ThreadMessageProvider {

  Context context;

  public SmsMessageProvider(){};
  
  public SmsMessageProvider(Context myContext) {
    context = myContext;
  }

  public Account getAccount() {
    return SmsAccount.account;
  }
  
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages)
          throws CertPathValidatorException, SSLHandshakeException,
          ConnectException, NoSuchProviderException, UnknownHostException,
          IOException, MessagingException, AuthenticationFailedException {
    return getMessageList(offset, limit, loadedMessages, 20);
  }
  
  @Override
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages, int snippetMaxLength)
          throws CertPathValidatorException, SSLHandshakeException,
          ConnectException, NoSuchProviderException, UnknownHostException,
          IOException, MessagingException, AuthenticationFailedException {

    final List<MessageListElement> messages = new LinkedList<MessageListElement>();
    int foundThreads = 0;
    
    Uri uriSMSURI = Uri.parse("content://sms");
    Cursor cur = context.getContentResolver().query(uriSMSURI,
            new String[]{"thread_id", "body", "date", "seen", "person", "address", "type"},
            null,
            null,
            "date DESC ");
//    Cursor cur2 = context.getContentResolver().query(uriSMSURI,
//            null,
//            null,
//            null,
//            "date DESC ");
//    while (cur2.moveToNext()) {
//      String[] cn = cur2.getColumnNames();
//      int tid = cur2.getInt(cur2.getColumnIndex("thread_id"));
//      if (tid == 4 || tid == 3) {
//        for (int i = 0; i < cn.length; i++) {
//          String v = "";
//          if (cur2.getType(i) == Cursor.FIELD_TYPE_INTEGER) {
//            v = cur2.getInt(i) + "";
//          } else if (cur2.getType(i) == Cursor.FIELD_TYPE_STRING) {
//            v = cur2.getString(i) + "";
//          }
//          Log.d("rgai", cn[i] + " -> " + v);
//        }
//      }
//      Log.d("rgai", "----------------------------------------------");
//      Log.d("rgai", "----------------------------------------------");
//      
//    }
//    cur2.close();
    if (cur != null) {
      while (cur.moveToNext()) {
        String title = cur.getString(1);
        if (title.length() > Settings.MAX_SNIPPET_LENGTH) {
          title = title.substring(0, Settings.MAX_SNIPPET_LENGTH) + "...";
        }
        boolean seen = cur.getInt(3) == 1;
        boolean isMe = cur.getInt(6) == 2;
        MessageItem ti = new MessageItem(cur.getString(0), title, seen, isMe, cur.getLong(4),
                cur.getString(5), cur.getLong(2));
//        Log.d("rgai", "MessageItem -> " + ti);
        boolean contains = false;
        int containIndex = -1;
        for (MessageListElement mle : messages) {
          containIndex++;
          if (mle.getId().equals(ti.threadId)) {
            contains = true;
            break;
          }
        }

        Person from = null;
  //      if (!ti.isMe) {
        if (ti.personId == 0) {
          from = new Person(ti.address, ti.address, MessageProvider.Type.SMS);
        } else {
          from = new Person(ti.personId+"", ti.address, MessageProvider.Type.SMS);
        }
  //      } else {
  ////        from = new Person(ti.personId+"", ti.address, Type.SMS);
  //      }
//          Log.d("rgai", "pureFrom -> " + from.toString());
        if (contains) {
          MessageListElement mle = messages.get(containIndex);
          if (!ti.isMe && !from.getId().equals("0")) {
            mle.setFrom(from);
          }
          if (!ti.seen && !ti.isMe) {
            mle.setSeen(false);
          }
        } else {
          foundThreads++;
          if (foundThreads > limit + offset) break;
          messages.add(new MessageListElement(ti.threadId, ti.isMe ? true : ti.seen, ti.title,
                  from, null, new Date(ti.date), SmsAccount.account, Type.SMS));
        }
      }
      cur.close();
    }
    return new MessageListResult(messages, MessageListResult.ResultType.CHANGED);
    
  }

  @Override
  public FullThreadMessage getMessage(String threadId, int offset, int limit)throws NoSuchProviderException,
          MessagingException, IOException {
    // TODO Auto-generated method stub
    final FullThreadMessage ftm = new FullThreadMessage();
    String selection = "thread_id = ?";
    String[] selectionArgs = new String[]{threadId};

    Uri uriSMSURI = Uri.parse("content://sms");
    Cursor cur = context.getContentResolver().query(uriSMSURI,
            new String[]{"thread_id", "_id", "subject", "body", "date", "person", "address", "type"},
            selection, selectionArgs, "_id DESC LIMIT "+offset+","+limit);

    /**
     * 0: _id 1: thread_id 2: address 3: person 4: date 5: date_sent 6: protocol 7: read
     * 8: status 9: type 10: reply_path_present 11: subject 12: body 13: service_center
     * 14: locked 15: error_code 16: seen
     */
    while (cur.moveToNext()) {
      if (cur.getString(0).equals(threadId)) {

//        Log.d("rgai", "SMS DATE -> " + cur.getString(4));
//        Log.d("rgai", "SMS ADDRESS -> " + cur.getString(2));
//        Log.d("rgai", "SMS PERSON -> " + cur.getLong(3));
//        Log.d("rgai", "SMS READ -> " + cur.getLong(7));
//        Log.d("rgai", "SMS STATUS -> " + cur.getLong(8));
//        Log.d("rgai", "SMS TYPE -> " + cur.getLong(9));
//        Log.d("rgai", "SMS SEEN -> " + cur.getLong(16));
        ftm.addMessage(new FullSimpleMessage(
                cur.getString(1),
                cur.getString(2),
                new HtmlContent(cur.getString(3), HtmlContent.ContentType.TEXT_PLAIN),
                new Date(cur.getLong(4)),
                new Person(cur.getLong(5) + "", cur.getString(6), MessageProvider.Type.SMS),
                cur.getLong(7) == 2, //vmit ezzel kezdeni
                MessageProvider.Type.SMS,
                null));
      }
    }
    
    markMessagesOfThreadToSeen(threadId);

    return ftm;
  }
  
  private void markMessagesOfThreadToSeen(String threadId) {
    
    Uri uriSMSURI = Uri.parse("content://sms");
    ContentValues values = new ContentValues();
    values.put("seen", 1);
    context.getContentResolver().update(
            uriSMSURI,
            values,
            "thread_id = ?",
            new String[]{threadId});
    
  }

  @Override
  public void sendMessage(Set<? extends MessageRecipient> to, String content, String subject)
          throws NoSuchProviderException, MessagingException, IOException {

    for (MessageRecipient mr : to) {

      SmsMessageRecipient smr = (SmsMessageRecipient) mr;

      SmsManager smsMan = SmsManager.getDefault();
      String rawPhoneNum = smr.getData().replaceAll("[^\\+0-9]", "");
//      Log.d("rgai", "SENDING SMS TO THIS PHONE NUMBER -> " + rawPhoneNum);
      
        ArrayList<String> dividedMessages = smsMan.divideMessage(content);
        smsMan.sendMultipartTextMessage(rawPhoneNum, null, dividedMessages, null, null);
        ContentValues sentSms = new ContentValues();
        sentSms.put("address", rawPhoneNum);
        sentSms.put("body", content);

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms/sent");
        contentResolver.insert(uri, sentSms);

    }

  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
      // sms broadcast arrives earlier than sms actually stored in inbox, we have to delay
      // a bit the reading from inbox
      try {
        Thread.sleep(750);
      } catch (InterruptedException ex) {
        Logger.getLogger(SmsMessageProvider.class.getName()).log(Level.SEVERE, null, ex);
      }
      
      Intent res = new Intent(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
      res.putExtra("type", MessageProvider.Type.SMS.toString());
      context.sendBroadcast(res);
      
      // TODO: do not make a full query to the given account/type, query only the
      // affected message element, so select only 1 element instead of all messages of the given account
      if (MainService.actViewingMessage == null || !MainService.actViewingMessage.getMessageType().equals(MessageProvider.Type.SMS)) {
        Intent service = new Intent(context, MainScheduler.class);
        service.setAction(Context.ALARM_SERVICE);
        
        MainServiceExtraParams eParams = new MainServiceExtraParams();
        eParams.setAccount(SmsAccount.account);
        eParams.setForceQuery(true);
        service.putExtra(MainServiceExtraParams.ParamStrings.EXTRA_PARAMS, eParams);
        
        context.sendBroadcast(service);
      }
      
//      // in case the first attempt was too quick, request the display again a little bit later
//      try {
//        Thread.sleep(2000);
//      } catch (InterruptedException ex) {
//        Logger.getLogger(SmsMessageProvider.class.getName()).log(Level.SEVERE, null, ex);
//      }
//      res = new Intent(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
//      context.sendBroadcast(res);
    }
  }

  public FullMessage getMessage(String id) throws NoSuchProviderException, MessagingException, IOException {
    return getMessage(id, 0, 20);
  }

  public void markMessageAsRead(String threadId, boolean seen) throws NoSuchProviderException, MessagingException, IOException {
    if (seen) {
      markMessagesOfThreadToSeen(threadId);
    } else {
      // just setting last sms of the thread to unseen
      
      Uri uriSMSURI = Uri.parse("content://sms");
      Cursor cur = context.getContentResolver().query(uriSMSURI,
              new String[]{"_id"},
              "thread_id = ? AND type != 2",
              new String[]{threadId},
              "date DESC ");

      if (cur != null) {
        while (cur.moveToNext()) {
          
          String messageId = cur.getString(0);
          
          ContentValues values = new ContentValues();
          values.put("seen", 2);
          context.getContentResolver().update(
                  uriSMSURI,
                  values,
                  "_id = ?",
                  new String[]{messageId});
          
          break;
        }
      }
      cur.close();
      
    }
  }
  
  public void markMessagesAsRead(String[] ids, boolean seen) throws NoSuchProviderException, MessagingException, IOException {
    for (int i = 0; i < ids.length; i++) {
      markMessageAsRead(ids[i], seen);
    }
  }
  
  public boolean canBroadcastOnNewMessage() {
    return true;
  }

  public boolean isConnectionAlive() {
    return true;
  }

  public void establishConnection(Context context) {
    // this class is a broadcast receiver too, so the connection is established this way
  }
  
  public boolean canBroadcastOnMessageChange() {
    return false;
  }

  /**
   * We never want to drop this connection.
   */
  public void dropConnection() {
  }

  

  private class MessageItem {
    private String threadId;
    private String title;
    private boolean seen;
    private boolean isMe;
    private long personId;
    private String address;
    private long date;

    public MessageItem(String threadId, String title, boolean seen, boolean isMe, long personId, String address, long date) {
      this.threadId = threadId;
      this.title = title;
      this.seen = seen;
      this.isMe = isMe;
      this.personId = personId;
      this.address = address;
      this.date = date;
    }

    @Override
    public String toString() {
      return "MessageItem{" + "threadId=" + threadId + ", title=" + title + ", seen=" + seen + ", isMe=" + isMe + ", personId=" + personId + ", address=" + address + ", date=" + date + '}';
    }

  }

  @Override
  public String toString() {
    return "SmsMessageProvider{" + '}';
  }
  
  
  
}
