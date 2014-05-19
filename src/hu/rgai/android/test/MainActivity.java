//TODO: batched contact list update
//TODO: display message when attempting to add freemail account: Freemail has no IMAP support
package hu.rgai.android.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
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
import android.view.Window;
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
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.FullMessage;
import hu.rgai.android.beens.MainServiceExtraParams;
import hu.rgai.android.beens.MainServiceExtraParams.ParamStrings;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.SmsAccount;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.LogUploadScheduler;
import hu.rgai.android.eventlogger.ScreenReceiver;
import hu.rgai.android.services.MainService;
import hu.rgai.android.services.schedulestarters.MainScheduler;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.settings.AccountSettingsList;
import hu.rgai.android.test.settings.SystemPreferences;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This is the main view of the application.
 * 
 * This activity lists the messages in a common list view.
 * 
 * @author Tamas Kojedzinszky
 */
public class MainActivity extends ActionBarActivity {

  // this variable holds the MainActivity instance if exists
  public static volatile MainActivity instance;

  // holds the Facebook token
  private static volatile String fbToken = null;

  // holds the activity visibility state
  private static volatile boolean is_activity_visible = false;

  // stores the last notification state to all different account types
  private static volatile HashMap<Account, Date> last_notification_dates = null;

  // this is the adapter for the main view
  private static volatile LazyAdapter adapter;

  // receiver for logging screen status
  private ScreenReceiver screenReceiver;

  // a progress dialog to display message load status
  private static ProgressDialog pd = null;

  // the static listview where messages displayed
  private static ListView lv = null;

  // button to load more messages
  private static Button loadMoreButton = null;

  // an indicator when more messages are loading
  private static View loadIndicator = null;

  // true if more messages are currently loading
  private static volatile boolean isLoading = false;

  public static Account actSelectedFilter = null;

  private static final String APPLICATION_START_STR = "application:start";

  private UncaughtExceptionHandler defaultUEH;
  
