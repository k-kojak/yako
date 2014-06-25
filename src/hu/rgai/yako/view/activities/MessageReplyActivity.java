package hu.rgai.yako.view.activities;

import static android.app.Activity.RESULT_OK;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
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
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.adapters.ContactListAdapter;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.EmailMessageRecipient;
import hu.rgai.yako.beens.FacebookMessageRecipient;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.MessageRecipient;
import hu.rgai.yako.beens.Person;
import hu.rgai.yako.beens.SentMessageBroadcastDescriptor;
import hu.rgai.yako.beens.SimpleSentMessageData;
import hu.rgai.yako.beens.SentMessageData;
import hu.rgai.yako.beens.SmsAccount;
import hu.rgai.yako.beens.SmsMessageRecipient;
import hu.rgai.yako.beens.SmsSentMessageData;
import hu.rgai.yako.broadcastreceivers.SimpleMessageSentBroadcastReceiver;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.view.extensions.ChipsMultiAutoCompleteTextView;
import hu.rgai.yako.workers.MessageSeenMarkerAsyncTask;
import hu.rgai.yako.workers.MessageSender;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import net.htmlparser.jericho.Source;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageReplyActivity extends ActionBarActivity {

  public static final int MESSAGE_SENT_OK = 1;

  public static final int MESSAGE_SENT_FAILED = 2;

  private int messageResult;
  private TextView mContent;
  private TextView mCharCount;
  private ChipsMultiAutoCompleteTextView recipients;
  private Account mAccount;
  private WebView mQuotedMessage = null;
  private CheckBox mQuoteCheckbox = null;
  private EditText mSubject = null;
  private MessageListElement mMessage = null;
  private FullSimpleMessage mFullMessage = null;
  private boolean isCharCountVisible = false;
  private final TreeSet<Account> mChoosenAccountsToSend = new TreeSet<Account>();

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
    mCharCount = (TextView) findViewById(R.id.char_count);
    mContent = (TextView) findViewById(R.id.message_content);
    mQuotedMessage = (WebView) findViewById(R.id.quoted_message);
    mQuoteCheckbox = (CheckBox) findViewById(R.id.quote_origi);
    
    
    if (getIntent().getExtras() != null) {
      if (getIntent().getExtras().containsKey(IntentStrings.Params.MESSAGE_ID)) {
        String msgId = getIntent().getExtras().getString(IntentStrings.Params.MESSAGE_ID);
        Account acc = getIntent().getExtras().getParcelable(IntentStrings.Params.MESSAGE_ACCOUNT);
        mMessage = YakoApp.getMessageById_Account_Date(msgId, acc);
        if (mMessage != null) {
          mAccount = mMessage.getAccount();
        }
        if (mMessage.getFullMessage() != null && mMessage.getFullMessage() instanceof FullSimpleMessage) {
          mFullMessage = (FullSimpleMessage) mMessage.getFullMessage();
          mSubject.setText(mFullMessage.getSubject());
        }
      }
      if (getIntent().getExtras().containsKey(IntentStrings.Params.FROM_NOTIFIER)) {
        NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
        // mMessage shouldn be null here...
        if (mMessage != null) {
          YakoApp.setMessageSeenAndReadLocally(mMessage);
          MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(mMessage.getAccount(), this);
          MessageSeenMarkerAsyncTask marker = new MessageSeenMarkerAsyncTask(provider,
                  new TreeSet<MessageListElement>(Arrays.asList(new MessageListElement[]{mMessage})), true, null);
          marker.executeTask(this, null);
        }
        
      }
    }
    
    
    

    mContent.addTextChangedListener(new TextWatcher() {

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
        if (isCharCountVisible) {
          mCharCount.setText(AndroidUtils.getCharCountStringForSMS(mContent.getText().toString()));
        }
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
        showHideCharacterCountField();
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
    showHideCharacterCountField();

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
  
  
  private void showHideCharacterCountField() {
    boolean hasSmsRecipient = false;
    for (MessageRecipient ri : recipients.getRecipients()) {
      if (ri.getType().equals(MessageProvider.Type.SMS)) {
        hasSmsRecipient = true;
        break;
      }
    }
    if (hasSmsRecipient) {
      isCharCountVisible = true;
      expand(mCharCount);
      mCharCount.setText(AndroidUtils.getCharCountStringForSMS(mContent.getText().toString()));
    } else {
      isCharCountVisible = false;
      collapse(mCharCount);
    }
  }
  
  public void onQuoteClicked(View view) {
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
        List<MessageRecipient> to = recipients.getRecipients();
        mChoosenAccountsToSend.clear();
        if (to.isEmpty()) {
          Toast.makeText(this, R.string.no_recipient_selected, Toast.LENGTH_SHORT).show();
        } else {
          if (mContent.getText().toString().trim().length() == 0) {
            Toast.makeText(this, R.string.empty_message, Toast.LENGTH_SHORT).show();
          } else {
            prepareMessageSending(to);
          }
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }


  public void sendMessage(MessageRecipient recipient, Account from, List<MessageRecipient> recipients) {
    if (from == null) {
      return;
    }
    
    mChoosenAccountsToSend.add(from);
    
    String content = mContent.getText().toString().trim();
    String subject = null;
    if (mSubject.getVisibility() == View.VISIBLE) {
      subject = mSubject.getText().toString();
    }
    if (subject == null) {
      subject = "";
    }
    
    if (mQuotedMessage.getVisibility() == View.VISIBLE) {
      Source source = new Source("<br /><br />" + mFullMessage.getContent().getContent());
      content += source.getRenderer().toString();
    }
    
    
    // setting intent data for handling returning result
    SentMessageBroadcastDescriptor sentMessBroadcD = new SentMessageBroadcastDescriptor(SimpleMessageSentBroadcastReceiver.class,
            IntentStrings.Actions.MESSAGE_SENT_BROADCAST);
    SentMessageData smd = getSentMessageDataToAccount(recipient.getDisplayName(), from);
    sentMessBroadcD.setMessageData(smd);
    
    
    MessageSender rs = new MessageSender(recipient, from, sentMessBroadcD,
            new TimeoutHandler() {
              @Override
              public void timeout(Context context) {
                Toast.makeText(context, "Unable to send message...", Toast.LENGTH_SHORT).show();
              }
            },
            subject, content, this);
    
    rs.setTimeout(20000);
    rs.executeTask(this, null);
    
    // this means this was the last call of sendMessage
    if (recipients.isEmpty()) {
      // request a message list for the account we sent the message with
//      int threadAccountCount = 0;
//      Account threadAccount = null;
//      for (Account a : mChoosenAccountsToSend) {
//        if (a.isThreadAccount()) {
//          threadAccountCount++;
//          threadAccount = a;
//        }
//      }
//      Log.d("rgai", "thread account count: " + threadAccountCount);
//      Log.d("rgai", "thread account: " + threadAccount);
//      if (!mChoosenAccountsToSend.isEmpty()) {
//        Log.d("rgai", "sending request to get messages");
//        MainServiceExtraParams eParams = new MainServiceExtraParams();
//        if (threadAccountCount == 1) {
//          eParams.setAccount(threadAccount);
//        }
//        eParams.setForceQuery(true);
//        Intent intent = new Intent(this, MainScheduler.class);
//        intent.setAction(Context.ALARM_SERVICE);
//        intent.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
//        this.sendBroadcast(intent);
//      }
      finish();
    } else {
      prepareMessageSending(recipients);
    }
  }
  
  private SentMessageData getSentMessageDataToAccount(String recipientName, Account from) {
  
    if (from.getAccountType().equals(MessageProvider.Type.SMS)) {
      SmsSentMessageData smsData = new SmsSentMessageData(recipientName);
      if (from.isThreadAccount()) {
        smsData.setAccountToLoad(from);
      }
      return smsData;
    } else {
      SimpleSentMessageData simpleData = new SimpleSentMessageData(recipientName);
      if (from.isThreadAccount()) {
        simpleData.setAccountToLoad(from);
      }
      return simpleData;
    }
  }

  private void prepareMessageSending(List<MessageRecipient> recipients) {

    if (recipients.isEmpty()) return;
    
    TreeSet<Account> accounts = AccountDAO.getInstance(this).getAllAccounts();

    MessageRecipient ri = recipients.remove(0);
    List<Account> availableAccounts = new LinkedList<Account>();

    for (Account actAcc : accounts) {
      if (((ri.getType().equals(MessageProvider.Type.EMAIL) || ri.getType().equals(MessageProvider.Type.GMAIL))
              && (actAcc.getAccountType().equals(MessageProvider.Type.EMAIL)
              || actAcc.getAccountType().equals(MessageProvider.Type.GMAIL)))
              || ri.getType().equals(actAcc.getAccountType())) {
        if (!availableAccounts.contains(actAcc)) {
          availableAccounts.add(actAcc);
        }
      }
    }
    if (availableAccounts.isEmpty()) {
      Toast.makeText(this,
              "Cannot send message to " + ri.getDisplayData() + ". A " + ri.getType().toString() + " account required for that.",
              Toast.LENGTH_LONG).show();
    } else if (availableAccounts.size() == 1) {
      sendMessage(ri, availableAccounts.get(0), recipients);
    } else {
      chooseAccount(ri, availableAccounts, recipients);
    }
    
  }
  
  
  private void expand(final View v) {
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

  private void collapse(final View v) {
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

  private void chooseAccount(final MessageRecipient recipient, final List<Account> accs,
          final List<MessageRecipient> recipients) {

    String[] items = new String[accs.size()];
    int i = 0;
    for (Account a : accs) {
      items[i++] = a.getDisplayName();
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Send message to " + recipient.getDisplayName() + " from:");
    builder.setItems(items, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        sendMessage(recipient, accs.get(which), recipients);
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
