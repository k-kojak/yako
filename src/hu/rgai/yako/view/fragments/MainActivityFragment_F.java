//TODO: batched contact list update
//TODO: display message when attempting to add freemail account: Freemail has no IMAP support
package hu.rgai.yako.view.fragments;

import hu.rgai.android.test.*;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.yako.adapters.MainListAdapter;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.Session;
import com.facebook.SessionState;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.BatchedProcessState;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.SmsAccount;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.LogUploadScheduler;
import hu.rgai.yako.eventlogger.ScreenReceiver;
import hu.rgai.yako.handlers.BatchedAsyncTaskHandler;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.handlers.MessageSeenMarkerHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.services.MainService;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.view.activities.AccountSettingsListActivity;
import hu.rgai.yako.view.activities.SystemPreferences;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.tools.IntentParamStrings;
import hu.rgai.yako.workers.BatchedAsyncTaskExecutor;
import hu.rgai.yako.workers.BatchedTimeoutAsyncTask;
import hu.rgai.yako.workers.MessageSeenMarkerAsyncTask;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the main view of the application.
 * 
 * This activity lists the messages in a common list view.
 * 
 * @author Tamas Kojedzinszky
 */
public class MainActivityFragment_F extends Fragment {

  
  public static final int PREFERENCES_REQUEST_CODE = 1;

  // holds the Facebook token
  private static volatile String fbToken = null;

  // holds the activity visibility state
//  private static volatile boolean is_activity_visible = false;

  // this is the adapter for the main view
  private volatile MainListAdapter adapter;

  // receiver for logging screen status
//  private ScreenReceiver screenReceiver;

  // a progress dialog to display message load status
//  private volatile ProgressDialog pd = null;

  // the static listview where messages displayed
  private ListView lv = null;

  // button to load more messages
  private Button loadMoreButton = null;

//  public static Account actSelectedFilter = null;

//  private UncaughtExceptionHandler defaultUEH;
  
//  private Menu mMenu;
  
  private TreeSet<MessageListElement> contextSelectedElements = null;
  
//  private MessageLoadedReceiver mMessageLoadedReceiver = null;
  
  private ProgressBar mTopProgressBar;
  
//  private static final String BATCHED_MESSAGE_MARKER_KEY = "batched_message_marker_key";
  
  private MainActivity_A mMainActivity;

  
  
//  private static final String APPLICATION_START_STR = "application:start";
//  private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
//    @Override
//    public void uncaughtException(Thread thread, Throwable ex) {
//      EventLogger.INSTANCE.writeToLogFile("uncaughtException : " + ex.getMessage() + " " + ex.getLocalizedMessage(), true);
//      // re-throw critical exception further to the os (important)
//      defaultUEH.uncaughtException(thread, ex);
//    }
//  };

//  @Override
//  public void onBackPressed() {
//    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.MAINPAGE.BACKBUTTON_STR, true);
//    super.onBackPressed();
//  }

  public static MainActivityFragment_F newInstance() {
    return null;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mMainActivity = (MainActivity_A)getActivity();
    
    contextSelectedElements = new TreeSet<MessageListElement>();
    
    return null;
  }
  
  
  
  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
//    Tracker t = ((YakoApp)getApplication()).getTracker();
//    t.setScreenName(this.getClass().getName());
//    t.send(new HitBuilders.AppViewBuilder().build());
    
//    contextSelectedElements = new TreeSet<MessageListElement>();
    
    
//    if (!EventLogger.INSTANCE.isLogFileOpen()) {
//      EventLogger.INSTANCE.setContext(this);
//      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
//    }
//    EventLogger.INSTANCE.writeToLogFile(APPLICATION_START_STR + " " + EventLogger.INSTANCE.getAppVersion() + " " + android.os.Build.VERSION.RELEASE, true);

//    LogUploadScheduler.INSTANCE.setContext(this);
//    if (!LogUploadScheduler.INSTANCE.isRunning)
//      LogUploadScheduler.INSTANCE.startRepeatingTask();

//    mMainActivity.getSupportActionBar().setDisplayShowTitleEnabled(false);

    
    // disaplying loading dialog, since the mails are not ready, but the user
//    toggleProgressDialog(true);
    
    
    // load saved filter account
//    actSelectedFilter = StoreHandler.getSelectedFilterAccount(this);
  }
  
  /**
   * Displays a loading progress dialog, which tells the user that messages are
   * loading.
   */