  private static Menu mMenu;

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
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.MAINPAGE.BACKBUTTON_STR, true);
    super.onBackPressed();
  }

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
//    Debug.startMethodTracing("calc_store_connect");
    Tracker t = ((AnalyticsApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());
    
    // this loads the last notification dates from file
    MainActivity.initLastNotificationDates(this);
    instance = this;
    if (!EventLogger.INSTANCE.isLogFileOpen()) {
      EventLogger.INSTANCE.setContext(this);
      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
    }
    EventLogger.INSTANCE.writeToLogFile(APPLICATION_START_STR + " " + EventLogger.INSTANCE.getAppVersion() + " " + android.os.Build.VERSION.RELEASE, true);

    LogUploadScheduler.INSTANCE.setContext(this);
    if (!LogUploadScheduler.INSTANCE.isRunning)
      LogUploadScheduler.INSTANCE.startRepeatingTask();

    getSupportActionBar().setDisplayShowTitleEnabled(false);

    
    // disaplying loading dialog, since the mails are not ready, but the user
    showProgressDialog();
  }

  /**
   * Displays a loading progress dialog, which tells the user that messages are
   * loading.
   */
  private static void showProgressDialog() {
//    instance.setProgressBarIndeterminateVisibility(Boolean.TRUE); 
//    mMenu.findItem(R.id.refresh_message_list).setVisible(false);
    pd = new ProgressDialog(instance);
    pd.setMessage(instance.getString(R.string.loading));
    pd.setCancelable(false);
    pd.show();
  }

  /**
   * Hides the message loading dialog if is there any.
   */
  private static void hideProgressDialog() {
//    if (instance != null) {
//      instance.setProgressBarIndeterminateVisibility(Boolean.FALSE); 
//    }
//    if (mMenu != null) {
//      mMenu.findItem(R.id.refresh_message_list).setVisible(true);
//    }
    if (pd != null) {
      pd.dismiss();
    }
  }

  /**
   * Opens Facebook session if exists.
   * 
   * @param context
   */
  public static void openFbSession(Context context) {
    if (fbToken == null) {
      fbToken = StoreHandler.getFacebookAccessToken(context);
      Date expirationDate = StoreHandler.getFacebookAccessTokenExpirationDate(context);
      // Log.d( "rgai", "expiration date readed -> " + expirationDate.toString());
      if (fbToken != null) {
        Session.openActiveSessionWithAccessToken(context,
            AccessToken.createFromExistingAccessToken(fbToken, expirationDate, new Date(2013, 1, 1), AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, Settings.getFacebookPermissions()),
            new Session.StatusCallback() {
              @Override
              public void call(Session sn, SessionState ss, Exception excptn) {
              }
            });
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_settings_menu, menu);
    this.mMenu = menu;
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    Intent intent;
    switch (item.getItemId()) {
      case R.id.accounts:
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_ACCOUNT_BTN, true);
        intent = new Intent(this, AccountSettingsList.class);
        startActivityForResult(intent, Settings.ActivityRequestCodes.ACCOUNT_SETTING_RESULT);
        return true;
      case R.id.message_send_new:
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_MESSAGE_SEND_BTN, true);
        intent = new Intent(this, MessageReply.class);
        startActivity(intent);
        return true;
      case R.id.refresh_message_list:
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_REFRESH_BTN, true);
        // item.setEnabled(false);

        Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
        reloadMessages(true);
        return true;
      case R.id.system_preferences:
        Intent i = new Intent(instance, SystemPreferences.class);
        startActivity(i);
        return true;
      case R.id.filter_list:
        showListFilter();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void showListFilter() {
    final List<Account> allAccount = getAllAccounts();
    final CharSequence[] items = new CharSequence[allAccount.size() + 1];
    int selectedIndex = 0;
    items[0] = "All";
    for (int i = 0; i < allAccount.size(); i++) {
      String dn = allAccount.get(i).getDisplayName();
      if (dn == null) {
        items[i + 1] = allAccount.get(i).getAccountType().toString();
      } else {
        items[i + 1] = dn + " (" + allAccount.get(i).getAccountType().toString() + ")";
      }

      if (allAccount.get(i).equals(actSelectedFilter)) {
        selectedIndex = i + 1;
      }
    }

    // Creating and Building the Dialog
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Filter list");
    builder.setSingleChoiceItems(items, selectedIndex, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int item) {
        if (item == 0) {
          actSelectedFilter = null;
        } else {
          if (allAccount.size() >= item) {
            actSelectedFilter = allAccount.get(item - 1);
          }
        }
        dialog.dismiss();
        setContent();
      }
    });
    builder.create().show();
  }

  private List<Account> getAllAccounts() {
    List<Account> list = StoreHandler.getAccounts(this);
    if (isPhone(this)) {
      list.add(SmsAccount.account);
    }

    return list;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case (Settings.ActivityRequestCodes.FULL_MESSAGE_RESULT):
        if (resultCode == Activity.RESULT_OK) {
          if (data != null) {
            if (data.hasExtra("message_data")) {
              FullMessage fm = data.getParcelableExtra("message_data");
              String messageId = data.getStringExtra("message_id");
              Account acc = data.getParcelableExtra("account");
              MainService.setMessageContent(messageId, acc, fm);
            }
            if (data.hasExtra("thread_displayer")) {
  //            Intent service = new Intent(this, MainScheduler.class);
  //            service.setAction(Context.ALARM_SERVICE);
  //            MainServiceExtraParams eParams = new MainServiceExtraParams();
  //            eParams.setType(data.getStringExtra("account_type"));
  //            eParams.setActViewingMessage((MessageListElement)data.getParcelableExtra("act_view_msg"));
  //            
  //            service.putExtra(ParamStrings.EXTRA_PARAMS, eParams);
  //            this.sendBroadcast(service);
            }
          }
        }
        break;
      case (Settings.ActivityRequestCodes.ACCOUNT_SETTING_RESULT):
        if (resultCode == Activity.RESULT_OK) {
          reloadMessages(true);
        }
        break;
      default:
        break;
    }
  }

  /**
   * Sends a broadcast to the Service to load fresh messages again.
   */
  private void reloadMessages(boolean forceQuery) {
    Intent intent = new Intent(this, MainScheduler.class);
    intent.setAction(Context.ALARM_SERVICE);
    MainServiceExtraParams eParams = new MainServiceExtraParams();
    if (forceQuery) {
      eParams.setForceQuery(true);
    }
    if (actSelectedFilter != null) {
      eParams.setAccount(actSelectedFilter);
    }
    intent.putExtra(ParamStrings.EXTRA_PARAMS, eParams);
    this.sendBroadcast(intent);
  }

  /**
   * Removes load more indicator.
   * The indicator depends on Android version.
   */
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

  /**
   * Sets a message's status to seen.
   * @param message the message to set seen
   */
  private static void setMessageSeen(MessageListElement message) {
    for (MessageListElement m : MainService.messages) {
      if (m.equals(message)) {
        m.setSeen(true);
        m.setUnreadCount(0);
        break;
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    is_activity_visible = true;
    removeNotificationIfExists();
    
    if (!MainService.RUNNING) {
      reloadMessages(true);
    } else {
      long now = System.currentTimeMillis();
      if (MainService.last_message_update == null || MainService.last_message_update.getTime() + 1000l * Settings.MESSAGE_LOAD_INTERVAL < now) {
        reloadMessages(false);
      }
    }
    
    // initLastNotificationDates();
    updateLastNotification(instance, null);
    setUpAndRegisterScreenReceiver();

    setContent();
    if (!EventLogger.INSTANCE.isLogFileOpen())
      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
    LogUploadScheduler.INSTANCE.setContext(this);
    if (!LogUploadScheduler.INSTANCE.isRunning)
      LogUploadScheduler.INSTANCE.startRepeatingTask();
    logActivityEvent(EventLogger.LOGGER_STRINGS.MAINPAGE.RESUME_STR);
  }

  /**
   * Initializes the lastNotification map.
   */
  public static void initLastNotificationDates(Context context) {
    last_notification_dates = StoreHandler.readLastNotificationObject(context);
    if (last_notification_dates == null) {
      last_notification_dates = new HashMap<Account, Date>();
    }
  }

  /**
   * Sets up the screen receiver for logging.
   */
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
    if (screenReceiver != null) {
      unregisterReceiver(screenReceiver);
    }
//    Debug.stopMethodTracing();
  }

  /**
   * Returns true if the main activity is visible.
   * 
   * @return true if main activity visible, false otherwise
   */
  public static boolean isMainActivityVisible() {
    return is_activity_visible;
  }

  /**
   * Updates the last notification date of the given account. Sets all of the
   * accounts last notification date to the current date if null given.
   * 
   * @param acc
   *          the account to update, or null if update all account's last event
   *          time
   */
  public static void updateLastNotification(Context context, Account acc) {
    if (acc != null) {
      last_notification_dates.put(acc, new Date());
    } else {
      for (Account a : last_notification_dates.keySet()) {
        last_notification_dates.get(a).setTime(new Date().getTime());
      }
    }
    StoreHandler.writeLastNotificationObject(context, last_notification_dates);
  }

  /**
   * Returns the last notification of the given account.
   * 
   * @param acc
   *          last notification time will be set to this account
   * @return
   */
  public static Date getLastNotification(Context context, Account acc) {
    Date ret = null;
    if (last_notification_dates == null || acc == null) {
      ret = new Date(new Date().getTime() - 86400L * 365 * 1000);
    } else {
      ret = last_notification_dates.get(acc);
    }
    if (ret == null) {
      ret = new Date(new Date().getTime() - 86400L * 365 * 1000);
    }
    return ret;
  }

  /**
   * Removes the messages from the displayview to the given account.
   * 
   * @param acc
   *          messages connected to this account will be removed
   */
  public static void removeMessagesToAccount(final Account acc) {
    Log.d("rgai", "REMOVE MESSAGES FROM MAIN ACTIVITY");
    Iterator<MessageListElement> it = MainService.messages.iterator();
    while (it.hasNext()) {
      MessageListElement mle = it.next();
      if (mle.getAccount().equals(acc)) {
        it.remove();
      }
    }
    if (adapter != null) {
      adapter.notifyDataSetChanged();
    }
  }

  /**
   * Sets the content of the listview.
   */
  private static void setContent() {
//    Log.d("rgai", "SET MAIN CONTENT");
    if (instance == null) {
      return;
    }
    if (MainService.messages == null) {
      MainService.initMessages();
    }
    if (!MainService.messages.isEmpty()) {
      hideProgressDialog();
    }
    boolean isListView = instance.findViewById(R.id.list) != null;
    boolean isNet = isNetworkAvailable(instance);
    if (isNet || isPhone(instance)) {
      if (!MainService.messages.isEmpty() && adapter != null && isListView) {
//        Log.d("rgai", "ag1: " + MainService.messages.size());
//        adapter = new LazyAdapter(instance);
//        adapter.setListFilter(actSelectedFilter);
        adapter.setListFilter(actSelectedFilter);
        adapter.notifyDataSetChanged();
//        lv.setAdapter(adapter);
//        lv.invalidate();

      } else if (!MainService.messages.isEmpty() && !isListView) {
//        Log.d("rgai", "ag2");
        instance.setContentView(R.layout.main);
        lv = (ListView) instance.findViewById(R.id.list);

        loadMoreButton = new Button(instance);
        loadMoreButton.setText("Load more ...");
        loadMoreButton.getBackground().setAlpha(0);
        loadMoreButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View arg0) {
            EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_LOAD_MORE_BTN, true);
            loadMoreMessage();
          }
        });
        lv.addFooterView(loadMoreButton);

        LayoutInflater inflater = (LayoutInflater) instance.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        loadIndicator = inflater.inflate(R.layout.loading_indicator, null);

        adapter = new LazyAdapter(instance);
        adapter.setListFilter(actSelectedFilter);
        lv.setAdapter(adapter);
        lv.setOnScrollListener(new LogOnScrollListener(lv, adapter));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> av, View arg1, int itemIndex, long arg3) {
            if (av.getItemAtPosition(itemIndex) == null) return;
            MessageListElement message = (MessageListElement) av.getItemAtPosition(itemIndex);
            Account a = message.getAccount();
            Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(a.getAccountType());
            Intent intent = new Intent(instance, classToLoad);
            intent.putExtra("msg_list_element_id", message.getId());
            intent.putExtra("account", (Parcelable) a);

            boolean changed = MainService.setMessageSeenAndRead(message);
            if (changed) {
              setMessageSeen(message);
              adapter.notifyDataSetChanged();
            }

            loggingOnClickEvent(message, changed);
            instance.startActivityForResult(intent, Settings.ActivityRequestCodes.FULL_MESSAGE_RESULT);
          }

          /**
           * Performs a log event when an item clicked on the main view list.
           */
          private void loggingOnClickEvent(MessageListElement message, boolean changed) {
            StringBuilder builder = new StringBuilder();
            appendClickedElementDatasToBuilder(message, builder);
            instance.appendVisibleElementToStringBuilder(builder, lv, adapter);
            builder.append(changed);
            EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
          }

          private void appendClickedElementDatasToBuilder(MessageListElement message, StringBuilder builder) {
            builder.append(EventLogger.LOGGER_STRINGS.MAINPAGE.STR);
            builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
            builder.append(EventLogger.LOGGER_STRINGS.OTHER.CLICK_TO_MESSAGEGROUP_STR);
            builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
            builder.append(message.getId());
            builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
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

  /**
   * Displays an error Toast message if something went wrong at the Service
   * during retrieving messages.
   * 
   * @param result
   *          the result code of the message query
   * @param message
   *          the content of the error message
   */
  public static void showErrorMessage(int result, String message) {
    if (result != MainService.OK) {
      Log.d("rgai", "ERROR MSG FROM Service -> " + message);
      String msg = "";
      switch (result) {
        case MainService.AUTHENTICATION_FAILED_EXCEPTION:
          msg = "Authentication failed: " + message;
          break;
        case MainService.UNKNOWN_HOST_EXCEPTION:
          msg = message;
          break;
        case MainService.IOEXCEPTION:
          msg = message;
          break;
        case MainService.CONNECT_EXCEPTION:
          msg = message;
          break;
        case MainService.NO_SUCH_PROVIDER_EXCEPTION:
          msg = message;
          break;
        case MainService.MESSAGING_EXCEPTION:
          msg = message;
          break;
        case MainService.SSL_HANDSHAKE_EXCEPTION:
          msg = message;
          break;
        case MainService.NO_INTERNET_ACCESS:
          msg = message;
          break;
        case MainService.NO_ACCOUNT_SET:
          msg = instance.getString(R.string.no_account_set);
          break;
        default:
          msg = instance.getString(R.string.exception_unknown);
          break;
      }
      if (is_activity_visible && instance != null) {
        Toast.makeText(instance, msg, Toast.LENGTH_LONG).show();
      }
    }
  }

  /**
   * This function is called by the main Service, when a new message arrives.
   * 
   * @param loadMore
   *          true if notification comes from a loadMore request(Load More was
   *          pressed), false otherwise
   */
  public static void notifyMessageChange(boolean loadMore) {
    //TODO: we should replace this with broadcast receivers, not like static stuffs...
    hideProgressDialog();
    setContent();
    if (loadMore) {
      removeLoadMoreIndicator();
    }
  }

  /**
   * Handles LoadMore button press.
   */
  public static void loadMoreMessage() {
    Intent service = new Intent(instance, MainScheduler.class);
    service.setAction(Context.ALARM_SERVICE);
    MainServiceExtraParams eParams = new MainServiceExtraParams();
    eParams.setLoadMore(true);
    if (actSelectedFilter != null) {
      eParams.setAccount(actSelectedFilter);
    }
    service.putExtra(ParamStrings.EXTRA_PARAMS, eParams);
    instance.sendBroadcast(service);

    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      // getting height of load button
      LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) loadIndicator.findViewById(R.id.pbHeaderProgress).getLayoutParams();
      params.height = loadMoreButton.getHeight();
      loadIndicator.findViewById(R.id.pbHeaderProgress).setLayoutParams(params);
      lv.removeFooterView(loadMoreButton);
      lv.addFooterView(loadIndicator);
      isLoading = true;
    } else {
      Toast.makeText(instance, "Loading more...", Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Removes the notification from statusbar if exists.
   */
  private void removeNotificationIfExists() {
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
  }

  @Override
  protected void onPause() {
    logActivityEvent(EventLogger.LOGGER_STRINGS.MAINPAGE.PAUSE_STR);
    super.onPause();
    is_activity_visible = false;

    Tracker t = ((AnalyticsApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName() + " - pause");
    t.send(new HitBuilders.AppViewBuilder().build());
    
    // refreshing last notification date when closing activity
    updateLastNotification(instance, null);
  }

  /**
   * Logs event.
   * 
   * @param event
   *          the text of log
   */
  private void logActivityEvent(String event) {
    StringBuilder builder = new StringBuilder();
    builder.append(event);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    appendVisibleElementToStringBuilder(builder, lv, adapter);
    Log.d("willrgai", builder.toString());
    EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
  }

  /**
   * Decides if is network available.
   * 
   * @return true if network is available, false otherwise
   */
  public static boolean isNetworkAvailable(Context c) {
    ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  /**
   * Decides if the device has SIM card or not.
   * 
   * @return true if has SIM card, false otherwise
   */
  public static boolean isPhone(Context c) {
    TelephonyManager telMgr = (TelephonyManager) instance.getSystemService(Context.TELEPHONY_SERVICE);
    int simState = telMgr.getSimState();
    if (simState == TelephonyManager.SIM_STATE_READY) {
      return true;
    } else {
      return false;
    }
  }

  private void appendVisibleElementToStringBuilder(StringBuilder builder, ListView lv, LazyAdapter adapter) {
    if (lv == null || adapter == null) {
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      return;
    }
    int firstVisiblePosition = lv.getFirstVisiblePosition();
    int lastVisiblePosition = lv.getLastVisiblePosition();
    // TODO: null pointer exception occures here....
    try {

      if (actSelectedFilter == null)
        builder.append(EventLogger.LOGGER_STRINGS.MAINPAGE.ALL_STR);
      else
        builder.append(actSelectedFilter.getDisplayName());

      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      for (int actualVisiblePosition = firstVisiblePosition; actualVisiblePosition < lastVisiblePosition; actualVisiblePosition++) {
        if (adapter.getItem(actualVisiblePosition) != null) {
          builder.append(((MessageListElement) (adapter.getItem(actualVisiblePosition))).getId());
          builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        }
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
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
      // TODO Auto-generated method stub
      StringBuilder builder = new StringBuilder();

      builder.append(EventLogger.LOGGER_STRINGS.MAINPAGE.STR);
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      if (scrollState == 1) {
        builder.append(EventLogger.LOGGER_STRINGS.SCROLL.START_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      } else {
        builder.append(EventLogger.LOGGER_STRINGS.SCROLL.END_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      }
      instance.appendVisibleElementToStringBuilder(builder, lv, adapter);
      EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
    }

  }
}
