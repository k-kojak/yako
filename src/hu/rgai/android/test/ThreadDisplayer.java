package hu.rgai.android.test;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import hu.rgai.android.asynctasks.MessageSender;
import hu.rgai.android.asynctasks.ThreadContentGetter;
import hu.rgai.android.tools.adapter.ThreadViewAdapter;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.intent.beens.FacebookRecipientAndr;
import hu.rgai.android.intent.beens.FullThreadMessageParc;
import hu.rgai.android.intent.beens.MessageAtomParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.SmsMessageRecipientAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.services.MainService;
import hu.rgai.android.services.ThreadMsgService;
import hu.rgai.android.services.schedulestarters.ThreadMsgScheduler;
import hu.rgai.android.tools.Utils;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullThreadMessage;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.htmlparser.jericho.Source;

public class ThreadDisplayer extends ActionBarActivity {

  private static final String THREAD_BACKBUTTON_STR = "thread:backbutton";
  private static final String THREAD_PAUSE_STR = "thread:pause";
  private static final String THREAD_RESUME_STR = "thread:resume";
  private static final String EDITTEXT_WRITE_STR = "edittext_write";
  private static final String SPACE_STR = " ";
  private static final String SCROLL_END_STR = "scroll end";
  private static final String SCROLL_START_STR = "scroll start";
  private ProgressDialog pd = null;
  private Handler messageSendHandler = null;
  private Handler messageArrivedHandler = null;
  private FullThreadMessageParc content = null;
  private String subject = null;
  private String threadId = "-1";
  private String userId = null;
  private String recipientName = null;
  private AccountAndr account;
  private PersonAndr from;
  private ListView lv = null;
  private EditText text = null;
  private ThreadViewAdapter adapter = null;
  private Set<String> tempMessageIds = null;
  private Date lastLoadMoreEvent = null;
  private boolean firstLoad = true;
  
//  private WebView webView = null;
  private String mailCharCode = "UTF-8";
  
  public static final int MESSAGE_REPLY_REQ_CODE = 1;
  
  private NewMessageReceiver nmr = null;
  
  @Override
  public void onBackPressed() {
    Log.d( "willrgai", THREAD_BACKBUTTON_STR + SPACE_STR+ threadId );
    EventLogger.INSTANCE.writeToLogFile( THREAD_BACKBUTTON_STR + SPACE_STR + threadId, true );
    super.onBackPressed();
  }
  
  private void logActivityEvent(String event) {
    StringBuilder builder = new StringBuilder();
    builder.append( event );
    builder.append( SPACE_STR );
    builder.append( threadId );
    builder.append( SPACE_STR );
    appendVisibleElementToStringBuilder(builder);
    Log.d( "willrgai", builder.toString());
    EventLogger.INSTANCE.writeToLogFile( builder.toString(), true);
  }
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    tempMessageIds = new HashSet<String>();
    MessageListElementParc mlep = (MessageListElementParc)getIntent().getExtras().getParcelable("msg_list_element");
    // register messagereceiver
    if (nmr == null) {
      nmr = new NewMessageReceiver();
    }
    IntentFilter systemIntentFilter = new IntentFilter(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
    registerReceiver(nmr, systemIntentFilter);
    
    threadId = mlep.getId();
    account = getIntent().getExtras().getParcelable("account");
    subject = mlep.getTitle();
    from = (PersonAndr)mlep.getFrom();
    Log.d("rgai", from.toString());
    MainService.actViewingThreadId = threadId;
    String accName = "";
    if(!account.getAccountType().equals(MessageProvider.Type.SMS)) {
      accName = " | " + account.getDisplayName();
    }
    getSupportActionBar().setTitle(account.getAccountType().toString() + accName);
    
    messageSendHandler = new MessageSendTaskHandler(this);
    messageArrivedHandler = new NewMessageHandler(this);
    // getting content at first time
    ThreadContentGetter myThread = new ThreadContentGetter(this, messageArrivedHandler, account, 0, true);
    myThread.execute(threadId);
    
//    bindMessageNotifier();
    setContentView(R.layout.threadview_main);
    lv = (ListView) findViewById(R.id.main);
    text = (EditText) findViewById(R.id.text);
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
        Log.d( "willrgai" , EDITTEXT_WRITE_STR + SPACE_STR + MainService.actViewingThreadId + SPACE_STR + s.toString());
        EventLogger.INSTANCE.writeToLogFile( EDITTEXT_WRITE_STR + SPACE_STR + MainService.actViewingThreadId + SPACE_STR + s.toString(), true);
      }
    });
