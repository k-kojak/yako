//TODO: batched contact list update
//TODO: display message when attempting to add freemail account: Freemail has no IMAP support
package hu.rgai.android.test;

import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.ScreenReceiver;
import hu.rgai.android.intent.beens.FullMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.services.MainService;
import hu.rgai.android.services.schedulestarters.MainScheduler;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.settings.AccountSettingsList;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
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
  private static String fbToken = null;
  // holds the activity visibility state
  private static boolean is_activity_visible = false;
  // stores the last notification state to all different account types
  private static HashMap<AccountAndr, Date> last_notification_dates = null;
  // this is the adapter for the main view
  private static volatile LazyAdapter adapter;
  // receiver for logging screen status
  private ScreenReceiver screenReceiver;
  // a progress dialog to display message load status
  private static ProgressDialog pd = null;
  // a variable to store the last date of "Load more" button press time
  private static Date lastLoadMoreEvent = null;
  // the static listview where messages displayed
  private static ListView lv = null;
  // button to load more messages
  private static Button loadMoreButton = null;
  // an indicator when more messages are loading
  private static View loadIndicator = null;
  // true if more messages are currently loading
  private static volatile boolean isLoading = false;

  private static final String APPLICATION_START_STR = "application:start";

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
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.MAINPAGE.BACKBUTTON_STR, true);
    super.onBackPressed();
  }

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    instance = this;
    if (!EventLogger.INSTANCE.isLogFileOpen()) {
      EventLogger.INSTANCE.setContext(this);
      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
    }
    EventLogger.INSTANCE.writeToLogFile(APPLICATION_START_STR, true);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    if (!MainService.RUNNING) {
      Intent intent = new Intent(this, MainScheduler.class);
      intent.setAction(Context.ALARM_SERVICE);
      this.sendBroadcast(intent);
      // disaplying loading dialog, since the mails are not ready, but the user
      // opened the list
    }
    showProgressDialog();
  }

  /**
   * Displays a loading progress dialog, which tells the user that messages are
   * loading.
   */
  private static void showProgressDialog() {
    pd = new ProgressDialog(instance);
    pd.setMessage("Fetching messages...");
    pd.setCancelable(false);
    pd.show();
  }

  /**
   * Hides the message loading dialog if is there any.
   */
  private static void hideProgressDialog() {
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
      Log.d("rgai", "expiration date readed -> " + expirationDate.toString());
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

  /**
   * Sends a broadcast to the Service to load fresh messages again.
   */
  private void reloadMessages() {
    Intent intent = new Intent(this, MainScheduler.class);
    intent.setAction(Context.ALARM_SERVICE);
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

    setUpAndRegisterScreenReceiver();

    setContent();
    if (!EventLogger.INSTANCE.isLogFileOpen())
      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
    logActivityEvent(EventLogger.LOGGER_STRINGS.MAINPAGE.RESUME_STR);
  }

  /**
   * Initializes the lastNotification map.
   */
  private static void initLastNotificationDates() {
    if (last_notification_dates == null) {
      last_notification_dates = new HashMap<AccountAndr, Date>();
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
   *          last notification time will be set to this account
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

  /**
   * Removes the messages from the displayview to the given account.
   * 
   * @param acc
   *          messages connected to this account will be removed
   */
  public static void removeMessagesToAccount(final AccountAndr acc) {
    Log.d("rgai", "REMOVE MESSAGES FROM MAIN ACTIVITY");
    Iterator<MessageListElementParc> it = MainService.messages.iterator();
    while (it.hasNext()) {
      MessageListElementParc mle = it.next();
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
    if (isNet || isPhone()) {
      if (!MainService.messages.isEmpty() && adapter != null && isListView) {
        adapter.notifyDataSetChanged();
      } else if (!MainService.messages.isEmpty() && !isListView) {
        instance.setContentView(R.layout.main);
        lv = (ListView) instance.findViewById(R.id.list);

        loadMoreButton = new Button(instance);
        loadMoreButton.setText("Load more ...");
        loadMoreButton.getBackground().setAlpha(0);
        loadMoreButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View arg0) {
            loadMoreMessage();
          }
        });
        lv.addFooterView(loadMoreButton);

        LayoutInflater inflater = (LayoutInflater) instance.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        loadIndicator = inflater.inflate(R.layout.loading_indicator, null);

        adapter = new LazyAdapter(instance);
        lv.setAdapter(adapter);
        lv.setOnScrollListener(new LogOnScrollListener(lv, adapter));
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
          }

          /**
           * Performs a log event when an item clicked on the main view list.
           */
          private void loggingOnClickEvent(MessageListElementParc message, boolean changed) {
            StringBuilder builder = new StringBuilder();
            appendClickedElementDatasToBuilder(message, builder);
            instance.appendVisibleElementToStringBuilder(builder, lv, adapter);
            builder.append(changed);
            Log.d("willrgai", builder.toString());
            EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
          }

          private void appendClickedElementDatasToBuilder(MessageListElementParc message, StringBuilder builder) {
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
    int coolDown = 5; // sec
    if (lastLoadMoreEvent == null || lastLoadMoreEvent.getTime() + coolDown * 1000 < new Date().getTime()) {
      Intent service = new Intent(instance, MainService.class);
      service.putExtra("load_more", true);
      instance.startService(service);
      lastLoadMoreEvent = new Date();

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
    } else {
      Log.d("rgai", "@@@skipping load button press for " + coolDown + " sec");
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

    // refreshing last notification date when closing activity
    updateLastNotification(null);
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
  private static boolean isPhone() {
    TelephonyManager telMgr = (TelephonyManager) instance.getSystemService(Context.TELEPHONY_SERVICE);
    int simState = telMgr.getSimState();
    if (simState == TelephonyManager.SIM_STATE_READY) {
      return true;
    } else {
      return false;
    }
  }

  /*private class DataUpdateReceiver extends BroadcastReceiver {

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
//          hideProgressDialog();
        } else {
          Parcelable[] messagesParc = intent.getExtras().getParcelableArray("messages");
          MessageListElementParc[] messages = new MessageListElementParc[messagesParc.length];
          for (int i = 0; i < messagesParc.length; i++) {
            messages[i] = (MessageListElementParc) messagesParc[i];
          }

          boolean loadMoreResult = intent.getExtras().getBoolean("load_more");
          Log.d("rgai", "DATE UPDATE RECEIVER -> update list");
          updateList(messages, loadMoreResult);
//          hideProgressDialog();
        }
      }
    }
  }*/

  private void appendVisibleElementToStringBuilder(StringBuilder builder, ListView lv, LazyAdapter adapter) {
    if (lv == null || adapter == null) {
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      return;
    }
    int firstVisiblePosition = lv.getFirstVisiblePosition();
    int lastVisiblePosition = lv.getLastVisiblePosition();
    // TODO: null pointer exception occures here....
    try {
      for (int actualVisiblePosition = firstVisiblePosition; actualVisiblePosition < lastVisiblePosition; actualVisiblePosition++) {
        builder.append(((MessageListElementParc) (adapter.getItem(actualVisiblePosition))).getId());
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
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