//  private synchronized void toggleProgressDialog(boolean show) {
//    if (show) {
//      pd = new ProgressDialog(this);
//      pd.setMessage(this.getString(R.string.loading));
//      pd.setCancelable(false);
//      pd.show();
//    } else {
//      if (pd != null) {
//        pd.dismiss();
//      }
//    }
//  }


//  /**
//   * Opens Facebook session if exists.
//   * 
//   * @param context
//   */
//  public static void openFbSession(Context context) {
//    if (fbToken == null) {
//      fbToken = StoreHandler.getFacebookAccessToken(context);
//      Date expirationDate = StoreHandler.getFacebookAccessTokenExpirationDate(context);
//      // Log.d( "rgai", "expiration date readed -> " + expirationDate.toString());
//      if (fbToken != null) {
//        Session.openActiveSessionWithAccessToken(context,
//            AccessToken.createFromExistingAccessToken(fbToken, expirationDate, new Date(2013, 1, 1), AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, Settings.getFacebookPermissions()),
//            new Session.StatusCallback() {
//              @Override
//              public void call(Session sn, SessionState ss, Exception excptn) {
//              }
//            });
//      }
//    }
//  }

//  @Override
//  public boolean onCreateOptionsMenu(Menu menu) {
//    MenuInflater inflater = getMenuInflater();
//    inflater.inflate(R.menu.main_settings_menu, menu);
//    mMenu = menu;
//    refreshLoadingIndicatorState();
//    return true;
//  }
  
//  private void refreshLoadingIndicatorState() {
//    if (!BatchedAsyncTaskExecutor.isProgressRunning(MainService.MESSAGE_LIST_QUERY_KEY)) {
//      setRefreshActionButtonState(false);
//    } else {
//      setRefreshActionButtonState(true);
//      if (mMenu != null) {
//        MenuItem refreshItem = mMenu.findItem(R.id.refresh_message_list);
//        if (refreshItem != null && refreshItem.getActionView() != null) {
//          BatchedProcessState ps = BatchedAsyncTaskExecutor.getProgressState(MainService.MESSAGE_LIST_QUERY_KEY);
//          ((TextView)refreshItem.getActionView().findViewById(R.id.refresh_stat)).setText(ps.getProcessDone()+"/"+ps.getTotalProcess());
//        }
//      }
//    }
//  }
  
//  public void setRefreshActionButtonState(boolean refreshing) {
//    if (mMenu != null) {
//      MenuItem refreshItem = mMenu.findItem(R.id.refresh_message_list);
//      if (refreshItem != null) {
//        if (refreshing) {
//          if (refreshItem.getActionView() == null) {
//            refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
//          } else {
//            // do nothing, since we already displaying the progressbar
//          }
//        } else {
//          refreshItem.setActionView(null);
//        }
//      }
//    }
//    if (loadMoreButton != null) {
//      if (refreshing) {
//        loadMoreButton.setEnabled(false);
//      } else {
//        loadMoreButton.setEnabled(true);
//      }
//    }
//  }

