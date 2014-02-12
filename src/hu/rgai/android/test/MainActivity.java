//TODO: refresh button at main setting panel
//TODO: batched contact list update
//TODO: display message when attempting to add freemail account: Freemail has no IMAP support
package hu.rgai.android.test;

//import android.app.ActionBar;
//import com.testflightapp.lib.TestFlight;
import hu.rgai.android.asynctasks.XmppConnector;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.LogUploadScheduler;
import hu.rgai.android.eventlogger.ScreenReceiver;
import hu.rgai.android.intent.beens.FullMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.services.MainService;
import hu.rgai.android.services.schedulestarters.MainScheduler;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.settings.AccountSettingsList;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.Session;
import com.facebook.SessionState;
import java.util.HashMap;

public class MainActivity extends ActionBarActivity {

  public static volatile MainActivity instance;
  private static String fbToken = null;
  
  private static final String APPLICATION_START_STR = "application:start";
  private static final String APPLICATION_OVER_STR = "application:over";
  private static final String MAINPAGE_BACKBUTTON_STR = "mainpage:backbutton";
  private static final String MAINPAGE_PAUSE_STR = "mainpage:pause";
  private static final String MAINPAGE_RESUME_STR = "mainpage:resume";
  private static final String CLICK_TO_MESSAGEGROUP_STR = "click to messagegroup";
  private static final String SCROLL_END_STR = "scroll:end";
  private static final String SCROLL_START_STR = "scroll:start";
  private static final String MAIN_PAGE_STR = "MainPage";
  // private Boolean isInternetAvailable = null;

  public static final int PICK_CONTACT = 101;
  private static final String SPACE_STR = " ";
  private static boolean is_activity_visible = false;
//  private static Date last_notification_date = null;
  private static HashMap<AccountAndr, Date> last_notification_dates = null;

  private boolean serviceConnectionEstablished = false;
//  private static volatile List<MessageListElementParc> messages;
  private static volatile LazyAdapter adapter;

  private MainService s;

  private final LogUploadScheduler logUploadScheduler = new LogUploadScheduler(this);
  private DataUpdateReceiver serviceReceiver;
//<<<<<<< HEAD
//  private BroadcastReceiver systemReceiver;
  private ScreenReceiver screenReceiver;
//=======
//  private BroadcastReceiver systemReceiver;
//>>>>>>> master
  private ProgressDialog pd = null;
  private static Date lastLoadMoreEvent = null;
  private static ListView lv = null;
  private static Button loadMoreButton = null;
  private static View loadIndicator = null;
  private static volatile boolean isLoading = false;

  // private boolean activityOpenedFromNotification = false;
  private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
      s = ((MainService.MyBinder) binder).getService();
      // if (activityOpenedFromNotification) {
      // s.setAllMessagesToSeen();
      // activityOpenedFromNotification = false;
      // }
      updateList(s.getEmails(), false);
      if ((MainService.messages == null || MainService.messages.isEmpty()) && pd != null) {
        pd.dismiss();
      }
      serviceConnectionEstablished = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      s = null;
    }
  };
  private UncaughtExceptionHandler defaultUEH;

  private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
      EventLogger.INSTANCE.writeToLogFile("uncaughtException : " + ex.getMessage() + " " + ex.getLocalizedMessage(), true);
      // re-throw critical exception further to the os (important)
      defaultUEH.uncaughtException(thread, ex);
    }
  };

  @Override
  public void onBackPressed() {
    Log.d("willrgai", MAINPAGE_BACKBUTTON_STR);
    EventLogger.INSTANCE.writeToLogFile(MAINPAGE_BACKBUTTON_STR, true);
    super.onBackPressed();
  }

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    instance = this;
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (logUploadScheduler.isRunning)
          logUploadScheduler.stopRepeatingTask();
        EventLogger.INSTANCE.closeLogFile();
      }
    });

    // defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    // setup handler for uncaught exception
    // Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);
    // setContentView(R.layout.main);

    // activityOpenedFromNotification =
    // getIntent().getBooleanExtra("from_notifier", false);
    // Log.d("rgai", "WE CAME FROM NOTIFIER CLICK -> " +
    // activityOpenedFromNotification);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    // TODO: session and access token opening and handling
