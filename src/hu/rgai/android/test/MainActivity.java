package hu.rgai.android.test;

import hu.rgai.android.test.settings.AccountSettings;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.store.StoreHandler;
import static hu.rgai.android.test.EmailDisplayer.MESSAGE_REPLY_REQ_CODE;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullEmailMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.htmlparser.jericho.Source;

public class MainActivity extends Activity {

//  private Boolean isInternetAvailable = null;
  
  
  public static final int EMAIL_CONTENT_RESULT = 1;
  public static final int EMAIL_SETTINGS_RESULT = 2;
  
  public static final int PICK_CONTACT = 101;
  
  private boolean serviceConnectionEstablished = false;
  private List<Map<String, Object>> messages;
//  private ArrayAdapter<String> adapter;
  private LazyAdapter adapter;
  private MyService s;
  private DataUpdateReceiver serviceReceiver;
  private SystemBroadcastReceiver systemReceiver;
  private ProgressDialog pd = null;
  private ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder binder) {
      s = ((MyService.MyBinder) binder).getService();

      updateList(s.getEmails());
      if ((messages == null || !messages.isEmpty()) && pd != null) {
        pd.dismiss();
      }
      serviceConnectionEstablished = true;
    }

    public void onServiceDisconnected(ComponentName className) {
      s = null;
    }
  };

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
//    setContentView(R.layout.main);
    bindService(new Intent(this, MyService.class), serviceConnection, Context.BIND_AUTO_CREATE);

//    Log.d("rgai", "myService.running -> " + MyService.RUNNING);
//    emails = new ArrayList<Map<String, String>>();
    if (!MyService.RUNNING) {
      Intent intent = new Intent(this, ScheduleStarter.class);
      intent.setAction(Context.ALARM_SERVICE);
      this.sendBroadcast(intent);
      // disaplying loading dialog, since the mails are not ready, but the user opened the list
      pd = new ProgressDialog(this);
      pd.setMessage("Fetching emails...");
      pd.setCancelable(false);
      pd.show();
    }
