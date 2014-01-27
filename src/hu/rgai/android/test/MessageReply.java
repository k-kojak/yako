package hu.rgai.android.test;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import hu.rgai.android.asynctasks.MessageSender;
import hu.rgai.android.intent.beens.EmailRecipientAndr;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.tools.adapter.ContactListAdapter;
import hu.rgai.android.tools.view.ChipsMultiAutoCompleteTextView;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageReply extends ActionBarActivity {

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
    
    getSupportActionBar().setDisplayShowTitleEnabled(false);
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
    if (from != null && account != null
            && (account.getAccountType().equals(MessageProvider.Type.EMAIL) || account.getAccountType().equals(MessageProvider.Type.GMAIL))) {
      RecipientItem ri = new EmailRecipientAndr(from.getName(), from.getId(), from.getId(),
              null, (int)from.getContactId());
      recipients.addRecipient(ri);
    }
    
    Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
    ContactListAdapter adapter = new ContactListAdapter(this, c);
//    adapter.
//    CustomAdapter adapter = new CustomAdapter(this, c);
    recipients.setAdapter(adapter);
    recipients.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    LinearLayout fake = (LinearLayout)findViewById(R.id.fake_focus);
    fake.requestFocus();
    if (content.length() > 0) {
      text.setText("\n\n" + content);
    }
    if (from != null) {
      Log.d("rgai", "REPLYING TO -> " + from);
    }
    handler = new MessageReplyTaskHandler(this);
        
  }

  public void setMessageResult(int messageResult) {
    this.messageResult = messageResult;
  }
  
  public void sendMessage(View v) {
    List<RecipientItem> to = recipients.getRecipients();
    List<AccountAndr> accs = StoreHandler.getAccounts(this);
    
    for (RecipientItem ri : to) {
      MessageSender rs = new MessageSender(ri, accs, handler, text.getText().toString(), this);
      rs.execute();
    }
    
//    EmailReplySender replySender = new EmailReplySender(account, handler, text.getText().toString(), subject, recipients.getText().toString());
//    replySender.execute();
  }

  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
  public void afterTextChanged(Editable arg0) {}

  
  @Override
  public void finish() {
    setResult(messageResult);
    super.finish();
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
  
}
