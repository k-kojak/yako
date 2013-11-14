package hu.rgai.android.test;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;
import hu.rgai.android.tools.adapter.ThreadViewAdapter;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.FullThreadMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.services.ThreadMsgService;
import hu.rgai.android.services.schedulestarters.ThreadMsgScheduler;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullThreadMessage;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;
import net.htmlparser.jericho.Source;

public class ThreadDisplayer extends Activity {

  private ProgressDialog pd = null;
  private Handler handler = null;
  private FullThreadMessageParc content = null;
  private String subject = null;
  private boolean loadedWithContent = false;
  private String threadId = "-1";
  private AccountAndr account;
  private PersonAndr from;
  private ListView lv = null;
  private ThreadViewAdapter adapter = null;
  
//  private WebView webView = null;
  private String mailCharCode = "UTF-8";
  
  public static final int MESSAGE_REPLY_REQ_CODE = 1;
  
  private DataUpdateReceiver serviceReceiver;
  private ThreadMsgService service;
  private boolean serviceConnectionEstablished = false;
  private ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder binder) {
      Log.d("rgai", "# ON ServiceConnected callback");
      service = ((ThreadMsgService.MyBinder) binder).getService();
      service.setAccount(account);
      service.setThreadId(threadId);

//      updateList(service.getEmails());
//      if ((messages == null || !messages.isEmpty()) && pd != null) {
//        pd.dismiss();
//      }
      serviceConnectionEstablished = true;
    }

    public void onServiceDisconnected(ComponentName className) {
      service = null;
    }
  };
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    MessageListElementParc mlep = (MessageListElementParc)getIntent().getExtras().getParcelable("msg_list_element");
    
    threadId = mlep.getId();
    account = getIntent().getExtras().getParcelable("account");
    subject = mlep.getTitle();
    from = new PersonAndr(mlep.getFrom());
    
//    Intent serviceIntent = new Intent(this, ThreadMsgService.class);
//    serviceIntent.putExtra("account", (Parcelable)account);
//    serviceIntent.putExtra("threadId", threadId);
//    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
//    
//    Intent intent = new Intent(this, ThreadMsgScheduler.class);
//    intent.setAction(Settings.Alarms.THREAD_MSG_ALARM_START);
//    this.sendBroadcast(intent);
    
    setContentView(R.layout.threadview_main);
    lv = (ListView) findViewById(R.id.main);
    
//    webView = (WebView) findViewById(R.id.email_content);
//    webView.getSettings().setDefaultTextEncodingName(mailCharCode);
    
    
    
    adapter = new ThreadViewAdapter(getApplicationContext(), R.layout.threadview_list_item, account);
    lv.setAdapter(adapter);
    
    if (mlep.getFullMessage() != null) {
      // converting to full thread message, since we MUST use  that here
//      content = (FullThreadMessageParc)mlep.getFullMessage();
      loadedWithContent = true;
//      content = getIntent().getExtras().getString("email_content");
//      webView.loadData(content, "text/html", mailCharCode);
//      webView.loadDataWithBaseURL(null, content, "text/html", mailCharCode, null);
//      displayMessage(content);
    } else {
//      handler = new ThreadContentTaskHandler();
//      ThreadContentGetter contentGetter = new ThreadContentGetter(handler, account);
//      contentGetter.execute(threadId);
//
      pd = new ProgressDialog(this);
      pd.setMessage("Fetching content...");
      pd.setCancelable(false);
      pd.show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume(); //To change body of generated methods, choose Tools | Templates.
    
    Intent serviceIntent = new Intent(this, ThreadMsgService.class);
    serviceIntent.putExtra("account", (Parcelable)account);
    serviceIntent.putExtra("threadId", threadId);
    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    
    Intent intent = new Intent(this, ThreadMsgScheduler.class);
    intent.setAction(Settings.Alarms.THREAD_MSG_ALARM_START);
    this.sendBroadcast(intent);
    
    if (serviceReceiver == null) {
      serviceReceiver = new DataUpdateReceiver(this);
    }
    IntentFilter intentFilter = new IntentFilter(Settings.Intents.THREAD_SERVICE_INTENT);
    registerReceiver(serviceReceiver, intentFilter);
  }

  @Override
  protected void onPause() {
    super.onPause(); //To change body of generated methods, choose Tools | Templates.
    Log.d("rgai", "ThreadDisplayer onPause");
    if (serviceReceiver != null) {
      unregisterReceiver(serviceReceiver);
    }
    if (serviceConnection != null) {
      unbindService(serviceConnection);
    }
    
    Intent intent = new Intent(this, ThreadMsgScheduler.class);
    intent.setAction(Settings.Alarms.THREAD_MSG_ALARM_STOP);
    this.sendBroadcast(intent);
    
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.message_options_menu, menu);
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
      case R.id.message_reply:
        Intent intent = new Intent(this, MessageReply.class);
        Source source = new Source(messageThreadToString(content));
        intent.putExtra("content", source.getRenderer().toString());
        intent.putExtra("subject", subject);
        intent.putExtra("account", (Parcelable)account);
        intent.putExtra("from", from);
        startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
        return true;