//    setContent();
//    setListAdapter(adapter);
//    set

  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.email_settings_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    Intent intent;
    switch (item.getItemId()) {
      case R.id.email_accounts:
        intent = new Intent(this, AccountSettings.class);
        startActivityForResult(intent, MainActivity.EMAIL_SETTINGS_RESULT);
        return true;
      case R.id.email_send_new:
        intent = new Intent(this, MessageReply.class);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case (EMAIL_CONTENT_RESULT):
        if (resultCode == Activity.RESULT_OK) {
          // TODO: only saving simple string content
          int emailID = data.getIntExtra("email_id", 0);
          AccountAndr acc = data.getParcelableExtra("account");
          String content = data.getStringExtra("email_content");
          s.setMessageContent(emailID, acc, content);
        }
        break;
      case (EMAIL_SETTINGS_RESULT):
        if (resultCode == Activity.RESULT_OK) {
          Log.d("rgai", "email setting result");
          Intent intent = new Intent(this, ScheduleStarter.class);
          intent.setAction(Context.ALARM_SERVICE);
          this.sendBroadcast(intent);
          
//          pd = new ProgressDialog(this);
//          pd.setMessage("Fetching emails...");
//          pd.setCancelable(false);
//          pd.show();
        }
        break;
      case (PICK_CONTACT):
        if (resultCode == Activity.RESULT_OK) {
          Uri contactData = data.getData();
          Cursor c =  getContentResolver().query(contactData, null, null, null, null);
          if (c.moveToFirst()) {
            String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            AccountAndr account = StoreHandler.getAccounts(this).get(0);
            Intent intent = new Intent(this, MessageReply.class);
//            Source source = new Source("");
            intent.putExtra("content", "");
            intent.putExtra("subject", "");
            intent.putExtra("account", (Parcelable)account);
            intent.putExtra("from", new PersonAndr(1, name, name));
            startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
          }
        }
        
        break;
      default:
        break;
    }
  }

  private void updateList(MessageListElementParc[] newMessages) {
    Log.d("rgai", "updating list...");
    if (messages != null) {
      
      messages.clear();
      if (newMessages != null) {
        for (int i = 0; i < newMessages.length; i++) {
          HashMap<String, Object> item = new HashMap<String, Object>();
          item.put("subject", newMessages[i].getTitle());
          // using only a temporary solution
          item.put("from", newMessages[i].getFrom().getName());
          item.put("date", "" + newMessages[i].getFormattedDate());
          item.put("seen", "" + newMessages[i].isSeen());
          item.put("id", "" + newMessages[i].getId());
          item.put("account", newMessages[i].getAccount());
          messages.add(item);
        }
        adapter.notifyDataSetChanged();
      }
    }
  }
  
  public void setEmailSeen(int id) {
    for (Map<String, Object> entry : messages) {
      if(entry.get("id").equals(""+id)) {
        entry.put("seen", "true");
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // register service broadcast receiver
    if (serviceReceiver == null) {
      serviceReceiver = new DataUpdateReceiver(this);
    }
    IntentFilter intentFilter = new IntentFilter(Constants.MAIL_SERVICE_INTENT);
    registerReceiver(serviceReceiver, intentFilter);
    
    // register system broadcast receiver for internet connection state change
    if (systemReceiver == null) {
      systemReceiver = new SystemBroadcastReceiver(this);
    }
    IntentFilter systemIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    registerReceiver(systemReceiver, systemIntentFilter);
    
    // setting content
    setContent();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (systemReceiver != null) {
      unregisterReceiver(systemReceiver);
    }
    if (serviceConnection != null) {
      unbindService(serviceConnection);
    }
  }
  
  private void setContent() {
    // TODO: itt is kell ellenorizni, hogy van-e jelszo, mer ha nincs akkor nem lehet csinalni semmit...
    boolean isNet = isNetworkAvailable();
//    if (isNet == falseInternetAvailable == null || isInternetAvailable != isNet) {
//      isInternetAvailable = isNetworkAvailable();
      if (isNet) {
        View currentView = this.findViewById(R.id.list);
        if (currentView == null || currentView.getId() != R.id.list) {
          setContentView(R.layout.main);
          messages = new ArrayList<Map<String, Object>>();

          ListView lv = (ListView) findViewById(R.id.list);
  //        String[] from = {"subject", "from"};
  //        int[] to = {android.R.id.text1, android.R.id.text2};
  //        adapter = new SimpleAdapter(this, emails, android.R.layout.simple_list_item_2, from, to);
          adapter = new LazyAdapter(this, messages);
          lv.setAdapter(adapter);
          lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View arg1, int itemIndex, long arg3) {

              Map<String, Object> email = (Map) av.getItemAtPosition(itemIndex);

              int emailID = Integer.parseInt((String)email.get("id"));
              AccountAndr a = (AccountAndr)email.get("account");
              MessageListElementParc ele = s.getListElementById(emailID, a);
              Intent i = new Intent(MainActivity.this, EmailDisplayer.class);
              
              // TODO: getFull message now always converted to FullEmailMessage
              if (ele != null) {
                if (ele.getFullMessage() != null) {
                  if (ele.getFullMessage() instanceof FullEmailMessage) {
                    i.putExtra("email_content", ((FullEmailMessage)ele.getFullMessage()).getContent());
                  }
                }
              }
              
              i.putExtra("email_id", emailID);
              i.putExtra("subject", ele.getTitle());
              i.putExtra("from", new PersonAndr(ele.getFrom()));
              i.putExtra("account", (Parcelable)a);
              
              startActivityForResult(i, EMAIL_CONTENT_RESULT);
              boolean changed = s.setMailSeen(emailID);
              if (changed) {
                setEmailSeen(emailID);
                adapter.notifyDataSetChanged();
              }
            }
          });
          if (serviceConnectionEstablished) {
            updateList(s.getEmails());
          }
        } else {
          updateList(s.getEmails());
          adapter.notifyDataSetChanged();
        }
        
        // if no pass or email or imap provided, then redirect to settings panel
//        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.settings_email_file_key), Context.MODE_PRIVATE);
//        String email = sharedPref.getString(getString(R.string.settings_saved_email), "");
//        String pass = sharedPref.getString(getString(R.string.settings_saved_pass), "");
//        String imap = sharedPref.getString(getString(R.string.settings_saved_imap), "");
//        if (email.length() + pass.length() + imap.length() == 0) {
//          TextView text = new TextView(this);
//          text.setText(getString(R.string.no_account_set));
//          text.setGravity(Gravity.CENTER);
//          this.setContentView(text);
//        }
      } else {
        TextView text = new TextView(this);
        text.setText(getString(R.string.no_internet_access));
        text.setGravity(Gravity.CENTER);
        this.setContentView(text);
      }
