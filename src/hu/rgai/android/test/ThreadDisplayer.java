package hu.rgai.android.test;


import hu.rgai.android.asynctasks.MessageSender;
import hu.rgai.android.asynctasks.ThreadContentGetter;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.intent.beens.FacebookRecipientAndr;
import hu.rgai.android.intent.beens.FullThreadMessageParc;
import hu.rgai.android.intent.beens.MessageAtomParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.intent.beens.SmsMessageRecipientAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.services.MainService;
import hu.rgai.android.services.ThreadMsgService;
import hu.rgai.android.tools.adapter.ThreadViewAdapter;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
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

public class ThreadDisplayer extends ActionBarActivity {

  private ProgressDialog pd = null;
  private Handler messageArrivedHandler = null;
  private FullThreadMessageParc content = null;
  private String threadId = "-1";
  private AccountAndr account;
  private PersonAndr from;
  private ListView lv = null;
  private EditText text = null;
  private ThreadViewAdapter adapter = null;
  private Set<String> tempMessageIds = null;
  private final Date lastLoadMoreEvent = null;
  private boolean firstLoad = true;
  private DataUpdateReceiver dur = null;
  private LogOnScrollListener los = null;
  private boolean fromNotification = false;


  public static final int MESSAGE_REPLY_REQ_CODE = 1;

  private static boolean unsopportedThreadChat = false;
  
  @Override
  public void onBackPressed() {
    Log.d("willrgai", EventLogger.LOGGER_STRINGS.THREAD.THREAD_BACKBUTTON_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + threadId);
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.THREAD.THREAD_BACKBUTTON_STR + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + threadId, true);
    super.onBackPressed();
  }

  private void logActivityEvent(String event) {
    StringBuilder builder = new StringBuilder();
    builder.append(event);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(threadId);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    appendVisibleElementToStringBuilder(builder);
    Log.d("willrgai", builder.toString());
    EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    
    tempMessageIds = new HashSet<String>();

//    MessageListElementParc mlep = (MessageListElementParc)getIntent().getExtras().getParcelable("msg_list_element_id");
    account = getIntent().getExtras().getParcelable("account");
    String mlepId = getIntent().getExtras().getString("msg_list_element_id");
    MessageListElementParc mlep = MainService.getListElementById(mlepId, account);
    MainService.setMessageSeenAndRead(mlep);
    if (mlep.isGroupMessage() && mlep.getMessageType().equals(MessageProvider.Type.FACEBOOK)) {
      unsopportedThreadChat = true;
    } else {
      unsopportedThreadChat = false;
    }

    threadId = mlep.getId();
    if (getIntent().getExtras().containsKey("from_notifier") && getIntent().getExtras().getBoolean("from_notifier")) {
      fromNotification = true;
    }
    from = mlep.getFrom();
    MainService.actViewingThreadId = threadId;
    
    getSupportActionBar().setTitle(from.getName() + " | " + account.getAccountType().toString());

    messageArrivedHandler = new NewMessageHandler(this);
    // getting content at first time
    refreshMessageList();

    setContentView(R.layout.threadview_main);
    lv = (ListView) findViewById(R.id.main);
    text = (EditText) findViewById(R.id.text);
    
    if (unsopportedThreadChat) {
      Toast.makeText(this, "Sorry, but group message sending is not possible (because of Facebook).", Toast.LENGTH_LONG).show();
      text.setVisibility(View.GONE);
      findViewById(R.id.sendButton).setVisibility(View.GONE);
    } else {
    
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
                  + MainService.actViewingThreadId + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString());
          EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.OTHER.EDITTEXT_WRITE_STR
                  + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + MainService.actViewingThreadId + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + s.toString(), true);
        }
      });
    }

    adapter = new ThreadViewAdapter(getApplicationContext(), R.layout.threadview_list_item, account);
    lv.setAdapter(adapter);
    lv.setOnScrollListener(new LogOnScrollListener());

    if (mlep.getFullMessage() != null) {
      // converting to full thread message, since we MUST use that here
      content = (FullThreadMessageParc) mlep.getFullMessage();
      displayMessage(true);
    } else {
      pd = new ProgressDialog(this);
      pd.setMessage(getString(R.string.loading));
      pd.setCancelable(true);
      pd.show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    dur = new DataUpdateReceiver(this);
    IntentFilter iFilter = new IntentFilter(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE);
    iFilter.addAction(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
    registerReceiver(dur, iFilter);

    // init connection...Facebook needs this
    // TODO: ugly code
    if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
      FacebookMessageProvider.initConnection((FacebookAccount) account, this);
    }

    logActivityEvent(EventLogger.LOGGER_STRINGS.THREAD.THREAD_RESUME_STR);
  }

  public void sendMessage(View view) {
    List<AccountAndr> accs = new LinkedList<AccountAndr>();
    accs.add(account);
    RecipientItem ri = null;
    if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
      ri = new FacebookRecipientAndr("a", from.getId(), from.getName(), null, 1);
    } else {
      ri = new SmsMessageRecipientAndr(from.getId(), from.getId(), from.getName(), null, 1);
    }
    MessageSender rs = new MessageSender(ri, accs, null, text.getText().toString(), this);
    rs.execute();
    text.setText("");
    refreshMessageList();
  }

  @Override
  protected void onPause() {
    logActivityEvent(EventLogger.LOGGER_STRINGS.THREAD.THREAD_PAUSE_STR);
    super.onPause();
    
    if (dur != null) {
      unregisterReceiver(dur);
    }
    
    MainService.actViewingThreadId = null;
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
          refreshMessageList(content.getMessagesParc().size());
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
    Intent resultIntent = new Intent();
    resultIntent.putExtra("message_data", content);
    resultIntent.putExtra("message_id", threadId);

    // if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
    resultIntent.putExtra("account", (Parcelable) account);
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
      for (MessageAtomParc ma : content.getMessagesParc()) {
        adapter.add(ma);
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
    ThreadContentGetter myThread = new ThreadContentGetter(this, messageArrivedHandler, account, 0, true);
    if (offset > 0) {
      myThread.setOffset(offset);
    }
    myThread.execute(threadId);
  }
  
  private void refreshMessageList() {
    refreshMessageList(-1);
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
        FullThreadMessageParc newMessages = bundle.getParcelable("threadMessage");
        if (content != null) {
          content.getMessagesParc().addAll(newMessages.getMessagesParc());
          if (!tempMessageIds.isEmpty()) {
            for (Iterator<MessageAtomParc> it = content.getMessagesParc().iterator(); it.hasNext();) {
              MessageAtom ma = it.next();
              if (tempMessageIds.contains(ma.getId())) {
                tempMessageIds.remove(ma.getId());
                it.remove();
              }
            }
          }
        } else {
          content = newMessages;
        }
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
                && unsopportedThreadChat) {
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
      builder.append(MainService.actViewingThreadId);
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