//        EmailReplySender replySender = new EmailReplySender();
//        replySender.execute();
//        return true;
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
    if (!loadedWithContent) {
      Intent resultIntent = new Intent();
      resultIntent.putExtra("email_content", content);
      resultIntent.putExtra("email_id", threadId);
      
//      if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
        resultIntent.putExtra("account", (Parcelable)account);
//      } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
//        resultIntent.putExtra("account", new GmailAccountParc((GmailAccount)account));
//      } else if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
//        resultIntent.putExtra("account", new FacebookAccountParc((FacebookAccount)account));
//      }
      setResult(Activity.RESULT_OK, resultIntent);
    }
    super.finish(); //To change body of generated methods, choose Tools | Templates.
  }
  
  private void displayMessage(FullThreadMessageParc content) {
//    String c = "";
//    String mail = from.getEmails().isEmpty() ? "" : " ("+ from.getEmails().get(0) +")";
//    c = from.getName() + mail + "<br/>" + messageThreadToString(content);
//    webView.loadDataWithBaseURL(null, c, "text/html", mailCharCode, null);
    if (content != null) {
      adapter = new ThreadViewAdapter(getApplicationContext(), R.layout.threadview_list_item, account);
      for (MessageAtom ma : content.getMessages()) {
        adapter.add(ma);
      }
      lv.setAdapter(adapter);
      lv.setSelection(lv.getAdapter().getCount() - 1);
    }
  }
  
  private class DataUpdateReceiver extends BroadcastReceiver {

    private ThreadDisplayer activity;
    
    public DataUpdateReceiver(ThreadDisplayer activity) {
      this.activity = activity;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(Settings.Intents.THREAD_SERVICE_INTENT)) {
        if (intent.getExtras().getInt("result") != ThreadMsgService.OK) {
          String msg = "Error";
          Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
          if (pd != null) {
            pd.dismiss();
          }
        } else {
          content = intent.getExtras().getParcelable("threadMessage");
          displayMessage(content);
//          Parcelable[] messagesParc = intent.getExtras().getParcelableArray("messages");
//          MessageListElementParc[] messages = new MessageListElementParc[messagesParc.length];
//          for (int i = 0; i < messagesParc.length; i++) {
//            messages[i] = (MessageListElementParc) messagesParc[i];
//          }
//
//          updateList(messages);
          if (pd != null) {
            pd.dismiss();
          }
        }
      }
    }
  }
  
  private class ThreadContentTaskHandler extends Handler {
    
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.get("threadMessage") != null) {
          content = bundle.getParcelable("threadMessage");
//          webView.loadData(content, "text/html", mailCharCode);
//          webView.loadDataWithBaseURL(null, content, "text/html", mailCharCode, null);
          displayMessage(content);
          if (pd != null) {
            pd.dismiss();
          }
        }
      }
    }
  }
  
 /* private class ThreadContentGetter extends AsyncTask<String, Integer, FullThreadMessageParc> {

    Handler handler;
    AccountAndr account;
    
    public ThreadContentGetter(Handler handler, AccountAndr account) {
      this.handler = handler;
      this.account = account;
    }
    
    @Override
    protected FullThreadMessageParc doInBackground(String... params) {
//      SharedPreferences sharedPref = getSharedPreferences(getString(R.string.settings_email_file_key), Context.MODE_PRIVATE);
//      String email = sharedPref.getString(getString(R.string.settings_saved_email), "");
//      String pass = sharedPref.getString(getString(R.string.settings_saved_pass), "");
//      String imap = sharedPref.getString(getString(R.string.settings_saved_imap), "");
//      MailProvider2 em = new MailProvider2(email, pass, imap, Pass.smtp);
      FullThreadMessageParc threadMessage = null;
      
      try {
        Class providerClass = Settings.getAccountTypeToMessageProvider().get(account.getAccountType());
        Class accountClass = Settings.getAccountTypeToAccountClass().get(account.getAccountType());
        Constructor constructor = null;
        
        if (providerClass == null) {
          throw new RuntimeException("Provider class is null, " + account.getAccountType() + " is not a valid TYPE.");
        }
        constructor = providerClass.getConstructor(accountClass);
        MessageProvider mp = (MessageProvider) constructor.newInstance(account);
        // force result to ThreadMessage, since this is a thread displayer
        threadMessage = new FullThreadMessageParc((FullThreadMessage)mp.getMessage(threadId));

      // TODO: handle exceptions
      } catch (NoSuchMethodException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (InstantiationException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IllegalAccessException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IllegalArgumentException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (InvocationTargetException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (NoSuchProviderException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (MessagingException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      }
//      try {
//        content = em.getMailContent2(params[0]);
//      } catch (IOException ex) {
//        Logger.getLogger(MyService.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (MessagingException ex) {
//        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
//      }
//
      return threadMessage;
//      return "";
    }

    @Override
    protected void onPostExecute(FullThreadMessageParc result) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putParcelable("threadMessage", result);
      msg.setData(bundle);
      handler.sendMessage(msg);
    }


//    @Override
//    protected void onProgressUpdate(Integer... values) {
//      Log.d(Constants.LOG, "onProgressUpdate");
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//
//      bundle.putInt("progress", values[0]);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
//    }
  }*/
  
}
