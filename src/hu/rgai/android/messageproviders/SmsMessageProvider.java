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
            new String[]{"_id", "body", "date", "seen"},
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
              new Person("0", "én", MessageProvider.Type.SMS),
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


        ftm.addMessage(new MessageAtomParc(
                cur.getString(0),
                "vmi",
                cur.getString(12),
                new Date(),
                new Person("0", "en", MessageProvider.Type.SMS),
                true, //vmit ezzel kezdeni
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