//    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (serviceReceiver != null) {
      unregisterReceiver(serviceReceiver);
    }
  }
  
  public boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  private class SystemBroadcastReceiver extends BroadcastReceiver {

//    private MainActivity activity = null;

    public SystemBroadcastReceiver(MainActivity activity) {
//      this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      // listening for internet access change
      if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
        NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        String typeName = info.getTypeName();
        String subtypeName = info.getSubtypeName();
        System.out.println("Network is up ******** " + typeName + ":::" + subtypeName);

//        activity.setContent("onInternetBroadcast receive");
      }
    }
  }
  
  private class DataUpdateReceiver extends BroadcastReceiver {

    private MainActivity activity;
    
    public DataUpdateReceiver(MainActivity activity) {
      this.activity = activity;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(Constants.MAIL_SERVICE_INTENT)) {
        if (intent.getExtras().getInt("result") != MyService.OK) {
          int result = intent.getExtras().getInt("result");
          String msg = null;
          switch (result) {
            case MyService.AUTHENTICATION_FAILED_EXCEPTION:
              msg = "Authentication failed: " + intent.getExtras().getString("errorMessage");
              break;
            case MyService.UNKNOWN_HOST_EXCEPTION:
              msg = getString(R.string.exception_unknown_host);
              break;
            case MyService.IOEXCEPTION:
              msg = getString(R.string.exception_io);
              break;
            case MyService.CONNECT_EXCEPTION:
              msg = getString(R.string.exception_connect);
              break;
            case MyService.NO_SUCH_PROVIDER_EXCEPTION:
              msg = getString(R.string.exception_nosuch_provider);
              break;
            case MyService.MESSAGING_EXCEPTION:
              msg = getString(R.string.exception_messaging);
              break;
            case MyService.SSL_HANDSHAKE_EXCEPTION:
              msg = getString(R.string.exception_ssl_handshake);
              break;
            case MyService.NO_INTERNET_ACCESS:
              msg = getString(R.string.no_internet_access);
              break;
            case MyService.NO_ACCOUNT_SET:
              TextView text = new TextView(context);
              text.setText(getString(R.string.no_account_set));
              text.setGravity(Gravity.CENTER);
              activity.setContentView(text);
              break;
            default:
              msg = getString(R.string.exception_unknown);
              break;
          }
          if (msg != null) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
          }
          if (pd != null) {
            pd.dismiss();
          }
        } else {
          Parcelable[] messagesParc = intent.getExtras().getParcelableArray("messages");
          MessageListElementParc[] messages = new MessageListElementParc[messagesParc.length];
          for (int i = 0; i < messagesParc.length; i++) {
            messages[i] = (MessageListElementParc) messagesParc[i];
          }

          updateList(messages);
          if (pd != null) {
            pd.dismiss();
          }
        }
      }
    }
  }
}
