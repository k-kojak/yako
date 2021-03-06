package hu.rgai.yako.view.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
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
import android.widget.*;
import hu.rgai.android.test.R;
import hu.rgai.yako.adapters.ContactListAdapter;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.broadcastreceivers.SimpleMessageSentBroadcastReceiver;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.FullMessageDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.sql.MessageRecipientDAO;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.view.extensions.ChipsMultiAutoCompleteTextView;
import hu.rgai.yako.view.extensions.ZoneDisplayActionBarActivity;
import hu.rgai.yako.workers.MessageSeenMarkerAsyncTask;
import hu.rgai.yako.workers.MessageSender;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageReplyActivity extends ZoneDisplayActionBarActivity {

  public static final int MESSAGE_SENT_OK = 1;

  public static final int MESSAGE_SENT_FAILED = 2;

  private TextView mContent;
  private TextView mCharCount;
  private ChipsMultiAutoCompleteTextView recipients;
  private Account mAccount;
  private WebView mQuotedMessage = null;
  private CheckBox mQuoteCheckbox = null;
  private View mQuotedSeparator = null;
  private EditText mSubject = null;
  private MessageListElement mMessage = null;
  private FullSimpleMessage mFullMessage = null;
  private boolean isCharCountVisible = false;
  private final TreeSet<Account> mChoosenAccountsToSend = new TreeSet<Account>();

  @Override
  public void onBackPressed() {
    Log.d("willrgai", EventLogger.LOGGER_STRINGS.MESSAGE_REPLY.MESSAGE_REPLY_BACKBUTTON_STR);
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.MESSAGE_REPLY.MESSAGE_REPLY_BACKBUTTON_STR, true);
    super.onBackPressed();
  }

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle, true, true, true);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setDisplayHomeAsUpEnabled(true);

    setContentView(R.layout.message_reply);

    
    mSubject = (EditText) findViewById(R.id.subject);
    mCharCount = (TextView) findViewById(R.id.char_count);
    mContent = (TextView) findViewById(R.id.message_content);

    mQuotedMessage = (WebView) findViewById(R.id.quoted_message);
    mQuotedMessage.getSettings().setBuiltInZoomControls(true);
    mQuotedMessage.getSettings().setDisplayZoomControls(false);

    mQuoteCheckbox = (CheckBox) findViewById(R.id.quote_origi);
    mQuotedSeparator = findViewById(R.id.quoted_separator);

    List<Person> sendTo = new ArrayList<>();
    boolean replyToAll = false;

    if (getIntent().getExtras() != null) {
      if (getIntent().getExtras().containsKey(IntentStrings.Params.MESSAGE_REPLY_TO_ALL)) {
        replyToAll = getIntent().getExtras().getBoolean(IntentStrings.Params.MESSAGE_REPLY_TO_ALL, false);
      }
      if (getIntent().getExtras().containsKey(IntentStrings.Params.MESSAGE_RAW_ID)) {
        long rawId = getIntent().getExtras().getLong(IntentStrings.Params.MESSAGE_RAW_ID);
        TreeMap<Long, Account> accounts = AccountDAO.getInstance(this).getIdToAccountsMap();
        mMessage = MessageListDAO.getInstance(this).getMessageByRawId(rawId, accounts);
        if (mMessage != null) {
          mAccount = mMessage.getAccount();
        }
        TreeSet<FullSimpleMessage> fullMessage = FullMessageDAO.getInstance(this).getFullSimpleMessages(this,
                mMessage.getRawId());
        mFullMessage = fullMessage.first();
        mSubject.setText(mFullMessage.getSubject());
      }
      if (getIntent().getExtras().containsKey(IntentStrings.Params.FROM_NOTIFIER)) {
        NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
        // mMessage shouldnt be null here...
        if (mMessage != null) {
          // TODO: check if message is set to read remotely in this case or not
          MessageListDAO.getInstance(this).updateMessageToSeen(mMessage.getRawId(), true);
          MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(mMessage.getAccount(), this);
          TreeSet<String> messagesToMark = new TreeSet<>();
          messagesToMark.add(mMessage.getId());
          MessageSeenMarkerAsyncTask marker = new MessageSeenMarkerAsyncTask(provider, messagesToMark, true, null);
          marker.executeTask(this, null);
        }
      }
      if (getIntent().getAction() != null) {
        if (getIntent().getAction().equals(IntentStrings.Actions.DIRECT_EMAIL)) {
          sendTo.add((Person)getIntent().getExtras().getParcelable(IntentStrings.Params.PERSON));
        }
      }
    }
    
    
    

    mContent.addTextChangedListener(new TextWatcher() {

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        if (isCharCountVisible) {
          mCharCount.setText(AndroidUtils.getCharCountStringForSMS(mContent.getText().toString()));
        }
        Log.d("willrgai", EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR
                + "null" + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString());
        EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_MESSAGES_PATH, EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR
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
        sendTo.add(mMessage.getFrom());
        if (replyToAll) {
          String myEmail = mAccount.getAccountType().equals(MessageProvider.Type.GMAIL)
                  ? ((GmailAccount)mAccount).getEmail() + "@gmail.com"
                  : ((EmailAccount)mAccount).getEmail();
          List<Person> recs = MessageRecipientDAO.getInstance(this).getRecipientsToMessageId(this, mMessage);
          for (Person p : recs) {
            if (!p.getId().equals(myEmail)) {
              sendTo.add(p);
            }
          }
        }
      }
    } else {
      mQuotedMessage.setVisibility(View.GONE);
      mQuoteCheckbox.setVisibility(View.GONE);
      mQuotedSeparator.setVisibility(View.GONE);
    }

    if (!sendTo.isEmpty()) {
      for (int i = 0; i < sendTo.size(); i++) {
        Person sendToAndr = Person.searchPersonAndr(this, sendTo.get(i));
        if (sendToAndr != null && !sendTo.get(i).equals(sendToAndr)) {
          sendTo.set(i, sendToAndr);
        }
      }
      for (Person p : sendTo) {
        MessageRecipient rec = new EmailMessageRecipient(p.getName(), p.getId(), p.getName(), null,
                (int) p.getContactId());
        recipients.addRecipient(rec);
      }
    }


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

    showHideCharacterCountField();
  }

  @Override
  protected void onResume() {
    super.onResume();
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        showHideSubjectField();
      }
    }, 300);

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
      EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.OTHER.MESSAGE_WRITE_FROM_CONTACT_LIST, true);
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
      Log.d("rgai", "", ex);
      Toast.makeText(this, getString(R.string.unsupported_encoding), Toast.LENGTH_LONG).show();
    } catch (UnsupportedEncodingException ex) {
      Log.d("rgai", "", ex);
      Toast.makeText(this, getString(R.string.unsupported_charset), Toast.LENGTH_LONG).show();
    }
  }

  private void searchUserAndInsertAsRecipient(MessageProvider.Type type, String id) {
    Person pa = Person.searchPersonAndr(this, new Person(id, id, type));
    if (pa != null) {
      MessageRecipient ri = MessageRecipient.Helper.personToRecipient(pa);
      if (ri != null) {
        recipients.addRecipient(ri);
      }
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
//            prepareSend(to);
            prepareMessageSending(to);
          }
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }


  public static String getDesignedQuotedText(String s) {
    return "<br /><br />"
            + " <blockquote"
            + " style=\"color: #666; padding-left: 1ex; margin: 0px 0px 0px 0.8ex; border-left: 1px solid rgb(204, 204, 204);\">"
            + s
            + "</blockquote>";
  }

  public void sendMessage(MessageProvider.Type sendType, List<MessageRecipient> toSend,
                          Account from, List<MessageRecipient> recipients) {
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
      content += getDesignedQuotedText(mFullMessage.getContent().getContent().toString());
    }
    
    
    // setting intent data for handling returning result
    SentMessageBroadcastDescriptor sentMessBroadcD = new SentMessageBroadcastDescriptor(SimpleMessageSentBroadcastReceiver.class,
            IntentStrings.Actions.MESSAGE_SENT_BROADCAST);
    SentMessageData smd = getSentMessageDataToAccount(recipients, from);
    sentMessBroadcD.setMessageData(smd);
    
    
    MessageSender rs = new MessageSender(sendType, toSend, from, sentMessBroadcD,
            new TimeoutHandler() {
              @Override
              public void onTimeout(Context context) {
                Toast.makeText(context, "Unable to send message...", Toast.LENGTH_SHORT).show();
              }
            },
            subject, content, this);
    
    rs.setTimeout(20000);
    rs.executeTask(this, null);
    
    // this means this was the last call of sendMessage
    if (recipients.isEmpty()) {
      finish();
    } else {
      prepareMessageSending(recipients);
    }
  }
  
  public static SentMessageData getSentMessageDataToAccount(List<MessageRecipient> recipients, Account from) {

    StringBuilder recName = new StringBuilder();
    int i = 0;
    for (MessageRecipient mr : recipients) {
      if (i > 0) {
        recName.append(", ");
      }
      recName.append(mr.getDisplayData());
      i++;
    }

    if (from.getAccountType().equals(MessageProvider.Type.SMS)) {
      SmsSentMessageData smsData = new SmsSentMessageData(recName.toString());
      if (from.isThreadAccount()) {
        smsData.setAccountToLoad(from);
      }
      return smsData;
    } else {
      SimpleSentMessageData simpleData = new SimpleSentMessageData(recName.toString());
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
    List<Account> availableAccounts = new LinkedList<>();

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

    List<MessageRecipient> allRecipients = new LinkedList<>();
    allRecipients.add(ri);
    Iterator<MessageRecipient> it = recipients.iterator();
    while (it.hasNext()) {
      MessageRecipient mr = it.next();
      if (mr.getType().equals(ri.getType())) {
        allRecipients.add(mr);
        it.remove();
      }
    }

    if (availableAccounts.isEmpty()) {
      Toast.makeText(this,
              "Cannot send message to " + ri.getDisplayData() + ". A " + ri.getType().toString() + " instance required for that.",
              Toast.LENGTH_LONG).show();
    } else if (availableAccounts.size() == 1) {
      sendMessage(ri.getType(), allRecipients, availableAccounts.get(0), recipients);
    } else {
      chooseAccount(ri.getType(), allRecipients, availableAccounts, recipients);
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

  private void chooseAccount(final MessageProvider.Type sendType, final List<MessageRecipient> toSend,
                             final List<Account> accs, final List<MessageRecipient> recipients) {

    String[] items = new String[accs.size()];
    int i = 0;
    for (Account a : accs) {
      items[i++] = a.getDisplayName();
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Send " + toSend.get(0).getType() + " message with:");
    builder.setItems(items, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        sendMessage(sendType, toSend, accs.get(which), recipients);
      }
    });

    Dialog dialog = builder.create();
    dialog.show();
  }

  @Override
  public void finish() {
    super.finish();
  }

}
