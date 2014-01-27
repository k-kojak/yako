package hu.rgai.android.test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import hu.rgai.android.test.settings.FacebookSettingActivity;
import hu.rgai.android.test.settings.GmailSettingActivity;
import hu.rgai.android.test.settings.SimpleEmailSettingActivity;
import hu.rgai.android.tools.adapter.ContactListAdapter;
import hu.rgai.android.tools.view.ChipsMultiAutoCompleteTextView;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import java.util.Iterator;
import java.util.LinkedList;
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
  
  public void sendMessage(AccountAndr from) {
    if (from == null) {
      return;
    }
    
    List<RecipientItem> to = recipients.getRecipients();
    List<AccountAndr> accs = new LinkedList<AccountAndr>();
    accs.add(from);
    
    for (RecipientItem ri : to) {
      MessageSender rs = new MessageSender(ri, accs, handler, text.getText().toString(), this);
      rs.execute();
    }
    
  }
  
  public void prepareMessageSending(View v) {
    List<RecipientItem> to = recipients.getRecipients();
    List<AccountAndr> accs = StoreHandler.getAccounts(this);
    
    for (RecipientItem ri : to) {
      List<AccountAndr> selectedAccs = new LinkedList<AccountAndr>();
      Iterator<AccountAndr> accIt = accs.iterator();
      while (accIt.hasNext()) {
        AccountAndr actAcc = accIt.next();
        if (((ri.getType().equals(MessageProvider.Type.EMAIL) || ri.getType().equals(MessageProvider.Type.GMAIL))
                && (actAcc.getAccountType().equals(MessageProvider.Type.EMAIL)
                || actAcc.getAccountType().equals(MessageProvider.Type.GMAIL)))
                || ri.getType().equals(actAcc.getAccountType())) {
          selectedAccs.add(actAcc);
        }
      }
      if (selectedAccs.isEmpty()) {
        Toast.makeText(this,
                "Cannot send message to "+ ri.getDisplayData() +". A "+ri.getType().toString() + " account required for that.",
                Toast.LENGTH_LONG).show();
      } else if (selectedAccs.size() == 1) {
        sendMessage(selectedAccs.get(0));
      } else {
        chooseAccount(selectedAccs);
      }
    }
    
  }
  
  private void chooseAccount(final List<AccountAndr> accs) {

    String[] items = new String[accs.size()];
    int i = 0;
    for (AccountAndr a : accs) {
      items[i++] = a.getDisplayName();
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Choose account to send from");
    builder.setItems(items, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        sendMessage(accs.get(which));
      }
    });
    
    Dialog dialog = builder.create();
    dialog.show();
    
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