//  @Override
//  public boolean onOptionsItemSelected(MenuItem item) {
//    // Handle item selection
//    Intent intent;
//    switch (item.getItemId()) {
//      case R.id.accounts:
//        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_ACCOUNT_BTN, true);
//        intent = new Intent(this, AccountSettingsListActivity.class);
//        startActivity(intent);
//        return true;
//      case R.id.message_send_new:
//        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_MESSAGE_SEND_BTN, true);
//        intent = new Intent(this, MessageReplyActivity.class);
//        startActivity(intent);
//        return true;
//      case R.id.refresh_message_list:
//        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_REFRESH_BTN, true);
//
//        reloadMessages(true);
//        return true;
//      case R.id.system_preferences:
//        Intent i = new Intent(this, SystemPreferences.class);
//        startActivityForResult(i, PREFERENCES_REQUEST_CODE);
//        return true;
//      case R.id.filter_list:
//        showListFilter();
//        return true;
//      default:
//        return super.onOptionsItemSelected(item);
//    }
//  }

//  private void showListFilter() {
//    final TreeSet<Account> allAccount = YakoApp.getAccounts(mMainActivity);
//    final CharSequence[] items = new CharSequence[allAccount.size() + 1];
//    int selectedIndex = 0;
//    items[0] = "All";
//    int i = 0;
//    for (Account acc : allAccount) {
//      String dn = acc.getDisplayName();
//      if (dn == null) {
//        items[i + 1] = acc.getAccountType().toString();
//      } else {
//        items[i + 1] = dn + " (" + acc.getAccountType().toString() + ")";
//      }
//      if (acc.equals(MainActivity.actSelectedFilter)) {
//        selectedIndex = i + 1;
//      }
//      i++;
//    }
//
//    // Creating and Building the Dialog
//    AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
//    builder.setTitle("Filter list");
//    builder.setSingleChoiceItems(items, selectedIndex, new DialogInterface.OnClickListener() {
//      @Override
//      public void onClick(DialogInterface dialog, int item) {
//        if (item == 0) {
//          MainActivity.actSelectedFilter = null;
//        } else {
//          if (allAccount.size() >= item) {
//            int k = 0;
//            for (Account a : allAccount) {
//              if (k < item - 1) {
//                k++;
//                continue;
//              }
//              MainActivity.actSelectedFilter = a;
//              break;
//            }
//          }
//        }
//        Log.d("rgai2", "act selected filter: " + actSelectedFilter);
//        StoreHandler.saveSelectedFilterAccount(mMainActivity, actSelectedFilter);
//        dialog.dismiss();
//        setContent();
//        reloadMessages(false);
//      }
//    });
//    builder.create().show();
//  }

  
  /**
   * Sends a broadcast to the Service to load fresh messages again.
   */
//  private void reloadMessages(boolean forceQuery) {
////    Log.d("rgai", "RELOAD messages");
//    refreshLoadingIndicatorState();
//    Intent intent = new Intent(this, MainScheduler.class);
//    intent.setAction(Context.ALARM_SERVICE);
//    MainServiceExtraParams eParams = new MainServiceExtraParams();
//    if (forceQuery) {
//      eParams.setForceQuery(true);
//    }
//    if (actSelectedFilter != null) {
//      eParams.setAccount(actSelectedFilter);
//    }
//    intent.putExtra(IntentParamStrings.EXTRA_PARAMS, eParams);
//    this.sendBroadcast(intent);
//  }


