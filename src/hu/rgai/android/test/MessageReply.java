package hu.rgai.android.test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.intent.beens.EmailRecipientAndr;
import hu.rgai.android.intent.beens.FacebookRecipientAndr;
import hu.rgai.android.intent.beens.FullSimpleMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.intent.beens.SmsMessageRecipientAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.SmsAccountAndr;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.tools.adapter.ContactListAdapter;
import hu.rgai.android.tools.view.ChipsMultiAutoCompleteTextView;
import hu.rgai.android.workers.MessageSender;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.Person;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.htmlparser.jericho.Source;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageReply extends ActionBarActivity {

  public static final int MESSAGE_SENT_OK = 1;

  public static final int MESSAGE_SENT_FAILED = 2;

  private int messageResult;
  private Handler handler = null;
  // private String content = null;
  private TextView text;
  private ChipsMultiAutoCompleteTextView recipients;
  private AccountAndr account;
  private WebView mQuotedMessage = null;
  private CheckBox mQuoteCheckbox = null;
  private EditText mSubject = null;
  private MessageListElementParc mMessage = null;
  private FullSimpleMessageParc mFullMessage = null;

  @Override
  public void onBackPressed() {
    Log.d("willrgai", EventLogger.LOGGER_STRINGS.MESSAGE_REPLY.MESSAGE_REPLY_BACKBUTTON_STR);
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.MESSAGE_REPLY.MESSAGE_REPLY_BACKBUTTON_STR, true);
    super.onBackPressed();
  }

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setDisplayHomeAsUpEnabled(true);

    setContentView(R.layout.message_reply);
    mSubject = (EditText) findViewById(R.id.subject);
    if (getIntent().getExtras() != null) {
      if (getIntent().getExtras().containsKey("message")) {
        mMessage = getIntent().getExtras().getParcelable("message");
        if (mMessage.getFullMessage() != null && mMessage.getFullMessage() instanceof FullSimpleMessageParc) {
          mFullMessage = (FullSimpleMessageParc) mMessage.getFullMessage();
          mSubject.setText(mFullMessage.getSubject());
        }
      }
      if (getIntent().getExtras().containsKey("account")) {
        account = getIntent().getExtras().getParcelable("account");
      }
    }
    text = (TextView) findViewById(R.id.message_content);
    mQuotedMessage = (WebView) findViewById(R.id.quoted_message);
    mQuoteCheckbox = (CheckBox) findViewById(R.id.quote_origi);

    text.addTextChangedListener(new TextWatcher() {

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        // TODO Auto-generated method stub
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // TODO Auto-generated method stub
      }

      @Override
      public void afterTextChanged(Editable s) {
        // TODO Auto-generated method stub
        Log.d("willrgai", EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR
                + "null" + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString());
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR
                + "null" + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString(), true);
      }
    });
    recipients = (ChipsMultiAutoCompleteTextView) findViewById(R.id.recipients);
    recipients.addOnChipChangeListener(new ChipsMultiAutoCompleteTextView.OnChipChangeListener() {
      public void onChipListChange() {
        showHideSubjectField();
      }
    });

    if (mMessage != null) {
      if (mMessage.getFrom() != null && account != null
              && (account.getAccountType().equals(MessageProvider.Type.EMAIL) || account.getAccountType().equals(MessageProvider.Type.GMAIL))) {

        RecipientItem ri = new EmailRecipientAndr(mMessage.getFrom().getName(), mMessage.getFrom().getId(), mMessage.getFrom().getName(),
                null, (int) mMessage.getFrom().getContactId());
        recipients.addRecipient(ri);
      }
    } else {
      mQuotedMessage.setVisibility(View.GONE);
      mQuoteCheckbox.setVisibility(View.GONE);
    }
    showHideSubjectField();

    Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
    ContactListAdapter adapter = new ContactListAdapter(this, c);
    // adapter.
    // CustomAdapter adapter = new CustomAdapter(this, c);
    recipients.setAdapter(adapter);
    recipients.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    LinearLayout fake = (LinearLayout) findViewById(R.id.fake_focus);
    fake.requestFocus();
    if (mFullMessage != null) {
      mQuotedMessage.loadDataWithBaseURL(null, mFullMessage.getContent().getContent().toString(),
              mFullMessage.getContent().getContentType().getMimeName(), "UTF-8", null);
//      text.setText( "\n\n" + content);
    }
    handler = new MessageReplyTaskHandler(this);

    if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SENDTO)) {
      processImplicitIntent(getIntent());
    }

  }

  private void showHideSubjectField() {
    boolean hasEmailRecipient = false;
    for (RecipientItem ri : recipients.getRecipients()) {
      if (ri.getType().equals(MessageProvider.Type.EMAIL) || ri.getType().equals(MessageProvider.Type.GMAIL)) {
        hasEmailRecipient = true;
        break;
      }
    }
    if (hasEmailRecipient) {
      expand(mSubject);
    } else {
      collapse(mSubject);
    }
    
  }

  public void onQuoteClicked(View view) {
    Log.d("rgai", "CLICKED");
    int visibility;
    if (mQuoteCheckbox.isChecked()) {
      visibility = View.VISIBLE;
    } else {
      visibility = View.GONE;
    }
    mQuotedMessage.setVisibility(visibility);
  }

  private void processImplicitIntent(Intent intent) {
    try {
      EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.OTHER.MESSAGE_WRITE_FROM_CONTACT_LIST, true);
      String uri = URLDecoder.decode(intent.getDataString(), "UTF-8");
      String[] uriParts = uri.split(":");
      if (uriParts.length > 1) {
        if (uriParts[0].equals("imto")) {
          String[] dataParts = uriParts[1].replaceFirst("^/+", "").split("/");
          if (dataParts.length == 2 && dataParts[0].equals("facebook")) {
            searchUserAndInsertAsRecipient(MessageProvider.Type.FACEBOOK, dataParts[1]);
          }
        } else if (uriParts[0].equals("smsto")) {
          searchUserAndInsertAsRecipient(MessageProvider.Type.SMS, uriParts[1]);
        } else if (uriParts[0].equals("mailto")) {
          searchUserAndInsertAsRecipient(MessageProvider.Type.EMAIL, uriParts[1]);
        }
      }
    } catch (UnsupportedCharsetException ex) {
      ex.printStackTrace();
      Toast.makeText(this, getString(R.string.unsupported_encoding), Toast.LENGTH_LONG).show();
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
      Toast.makeText(this, getString(R.string.unsupported_charset), Toast.LENGTH_LONG).show();
    }
  }

  private void searchUserAndInsertAsRecipient(MessageProvider.Type type, String id) {
    PersonAndr pa = PersonAndr.searchPersonAndr(this, new Person(id, id, type));

    if (pa != null) {
      RecipientItem ri = null;
      switch (type) {
        case FACEBOOK:
          ri = new FacebookRecipientAndr(pa.getName(), pa.getId(), pa.getName(), null, (int) pa.getContactId());
          break;
        case SMS:
          ri = new SmsMessageRecipientAndr(pa.getName(), pa.getId(), pa.getName(), null, (int) pa.getContactId());
          break;
        case EMAIL:
        case GMAIL:
          ri = new EmailRecipientAndr(pa.getName(), pa.getId(), pa.getName(), null, (int) pa.getContactId());
          break;
        default:
          break;
      }
      recipients.addRecipient(ri);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.email_message_send_options_menu, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      case R.id.send_message:
        prepareMessageSending();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
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
    String content = text.getText().toString().trim();
    String subject = null;
    if (mSubject.getVisibility() == View.VISIBLE) {
      subject = mSubject.getText().toString();
    }
    if (subject == null) {
      subject = "";
    }
    for (RecipientItem ri : to) {
      if (mQuotedMessage.getVisibility() == View.VISIBLE) {
        Source source = new Source("<br /><br />" + mFullMessage.getContent().getContent());
        content += source.getRenderer().toString();
      }
      MessageSender rs = new MessageSender(ri, accs, handler, subject, content, this);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        rs.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      } else {
        rs.execute();
      }
    }

  }

  private void prepareMessageSending() {
    List<RecipientItem> to = recipients.getRecipients();
    if (to.isEmpty()) {
      Toast.makeText(this, R.string.no_recipient_selected, Toast.LENGTH_SHORT).show();
      return;
    }

    if (text.getText().toString().trim().length() == 0) {
      Toast.makeText(this, R.string.empty_message, Toast.LENGTH_SHORT).show();
      return;
    }

    List<AccountAndr> accs = StoreHandler.getAccounts(this);

    boolean isPhone = MainActivity.isPhone(this);

    for (RecipientItem ri : to) {
      List<AccountAndr> selectedAccs = new LinkedList<AccountAndr>();
      Iterator<AccountAndr> accIt = accs.iterator();
      if (ri.getType().equals(MessageProvider.Type.SMS) && isPhone) {
        selectedAccs.add(new SmsAccountAndr());
      } else {
        while (accIt.hasNext()) {
          AccountAndr actAcc = accIt.next();
          if (((ri.getType().equals(MessageProvider.Type.EMAIL) || ri.getType().equals(MessageProvider.Type.GMAIL))
                  && (actAcc.getAccountType().equals(MessageProvider.Type.EMAIL)
                  || actAcc.getAccountType().equals(MessageProvider.Type.GMAIL)))
                  || ri.getType().equals(actAcc.getAccountType())) {
            selectedAccs.add(actAcc);
          }
        }
      }
      if (selectedAccs.isEmpty()) {
        Toast.makeText(this,
                "Cannot send message to " + ri.getDisplayData() + ". A " + ri.getType().toString() + " account required for that.",
                Toast.LENGTH_LONG).show();
      } else if (selectedAccs.size() == 1) {
        sendMessage(selectedAccs.get(0));
      } else {
        chooseAccount(selectedAccs);
      }
    }

  }

  private static void expand(final View v) {
    if (v.getVisibility() == View.VISIBLE) return;
    v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    final int targtetHeight = v.getMeasuredHeight();

    v.getLayoutParams().height = 0;
    v.setVisibility(View.VISIBLE);
    Animation a = new Animation() {
      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        v.getLayoutParams().height = interpolatedTime == 1
                ? LayoutParams.WRAP_CONTENT
                : (int) (targtetHeight * interpolatedTime);
        v.requestLayout();
      }

      @Override
      public boolean willChangeBounds() {
        return true;
      }
    };

    // 1dp/ms
    a.setDuration((int) (targtetHeight / v.getContext().getResources().getDisplayMetrics().density) * 5);
    v.startAnimation(a);
  }

  private static void collapse(final View v) {
    if (v.getVisibility() == View.GONE) return;
    final int initialHeight = v.getMeasuredHeight();

    Animation a = new Animation() {
      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        if (interpolatedTime == 1) {
          v.setVisibility(View.GONE);
        } else {
          v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
          v.requestLayout();
        }
      }

      @Override
      public boolean willChangeBounds() {
        return true;
      }
    };

    // 1dp/ms
    a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density) * 5);
    v.startAnimation(a);
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
      @Override
      public void onClick(DialogInterface dialog, int which) {
        sendMessage(accs.get(which));
      }
    });

    Dialog dialog = builder.create();
    dialog.show();
  }

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
          Toast.makeText(cont, bundle.getString("result"), Toast.LENGTH_LONG).show();
        } else {
          cont.setMessageResult(MESSAGE_SENT_OK);
          cont.finish();
        }
      }
    }
  }

}
