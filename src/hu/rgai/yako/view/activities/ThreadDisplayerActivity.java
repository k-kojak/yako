package hu.rgai.yako.view.activities;


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
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.adapters.ThreadViewAdapter;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.broadcastreceivers.MessageSentBroadcastReceiver;
import hu.rgai.yako.broadcastreceivers.ThreadMessageSentBroadcastReceiver;
import hu.rgai.yako.config.ErrorCodes;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.handlers.MessageDeleteHandler;
import hu.rgai.yako.handlers.ThreadContentGetterHandler;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.FullMessageDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.workers.MessageDeletionAsyncTask;
import hu.rgai.yako.workers.MessageSender;
import hu.rgai.yako.workers.ThreadContentGetter;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;

public class ThreadDisplayerActivity extends ActionBarActivity {

  public static final int MESSAGE_REPLY_REQ_CODE = 1;
  
  private ProgressDialog pd = null;
  private ThreadContentGetterHandler mHandler = null;
  private MessageListElement mMessage = null;
  private ListView lv = null;
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
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.THREAD.THREAD_BACKBUTTON_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + mMessage.getId(), true);
    super.onBackPressed();
  }


  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    // setting google analytics
    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());
    
    
    // setting variables
    if (getIntent().getExtras().containsKey(IntentStrings.Params.FROM_NOTIFIER)
            && getIntent().getExtras().getBoolean(IntentStrings.Params.FROM_NOTIFIER)) {
      fromNotification = true;
    }
    String msgId = getIntent().getExtras().getString(IntentStrings.Params.MESSAGE_ID);
    Account acc = getIntent().getExtras().getParcelable(IntentStrings.Params.MESSAGE_ACCOUNT);
    mMessage = MessageListDAO.getInstance(this).getMessageById(msgId, acc);
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
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

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
          int buttonInPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, findViewById(R.id.sendButton).getHeight(), r.getDisplayMetrics());
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
      public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override
      public void afterTextChanged(Editable s) {
        Log.d("willrgai", EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR
                + mMessage.getId() + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString());
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR
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
          builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              final Context c = ThreadDisplayerActivity.this;
              mMessageListChanged = true;
              FullSimpleMessage simpleMessage = mAdapter.getItem(info.position);

              MessageProvider mp = AndroidUtils.getMessageProviderInstanceByAccount(mMessage.getAccount(), c);
              MessageDeleteHandler handler = new MessageDeleteHandler(c) {
                @Override
                public void onMainListDelete(long deletedMessageListRawId) {}

                @Override
                public void onThreadListDelete(long deletedMessageListRawId, String deletedSimpleMessageId,
                                               boolean isInternetNeededForProvider) {
                  if (isInternetNeededForProvider) {
                    FullMessageDAO.getInstance(c).removeMessage(deletedSimpleMessageId, deletedMessageListRawId);
                  } else {
                    Iterator<FullSimpleMessage> msgIterator = ((FullThreadMessage)mMessage.getFullMessage()).getMessages().iterator();
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
                    MessageListDAO.getInstance(c).removeMessage(deletedMessageListRawId);
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
          builder.setNegativeButton("No", null);
          builder.setTitle("Delete message");
          builder.setMessage("Delete selected message?").show();
          
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
      IntentFilter iFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
      registerReceiver(mWifiListener, iFilter);
    }
    
    
    // dealing with group chat issue
    if (mMessage.isGroupMessage() && mMessage.getMessageType().equals(MessageProvider.Type.FACEBOOK)) {
      if (firstResume) {
        Toast.makeText(this, "Sorry, but group message sending is not available (because of Facebook).", Toast.LENGTH_LONG).show();
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
    IntentFilter iFilter = new IntentFilter(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE);
    iFilter.addAction(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
    registerReceiver(mDataUpdateReceiver, iFilter);

    mMessageSentResultReceiver = new MessageSentResultReceiver();
    iFilter = new IntentFilter(IntentStrings.Actions.MESSAGE_SENT_BROADCAST);
    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageSentResultReceiver, iFilter);
    
    
    displayContent(mMessage.getAccount().isInternetNeededForLoad());
    
    logActivityEvent(EventLogger.LOGGER_STRINGS.THREAD.THREAD_RESUME_STR);
  }
  
  private void finish(int code) {
    ((YakoApp)getApplication()).sendAnalyticsError(code);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        finish();
      }
    });
    builder.setTitle("Error");
    builder.setMessage("Connection error, please try later.\n(Error " + code + ")").show();
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
  
  
//  public void setMessageContent(FullThreadMessage content) {
//    mMessage.setFullMessage(content);
//    YakoApp.setMessageContent(mMessage, content);
//  }


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
      displayMessage(true, isInternetNeededForLoad);
      refreshMessageList();
    } else {
      if (pd == null) {
        pd = new ProgressDialog(this);
        pd.setCancelable(true);
      }
      pd.show();
      pd.setContentView(R.layout.progress_dialog);
      refreshMessageList();
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
      Toast.makeText(this, "Empty message", Toast.LENGTH_SHORT).show();
      return;
    }
    
    MessageRecipient ri = null;
    if (mMessage.getAccount().getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
      ri = new FacebookMessageRecipient("", mMessage.getFrom().getId(), mMessage.getFrom().getName(), null, 1);
    } else {
      ri = new SmsMessageRecipient(mMessage.getFrom().getId(), mMessage.getFrom().getId(), mMessage.getFrom().getName(), null, 1);
    }
    
    // TODO: write a nice handler here!!!
//    Intent handlerIntent = new Intent(this, ThreadMessageSentBroadcastReceiver.class);
//    handlerIntent.setAction(IntentStrings.Actions.MESSAGE_SENT_BROADCAST);
    
    SentMessageBroadcastDescriptor sentMessBroadcD = new SentMessageBroadcastDescriptor(ThreadMessageSentBroadcastReceiver.class,
            IntentStrings.Actions.MESSAGE_SENT_BROADCAST);
      
    MessageSender rs = new MessageSender(ri, mMessage.getAccount(), sentMessBroadcD,
            new TimeoutHandler() {
              @Override
              public void onTimeout(Context context) {
                Toast.makeText(context, "Unable to send message...", Toast.LENGTH_SHORT).show();
              }
            },
            "", mText.getText().toString(), this);
    rs.executeTask(this, null);
    mText.setText("");
  }

  @Override
  protected void onPause() {
    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName() + " - pause");
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
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.thread_message_options_menu, menu);
    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case (MESSAGE_REPLY_REQ_CODE):
        if (resultCode == MessageReplyActivity.MESSAGE_SENT_OK) {
          Toast.makeText(this, "Message sent", Toast.LENGTH_LONG).show();
        } else if (resultCode == MessageReplyActivity.MESSAGE_SENT_FAILED) {
          Toast.makeText(this, "Failed to send message ", Toast.LENGTH_LONG).show();
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
          refreshMessageList(messagesSize);
          Toast.makeText(this, getString(R.string.loading_more_elements), Toast.LENGTH_LONG).show();
        } else {
//          Log.d("rgai", "@@@skipping load button press for 5 sec");
        }
        return true;
      case android.R.id.home:
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        if (fromNotification) {
            TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
        } else {
          finish();
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public void appendLoadedMessages(FullThreadMessage fullMessage, boolean saveToDatabase) {
    if (saveToDatabase) {
      FullMessageDAO.getInstance(this).appendMessages(this, mMessage.getRawId(), fullMessage);
    } else {
      if (mMessage.getFullMessage() == null) {
        mMessage.setFullMessage(fullMessage);
      } else {
        FullThreadMessage tm = (FullThreadMessage)mMessage.getFullMessage();
        tm.getMessages().addAll(fullMessage.getMessages());
      }
    }

//    YakoApp.setMessageContent(mMessage, mMessage.getFullMessage());
  }
  
  public void displayMessage(boolean scrollToBottom, boolean isInternetNeededForLoad) {
    int firstVisiblePos = lv.getFirstVisiblePosition();
    int oldItemCount = 0;
    if (mAdapter != null) {
      oldItemCount = mAdapter.getCount();
    }
    TreeSet<FullSimpleMessage> messages = null;
    if (isInternetNeededForLoad) {
      messages = FullMessageDAO.getInstance(this).getFullSimpleMessages(mMessage.getRawId());
    } else {
      FullMessage fm = mMessage.getFullMessage();
      if (fm != null) {
        messages = ((FullThreadMessage)fm).getMessages();
      }
    }
    if (messages != null && !messages.isEmpty()) {
      if (mAdapter == null) {
        mAdapter = new ThreadViewAdapter(this.getApplicationContext(), R.layout.threadview_list_item);
      } else {
        mAdapter.clear();
      }
      for (FullSimpleMessage m : messages) {
        mAdapter.add(m);
      }
      mAdapter.notifyDataSetChanged();
      if (lv.getAdapter() == null) {
        lv.setAdapter(mAdapter);
      }
      if (firstLoad || scrollToBottom) {
        firstLoad = false;
        lv.setSelection(lv.getAdapter().getCount() - 1);
      } else {
//        int newItemCount = mAdapter.getCount();
//        lv.setSelection(newItemCount - oldItemCount + firstVisiblePos);
      }
    }
  }
  
  private void refreshMessageList(int offset) {
    Log.d("rgai", "loading messages with offset: " + offset);
    ThreadContentGetter myThread = new ThreadContentGetter(this, mHandler, mMessage.getAccount(), offset <= 0);
    if (offset > 0) {
      myThread.setOffset(offset);
    }
    myThread.executeTask(this, new String[]{mMessage.getId()});
  }
  
  private void refreshMessageList() {
    refreshMessageList(-1);
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
    if (mMessage == null) return;
    StringBuilder builder = new StringBuilder();
    builder.append(event);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(mMessage.getId());
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    appendVisibleElementToStringBuilder(builder);
    Log.d("willrgai", builder.toString());
    EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
  }
  
  private class ConnectivityListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
          ThreadDisplayerActivity.this.refreshMessageList();
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
            ThreadDisplayerActivity.this.refreshMessageList();
            mMessageListChanged = true;
            break;
          case MessageSentBroadcastReceiver.MESSAGE_SENT_FAILED:
            Toast.makeText(context, "Unable to send message", Toast.LENGTH_SHORT).show();
            break;
          default:
            break;
        }
      }
    }
  }
  
  
  public class DataUpdateReceiver extends BroadcastReceiver {

    private ThreadDisplayerActivity activity;

    public DataUpdateReceiver(ThreadDisplayerActivity activity) {
      this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() != null) {
        if (intent.getAction().equals(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE)
                || intent.getAction().equals(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST)) {
          activity.refreshMessageList();
        }
      }
    }
  }

  class LogOnScrollListener implements OnScrollListener {

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}

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
      EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
    }
    
  }
}
