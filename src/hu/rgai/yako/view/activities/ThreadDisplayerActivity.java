package hu.rgai.yako.view.activities;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.widget.*;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.adapters.ThreadViewAdapter;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.FacebookMessageRecipient;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.FullThreadMessage;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.MessageRecipient;
import hu.rgai.yako.beens.SmsMessageRecipient;
import hu.rgai.yako.config.ErrorCodes;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.handlers.MessageDeleteHandler;
import hu.rgai.yako.handlers.ThreadContentGetterHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.tools.RemoteMessageController;
import hu.rgai.yako.view.extensions.ZoneDisplayActionBarActivity;
import hu.rgai.yako.workers.MessageDeletionAsyncTask;
import hu.rgai.yako.workers.MessageSender;
import hu.rgai.yako.workers.ThreadContentGetter;

import java.util.*;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import hu.rgai.yako.beens.*;
import hu.rgai.yako.broadcastreceivers.MessageSentBroadcastReceiver;
import hu.rgai.yako.broadcastreceivers.ThreadMessageSentBroadcastReceiver;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.FullMessageDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.workers.TimeoutAsyncTask;
import net.htmlparser.jericho.Source;
import org.apache.http.HttpResponse;

public class ThreadDisplayerActivity extends ZoneDisplayActionBarActivity {

  public static final int MESSAGE_REPLY_REQ_CODE = 1;

  private boolean mIsQuickAnswerHidden = true;

  private ProgressDialog pd = null;
  private ThreadContentGetterHandler mHandler = null;
  private MessageListElement mMessage = null;
  private ListView lv = null;
  private ViewGroup mQuickAnswers = null;
  private ViewGroup mQuickAnswersInner = null;
  private ViewGroup mQuickAnswersCaret = null;
  private EditText mText = null;
  private ThreadViewAdapter mAdapter = null;
  private final Date lastLoadMoreEvent = null;
  private boolean firstLoad = true;
  private DataUpdateReceiver mDataUpdateReceiver = null;
  private MessageSentResultReceiver mMessageSentResultReceiver = null;
  private boolean fromNotification = false;
  private TextWatcher mTextWatcher = null;
  private TextView mCharCount = null;

  private static boolean firstResume = true;

  private ConnectivityListener mWifiListener = null;
  private boolean mMessageListChanged = false;

  /**
   * This variable holds the ID of the actually displayed thread. That's why if
   * a new message comes from this thread id, we set it immediately to seen.
   */
  public static volatile MessageListElement actViewingMessage = null;

  @Override
  public void onBackPressed() {
    Log.d("willrgai", EventLogger.LOGGER_STRINGS.THREAD.THREAD_BACKBUTTON_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + mMessage.getId());
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.THREAD.THREAD_BACKBUTTON_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + mMessage.getId(), true);
    super.onBackPressed();
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle, true, false, true);

    // setting google analytics
    Tracker t = ((YakoApp) getApplication()).getTracker();
    t.setScreenName(((Object)this).getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());

    // setting variables
    if (getIntent().getExtras().containsKey(IntentStrings.Params.FROM_NOTIFIER)
            && getIntent().getExtras().getBoolean(IntentStrings.Params.FROM_NOTIFIER)) {
      fromNotification = true;
    }
//    String msgId = getIntent().getExtras().getString(IntentStrings.Params.MESSAGE_ID);
//    Account acc = getIntent().getExtras().getParcelable(IntentStrings.Params.MESSAGE_ACCOUNT);
    long rawId = getIntent().getExtras().getLong(IntentStrings.Params.MESSAGE_RAW_ID);
    TreeMap<Long, Account> accounts = AccountDAO.getInstance(this).getIdToAccountsMap();
    mMessage = MessageListDAO.getInstance(this).getMessageByRawId(rawId, accounts);
