package hu.rgai.android.test;


import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.FacebookMessageRecipient;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.FullThreadMessage;
import hu.rgai.android.beens.MainServiceExtraParams.ParamStrings;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.MessageRecipient;
import hu.rgai.android.beens.Person;
import hu.rgai.android.beens.SmsMessageRecipient;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.services.MainService;
import hu.rgai.android.services.ThreadMsgService;
import hu.rgai.android.tools.AndroidUtils;
import hu.rgai.android.tools.adapter.ThreadViewAdapter;
import hu.rgai.android.workers.MessageSender;
import hu.rgai.android.workers.ThreadContentGetter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ThreadDisplayer extends ActionBarActivity {

  private static ProgressDialog pd = null;
  private Handler messageArrivedHandler = null;
  private FullThreadMessage content = null;
//  private String threadId = "-1";
  private Account account;
  private Person from;
  private ListView lv = null;
  private EditText text = null;
  private ThreadViewAdapter adapter = null;
  private Set<String> tempMessageIds = null;
  private final Date lastLoadMoreEvent = null;
  private boolean firstLoad = true;
  private DataUpdateReceiver dur = null;
  private LogOnScrollListener los = null;
  private boolean fromNotification = false;
  private MessageListElement mle = null;
  private String mThreadId = null;
  private TextWatcher mTextWatcher = null;
  private boolean mTextWatcherAdded = false;

  private static boolean firstResume = true;

  public static final int MESSAGE_REPLY_REQ_CODE = 1;

  private boolean unsopportedGroupChat = false;
  
  @Override
  public void onBackPressed() {
    Log.d("willrgai", EventLogger.LOGGER_STRINGS.THREAD.THREAD_BACKBUTTON_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + mThreadId);
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.THREAD.THREAD_BACKBUTTON_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + mThreadId, true);
    super.onBackPressed();
  }

  private void logActivityEvent(String event) {
    StringBuilder builder = new StringBuilder();
    builder.append(event);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(mThreadId);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    appendVisibleElementToStringBuilder(builder);
    Log.d("willrgai", builder.toString());
    EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    Tracker t = ((AnalyticsApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());
    
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    
    tempMessageIds = new HashSet<String>();

    account = getIntent().getExtras().getParcelable("account");
    mThreadId = getIntent().getExtras().getString("msg_list_element_id");
    
    if (getIntent().getExtras().containsKey(ParamStrings.FROM_NOTIFIER)
            && getIntent().getExtras().getBoolean(ParamStrings.FROM_NOTIFIER)) {
      fromNotification = true;
    }
    
    setContentView(R.layout.threadview_main);
    lv = (ListView) findViewById(R.id.main);
    text = (EditText) findViewById(R.id.text);
    
    messageArrivedHandler = new NewMessageHandler(this);
    adapter = new ThreadViewAdapter(getApplicationContext(), R.layout.threadview_list_item, account);
    lv.setAdapter(adapter);
    lv.setOnScrollListener(new LogOnScrollListener());
    
    mTextWatcher = new TextWatcher() {

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
        if (MainService.actViewingMessage != null) {
          Log.d("willrgai", EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR
                  + MainService.actViewingMessage.getId() + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString());
          EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR
                  + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + MainService.actViewingMessage.getId() + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString(), true);
        }
      }
    };

  }
  
  private void displayContent(MessageListElement mlep) {
    if (mlep.getFullMessage() != null) {
      // converting to full thread message, since we MUST use that here
      content = (FullThreadMessage) mlep.getFullMessage();
      displayMessage(true);
    } else {
      pd = new ProgressDialog(this);
      pd.setMessage(getString(R.string.loading));
      pd.setCancelable(true);
      pd.show();
      refreshMessageList();
    }
  }

  
  private void removeNotificationIfExists() {
    if (mle.equals(MainService.mLastNotifiedMessage)) {
      NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      mNotificationManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
      MainService.mLastNotifiedMessage = null;
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    mle = MainService.getListElementById(mThreadId, account);
//    Log.d("rgai", "the found message list element: " + mle);
    removeNotificationIfExists();
    MainService.setMessageSeenAndRead(mle);
    if (mle.isGroupMessage() && mle.getMessageType().equals(MessageProvider.Type.FACEBOOK)) {
      unsopportedGroupChat = true;
    } else {
      unsopportedGroupChat = false;
    }

    if (mTextWatcherAdded) {
      text.removeTextChangedListener(mTextWatcher);
    }
    if (unsopportedGroupChat) {
      if (firstResume) {
        Toast.makeText(this, "Sorry, but group message sending is not available (because of Facebook).", Toast.LENGTH_LONG).show();
        firstResume = false;
      }
      text.setVisibility(View.GONE);
      findViewById(R.id.sendButton).setVisibility(View.GONE);
      mTextWatcherAdded = false;
    } else {
      text.addTextChangedListener(mTextWatcher);
      mTextWatcherAdded = true;
    }
      
    from = mle.getFrom();
    
    getSupportActionBar().setTitle((from != null ? from.getName() : "") + " | " + account.getAccountType().toString());

    
    
    
    MainService.actViewingMessage = mle;
//    Log.d("rgai", "MainService.actViewingMessage = " + mle);
    
    dur = new DataUpdateReceiver(this);
    IntentFilter iFilter = new IntentFilter(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE);
    iFilter.addAction(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
    registerReceiver(dur, iFilter);

    // init connection...Facebook needs this
    // TODO: ugly code
    if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
//      FacebookMessageProvider.initConnection((FacebookAccount) account, this);
    }

    logActivityEvent(EventLogger.LOGGER_STRINGS.THREAD.THREAD_RESUME_STR);
    displayContent(mle);
  }

  public void sendMessage(View view) {
    String t = text.getText().toString().trim();
    if (t.length() == 0) {
      Toast.makeText(this, "Empty message", Toast.LENGTH_SHORT).show();
      return;
    }
    
    List<Account> accs = new LinkedList<Account>();
    accs.add(account);
    MessageRecipient ri = null;
    if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
      ri = new FacebookMessageRecipient("", from.getId(), from.getName(), null, 1);
    } else {
      ri = new SmsMessageRecipient(from.getId(), from.getId(), from.getName(), null, 1);
    }
    MessageSender rs = new MessageSender(ri, accs, new MessageSendHandler(this), "", text.getText().toString(), this, (AnalyticsApp)getApplication(), new Handler());
    AndroidUtils.<Integer, String, Boolean>startAsyncTask(rs);
    text.setText("");
  }

  @Override
  protected void onPause() {
    Tracker t = ((AnalyticsApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName() + " - pause");
    t.send(new HitBuilders.AppViewBuilder().build());
    logActivityEvent(EventLogger.LOGGER_STRINGS.THREAD.THREAD_PAUSE_STR);
    super.onPause();
    
    if (dur != null) {
      unregisterReceiver(dur);
    }
    
    MainService.actViewingMessage = null;
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
        if (resultCode == MessageReply.MESSAGE_SENT_OK) {
          Toast.makeText(this, "Message sent", Toast.LENGTH_LONG).show();
        } else if (resultCode == MessageReply.MESSAGE_SENT_FAILED) {
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
          refreshMessageList(content.getMessages().size());
          Toast.makeText(this, getString(R.string.loading_more_elements), Toast.LENGTH_LONG).show();
        } else {
          Log.d("rgai", "@@@skipping load button press for 5 sec");
        }
        return true;
      case android.R.id.home:
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        if (fromNotification) {
          Log.d("rgai", "NEW STACKBUILDER");
            TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
        } else {
          Log.d("rgai", "NO STACK BUILDER");
//          NavUtils.navigateUpTo(this, upIntent);
          finish();
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void finish() {
    Log.d("rgai", "TD-finish");
    Intent resultIntent = new Intent();
    resultIntent.putExtra("thread_displayer", true);
    resultIntent.putExtra("account_type", account.getAccountType().toString());
    resultIntent.putExtra("act_view_msg", (Parcelable)MainService.actViewingMessage);
    
//    resultIntent.putExtra("message_id", threadId);

    // if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
//    resultIntent.putExtra("account", (Parcelable) account);
    setResult(Activity.RESULT_OK, resultIntent);
    super.finish(); 
  }

  private void displayMessage(boolean scrollToBottom) {
    int firstVisiblePos = lv.getFirstVisiblePosition();
    int oldItemCount = 0;
    // lv.get
    if (adapter != null) {
      oldItemCount = adapter.getCount();
    }
    if (content != null) {
      adapter = new ThreadViewAdapter(this.getApplicationContext(), R.layout.threadview_list_item, account);
      for (FullSimpleMessage m : content.getMessages()) {
        adapter.add(m);
//        Log.d("rgai", "adding fullSimpleMessage to adapter: " + m);
      }
      lv.setAdapter(adapter);
      los = new LogOnScrollListener();
      lv.setOnScrollListener(los);
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
//    Log.d("rgai", "LEKERDEZES");
    ThreadContentGetter myThread = new ThreadContentGetter(this, messageArrivedHandler, account,
            0, true, (AnalyticsApp)getApplication(), new Handler());
    if (offset > 0) {
      myThread.setOffset(offset);
    }
    myThread.execute(mThreadId);
  }
  
  private void refreshMessageList() {
    refreshMessageList(-1);
  }
  
  private class MessageSendHandler extends Handler {
    
    private final Context context;
    
    public MessageSendHandler(Context context) {
      this.context = context;
    }
    
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.containsKey("success") && bundle.get("success") != null) {
          refreshMessageList();
        }
      }
    }
    
  }

  private class NewMessageHandler extends Handler {

    private final Context context;

    //
    public NewMessageHandler(Context context) {
      this.context = context;
    }

    @Override
    public void handleMessage(Message msg) {
      // Log.d("rgai", "message arrived");

      Bundle bundle = msg.getData();
      boolean scrollToBottom = bundle.getBoolean("scroll_to_bottom");
      if (bundle.getInt("result") != ThreadMsgService.OK) {
        String resMsg = "Error";
        Toast.makeText(context, resMsg, Toast.LENGTH_LONG).show();
        if (pd != null) {
          pd.dismiss();
        }
      } else {
        // Log.d("rgai", "HANDLING MESSAGE CONTENT");
        FullThreadMessage newMessages = bundle.getParcelable("threadMessage");
        if (content != null) {
          content.getMessages().addAll(newMessages.getMessages());
          if (!tempMessageIds.isEmpty()) {
            for (Iterator<FullSimpleMessage> it = content.getMessages().iterator(); it.hasNext();) {
              FullSimpleMessage ma = it.next();
              if (tempMessageIds.contains(ma.getId())) {
                tempMessageIds.remove(ma.getId());
                it.remove();
              }
            }
          }
        } else {
          content = newMessages;
        }
        MainService.setMessageContent(mThreadId, account, content);
        displayMessage(scrollToBottom);
        if (pd != null) {
          pd.dismiss();
        }
      }

    }
  }
  
  private void appendVisibleElementToStringBuilder(StringBuilder builder) {
      int firstVisiblePosition = lv.getFirstVisiblePosition();
      int lastVisiblePosition = lv.getLastVisiblePosition();

      for (int actualVisiblePosition = firstVisiblePosition; actualVisiblePosition <= lastVisiblePosition; actualVisiblePosition++) {
        builder.append((adapter.getItem(actualVisiblePosition)).getId());
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      }
    }
  
  private class DataUpdateReceiver extends BroadcastReceiver {

    private ThreadDisplayer activity;

    public DataUpdateReceiver(ThreadDisplayer activity) {
      this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() != null) {
        if (intent.getAction().equals(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE)
                && unsopportedGroupChat) {
          activity.refreshMessageList();
        } else if (intent.getAction().equals(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST)) {
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

      builder.append(account.getAccountType().name());
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      builder.append(MainService.actViewingMessage.getId());
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
