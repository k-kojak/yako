package hu.rgai.android.test;

import android.app.Activity;
import android.content.ContentResolver;
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
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.tools.adapter.ContactListAdapter;
import hu.rgai.android.tools.view.ChipsMultiAutoCompleteTextView;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.EmailMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.FacebookMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageReply extends Activity implements TextWatcher {

  public static final int MESSAGE_SENT_OK = 1;
  public static final int MESSAGE_SENT_FAILED = 2;
  
  private int messageResult;
  private Handler handler = null;
//  private String content = null;
  private String subject = null;
  private TextView text;
  private ChipsMultiAutoCompleteTextView recipients;
  private AccountAndr account;
  private PersonAndr from;
  
  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    setContentView(R.layout.message_reply);
    String content = "";
    if (getIntent().getExtras() != null) {
      if (getIntent().getExtras().containsKey("content")) {
        content = getIntent().getExtras().getString("content");
      }
      if (getIntent().getExtras().containsKey("subject")) {
        subject = getIntent().getExtras().getString("subject");
      }
      if (getIntent().getExtras().containsKey("account")) {
        account = getIntent().getExtras().getParcelable("account");
      }
      if (getIntent().getExtras().containsKey("from")) {
        from = getIntent().getExtras().getParcelable("from");
      }
    }
    text = (TextView) findViewById(R.id.message_content);
    recipients = (ChipsMultiAutoCompleteTextView) findViewById(R.id.recipients);
    
    String[] emails = this.getEmailContacts();
//    for (String s : emails) {
//      Log.d("rgai", "item -> " + s);
//    }
    
//    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//                 android.R.layout.simple_dropdown_item_1line, emails);
//    recipients.setAdapter(adapter);
//    recipients.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
//    recipients.addTextChangedListener(this);
    
    Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
    ContactListAdapter adapter = new ContactListAdapter(this, c);
//    adapter.
//    CustomAdapter adapter = new CustomAdapter(this, c);
    recipients.setAdapter(adapter);
    recipients.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    
    text.setText("\n\n" + content);
    if (from != null) {
      recipients.setText(from.getEmails().get(0));
    }
    handler = new MessageReplyTaskHandler(this);
        
//    String msgContent = getIntent().getExtras().getString("message_content");
//    
//    AccountAndr account = getIntent().getExtras().getParcelable("account");
    
  }

  public void setMessageResult(int messageResult) {
    this.messageResult = messageResult;
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
  
  public void sendMessage(View v) {
    List<RecipientItem> to = recipients.getRecipients();
    List<AccountAndr> accs = StoreHandler.getAccounts(this);
    
    for (RecipientItem ri : to) {
      MessageReplySender rs = new MessageReplySender(ri, accs, handler, text.getText().toString());
      rs.execute();
    }
//    EmailReplySender replySender = new EmailReplySender(account, handler, text.getText().toString(), subject, recipients.getText().toString());
//    replySender.execute();
  }

  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
  public void afterTextChanged(Editable arg0) {}

  public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
//    validateEmailsField(recipients);
  }
  
  @Override
  public void finish() {
    setResult(messageResult);
    super.finish();
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
  
  private class MessageReplyTaskHandler extends Handler {
    
    MessageReply cont;
    
    public MessageReplyTaskHandler(MessageReply cont) {
      this.cont = cont;
    }
    
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.containsKey("result") && bundle.get("result") != null) {
          Log.d("rgai", bundle.getString("result"));
          Toast.makeText(cont, bundle.getString("result"), Toast.LENGTH_LONG).show();
        } else {
          cont.setMessageResult(MESSAGE_SENT_OK);
          cont.finish();
        }
      }
    }
  }
  
  private class MessageReplySender extends AsyncTask<Integer, Integer, Boolean> {

    RecipientItem recipient;
    private Handler handler;
    private List<AccountAndr> accounts;
    private String content;
//    private String subject;
//    private String recipients;
    
    private String result = null;
    
    public MessageReplySender(RecipientItem recipient, List<AccountAndr> accounts, Handler handler, String content) {
      this.recipient = recipient;
      this.accounts = accounts;
      this.handler = handler;
      this.content = content;
//      this.subject = subject;
//      this.recipients = recipients;
    }
    
    @Override
    protected Boolean doInBackground(Integer... params) {
      AccountAndr acc = getAccountForType(recipient.getType());

      if (acc != null) {
        MessageProvider mp = null;
        Set<MessageRecipient> recipients = null;
        if (recipient.getType().equals(MessageProvider.Type.FACEBOOK)) {
          mp = new FacebookMessageProvider((FacebookAccount)acc);
          recipients = new HashSet<MessageRecipient>();
          recipients.add(new FacebookMessageRecipient(recipient.getData()));
          Log.d("rgai", "SENDING FACEBOOK MESSAGE");
        } else if (recipient.getType().equals(MessageProvider.Type.EMAIL) || recipient.getType().equals(MessageProvider.Type.GMAIL)) {
          mp = new SimpleEmailMessageProvider((EmailAccount)acc);
          recipients = new HashSet<MessageRecipient>();
          recipients.add(new EmailMessageRecipient(recipient.getDisplayName(), recipient.getData()));
        }
        if (mp != null && recipients != null) {
          try {
            mp.sendMessage(recipients, content, content.substring(0, Math.min(content.length(), 10)));
          } catch (NoSuchProviderException ex) {
            Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
          } catch (MessagingException ex) {
            Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
          } catch (IOException ex) {
            Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
//      MessageProvider mp = new SimpleEmailMessageProvider((EmailAccount)account);
//      Set<MessageRecipient> recipients = new HashSet<MessageRecipient>();
//      InternetAddress[] addr = null;
//      try {
//        addr = InternetAddress.parse(this.recipients, true);
//      } catch (AddressException ex) {
//        result = "Invalid address field";
//        return false;
//      }
//      if (addr != null && addr.length > 0) {
//        for (InternetAddress a : addr) {
//          recipients.add(new EmailMessageRecipient(a.getPersonal(), a.getAddress()));
//        }
//      }
//      
//      if (recipients.isEmpty()) {
//        return false;
//      }
//      
//      // TODO: handle exceptions
//      try {
//        try {
//          mp.sendMessage(recipients, content, subject);
//        } catch (AddressException ex) {
//          result = "Invalid address field";
//          return false;
//        }
//      } catch (NoSuchProviderException ex) {
//        Log.d("rgai", ex.getMessage());
//        Log.d("rgai", ex.getLocalizedMessage());
//        Log.d("rgai", ex.toString());
//      } catch (MessagingException ex) {
//        Log.d("rgai", ex.getMessage());
//        Log.d("rgai", ex.getLocalizedMessage());
//        Log.d("rgai", ex.toString());
//      } catch (IOException ex) {
//        Log.d("rgai", ex.getMessage());
//        Log.d("rgai", ex.getLocalizedMessage());
//        Log.d("rgai", ex.toString());
//      }
      return true;
    }
    
    // TODO: gmail != email
    private AccountAndr getAccountForType(MessageProvider.Type type) {
      boolean m = type.equals(MessageProvider.Type.EMAIL) || type.equals(MessageProvider.Type.GMAIL);
      for (AccountAndr acc : accounts) {
        if (m) {
          if (acc.getAccountType().equals(MessageProvider.Type.EMAIL) || acc.getAccountType().equals(MessageProvider.Type.GMAIL)) {
            return acc;
          }
        } else {
          if (acc.getAccountType().equals(type)) {
            return acc;
          }
        }
      }
      return null;
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
        result = "Invalid address field";
        return false;
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
          result = "Invalid address field";
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
