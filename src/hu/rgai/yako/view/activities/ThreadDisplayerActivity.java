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
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
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
import hu.rgai.yako.handlers.MessageSendHandler;
import hu.rgai.yako.handlers.ThreadContentGetterHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.tools.IntentParamStrings;
import hu.rgai.yako.workers.MessageDeletionAsyncTask;
import hu.rgai.yako.workers.MessageSender;
import hu.rgai.yako.workers.ThreadContentGetter;
import java.util.Date;

public class ThreadDisplayerActivity extends ActionBarActivity {

  public static final int MESSAGE_REPLY_REQ_CODE = 1;
  
  private ProgressDialog pd = null;
  private ThreadContentGetterHandler mHandler = null;
  private MessageListElement mMessage = null;
  private ListView lv = null;
  private EditText mText = null;
  private ThreadViewAdapter adapter = null;
  private final Date lastLoadMoreEvent = null;
  private boolean firstLoad = true;
  private DataUpdateReceiver mDataUpdateReceiver = null;
  private boolean fromNotification = false;
  private TextWatcher mTextWatcher = null;
  private TextView mCharCount = null;

  private static boolean firstResume = true;

  
  
  private ConnectivityListener mWifiListener = null;
  private boolean thereWasMessageDeletion = false;

  
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
    if (getIntent().getExtras().containsKey(IntentParamStrings.FROM_NOTIFIER)
            && getIntent().getExtras().getBoolean(IntentParamStrings.FROM_NOTIFIER)) {
      fromNotification = true;
    }
    String msgId = getIntent().getExtras().getString(IntentParamStrings.MESSAGE_ID);
    Account acc = getIntent().getExtras().getParcelable(IntentParamStrings.MESSAGE_ACCOUNT);
    mMessage = YakoApp.getMessageById_Account_Date(msgId, acc);
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
    mHandler = new ThreadContentGetterHandler(this, mMessage);
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
              thereWasMessageDeletion = true;
              FullSimpleMessage simpleMessage = adapter.getItem(info.position);
              
              MessageProvider mp = AndroidUtils.getMessageProviderInstanceByAccount(mMessage.getAccount(), ThreadDisplayerActivity.this);
              MessageDeleteHandler handler = new MessageDeleteHandler(ThreadDisplayerActivity.this) {
                @Override
                public void onMainListDelete(MessageListElement messageToDelete) {}

                @Override
                public void onThreadListDelete(MessageListElement messageToDelete, FullSimpleMessage simpleMessage) {
                  synchronized (YakoApp.getMessages()) {
                    ((FullThreadMessage)messageToDelete.getFullMessage()).getMessages().remove(simpleMessage);
                  }
                  for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).equals(simpleMessage)) {
                      adapter.removeItem(i);
                    }
                  }
                  adapter.notifyDataSetChanged();
                  if (adapter.getCount() == 0) {
                    synchronized (YakoApp.getMessages()) {
                      YakoApp.getMessages().remove(messageToDelete);
                    }
                    finish();
                  }
                }

                @Override
                public void onComplete() {}
              };
              MessageDeletionAsyncTask messageMarker = new MessageDeletionAsyncTask(mp, mMessage,
                      simpleMessage, simpleMessage.getId(), handler, false, false);
              messageMarker.setTimeout(10000);
              messageMarker.executeTask(null);
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
    thereWasMessageDeletion = false;
    if (mMessage == null) return;
    
    
    actViewingMessage = mMessage;
    removeNotificationIfExists();
    YakoApp.setMessageSeenAndReadLocally(mMessage);
        
    
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

    displayContent();
    
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
    resultIntent.putExtra(Settings.Intents.THERE_WAS_MESSAGE_DELETION, thereWasMessageDeletion);
    if (thereWasMessageDeletion) {
      resultIntent.putExtra(Settings.Intents.ACCOUNT, (Parcelable)mMessage.getAccount());
    }
    setResult(RESULT_OK, resultIntent);
    super.finish();
  }
  
  
  public void setMessageContent(FullThreadMessage content) {
    mMessage.setFullMessage(content);
    YakoApp.setMessageContent(mMessage, content);
  }
  
  public void dismissProgressDialog() {
    if (pd != null) {
      pd.dismiss();
    }
  }
  
  public void displayContent() {
    if (mMessage.getFullMessage() != null) {
      displayMessage(true);
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
    MessageSendHandler handler = new MessageSendHandler() {
      @Override
      public void success(String name) {
        refreshMessageList();
      }

      @Override
      public void fail(String name) {
        Toast.makeText(ThreadDisplayerActivity.this, "Failed to send message", Toast.LENGTH_LONG).show();
      }
    };
    MessageSender rs = new MessageSender(ri, mMessage.getAccount(), handler, "", mText.getText().toString(), this);
    rs.executeTask(null);
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
          if (mMessage.getFullMessage() != null) {
            FullThreadMessage threadMessage = (FullThreadMessage)mMessage.getFullMessage();
            refreshMessageList(threadMessage.getMessages().size());
            Toast.makeText(this, getString(R.string.loading_more_elements), Toast.LENGTH_LONG).show();
          }
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

  public void appendLoadedMessages(FullThreadMessage fullMessage) {
    if (mMessage.getFullMessage() == null) {
      mMessage.setFullMessage(fullMessage);
    } else {
      FullThreadMessage tm = (FullThreadMessage)mMessage.getFullMessage();
      tm.getMessages().addAll(fullMessage.getMessages());
    }
    YakoApp.setMessageContent(mMessage, mMessage.getFullMessage());
  }
  
  public void displayMessage(boolean scrollToBottom) {
    int firstVisiblePos = lv.getFirstVisiblePosition();
    int oldItemCount = 0;
    if (adapter != null) {
      oldItemCount = adapter.getCount();
    }
    if (mMessage.getFullMessage() != null) {
      FullThreadMessage threadMessage = (FullThreadMessage)mMessage.getFullMessage();
      adapter = new ThreadViewAdapter(this.getApplicationContext(), R.layout.threadview_list_item);
      for (FullSimpleMessage m : threadMessage.getMessages()) {
        adapter.add(m);
      }
      lv.setAdapter(adapter);
      if (firstLoad || scrollToBottom) {
        firstLoad = false;
        lv.setSelection(lv.getAdapter().getCount() - 1);
      } else {
        int newItemCount = adapter.getCount();
        lv.setSelection(newItemCount - oldItemCount + firstVisiblePos);
      }
    }
  }
  
  private void refreshMessageList(int offset) {
    ThreadContentGetter myThread = new ThreadContentGetter(this, mHandler, mMessage.getAccount(), true);
    if (offset > 0) {
      myThread.setOffset(offset);
    }
    myThread.executeTask(new String[]{mMessage.getId()});
  }
  
  private void refreshMessageList() {
    refreshMessageList(-1);
  }
  
  private void appendVisibleElementToStringBuilder(StringBuilder builder) {
    int firstVisiblePosition = lv.getFirstVisiblePosition();
    int lastVisiblePosition = lv.getLastVisiblePosition();

    for (int actualVisiblePosition = firstVisiblePosition; actualVisiblePosition <= lastVisiblePosition; actualVisiblePosition++) {
      builder.append((adapter.getItem(actualVisiblePosition)).getId());
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
  
  
  private class DataUpdateReceiver extends BroadcastReceiver {

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
      // TODO Auto-generated method stub
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