//  @Override
//  protected void onResume() {
//    super.onResume();
//    is_activity_visible = true;
//    removeNotificationIfExists();
//    refreshLoadingIndicatorrefreshLoadingIndiState();
//    
//    
//    // register broadcast receiver for new message load
//    mMessageLoadedReceiver = new MessageLoadedReceiver();
//    IntentFilter filter = new IntentFilter(MainService.BATCHED_MESSAGE_LIST_TASK_DONE_INTENT);
//    filter.addAction(MessageListerHandler.MESSAGE_PACK_LOADED_INTENT);
//    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageLoadedReceiver, filter);
//    
//    
//    if (!MainService.RUNNING) {
//      reloadMessages(true);
//    } else {
//      long now = System.currentTimeMillis();
//      if (YakoApp.lastFullMessageUpdate == null || YakoApp.lastFullMessageUpdate.getTime() + 1000l * Settings.MESSAGE_LOAD_INTERVAL < now) {
//        reloadMessages(false);
//      }
//    }
//    
//    YakoApp.updateLastNotification(null, this);
//    setUpAndRegisterScreenReceiver();
//
//    setContent();
//    if (!EventLogger.INSTANCE.isLogFileOpen()) {
//      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
//    }
//    LogUploadScheduler.INSTANCE.setContext(this);
//    if (!LogUploadScheduler.INSTANCE.isRunning) {
//      LogUploadScheduler.INSTANCE.startRepeatingTask();
//    }
//    logActivityEvent(EventLogger.LOGGER_STRINGS.MAINPAGE.RESUME_STR);
//  }


  /**
   * Sets up the screen receiver for logging.
   */
//  private void setUpAndRegisterScreenReceiver() {
//    if (screenReceiver == null) {
//      screenReceiver = new ScreenReceiver();
//    }
//
//    IntentFilter screenIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
//    screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
//    registerReceiver(screenReceiver, screenIntentFilter);
//  }

  
//  @Override
//  protected void onDestroy() {
//    super.onDestroy();
//    if (screenReceiver != null) {
//      unregisterReceiver(screenReceiver);
//    }
//  }

  /**
   * Returns true if the main activity is visible.
   * 
   * @return true if main activity visible, false otherwise
   */
//  public static boolean isMainActivityVisible() {
//    return is_activity_visible;
//  }


  public void notifyAdapterChange() {
    adapter.notifyDataSetChanged();
  }
  
  /**
   * Sets the content of the listview.
   */
  public void setContent() {
//    if (!YakoApp.getMessages().isEmpty()) {
//      toggleProgressDialog(false);
//    }
    boolean isListView = getView().findViewById(R.id.list) != null;
    boolean isNet = AndroidUtils.isNetworkAvailable(mMainActivity);
    if (isNet || YakoApp.isPhone) {
      if (!YakoApp.getMessages().isEmpty() && adapter != null && isListView) {
//        adapter.setListFilter(MainActivity_A.actSelectedFilter);
        adapter.notifyDataSetChanged();
      } else if (!YakoApp.getMessages().isEmpty() && !isListView) {
//        setContentView(R.layout.main);
//        lv = (ListView) findViewById(R.id.list);
//        mTopProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lv.setMultiChoiceModeListener(new MultiChoiceModeListener() {

          public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
            if (position != 0) {
              if (checked) {
                contextSelectedElements.add((MessageListElement)adapter.getItem(position));
              } else {
                contextSelectedElements.remove((MessageListElement)adapter.getItem(position));
              }
              if (contextSelectedElements.size() == 1) {
                mode.getMenu().findItem(R.id.reply).setVisible(true);
              } else {
                mode.getMenu().findItem(R.id.reply).setVisible(false);
              }
              mode.setTitle(contextSelectedElements.size() + " selected");
            }
          }

          public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.main_list_context_menu, menu);
            contextSelectedElements.clear();
            return true;
          }

          public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
          }

          public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
              case R.id.reply:
                if (contextSelectedElements.size() == 1) {
                  MessageListElement message = contextSelectedElements.first();
                  Class classToLoad = Settings.getAccountTypeToMessageReplyer().get(message.getAccount().getAccountType());
//                  Intent intent = new Intent(MainActivityFragment_F.this, classToLoad);
//                  intent.putExtra(IntentParamStrings.MESSAGE_ID, message.getId());
//                  intent.putExtra(IntentParamStrings.MESSAGE_ACCOUNT, (Parcelable) message.getAccount());
//                  MainActivityFragment_F.this.startActivity(intent);
                }
                mode.finish();
                return true;
              case R.id.mark_seen:
                contextActionMarkMessage(true);
                mode.finish();
                return true;
              case R.id.mark_unseen:
                contextActionMarkMessage(false);
                mode.finish();
                return true;
              default:
                return false;
            }
          }

          public void onDestroyActionMode(ActionMode mode) {
            // Here you can make any necessary updates to the activity when
            // the CAB is removed. By default, selected items are deselected/unchecked.
          }
        });