//    webView = (WebView) findViewById(R.id.email_content);
//    webView.getSettings().setDefaultTextEncodingName(mailCharCode);
    
    
    
    adapter = new ThreadViewAdapter(getApplicationContext(), R.layout.threadview_list_item, account);
    lv.setAdapter(adapter);
    lv.setOnScrollListener( new LogOnScrollListener());
    
    if (mlep.getFullMessage() != null) {
      // converting to full thread message, since we MUST use  that here
      content = (FullThreadMessageParc)mlep.getFullMessage();
      displayMessage(true);
    } else {
      pd = new ProgressDialog(this);
      pd.setMessage("Fetching content...");
      pd.setCancelable(true);
      pd.show();
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume(); //To change body of generated methods, choose Tools | Templates.
    
    // init connection...Facebook needs this
    // TODO: ugly code
    if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
      FacebookMessageProvider.initConnection((FacebookAccount)account, this);
    }
    
    logActivityEvent( THREAD_RESUME_STR );
  }
  
  public void sendMessage(View view) {
//    List<RecipientItem> to = recipients.getRecipients();
    List<AccountAndr> accs = new LinkedList<AccountAndr>();
    accs.add(account);
    RecipientItem ri = null;
    if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
      ri = new FacebookRecipientAndr("a", from.getId(), from.getName(), null, 1);
    } else {
      ri = new SmsMessageRecipientAndr(from.getId(), from.getId(), from.getName(), null, 1);
    }
    MessageSender rs = new MessageSender(ri, accs, messageSendHandler, text.getText().toString(), this);
    rs.execute();
    
    String tempId = Utils.generateString(32);
//    content.getMessagesParc().add(new MessageAtomParc(tempId, null, text.getText().toString(), new Date(),
//            new PersonAndr(-1, "me", from.getId()), true, account.getAccountType(), null));
//    displayMessage();
    text.setText("");
//    tempMessageIds.add(tempId);
    ThreadContentGetter myThread = new ThreadContentGetter(this, messageArrivedHandler, account, 2000, true);
    myThread.execute(threadId);
