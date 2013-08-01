/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.test;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.EmailMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageReply extends Activity implements TextWatcher {

  private Handler handler = null;
//  private String content = null;
  private String subject = null;
  private TextView text;
  private MultiAutoCompleteTextView recipients;
  private AccountAndr account;
  private static final String[] COUNTRIES = new String[] {
         "Belgium", "France", "Italy", "Germany", "Spain"
     };
  
  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    setContentView(R.layout.email_reply);
    
    String content = getIntent().getExtras().getString("content");
    text = (TextView) findViewById(R.id.message_content);
    recipients = (MultiAutoCompleteTextView) findViewById(R.id.recipients);
    
    String[] emails = this.getEmailContacts();
//    for (String s : emails) {
//      Log.d("rgai", "item -> " + s);
//    }
    
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                 android.R.layout.simple_dropdown_item_1line, emails);
    recipients.setAdapter(adapter);
    recipients.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    recipients.addTextChangedListener(this);
    
    text.setText("\n\n" + content);
    subject = getIntent().getExtras().getString("subject");
    account = getIntent().getExtras().getParcelable("account");
    handler = new EmailReplyTaskHandler(this);
        
//    String msgContent = getIntent().getExtras().getString("message_content");
//    
//    AccountAndr account = getIntent().getExtras().getParcelable("account");
    
  }
  
  private String[] getEmailContacts() {
    
//    String[] contacts = null;
    LinkedList<String> emails = new LinkedList<String>();
    
    String name;
//    String mime;
    String email;
//    Map<String, String> emails = new HashMap<String, String>();
    
    ContentResolver cr = getContentResolver();
    Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
    int nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME);
//    int mimeIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE);
    int emailIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1);
    
    if (cursor.moveToFirst()) {
      do {
        name = cursor.getString(nameIdx);
//        mime = cursor.getString(mimeIdx);
        email = cursor.getString(emailIdx);
//        if (mime.equals(ContactsContract.Data.))
//        Log.d("rgai", name);
//        Log.d("rgai", mime);
//        Log.d("rgai", email);
        
        emails.add(name + " <"+ email +">");
        
        
//        for (int i = 0; i < colSize; i++) {
//        for (int i = 0; i < colSize; i++) {
//          Log.d("rgai", cursor.getColumnName(i));
//        }
//        Log.d("rgai", " - - - - - - - - - - - - - - ");
        
//          ArrayAdapter arr = new ArrayAdapter(this, android.R.layout.simple_list_item_1,str);

//          setListAdapter(arr);
      } while (cursor.moveToNext());
    }
    
    return emails.toArray(new String[emails.size()]);
  }
  
  public void sendEmail(View v) {
    EmailReplySender replySender = new EmailReplySender(account, handler, text.getText().toString(), subject, recipients.getText().toString());
    replySender.execute();
  }

  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
  public void afterTextChanged(Editable arg0) {}

  public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    validateEmailsField(recipients);
  }

  private static void validateEmailsField(TextView recipients) {
    String emails = (String) recipients.getText().toString();
    if (emails.length() == 0) {
      recipients.setError("Empty recipient list");
    } else {
      try {
        InternetAddress.parse(emails, true);
        recipients.setError(null);
      } catch (AddressException ex) {
        recipients.setError("Invalid address list");
      }
    }
  }
  
  private class EmailReplyTaskHandler extends Handler {
    
    Context cont;
    
    public EmailReplyTaskHandler(Context cont) {
      this.cont = cont;
    }
    
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.containsKey("result") && bundle.get("result") != null) {
          Log.d("rgai", bundle.getString("result"));
          Toast.makeText(cont, bundle.getString("result"), Toast.LENGTH_LONG);
        }
      }
    }
  }
  
  private class EmailReplySender extends AsyncTask<Integer, Integer, Boolean> {

    private Handler handler;
    private AccountAndr account;
    private String content;
    private String subject;
    private String recipients;
    
    private String result = null;
    
    public EmailReplySender(AccountAndr account, Handler handler, String content, String subject, String recipients) {
      this.handler = handler;
      this.account = account;
      this.content = content;
      this.subject = subject;
      this.recipients = recipients;
    }
    
    @Override
    protected Boolean doInBackground(Integer... params) {
      MessageProvider mp = new SimpleEmailMessageProvider((EmailAccount)account);
      Set<MessageRecipient> recipients = new HashSet<MessageRecipient>();
      InternetAddress[] addr = null;
      try {
        addr = InternetAddress.parse(this.recipients, true);
      } catch (AddressException ex) {
        Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
      }
      if (addr != null && addr.length > 0) {
        for (InternetAddress a : addr) {
          recipients.add(new EmailMessageRecipient(a.getPersonal(), a.getAddress()));
        }
      }
      
      if (recipients.isEmpty()) {
        return false;
      }
      
      // TODO: handle exceptions
      try {
        try {
          mp.sendMessage(recipients, content, subject);
        } catch (AddressException ex) {
          result = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(ex);
          return false;
        }
      } catch (NoSuchProviderException ex) {
        Log.d("rgai", ex.getMessage());
        Log.d("rgai", ex.getLocalizedMessage());
        Log.d("rgai", ex.toString());
      } catch (MessagingException ex) {
        Log.d("rgai", ex.getMessage());
        Log.d("rgai", ex.getLocalizedMessage());
        Log.d("rgai", ex.toString());
      } catch (IOException ex) {
        Log.d("rgai", ex.getMessage());
        Log.d("rgai", ex.getLocalizedMessage());
        Log.d("rgai", ex.toString());
      }
      return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      if (!success) {
        bundle.putString("result", result);
      }
      msg.setData(bundle);
      handler.sendMessage(msg);
    }


//    @Override
//    protected void onProgressUpdate(Integer... values) {
//      Log.d(Constants.LOG, "onProgressUpdate");
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//
//      bundle.putInt("progress", values[0]);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
//    }

  }
}