//        loadMoreButton = new Button(this);
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
        if (BatchedAsyncTaskExecutor.isProgressRunning(MainService.MESSAGE_LIST_QUERY_KEY)) {
          loadMoreButton.setEnabled(false);
        }

//        adapter = new MainListAdapter(this);
//        adapter.setListFilter(actSelectedFilter);
        lv.setAdapter(adapter);
        lv.setOnScrollListener(new LogOnScrollListener(lv, adapter));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> av, View arg1, int itemIndex, long arg3) {
            if (av.getItemAtPosition(itemIndex) == null) return;
            MessageListElement message = (MessageListElement) av.getItemAtPosition(itemIndex);
            Account a = message.getAccount();
            Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(a.getAccountType());
//            Intent intent = new Intent(MainActivityFragment_F.this, classToLoad);
//            intent.putExtra(IntentParamStrings.MESSAGE_ID, message.getId());
//            intent.putExtra(IntentParamStrings.MESSAGE_ACCOUNT, (Parcelable) message.getAccount());

            boolean changed = YakoApp.setMessageSeenAndReadLocally(message);
            if (changed) {
              message.setSeen(true);
              message.setUnreadCount(0);
              adapter.notifyDataSetChanged();
            }

            loggingOnClickEvent(message, changed);
//            MainActivityFragment_F.this.startActivityForResult(intent, Settings.ActivityRequestCodes.FULL_MESSAGE_RESULT);
          }

          /**
           * Performs a log event when an item clicked on the main view list.
           */
          private void loggingOnClickEvent(MessageListElement message, boolean changed) {
            StringBuilder builder = new StringBuilder();
            appendClickedElementDatasToBuilder(message, builder);
            MainActivityFragment_F.this.appendVisibleElementToStringBuilder(builder, lv, adapter);
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
      } else if (YakoApp.getMessages().isEmpty()) {
//        TextView text = new TextView(this);
//        text.setText(this.getString(R.string.empty_list));
//        text.setGravity(Gravity.CENTER);
//        this.setContentView(text);
      }
    } else {
//      TextView text = new TextView(this);
//      text.setText(this.getString(R.string.no_internet_access));
//      text.setGravity(Gravity.CENTER);
//      setContentView(text);
    }
  }
  
  private void contextActionMarkMessage(boolean seen) {
    
    HashMap<Account, TreeSet<MessageListElement>> messagesToAccounts = new HashMap<Account, TreeSet<MessageListElement>>();
    for (MessageListElement mle : contextSelectedElements) {
      if (!messagesToAccounts.containsKey(mle.getAccount())) {
        messagesToAccounts.put(mle.getAccount(), new TreeSet<MessageListElement>());
      }
      messagesToAccounts.get(mle.getAccount()).add(mle);
    }
    
    // TODO: block auto update while marking messages
    
//    MessageSeenMarkerHandler handler = new MessageSeenMarkerHandler(this);
    List<BatchedTimeoutAsyncTask> tasks = new LinkedList<BatchedTimeoutAsyncTask>();
    for (Map.Entry<Account, TreeSet<MessageListElement>> entry : messagesToAccounts.entrySet()) {
//      MessageProvider mp = AndroidUtils.getMessageProviderInstanceByAccount(entry.getKey(), this);
//      MessageSeenMarkerAsyncTask messageMarker = new MessageSeenMarkerAsyncTask(mp, entry.getValue(), seen, handler);
//      messageMarker.setTimeout(10000);
//      tasks.add(messageMarker);
    }
    mTopProgressBar.setVisibility(View.VISIBLE);
    try {
//      BatchedAsyncTaskExecutor batchedMarker = new BatchedAsyncTaskExecutor(tasks, BATCHED_MESSAGE_MARKER_KEY, new BatchedAsyncTaskHandler() {
//        public void batchedTaskDone(boolean cancelled, String progressKey, BatchedProcessState processState) {
//          if (processState.isDone()) {
//            mTopProgressBar.setVisibility(View.GONE);
//          }
//        }
//      });
//      batchedMarker.execute();
    } catch (Exception ex) {
      Logger.getLogger(MainActivityFragment_F.class.getName()).log(Level.SEVERE, null, ex);
    }
  }