//    final String fbToken = StoreHandler.getFacebookAccessToken(this);
//    if (fbToken != null) {
//      Session.openActiveSessionWithAccessToken(this,
//        AccessToken.createFromExistingAccessToken(fbToken, new Date(2014, 1, 1), new Date(2013, 1, 1), AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, Settings.getFacebookPermissions()),
//        new Session.StatusCallback() {
//          @Override
//          public void call(Session sn, SessionState ss, Exception excptn) {
//            Log.d("rgai", "REOPENING SESSION WITH ACCESS TOKEN -> " + fbToken);
//            Log.d("rgai", sn.toString());
//            Log.d("rgai", ss.toString());
//          }
//        });
//    }
    bindService(new Intent(this, MainService.class), serviceConnection, Context.BIND_AUTO_CREATE);

    if (!MainService.RUNNING) {
      Intent intent = new Intent(this, MainScheduler.class);
      intent.setAction(Context.ALARM_SERVICE);
      this.sendBroadcast(intent);
      // disaplying loading dialog, since the mails are not ready, but the user
      // opened the list
      pd = new ProgressDialog(this);
      pd.setMessage("Fetching messages...");
      pd.setCancelable(false);
      pd.show();
    }
    EventLogger.INSTANCE.setContext(this);
    EventLogger.INSTANCE.openLogFile("logFile.txt", false);
    EventLogger.INSTANCE.writeToLogFile(APPLICATION_START_STR, true);

    if (!logUploadScheduler.isRunning)
      logUploadScheduler.startRepeatingTask();
  }
  
  public static void openFbSession(Context context) {
    if (fbToken == null) {
      fbToken = StoreHandler.getFacebookAccessToken(context);
      Date expirationDate = StoreHandler.getFacebookAccessTokenExpirationDate(context);
      Log.d("rgai", "expiration date readed -> " + expirationDate.toString());
      if (fbToken != null) {
        Session.openActiveSessionWithAccessToken(context,
          AccessToken.createFromExistingAccessToken(fbToken, expirationDate, new Date(2013, 1, 1),
                AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, Settings.getFacebookPermissions()),
          new Session.StatusCallback() {
            @Override
            public void call(Session sn, SessionState ss, Exception excptn) {
              Log.d("rgai", "REOPENING SESSION WITH ACCESS TOKEN -> " + fbToken);
              Log.d("rgai", sn.toString());
              Log.d("rgai", ss.toString());
            }
          });
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_settings_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    Intent intent;
    switch (item.getItemId()) {
      case R.id.accounts:
        intent = new Intent(this, AccountSettingsList.class);
        startActivityForResult(intent, Settings.ActivityRequestCodes.ACCOUNT_SETTING_RESULT);
        return true;
      case R.id.message_send_new:
        intent = new Intent(this, MessageReply.class);
        startActivity(intent);
        return true;
      case R.id.refresh_message_list:
        // item.setEnabled(false);
        Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
        reloadMessages();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case (Settings.ActivityRequestCodes.FULL_MESSAGE_RESULT):
        if (resultCode == Activity.RESULT_OK) {
          // TODO: only saving simple string content
          FullMessageParc fm = data.getParcelableExtra("message_data");
          String messageId = data.getStringExtra("message_id");
          AccountAndr acc = data.getParcelableExtra("account");
          MainService.setMessageContent(messageId, acc, fm);
        }
        break;
      case (Settings.ActivityRequestCodes.ACCOUNT_SETTING_RESULT):
        if (resultCode == Activity.RESULT_OK) {
          reloadMessages();
        }
        break;
      default:
        break;
    }
  }

  private void reloadMessages() {
    Intent intent = new Intent(this, MainScheduler.class);
    intent.setAction(Context.ALARM_SERVICE);
    this.sendBroadcast(intent);
  }

  private void updateList(MessageListElementParc[] newMessages, boolean loadMoreResult) {
    // Log.d("rgai", "updating list...");
    if (loadMoreResult) {
      removeLoadMoreIndicator();
    }
//    if (newMessages != null && messages != null) {
//      if (messages != null) {
//        messages.clear();
//        for (int i = 0; i < newMessages.length; i++) {
//          messages.add(newMessages[i]);
//        }
//      }
      setContent();
//    }

  }

  private static void removeLoadMoreIndicator() {
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      if (isLoading) {
        boolean removeResult = lv.removeFooterView(loadIndicator);
        Log.d("rgai", "REMOVEFOOTER VIEW INDICATOR -> " + removeResult);
        lv.addFooterView(loadMoreButton);
        isLoading = false;
      }
    }
  }

  private static void setMessageSeen(MessageListElementParc message) {
    for (MessageListElementParc mlep : MainService.messages) {
      if (mlep.equals(message)) {
        mlep.setSeen(true);
        mlep.setUnreadCount(0);
        break;
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    removeNotificationIfExists();
    Log.d("rgai", "MainActivitiy.onResume");
    is_activity_visible = true;
    initLastNotificationDates();

//    FacebookAccountAndr fba = StoreHandler.getFacebookAccount(this);
//    if (fba != null) {
//      // TODO: this should be an async task
//      XmppConnector xmppc = new XmppConnector(fba, this);
//      xmppc.execute();
//      // FacebookMessageProvider.initConnection(fba, this);
//    }
    // register service broadcast receiver
    if (serviceReceiver == null) {
      serviceReceiver = new DataUpdateReceiver(this);
    }
    IntentFilter intentFilter = new IntentFilter(Constants.MAIL_SERVICE_INTENT);
    registerReceiver(serviceReceiver, intentFilter);

    // register system broadcast receiver for internet connection state change
//<<<<<<< HEAD
//    if (systemReceiver == null) {
//      systemReceiver = new CustomBroadcastReceiver(this);
//    }
//    IntentFilter customIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
//    customIntentFilter.addAction(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
//    registerReceiver(systemReceiver, customIntentFilter);

    setUpAndRegisterScreenReceiver();

//=======
//    if (systemReceiver == null) {
//      systemReceiver = new CustomBroadcastReceiver(this);
//    }
//    IntentFilter customIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
//    customIntentFilter.addAction(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
//    registerReceiver(systemReceiver, customIntentFilter);
    
//>>>>>>> master
    // setting content
    setContent();
    logActivityEvent(MAINPAGE_RESUME_STR);
  }
  
  private static void initLastNotificationDates() {
    if (last_notification_dates == null) {
      last_notification_dates = new HashMap<AccountAndr, Date>();
    }
  }

  private void setUpAndRegisterScreenReceiver() {
    if (screenReceiver == null) {
      screenReceiver = new ScreenReceiver();
    }

    IntentFilter screenIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
    registerReceiver(screenReceiver, screenIntentFilter);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
//<<<<<<< HEAD
//    if (systemReceiver != null) {
//      unregisterReceiver(systemReceiver);
//    }
    if (screenReceiver != null) {
      unregisterReceiver(screenReceiver);
    }
//=======
//    if (systemReceiver != null) {
//      unregisterReceiver(systemReceiver);
//    }
//>>>>>>> master
    if (serviceConnection != null) {
      unbindService(serviceConnection);
    }

  }

  /**
   * Returns true if the main activity is visible.
   * @return true if main activity visible, false otherwise
   */
  public static boolean isMainActivityVisible() {
    return is_activity_visible;
  }

  /**
   * Updates the last notification date of the given account.
   * Sets all of the accounts last notification date to the current date if null given.
   * @param acc the account to update, or null if update all account's last event time
   */
  public static void updateLastNotification(AccountAndr acc) {
    initLastNotificationDates();
    if (acc != null) {
      last_notification_dates.put(acc, new Date());
    } else {
      for (AccountAndr a : last_notification_dates.keySet()) {
        last_notification_dates.get(a).setTime(new Date().getTime());
      }
    }
  }

  /**
   * Returns the last notification of the given account.
   * 
   * @param acc
   * @return 
   */
  public static Date getLastNotification(AccountAndr acc) {
    Date ret = null;
    if (last_notification_dates == null || acc == null) {
      ret = new Date(new Date().getTime() - 86400 * 365 * 1000);
    } else {
      ret = last_notification_dates.get(acc);
    }
    if (ret == null) {
      ret = new Date(new Date().getTime() - 86400 * 365 * 1000);
    }
    return ret;
  }

  public static void removeMessagesToAccount(final AccountAndr acc) {
    Log.d("rgai", "REMOVE MESSAGES FROM MAIN ACTIVITY");
    Iterator<MessageListElementParc> it = MainService.messages.iterator();
    while (it.hasNext()) {
      MessageListElementParc mle = it.next();
      if (mle.getAccount().equals(acc)) {
        it.remove();
      }
    }
    adapter.notifyDataSetChanged();
  }

  private static void setContent() {
    if (instance == null) return;
    // TODO: itt is kell ellenorizni, hogy van-e jelszo, mer ha nincs akkor nem
    // lehet csinalni semmit...
    if (MainService.messages == null) {
      MainService.initMessages();
    }
    boolean isListView = instance.findViewById(R.id.list) != null;
    boolean isNet = isNetworkAvailable();
    if (isNet || isPhone()) {
      // if list is not empty and current view is listview, update adapter
      if (!MainService.messages.isEmpty() && adapter != null && isListView) {
        adapter.notifyDataSetChanged();
      }
      // insert listview, set adapter
      else if (!MainService.messages.isEmpty() && !isListView) {
        instance.setContentView(R.layout.main);
        lv = (ListView) instance.findViewById(R.id.list);

        loadMoreButton = new Button(instance);
        loadMoreButton.setText("Load more ...");
        loadMoreButton.getBackground().setAlpha(0);

        lv.addFooterView(loadMoreButton);
        LayoutInflater inflater = (LayoutInflater) instance.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        loadIndicator = inflater.inflate(R.layout.loading_indicator, null);

        loadIndicator.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View arg0) {
          }
        });
//        List<MessageListElementParc> msgsList = new LinkedList<MessageListElementParc>();
//        msgsList.addAll(MainService.messages);
        adapter = new LazyAdapter(instance);
        lv.setAdapter(adapter);
        Log.d("rgai", "setting message list");

        loadMoreButton.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View arg0) {
            // Starting a new async task
            loadMoreMessage();
          }
        });

        instance.lv.setOnScrollListener(new LogOnScrollListener(lv, adapter));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> av, View arg1, int itemIndex, long arg3) {
            MessageListElementParc message = (MessageListElementParc) av.getItemAtPosition(itemIndex);
            AccountAndr a = message.getAccount();
            Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(a.getAccountType());
            Intent intent = new Intent(instance, classToLoad);
            intent.putExtra("msg_list_element", message);
            intent.putExtra("account", (Parcelable) a);

            boolean changed = MainService.setMessageSeenAndRead(message);
            if (changed) {
              setMessageSeen(message);
              adapter.notifyDataSetChanged();
            }

            loggingOnClickEvent(message, changed);
            instance.startActivityForResult(intent, Settings.ActivityRequestCodes.FULL_MESSAGE_RESULT);
//            removeNotificationIfExists();
          }

          private void loggingOnClickEvent(MessageListElementParc message, boolean changed) {
            StringBuilder builder = new StringBuilder();
            appendClickedElementDatasToBuilder(message, builder);
            instance.appendVisibleElementToStringBuilder(builder, lv, adapter);
            builder.append(changed);
            Log.d("willrgai", builder.toString());
            EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
          }

          private void appendClickedElementDatasToBuilder(MessageListElementParc message, StringBuilder builder) {
            builder.append(MAIN_PAGE_STR);
            builder.append(SPACE_STR);
            builder.append(CLICK_TO_MESSAGEGROUP_STR);
            builder.append(SPACE_STR);
            builder.append(message.getId());
            builder.append(SPACE_STR);
          }
        });
      } else if (MainService.messages.isEmpty()) {
        TextView text = new TextView(instance);
        text.setText(instance.getString(R.string.empty_list));
        text.setGravity(Gravity.CENTER);
        instance.setContentView(text);
      }
    } else {
      TextView text = new TextView(instance);
      text.setText(instance.getString(R.string.no_internet_access));
      text.setGravity(Gravity.CENTER);
      instance.setContentView(text);
    }

  }
  
  public static void notifyMessageChange(boolean loadMore) {
    setContent();
//    adapter.notifyDataSetChanged();
    if (loadMore) {
      removeLoadMoreIndicator();
    }
  }

  public static void loadMoreMessage() {
    int coolDown = 5;
    if (lastLoadMoreEvent == null || lastLoadMoreEvent.getTime() + coolDown * 1000 < new Date().getTime()) {
      Intent service = new Intent(instance, MainService.class);
      service.putExtra("load_more", true);
      instance.startService(service);
      lastLoadMoreEvent = new Date();
      // Toast.makeText(this, getString(R.string.loading_more_elements),
      // Toast.LENGTH_LONG).show();

      if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        // getting height of load button
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) loadIndicator.findViewById(R.id.pbHeaderProgress).getLayoutParams();
        params.height = loadMoreButton.getHeight();
        Log.d("rgai", "LOAD BUTTON HEIGHT -> " + params.height);
        loadIndicator.findViewById(R.id.pbHeaderProgress).setLayoutParams(params);
        lv.removeFooterView(loadMoreButton);
        // Log.d("rgai", "REMOVEFOOTER VIEW BUTTON -> " + removeResult);
        lv.addFooterView(loadIndicator);
        isLoading = true;
      } else {
        Toast.makeText(instance, "Loading more...", Toast.LENGTH_LONG).show();
      }
      // loadMoreButton.setVisibility(View.GONE);
      // ()
      // loadIndicator.setVisibility(View.VISIBLE);
      // LayoutInflater inflater =
      // (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      // View load = inflater.inflate(R.layout.loading_indicator, null);
      // ((ListView)findViewById(R.id.list)).addFooterView(loadIndicator);
    } else {
      Log.d("rgai", "@@@skipping load button press for " + coolDown + " sec");
    }
  }

  private void removeNotificationIfExists() {
      NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      mNotificationManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
  }

  @Override
  protected void onPause() {
    logActivityEvent(MAINPAGE_PAUSE_STR);
    super.onPause();
    // Log.d("rgai","MainActivitiy.onPause");
    is_activity_visible = false;

    // refreshing last notification date when closing activity
    updateLastNotification(null);
    if (serviceReceiver != null) {
      unregisterReceiver(serviceReceiver);
    }
    // FacebookMessageProvider.closeConnection();
  }

  private void logActivityEvent(String event) {
    StringBuilder builder = new StringBuilder();
    builder.append(event);
    builder.append(SPACE_STR);
    appendVisibleElementToStringBuilder(builder, lv, adapter);
    Log.d("willrgai", builder.toString());
    EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
  }

  public static boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager) instance.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  private static boolean isPhone() {
    TelephonyManager telMgr = (TelephonyManager) instance.getSystemService(Context.TELEPHONY_SERVICE);
    int simState = telMgr.getSimState();
    if (simState == TelephonyManager.SIM_STATE_READY) {
      return true;
    } else {
      return false;
    }
  }


  private class DataUpdateReceiver extends BroadcastReceiver {

    private final MainActivity activity;

    public DataUpdateReceiver(MainActivity activity) {
      this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(Constants.MAIL_SERVICE_INTENT)) {
        if (intent.getExtras().getInt("result") != MainService.OK) {
          int result = intent.getExtras().getInt("result");
          String msg = null;
          switch (result) {
            case MainService.AUTHENTICATION_FAILED_EXCEPTION:
              msg = "Authentication failed: " + intent.getExtras().getString("errorMessage");
              break;
            case MainService.UNKNOWN_HOST_EXCEPTION:
              msg = getString(R.string.exception_unknown_host);
              break;
            case MainService.IOEXCEPTION:
              msg = getString(R.string.exception_io);
              break;
            case MainService.CONNECT_EXCEPTION:
              msg = getString(R.string.exception_connect);
              break;
            case MainService.NO_SUCH_PROVIDER_EXCEPTION:
              msg = getString(R.string.exception_nosuch_provider);
              break;
            case MainService.MESSAGING_EXCEPTION:
              msg = getString(R.string.exception_messaging);
              break;
            case MainService.SSL_HANDSHAKE_EXCEPTION:
              msg = getString(R.string.exception_ssl_handshake);
              break;
            case MainService.NO_INTERNET_ACCESS:
              msg = getString(R.string.no_internet_access);
              break;
            case MainService.NO_ACCOUNT_SET:
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

          boolean loadMoreResult = intent.getExtras().getBoolean("load_more");
          // Log.d("rgai", "LOAD MORE RESULT -> " + loadMoreResult);
          updateList(messages, loadMoreResult);
          if (pd != null) {
            pd.dismiss();
          }
        }
      }
    }
  }

  private void appendVisibleElementToStringBuilder(StringBuilder builder, ListView lv, LazyAdapter adapter) {
    if (lv == null || adapter == null) {
      builder.append(SPACE_STR);
      return;
    }
    int firstVisiblePosition = lv.getFirstVisiblePosition();
    int lastVisiblePosition = lv.getLastVisiblePosition();
    // TODO: null pointer exception occures here....
    try {
      for (int actualVisiblePosition = firstVisiblePosition; actualVisiblePosition < lastVisiblePosition; actualVisiblePosition++) {
        builder.append(((MessageListElementParc) (adapter.getItem(actualVisiblePosition))).getId());
        builder.append(SPACE_STR);
      }
    } catch (Exception ex) {
      Log.d("willrgai", "NULL POINTER EXCEPTION CATCHED");
      ex.printStackTrace();
    }
    
  }

  static class LogOnScrollListener implements OnScrollListener {
    final ListView lv;
    final LazyAdapter adapter;

    public LogOnScrollListener(ListView lv, LazyAdapter adapter) {
      this.lv = lv;
      this.adapter = adapter;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
      // TODO Auto-generated method stub
      StringBuilder builder = new StringBuilder();

      builder.append(MAIN_PAGE_STR);
      builder.append(SPACE_STR);
      if (scrollState == 1) {
        builder.append(SCROLL_START_STR);
        builder.append(SPACE_STR);
      } else {
        builder.append(SCROLL_END_STR);
        builder.append(SPACE_STR);
      }
      instance.appendVisibleElementToStringBuilder(builder, lv, adapter);
      Log.d("willrgai", builder.toString());
      EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
    }

  }
}
