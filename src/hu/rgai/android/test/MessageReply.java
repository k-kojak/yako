package hu.rgai.android.test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
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
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.EmailMessageRecipient;
import hu.rgai.android.beens.FacebookMessageRecipient;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.MainServiceExtraParams.ParamStrings;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.MessageRecipient;
import hu.rgai.android.beens.Person;
import hu.rgai.android.beens.SmsAccount;
import hu.rgai.android.beens.SmsMessageRecipient;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.handlers.MessageSendHandler;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.tools.adapter.ContactListAdapter;
import hu.rgai.android.tools.view.ChipsMultiAutoCompleteTextView;
import hu.rgai.android.workers.MessageSender;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import net.htmlparser.jericho.Source;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageReply extends ActionBarActivity {

  public static final int MESSAGE_SENT_OK = 1;

  public static final int MESSAGE_SENT_FAILED = 2;

  private int messageResult;
  private TextView text;
  private ChipsMultiAutoCompleteTextView recipients;
  private Account mAccount;
  private WebView mQuotedMessage = null;
  private CheckBox mQuoteCheckbox = null;
  private EditText mSubject = null;
  private MessageListElement mMessage = null;
  private FullSimpleMessage mFullMessage = null;

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
        if (mMessage != null) {
          mAccount = mMessage.getAccount();
        }
        if (mMessage.getFullMessage() != null && mMessage.getFullMessage() instanceof FullSimpleMessage) {
          mFullMessage = (FullSimpleMessage) mMessage.getFullMessage();
          mSubject.setText(mFullMessage.getSubject());
        }
      }
      if (getIntent().getExtras().containsKey(ParamStrings.FROM_NOTIFIER)) {
        Log.d("rgai", "yes, from notifier");
        NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
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
      if (mMessage.getFrom() != null && mAccount != null
              && (mAccount.getAccountType().equals(MessageProvider.Type.EMAIL) || mAccount.getAccountType().equals(MessageProvider.Type.GMAIL))) {

        MessageRecipient ri = new EmailMessageRecipient(mMessage.getFrom().getName(), mMessage.getFrom().getId(), mMessage.getFrom().getName(),
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
    recipients.setAdapter(adapter);
    recipients.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    LinearLayout fake = (LinearLayout) findViewById(R.id.fake_focus);
    fake.requestFocus();
    if (mFullMessage != null) {
      mQuotedMessage.loadDataWithBaseURL(null, mFullMessage.getContent().getContent().toString(),
              mFullMessage.getContent().getContentType().getMimeName(), "UTF-8", null);
    }

    if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SENDTO)) {
      processImplicitIntent(getIntent());
    }

  }

  private void showHideSubjectField() {
    boolean hasEmailRecipient = false;
    for (MessageRecipient ri : recipients.getRecipients()) {
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
    Person pa = Person.searchPersonAndr(this, new Person(id, id, type));

    if (pa != null) {
      MessageRecipient ri = null;
      switch (type) {
        case FACEBOOK:
          ri = new FacebookMessageRecipient(pa.getName(), pa.getId(), pa.getName(), null, (int) pa.getContactId());
          break;
        case SMS:
          ri = new SmsMessageRecipient(pa.getName(), pa.getId(), pa.getName(), null, (int) pa.getContactId());
          break;
        case EMAIL:
        case GMAIL:
          ri = new EmailMessageRecipient(pa.getName(), pa.getId(), pa.getName(), null, (int) pa.getContactId());
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


  public void sendMessage(Account from) {
    if (from == null) {
      return;
    }

    List<MessageRecipient> to = recipients.getRecipients();
    String content = text.getText().toString().trim();
    String subject = null;
    if (mSubject.getVisibility() == View.VISIBLE) {
      subject = mSubject.getText().toString();
    }
    if (subject == null) {
      subject = "";
    }
    for (MessageRecipient ri : to) {
      if (mQuotedMessage.getVisibility() == View.VISIBLE) {
        Source source = new Source("<br /><br />" + mFullMessage.getContent().getContent());
        content += source.getRenderer().toString();
      }
      MessageSendHandler handler = new MessageSendHandler() {
        public void success(String name) {
          displayNotification(true, name);
        }

        public void fail(String name) {
          displayNotification(false, name);
        }
        
        private void displayNotification(boolean success, String to) {
          
          String ticker = success ? "Message sent" : "Sending failed";
          String title = success ? "Message sent to" : "Failed to send message to:";
          
          NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
          
          NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MessageReply.this)
                  .setSmallIcon(R.drawable.not_ic_action_email)
                  .setTicker(ticker)
                  .setContentTitle(title)
                  .setContentText(to);
          mBuilder.setAutoCancel(true);
          mNotificationManager.notify(Settings.NOTIFICATION_SENT_MESSAGE_ID, mBuilder.build());
          
          if (success) {
            new Timer().schedule(new TimerTask() {
              @Override
              public void run() {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(Settings.NOTIFICATION_SENT_MESSAGE_ID);
              }
            }, 5000);
          }
        }
      };
      MessageSender rs = new MessageSender(ri, from, handler, subject, content, this);
      rs.executeTask(null);
      finish();
      
    }
  }

  private void prepareMessageSending() {
    List<MessageRecipient> to = recipients.getRecipients();
    if (to.isEmpty()) {
      Toast.makeText(this, R.string.no_recipient_selected, Toast.LENGTH_SHORT).show();
      return;
    }

    if (text.getText().toString().trim().length() == 0) {
      Toast.makeText(this, R.string.empty_message, Toast.LENGTH_SHORT).show();
      return;
    }

    List<Account> accs = StoreHandler.getAccounts(this);

    for (MessageRecipient ri : to) {
      List<Account> selectedAccs = new LinkedList<Account>();
      Iterator<Account> accIt = accs.iterator();
      if (ri.getType().equals(MessageProvider.Type.SMS) && YakoApp.isPhone) {
        selectedAccs.add(SmsAccount.account);
      } else {
        while (accIt.hasNext()) {
          Account actAcc = accIt.next();
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

  private void chooseAccount(final List<Account> accs) {

    String[] items = new String[accs.size()];
    int i = 0;
    for (Account a : accs) {
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

}