//  /**
//   * This function is called by the main Service, when a new message arrives.
//   * 
//   */
//  private void messegasArrivedToDisplay() {
//    toggleProgressDialog(false);
//    setContent();
//  }

  
  /**
   * Handles LoadMore button press.
   */
  private void loadMoreMessage() {
//    Intent service = new Intent(this, MainScheduler.class);
//    service.setAction(Context.ALARM_SERVICE);
    MainServiceExtraParams eParams = new MainServiceExtraParams();
    eParams.setLoadMore(true);
//    if (actSelectedFilter != null) {
//      eParams.setAccount(actSelectedFilter);
//    }
//    service.putExtra(IntentParamStrings.EXTRA_PARAMS, eParams);
//    sendBroadcast(service);
  }


//  /**
//   * Removes the notification from statusbar if exists.
//   */
//  private void removeNotificationIfExists() {
//    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//    mNotificationManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
//  }

  @Override
  public void onPause() {
    logActivityEvent(EventLogger.LOGGER_STRINGS.MAINPAGE.PAUSE_STR);
    super.onPause();
//    is_activity_visible = false;
    
    
    // unregister new message arrived broadcast
//    LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageLoadedReceiver);

//    Tracker t = ((YakoApp)getApplication()).getTracker();
//    t.setScreenName(this.getClass().getName() + " - pause");
//    t.send(new HitBuilders.AppViewBuilder().build());
    
    // refreshing last notification date when closing activity
//    YakoApp.updateLastNotification(null, this);
  }

  /**
   * Logs event.
   * 
   * @param event the text of log
   */
  private void logActivityEvent(String event) {
    StringBuilder builder = new StringBuilder();
    builder.append(event);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    appendVisibleElementToStringBuilder(builder, lv, adapter);
    Log.d("willrgai", builder.toString());
    EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
  }

  
  private void appendVisibleElementToStringBuilder(StringBuilder builder, ListView lv, MainListAdapter adapter) {
    if (lv == null || adapter == null) {
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      return;
    }
    int firstVisiblePosition = lv.getFirstVisiblePosition();
    int lastVisiblePosition = lv.getLastVisiblePosition();
    // TODO: null pointer exception occures here....
    try {

//      if (actSelectedFilter == null)
//        builder.append(EventLogger.LOGGER_STRINGS.MAINPAGE.ALL_STR);
//      else
//        builder.append(actSelectedFilter.getDisplayName());

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

  private class MessageLoadedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      // this one is responsible for GUI loading indicator update
      if (intent.getAction().equals(MainService.BATCHED_MESSAGE_LIST_TASK_DONE_INTENT)) {
//        MainActivityFragment_F.this.refreshLoadingIndicatorState();
      }
      // this one is responsible for list/data updates
      else if (intent.getAction().equals(MessageListerHandler.MESSAGE_PACK_LOADED_INTENT)) {
//        MainActivityFragment_F.this.messegasArrivedToDisplay();
      }
    }
  }
  
  class LogOnScrollListener implements OnScrollListener {
    final ListView lv;

    final MainListAdapter adapter;

    public LogOnScrollListener(ListView lv, MainListAdapter adapter) {
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
      MainActivityFragment_F.this.appendVisibleElementToStringBuilder(builder, lv, adapter);
      EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
    }

  }
}