//    }
  }

  @Override
  protected void onPause() {
    logActivityEvent( THREAD_PAUSE_STR );
    super.onPause(); //To change body of generated methods, choose Tools | Templates.
    Log.d("rgai", "ThreadDisplayer onPause");
    MainService.actViewingThreadId = null;
    // init connection...Facebook needs this
    // TODO: ugly code
    if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
//      FacebookMessageProvider.closeConnection();
    }
    
    if (nmr != null) {
      try {
      unregisterReceiver(nmr);
      } catch (IllegalArgumentException ex) {
        ex.printStackTrace();
    }
  }
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
          ThreadContentGetter myThread = new ThreadContentGetter(this, messageArrivedHandler, account, 0, false);
          myThread.setOffset(content.getMessagesParc().size());
          myThread.execute(threadId);
          
          Toast.makeText(this, getString(R.string.loading_more_elements), Toast.LENGTH_LONG).show();
        } else {
          Log.d("rgai", "@@@skipping load button press for 5 sec");
        }
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  private String messageThreadToString(FullThreadMessage ftm) {
    StringBuilder sb = new StringBuilder();
    if (ftm != null && ftm.getMessages() != null) {
      for (MessageAtom ma : ftm.getMessages()) {
        sb.append(ma.getContent()).append("<hr/>");
      }
    }
    return sb.toString();
  }

  @Override
  public void finish() {
    Intent resultIntent = new Intent();
    resultIntent.putExtra("message_data", content);
    resultIntent.putExtra("message_id", threadId);

//      if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
      resultIntent.putExtra("account", (Parcelable)account);
//      } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
//        resultIntent.putExtra("account", new GmailAccountParc((GmailAccount)account));
//      } else if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
//        resultIntent.putExtra("account", new FacebookAccountParc((FacebookAccount)account));
//      }
    setResult(Activity.RESULT_OK, resultIntent);
    super.finish(); //To change body of generated methods, choose Tools | Templates.
  }
  
  private void displayMessage(boolean scrollToBottom) {
//    Log.d("rgai", "DISPLAYING MESSAGE CONTENT");
//    String c = "";
//    String mail = from.getEmails().isEmpty() ? "" : " ("+ from.getEmails().get(0) +")";
//    c = from.getName() + mail + "<br/>" + messageThreadToString(content);
//    webView.loadDataWithBaseURL(null, c, "text/html", mailCharCode, null);
    int firstVisiblePos = lv.getFirstVisiblePosition();
    int oldItemCount = 0;
//    lv.get
    if (adapter != null) {
      oldItemCount = adapter.getCount();
    }
    if (content != null) {
      adapter = new ThreadViewAdapter(getApplicationContext(), R.layout.threadview_list_item, account);
      for (MessageAtomParc ma : content.getMessagesParc()) {
        adapter.add(ma);
      }
      lv.setAdapter(adapter);
      lv.setOnScrollListener( new LogOnScrollListener());
      if (firstLoad || scrollToBottom) {
        firstLoad = false;
//        lv.setSelection(lv.getAdapter().getCount() - 1);
//        firstLoad = false;
//      } else {
        
//      }
//      if (scrollToBottom) {
      lv.setSelection(lv.getAdapter().getCount() - 1);
      } else {
        int newItemCount = adapter.getCount();
        lv.setSelection(newItemCount - oldItemCount + firstVisiblePos);
    }
  }
  }
  
  private class NewMessageHandler extends Handler {
    
    private Context context;
//    
    public NewMessageHandler(Context context) {
      this.context = context;
    }
    
    @Override
    public void handleMessage(Message msg) {
//      Log.d("rgai", "message arrived");
      
      Bundle bundle = msg.getData();
      boolean scrollToBottom = bundle.getBoolean("scroll_to_bottom");
      if (bundle.getInt("result") != ThreadMsgService.OK) {
        String resMsg = "Error";
        Toast.makeText(context, resMsg, Toast.LENGTH_LONG).show();
        if (pd != null) {
          pd.dismiss();
        }
      } else {
//        Log.d("rgai", "HANDLING MESSAGE CONTENT");
        FullThreadMessageParc newMessages = bundle.getParcelable("threadMessage");
        if (content != null) {
          content.getMessagesParc().addAll(newMessages.getMessagesParc());
          if (!tempMessageIds.isEmpty()) {
            for (Iterator<MessageAtomParc> it = content.getMessagesParc().iterator(); it.hasNext(); ) {
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
      
//      Bundle bundle = msg.getData();
//      if (bundle != null) {
//        if (bundle.get("result") != null) {
//          
//          Intent intent = new Intent(Settings.Intents.THREAD_SERVICE_INTENT);
//          intent.putExtra("result", bundle.getInt("result"));
//          intent.putExtra("threadMessage", bundle.getParcelable("threadMessage"));
//          
//          sendBroadcast(intent);
//        }
//      }
    }
  }
  
  private class MessageSendTaskHandler extends Handler {
    
    ThreadDisplayer cont;
    
    public MessageSendTaskHandler(ThreadDisplayer cont) {
      this.cont = cont;
    }
    
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.containsKey("success") && bundle.get("success") != null) {
          boolean succ = bundle.getBoolean("success");
          if (succ) {
//            cont.text.setText("");
//            Intent intent = new Intent(ThreadDisplayer.this, ThreadMsgScheduler.class);
//            intent.setAction(Settings.Alarms.THREAD_MSG_ALARM_START);
//            ThreadDisplayer.this.sendBroadcast(intent);
          }
        }
      }
    }
  }
  
  private class NewMessageReceiver extends BroadcastReceiver {

    public NewMessageReceiver(){};
    
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() != null && intent.getAction().equals(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST)) {
        Log.d("rgai", "NEW MESSAGE BROADCAST");
        ThreadContentGetter myThread = new ThreadContentGetter(ThreadDisplayer.this, messageArrivedHandler, account, 0, true);
        myThread.execute(threadId);
      }
    }
  }
  
