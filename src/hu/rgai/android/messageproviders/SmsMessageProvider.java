package hu.rgai.android.messageproviders;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

import hu.rgai.android.intent.beens.MessageAtomParc;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.MessageListElement;
import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.Person;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullThreadMessage;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;

public class SmsMessageProvider implements MessageProvider {

  Context context;

  public SmsMessageProvider(Context myContext) {
    context = myContext;
  }

  @Override
  public List<MessageListElement> getMessageList(int offset, int limit)
          throws CertPathValidatorException, SSLHandshakeException,
          ConnectException, NoSuchProviderException, UnknownHostException,
          IOException, MessagingException, AuthenticationFailedException {
    // TODO Auto-generated method stub

    final List<MessageListElement> messages = new LinkedList<MessageListElement>();


    Uri uriSMSURI = Uri.parse("content://sms");
    Cursor cur = context.getContentResolver().query(uriSMSURI,
            new String[]{"thread_id", "body", "date", "seen", "person", "address"},
            null,
            null,
            "date DESC LIMIT " + limit);


    while (cur.moveToNext()) {

      Log.d("rgai", cur.getString(2));
      messages.add(new MessageListElement(
              cur.getString(0),
              true,
              cur.getString(1),
              1,
              new Person(cur.getLong(4) + "", cur.getString(5), MessageProvider.Type.SMS),
              new Date(Long.parseLong(cur.getString(2))),
              MessageProvider.Type.SMS));
    }

    return messages;


  }

  @Override
  public FullThreadMessage getMessage(String id) throws NoSuchProviderException,
          MessagingException, IOException {
    // TODO Auto-generated method stub

    final FullThreadMessage ftm = new FullThreadMessage();
    String selection = "thread_id = " + id;

    Uri uriSMSURI = Uri.parse("content://sms");
    Cursor cur = context.getContentResolver().query(uriSMSURI, null, selection, null, null);

    /**
     * 0: _id 1: thread_id 2: address 3: person 4: date 5: date_sent 6: protocol 7: read
     * 8: status 9: type 10: reply_path_present 11: subject 12: body 13: service_center
     * 14: locked 15: error_code 16: seen
     */
    while (cur.moveToNext()) {
      if (cur.getString(1).equals(id)) {

        Log.d("rgai", "SMS DATE -> " + cur.getString(4));
        Log.d("rgai", "SMS ADDRESS -> " + cur.getString(2));
        Log.d("rgai", "SMS PERSON -> " + cur.getLong(3));
        Log.d("rgai", "SMS READ -> " + cur.getLong(7));
        Log.d("rgai", "SMS STATUS -> " + cur.getLong(8));
        Log.d("rgai", "SMS TYPE -> " + cur.getLong(9));
        Log.d("rgai", "SMS SEEN -> " + cur.getLong(16));
        ftm.addMessage(new MessageAtom(
                cur.getString(0),
                cur.getString(11),
                cur.getString(12),
                new Date(cur.getLong(4)),
                new Person(cur.getLong(3) + "", cur.getString(2), MessageProvider.Type.SMS),
                cur.getLong(9) == 2, //vmit ezzel kezdeni
                MessageProvider.Type.SMS,
                null));

      }
    }

    return ftm;
  }

  @Override
  public void sendMessage(Set<? extends MessageRecipient> to, String content,
          String subject) throws NoSuchProviderException, MessagingException,
          IOException {
    // TODO Auto-generated method stub

    for (MessageRecipient mr : to) {

      SmsMessageRecipient smr = (SmsMessageRecipient) mr;

      SmsManager smsman = SmsManager.getDefault();
      smsman.sendTextMessage(smr.getAddress(), null, content, null, null);


      ContentValues sentSms = new ContentValues();
      sentSms.put("address", smr.getAddress());
      sentSms.put("body", content);



      ContentResolver contentResolver = context.getContentResolver();
      Uri uri = Uri.parse("content://sms/sent");
      contentResolver.insert(uri, sentSms);
    }

  }
}