//    mMessage = YakoApp.getMessageById_Account_Date(msgId, acc);
    if (mMessage == null) {
      finish(ErrorCodes.MESSAGE_IS_NULL_ON_MESSAGE_OPEN);
      return;
    }
    // setting action bar
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle((mMessage.getFrom() != null ? mMessage.getFrom().getName() : "") + " | " + mMessage.getAccount().getAccountType().toString());

    // initing GUI variables
    setContentView(R.layout.threadview_main);
    lv = (ListView) findViewById(R.id.main);
    mQuickAnswers = (ViewGroup)findViewById(R.id.quick_answers);
    mQuickAnswersInner = (ViewGroup) findViewById(R.id.quick_answers_inner);
    mQuickAnswersCaret = (ViewGroup) findViewById(R.id.quick_answer_caret);
    initQuickAnswerBar();
    lv.setOnScrollListener(new LogOnScrollListener());
    MessageProvider mp = AndroidUtils.getMessageProviderInstanceByAccount(mMessage.getAccount(), this);
    if (mp.isMessageDeletable()) {
      registerForContextMenu(lv);
    }

    mText = (EditText) findViewById(R.id.text);
    mHandler = new ThreadContentGetterHandler(this);
    mCharCount = (TextView)findViewById(R.id.char_count);
    
    // dealing with character counter
    if (mMessage.getMessageType().equals(MessageProvider.Type.SMS)) {
      mText.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
          if (mCharCount.getVisibility() == View.VISIBLE) {
            mCharCount.setText(AndroidUtils.getCharCountStringForSMS(mText.getText().toString()));
          }
        }
      });
      // triggering onTextChange
      mText.setText("");

      ViewTreeObserver vto = mText.getViewTreeObserver();
      vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          Resources r = getResources();
          int buttonInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, findViewById(R.id.sendButton).getHeight(), r.getDisplayMetrics());
          mText.setMinHeight(buttonInPx + mCharCount.getHeight());

          ViewTreeObserver obs = mText.getViewTreeObserver();

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            obs.removeOnGlobalLayoutListener(this);
          } else {
            obs.removeGlobalOnLayoutListener(this);
          }
        }
      });

    } else {
      mCharCount.setVisibility(View.GONE);
    }

    mTextWatcher = new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        Log.d("willrgai", EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR
                + mMessage.getId() + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString());
        EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_MESSAGES_PATH, EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR
                + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + mMessage.getId() + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString(), true);
      }
    };

    mText.addTextChangedListener(mTextWatcher);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.threadview_context_menu, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch (item.getItemId()) {
      case R.id.discard:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            final Context c = ThreadDisplayerActivity.this;
            mMessageListChanged = true;
            FullSimpleMessage simpleMessage = mAdapter.getItem(info.position);

            MessageProvider mp = AndroidUtils.getMessageProviderInstanceByAccount(mMessage.getAccount(), c);
            MessageDeleteHandler handler = new MessageDeleteHandler(c) {
              @Override
              public void onMainListDelete(List<MessageListElement> deletedMessage) {}

              @Override
              public void onThreadListDelete(long deletedMessageListRawId, String deletedSimpleMessageId,
                                             boolean isInternetNeededForProvider) {
                if (isInternetNeededForProvider) {
                  FullMessageDAO.getInstance(c).removeMessage(deletedSimpleMessageId, deletedMessageListRawId);
                } else {
                  Iterator<FullSimpleMessage> msgIterator = ((FullThreadMessage) mMessage.getFullMessage()).getMessages().iterator();
                  while (msgIterator.hasNext()) {
                    FullSimpleMessage fsm = msgIterator.next();
                    if (fsm.getId().equals(deletedSimpleMessageId)) {
                      msgIterator.remove();
                      break;
                    }
                  }
                }
                for (int i = 0; i < mAdapter.getCount(); i++) {
                  if (mAdapter.getItem(i).getId().equals(deletedSimpleMessageId)) {
                    mAdapter.removeItem(i);
                    break;
                  }
                }
                mAdapter.notifyDataSetChanged();
                if (mAdapter.getCount() == 0) {
                  try {
                    List<MessageListElement> deleteMessages = new LinkedList<MessageListElement>();
                    deleteMessages.add(new MessageListElement(deletedMessageListRawId, deletedSimpleMessageId, null));
                    MessageListDAO.getInstance(c).removeMessage(c, deleteMessages);
                  } catch (Exception e) {
                    Log.d("rgai", "", e);
                  }
                  finish();
                }
              }
              @Override
              public void onComplete() {}
            };
            MessageDeletionAsyncTask messageMarker = new MessageDeletionAsyncTask(mp, mMessage.getRawId(),
                    simpleMessage.getId(), simpleMessage.getId(), handler, false, false);
            messageMarker.setTimeout(10000);
            messageMarker.executeTask(c, null);
          }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.setTitle(R.string.delete_message);
        String delMsg = getResources().getQuantityString(R.plurals.delete_selected_msg, 1);
        builder.setMessage(delMsg).show();

        return true;
      default:
        return super.onContextItemSelected(item);
    }

  }

  @Override
  protected void onResume() {
    super.onResume();

    mMessageListChanged = false;
    if (mMessage == null) return;
    
    
    actViewingMessage = mMessage;
    removeNotificationIfExists();
    MessageListDAO.getInstance(this).updateMessageToSeen(mMessage.getRawId(), true);
//    YakoApp.setMessageSeenAndReadLocally(mMessage);


    // register wifi connection receiver if this acount depends on network, so after
    // wifi reconnect we can download messages immediately
    if (mMessage.getAccount().isInternetNeededForLoad()) {
      mWifiListener = new ConnectivityListener();
      IntentFilter iFilter = new IntentFilter(
          ConnectivityManager.CONNECTIVITY_ACTION);
      registerReceiver(mWifiListener, iFilter);
    }

    // dealing with group chat issue
    if (mMessage.isGroupMessage()
        && mMessage.getMessageType().equals(MessageProvider.Type.FACEBOOK)) {
      if (firstResume) {
        Toast
            .makeText(
                this,
                getString(R.string.sorry_grps_msg_send_not_available),
                Toast.LENGTH_LONG).show();
        firstResume = false;
      }
      mText.setVisibility(View.GONE);
      findViewById(R.id.sendButton).setVisibility(View.GONE);
    } else {
      mText.setVisibility(View.VISIBLE);
      findViewById(R.id.sendButton).setVisibility(View.VISIBLE);
    }

    // register broadcast receiver
    mDataUpdateReceiver = new DataUpdateReceiver(this);
    IntentFilter iFilter = new IntentFilter(
        Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE);
    iFilter.addAction(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
    registerReceiver(mDataUpdateReceiver, iFilter);

    mMessageSentResultReceiver = new MessageSentResultReceiver();
    iFilter = new IntentFilter(IntentStrings.Actions.MESSAGE_SENT_BROADCAST);
    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageSentResultReceiver, iFilter);
    
    
    displayContent(mMessage.getAccount().isInternetNeededForLoad());
    
    logActivityEvent(EventLogger.LOGGER_STRINGS.THREAD.THREAD_RESUME_STR);
  }

  private void finish(int code) {
    ((YakoApp) getApplication()).sendAnalyticsError(code);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        finish();
      }
    });
    builder.setTitle(R.string.error);
    builder.setMessage(String.format(getString(R.string.connection_error_pls_try_later_xcode), code)).show();
  }

  @Override
  public void finish() {
    Intent resultIntent = new Intent();
    resultIntent.putExtra(IntentStrings.Params.MESSAGE_THREAD_CHANGED, mMessageListChanged);
    if (mMessageListChanged) {
      resultIntent.putExtra(IntentStrings.Params.ACCOUNT, (Parcelable)mMessage.getAccount());
    }
    setResult(RESULT_OK, resultIntent);
    super.finish();
  }

  public void dismissProgressDialog() {
    if (pd != null) {
      pd.dismiss();
    }
  }

  public void displayContent(boolean isInternetNeededForLoad) {
    int fullMsgCount = 0;
    // only getting message from database if provider is not local (not SMS), otherwise we have to make the query anyway
    if (isInternetNeededForLoad) {
      fullMsgCount = FullMessageDAO.getInstance(this).getFullSimpleMessagesCount(mMessage.getRawId());
    }
    Log.d("rgai", "thread msg count: " + fullMsgCount);
    if (fullMsgCount > 0) {
      displayMessage(false, true, isInternetNeededForLoad);
      refreshMessageList(true);
    } else {
      if (pd == null) {
        pd = new ProgressDialog(this);
        pd.setCancelable(true);
      }
      pd.show();
      pd.setContentView(R.layout.progress_dialog);
      refreshMessageList(false);
    }
  }

  private void removeNotificationIfExists() {
    if (mMessage != null && mMessage.equals(YakoApp.getLastNotifiedMessage())) {
      NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      mNotificationManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
      YakoApp.setLastNotifiedMessage(null);
    }
  }

  public void sendMessage(View view) {
    String t = mText.getText().toString().trim();
    if (t.length() == 0) {
      Toast.makeText(this, getString(R.string.empty_message), Toast.LENGTH_SHORT).show();
      return;
    }

    MessageRecipient ri;
    if (mMessage.getAccount().getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
      ri = new FacebookMessageRecipient("", mMessage.getFrom().getId(), mMessage.getFrom().getName(), null, 1);
    } else {
      ri = new SmsMessageRecipient(mMessage.getFrom().getId(),
              mMessage.getFrom().getId(), mMessage.getFrom().getName(), null, 1);
    }
    
    // TODO: write a nice handler here!!!
//    Intent handlerIntent = new Intent(this, ThreadMessageSentBroadcastReceiver.class);
//    handlerIntent.setAction(IntentStrings.Actions.MESSAGE_SENT_BROADCAST);
    
    SentMessageBroadcastDescriptor sentMessBroadcD = new SentMessageBroadcastDescriptor(ThreadMessageSentBroadcastReceiver.class,
            IntentStrings.Actions.MESSAGE_SENT_BROADCAST);
    
    if (mMessage.getAccount().getAccountType()
        .equals(MessageProvider.Type.SMS)) {
      sentMessBroadcD.setMessageData(new SmsSentMessageData(ri.getDisplayName()));
    } else {
      sentMessBroadcD.setMessageData(new SimpleSentMessageData(ri.getDisplayName()));
    }

    List<MessageRecipient> recipients = new LinkedList<>();
    recipients.add(ri);
    MessageSender rs = new MessageSender(ri.getType(), recipients, mMessage.getAccount(), sentMessBroadcD,
            new TimeoutHandler() {
              @Override
              public void onTimeout(Context context) {
                Toast.makeText(context, R.string.unable_to_send_msg, Toast.LENGTH_SHORT).show();
              }
            },
            "", mText.getText().toString(), this);
    rs.executeTask(this, null);
    mText.setText("");
  }

  @Override
  protected void onPause() {
    Tracker t = ((YakoApp) getApplication()).getTracker();
    t.setScreenName(((Object)this).getClass().getName() + " - pause");
    t.send(new HitBuilders.AppViewBuilder().build());
    logActivityEvent(EventLogger.LOGGER_STRINGS.THREAD.THREAD_PAUSE_STR);
    super.onPause();

    if (mDataUpdateReceiver != null) {
      unregisterReceiver(mDataUpdateReceiver);
    }
    
    if (mMessageSentResultReceiver != null) {
      LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageSentResultReceiver);
    }
    
    if (mWifiListener != null) {
      unregisterReceiver(mWifiListener);
    }

    actViewingMessage = null;

    toggleQuickAnsers(false);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.thread_message_options_menu, menu);

    Person contactPerson = Person.searchPersonAndr(this, mMessage.getFrom());
    if (contactPerson.getContactId() != -1) {
      menu.findItem(R.id.add_thread_contact).setVisible(false);
    }

    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
    case (MESSAGE_REPLY_REQ_CODE):
      if (resultCode == MessageReplyActivity.MESSAGE_SENT_OK) {
        Toast.makeText(this, getString(R.string.msg_sent), Toast.LENGTH_LONG).show();
      } else if (resultCode == MessageReplyActivity.MESSAGE_SENT_FAILED) {
        Toast.makeText(this, getString(R.string.failed_to_send_msg), Toast.LENGTH_LONG)
            .show();
      }
      break;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.load_more:
        if (lastLoadMoreEvent == null || lastLoadMoreEvent.getTime() + 5000 < new Date().getTime()) {
          int messagesSize = 0;

          // if internetet needed for load that means we store messages at database
          if (mMessage.getAccount().isInternetNeededForLoad()) {
            messagesSize = FullMessageDAO.getInstance(this).getFullSimpleMessagesCount(mMessage.getRawId());
          }
          // if internet not needed for message load (SMS) than we load the message count from the object in memory
          else {
            FullMessage fm = mMessage.getFullMessage();
            if (fm != null) {
              messagesSize = ((FullThreadMessage)mMessage.getFullMessage()).getMessages().size();
            }
          }
          refreshMessageList(false, messagesSize);
          Toast.makeText(this, getString(R.string.loading_more_elements), Toast.LENGTH_LONG).show();
      } else {
        // Log.d("rgai", "@@@skipping load button press for 5 sec");
      }
      return true;
    case android.R.id.home:
      Intent upIntent = NavUtils.getParentActivityIntent(this);
      if (fromNotification) {
        TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent)
            .startActivities();
      } else {
        finish();
      }
      return true;

    case R.id.add_thread_contact:

      ArrayList<String> contactDatas = new ArrayList<String>();

      if (mMessage.getMessageType() == MessageProvider.Type.FACEBOOK) {

        contactDatas.add(mMessage.getFrom().getName());
        contactDatas.add(mMessage.getFrom().getId());
        contactDatas.add(mMessage.getFrom().getSecondaryName());

        AndroidUtils.addToFacebookContact(this, contactDatas);

      } else if (mMessage.getMessageType() == MessageProvider.Type.SMS) {

        contactDatas.add(mMessage.getFrom().getId());

        QuickContactBadge badgeSmall = AndroidUtils.addToContact(
            mMessage.getMessageType(), this, contactDatas);

        badgeSmall.onClick(item.getActionView());

      }

      return true;

    default:
      return super.onOptionsItemSelected(item);
    }
  }

  public void appendLoadedMessages(FullThreadMessage fullMessage, boolean saveToDatabase) {
    if (saveToDatabase) {
      FullMessageDAO.getInstance(this).insertMessages(this, mMessage.getRawId(), fullMessage);
    } else {
      if (mMessage.getFullMessage() == null) {
        mMessage.setFullMessage(fullMessage);
      } else {
        FullThreadMessage tm = (FullThreadMessage)mMessage.getFullMessage();
        tm.getMessages().addAll(fullMessage.getMessages());
      }
    }

  }

  public void displayMessage(boolean loadQuickAnswers, boolean scrollToBottom, boolean isInternetNeededForLoad) {
    TreeSet<FullSimpleMessage> messages = null;
    if (isInternetNeededForLoad) {
      messages = FullMessageDAO.getInstance(this).getFullSimpleMessages(this, mMessage.getRawId());
    } else {
      FullMessage fm = mMessage.getFullMessage();
      if (fm != null) {
        messages = ((FullThreadMessage)fm).getMessages();
      }
    }
    if (messages != null && !messages.isEmpty()) {
      if (mAdapter == null) {
        mAdapter = new ThreadViewAdapter(this.getApplicationContext(), R.layout.threadview_list_item_me);
      } else {
        mAdapter.clear();
      }
      FullSimpleMessage lastMessage = null;
      for (FullSimpleMessage m : messages) {
        mAdapter.add(m);
        lastMessage = m;
      }
      mAdapter.notifyDataSetChanged();
      if (lv.getAdapter() == null) {
        lv.setAdapter(mAdapter);
      }
      if (firstLoad || scrollToBottom) {
        firstLoad = false;
        lv.setSelection(lv.getAdapter().getCount() - 1);
      }
      if (!lastMessage.isIsMe() && loadQuickAnswers) {
        getQuickAnswers(lastMessage.getContent().getContent().toString());
      }
    }
  }

  private void getQuickAnswers(String content) {
    QuickAnswerLoader qal = new QuickAnswerLoader(LayoutInflater.from(this), mQuickAnswers, content);
    qal.executeTask(this, new Void[]{});
  }

  private void displayQuickAnswers(LayoutInflater inflater, ViewGroup container, List<String> answers, boolean timeout) {

    Resources r = getResources();
    float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics());
    int i = 0;

    if (answers == null || answers.isEmpty()) {
      toggleQuickAnsers(false);
    } else {
      mQuickAnswersInner.removeAllViews();
      for (final String s : answers) {
        if (i > 0) {
          LinearLayout v = new LinearLayout(this);
          LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (px * 1), ViewGroup.LayoutParams.MATCH_PARENT);
          params.topMargin = (int) (8 * px);
          params.bottomMargin = (int) (8 * px);
          v.setBackgroundColor(0xff393939);
          v.setLayoutParams(params);
          mQuickAnswersInner.addView(v);
        }

        TextView tv = (TextView) inflater.inflate(R.layout.quick_answer_item, container, false);
        tv.setText(s);
        tv.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            quickAnswerClicked(s);
          }
        });
        mQuickAnswersInner.addView(tv);
        i++;
      }
      toggleQuickAnsers(true);
    }
  }

  private void quickAnswerClicked(String s) {
    closeQuickAnsers(null);
    mText.setText(s);
    sendMessage(null);
  }

  private void refreshMessageList(boolean loadQuickAnswers, int offset) {
    ThreadContentGetter myThread = new ThreadContentGetter(this, loadQuickAnswers, mHandler, mMessage.getAccount(), offset <= 0);
    if (offset > 0) {
      myThread.setOffset(offset);
    }
    myThread.executeTask(this, new String[]{mMessage.getId()});
  }

  private void refreshMessageList(boolean loadQuickAnswers) {
    refreshMessageList(loadQuickAnswers, -1);
  }

  private void appendVisibleElementToStringBuilder(StringBuilder builder) {
    int firstVisiblePosition = lv.getFirstVisiblePosition();
    int lastVisiblePosition = lv.getLastVisiblePosition();

    for (int actualVisiblePosition = firstVisiblePosition; actualVisiblePosition <= lastVisiblePosition; actualVisiblePosition++) {
      builder.append((mAdapter.getItem(actualVisiblePosition)).getId());
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    }
  }

  private void logActivityEvent(String event) {
    if (mMessage == null)
      return;
    StringBuilder builder = new StringBuilder();
    builder.append(event);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(mMessage.getId());
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    appendVisibleElementToStringBuilder(builder);
    Log.d("willrgai", builder.toString());
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, builder.toString(), true);
  }

  public void closeQuickAnsers(View view) {
    toggleQuickAnsers(false);
  }

  private void initQuickAnswerBar() {

    Display display = getWindowManager().getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    int width = size.x;

    mQuickAnswersCaret.setAlpha(0.0f);
    mQuickAnswersCaret.setVisibility(View.VISIBLE);
    ObjectAnimator mover = ObjectAnimator.ofFloat(mQuickAnswersCaret, "translationX", 0, width);
    mover.setDuration(0);
    mover.addListener(new Animator.AnimatorListener() {
      public void onAnimationStart(Animator animation) {}
      public void onAnimationEnd(Animator animation) {
        mQuickAnswersCaret.setAlpha(1.0f);
      }
      public void onAnimationCancel(Animator animation) {}
      public void onAnimationRepeat(Animator animation) {}
    });
    mover.start();
  }

  private void toggleQuickAnsers(boolean show) {

    if (!show && mIsQuickAnswerHidden) return;

    mIsQuickAnswerHidden = !show;
    Log.d("yako", "left pos: " + mQuickAnswersCaret.getLeft());
    ObjectAnimator mover;
    int start;
    int end;
    Animator.AnimatorListener animListener;
    if (show) {
      start = mQuickAnswersCaret.getWidth();
      end = 0;
      animListener = new Animator.AnimatorListener() {
        public void onAnimationStart(Animator animation) {/*mQuickAnswersCaret.setVisibility(View.VISIBLE);*/}
        public void onAnimationEnd(Animator animation) {}
        public void onAnimationCancel(Animator animation) {}
        public void onAnimationRepeat(Animator animation) {}
      };
    } else {
      start = 0;
      end = mQuickAnswersCaret.getWidth();
      animListener = new Animator.AnimatorListener() {
        public void onAnimationStart(Animator animation) {}
        public void onAnimationEnd(Animator animation) {/*mQuickAnswersCaret.setVisibility(View.GONE);*/}
        public void onAnimationCancel(Animator animation) {}
        public void onAnimationRepeat(Animator animation) {}
      };
    }
    mover = ObjectAnimator.ofFloat(mQuickAnswersCaret, "translationX", start, end);
    mover.setDuration(250);
    mover.addListener(animListener);
    mover.start();
  }

  private class ConnectivityListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
        ConnectivityManager cm = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
          ThreadDisplayerActivity.this.refreshMessageList(false);
        }
      }
    }

  }
  public class MessageSentResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(IntentStrings.Actions.MESSAGE_SENT_BROADCAST)) {
        SentMessageBroadcastDescriptor sentMessageData = intent.getParcelableExtra(IntentStrings.Params.MESSAGE_SENT_BROADCAST_DATA);
        int resultType = sentMessageData.getResultType();
        switch(resultType) {
          case MessageSentBroadcastReceiver.MESSAGE_SENT_SUCCESS:
            ThreadDisplayerActivity.this.refreshMessageList(true);
            mMessageListChanged = true;
            break;
          case MessageSentBroadcastReceiver.MESSAGE_SENT_FAILED:
            Toast.makeText(context, R.string.unable_to_send_msg, Toast.LENGTH_SHORT).show();
            break;
          default:
            break;
        }
      }
    }
  }


  
  
  public class DataUpdateReceiver extends BroadcastReceiver {

    private final ThreadDisplayerActivity activity;

    public DataUpdateReceiver(ThreadDisplayerActivity activity) {
      this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() != null) {
        if (intent.getAction().equals(
            Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE)
            || intent.getAction().equals(
                Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST)) {
          activity.refreshMessageList(true);
        }
      }
    }
  }

  private class QuickAnswerLoader extends TimeoutAsyncTask<Void, Void, List<String>> {

    private static final String requestMod = "yako_quick_answer";
    private final LayoutInflater mInflater;
    private final ViewGroup mContainer;
    private final String mText;


    public QuickAnswerLoader(LayoutInflater inflater, ViewGroup container, String text) {
      super(null);
      mInflater = inflater;
      mContainer = container;
      mText = text;
    }

    @Override
    protected List<String> doInBackground(Void... params) {
      Source source = new Source(mText);
      String plainText = source.getRenderer().toString();

      Map<String, String> postParams = new HashMap<String, String>(2);
      postParams.put("mod", requestMod);
      postParams.put("text", plainText);
      HttpResponse response = RemoteMessageController.sendPostRequest(postParams);
      if (response != null) {
        String result = RemoteMessageController.responseToString(response);
        if (result != null) {
          return RemoteMessageController.responseStringToArray(result);
        }
      }
      return null;
    }

    @Override
    protected void onPostExecute(List<String> answers) {
      if (answers == null) {
        displayQuickAnswers(mInflater, mContainer, null, false);
      } else {
        displayQuickAnswers(mInflater, mContainer, answers, false);
      }
    }

  }

  class LogOnScrollListener implements OnScrollListener {

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
        int visibleItemCount, int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
      StringBuilder builder = new StringBuilder();

      builder.append(mMessage.getAccount().getAccountType().name());
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      builder.append(mMessage.getId());
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      if (scrollState == 1) {
        builder.append(EventLogger.LOGGER_STRINGS.SCROLL.START_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      } else {
        builder.append(EventLogger.LOGGER_STRINGS.SCROLL.END_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      }
      appendVisibleElementToStringBuilder(builder);
      Log.d("willrgai", builder.toString());
      EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, builder.toString(), true);
    }

  }
}