//  private class DataUpdateReceiver extends BroadcastReceiver {
//
//    private ThreadDisplayer activity;
//    
//    public DataUpdateReceiver(ThreadDisplayer activity) {
//      this.activity = activity;
//          }
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//      if (intent.getAction().equals(Settings.Intents.THREAD_SERVICE_INTENT)) {
//        if (intent.getExtras().getInt("result") != ThreadMsgService.OK) {
//          String msg = "Error";
//          Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
//          if (pd != null) {
//            pd.dismiss();
//          }
//        } else {
//          FullThreadMessageParc newMessages = intent.getExtras().getParcelable("threadMessage");
//          if (content != null) {
//            content.getMessagesParc().addAll(newMessages.getMessagesParc());
//            if (!tempMessageIds.isEmpty()) {
//              for (Iterator<MessageAtomParc> it = content.getMessagesParc().iterator(); it.hasNext(); ) {
//                MessageAtom ma = it.next();
//                if (tempMessageIds.contains(ma.getId())) {
//                  tempMessageIds.remove(ma.getId());
//                  it.remove();
//                }
//              }
//            }
//          } else {
//            content = newMessages;
//          }
////          content = intent.getExtras().getParcelable("threadMessage");
//          displayMessage(true);
////          Parcelable[] messagesParc = intent.getExtras().getParcelableArray("messages");
////          MessageListElementParc[] messages = new MessageListElementParc[messagesParc.length];
////          for (int i = 0; i < messagesParc.length; i++) {
////            messages[i] = (MessageListElementParc) messagesParc[i];
////          }
////
////          updateList(messages);
//          if (pd != null) {
//            pd.dismiss();
//          }
//        }
//      }
//    }
//      
////    private FullThreadMessageParc mergeMessages(FullThreadMessageParc newMessages) {
////      
////    }
//    }
  }
  
  private void appendVisibleElementToStringBuilder(StringBuilder builder) {
    int firstVisiblePosition = lv.getFirstVisiblePosition();
    int lastVisiblePosition = lv.getLastVisiblePosition();
    
    for ( int actualVisiblePosition = firstVisiblePosition; actualVisiblePosition <= lastVisiblePosition; actualVisiblePosition++ ) {
      builder.append( ((MessageAtomParc)(adapter.getItem(actualVisiblePosition))).getId() );
      builder.append( SPACE_STR );
    }
  }
  
  class LogOnScrollListener implements OnScrollListener{
    
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
        int visibleItemCount, int totalItemCount) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
      // TODO Auto-generated method stub
      StringBuilder builder = new StringBuilder();

      builder.append(account.getAccountType().name() );
      builder.append( SPACE_STR);
      builder.append( MainService.actViewingThreadId );
      builder.append( SPACE_STR);
      if ( scrollState == 1) {
        builder.append( SCROLL_START_STR );
        builder.append( SPACE_STR);
      } else {
        builder.append( SCROLL_END_STR );
        builder.append( SPACE_STR);
      }
      appendVisibleElementToStringBuilder( builder );
      Log.d( "willrgai", builder.toString());
      EventLogger.INSTANCE.writeToLogFile( builder.toString(), true);
    }

  }
}
