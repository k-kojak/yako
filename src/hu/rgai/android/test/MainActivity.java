package hu.rgai.android.test;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.Session;
import com.facebook.SessionState;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.adapters.MainListAdapter;
import hu.rgai.yako.adapters.MainListDrawerFilterAdapter;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.BatchedProcessState;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.LogUploadScheduler;
import hu.rgai.yako.eventlogger.ScreenReceiver;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.services.MainService;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.tools.IntentParamStrings;
import hu.rgai.yako.view.activities.AccountSettingsListActivity;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.yako.view.activities.SystemPreferences;
import hu.rgai.yako.view.fragments.MainActivityFragment;
import hu.rgai.yako.workers.BatchedAsyncTaskExecutor;
import java.util.Date;
import java.util.TreeSet;

/**
 * @author Tamas Kojedzinszky
 */
public class MainActivity extends ActionBarActivity {

  private DrawerLayout mDrawerLayout;
  private ListView mDrawerList;
  private ActionBarDrawerToggle mDrawerToggle;
  private MainActivityFragment mFragment = null;
  private MainListDrawerFilterAdapter mDrawerFilterAdapter = null;
  private boolean mDrawerIsVisible = false;
  private ProgressDialog pd = null;
  private MessageLoadedReceiver mMessageLoadedReceiver = null;
  private Menu mMenu;
  private ScreenReceiver screenReceiver;
  
  

  private static volatile String fbToken = null;
  public static Account actSelectedFilter = null;
  private static volatile boolean is_activity_visible = false;
  public static final String BATCHED_MESSAGE_MARKER_KEY = "batched_message_marker_key";
  public static final int PREFERENCES_REQUEST_CODE = 1;
  
  
  

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());
    
    
    // LOGGING
    setUpAndRegisterScreenReceiver();
    if (!EventLogger.INSTANCE.isLogFileOpen()) {
      EventLogger.INSTANCE.setContext(this);
      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
    }
    EventLogger.INSTANCE.writeToLogFile(APPLICATION_START_STR + " " + EventLogger.INSTANCE.getAppVersion() + " " + android.os.Build.VERSION.RELEASE, true);
    LogUploadScheduler.INSTANCE.setContext(this);
    if (!LogUploadScheduler.INSTANCE.isRunning) {
      LogUploadScheduler.INSTANCE.startRepeatingTask();
    }
    
    
    
    setContentView(R.layout.mainlist_navigation_drawer);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);

    
    actSelectedFilter = StoreHandler.getSelectedFilterAccount(this);
//    TreeSet<Account> accounts = YakoApp.getAccounts(this);
    
    mDrawerList = (ListView) findViewById(R.id.left_drawer);
    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    mFragment = loadFragment(true);
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    
    
    // the 0th element is "all account"
//    int indexOfAccount = 0;
//    if (actSelectedFilter != null) {
//      // +1 needed because 0th element in adapter is "all account"
//      indexOfAccount = AndroidUtils.getIndexOfAccount(accounts, actSelectedFilter) + 1;
//    }
    
    mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
//    mDrawerList.setItemChecked(indexOfAccount, true);

    

    // ActionBarDrawerToggle ties together the proper interactions
    // between the sliding drawer and the action bar app icon
    mDrawerToggle = new ActionBarDrawerToggle(
            this, /* host Activity */
            mDrawerLayout, /* DrawerLayout object */
            R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
            R.string.mainlist_open_drawer, /* "open drawer" description for accessibility */
            R.string.mainlist_close_drawer /* "close drawer" description for accessibility */) {
              
      float mPreviousOffset = 0f;
              
      @Override
      public void onDrawerClosed(View view) {
        super.onDrawerClosed(view);
        showHideMenuBarIcons(false);
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        showHideMenuBarIcons(true);
      }
      
      @Override
      public void onDrawerSlide(View arg0, float slideOffset) {
        super.onDrawerSlide(arg0, slideOffset);
        Log.d("rgai", "slideoffset: " + slideOffset);
        if (!mDrawerIsVisible && slideOffset > 0.0) {
          showHideMenuBarIcons(true);
        } else if (mDrawerIsVisible && slideOffset == 0.0) {
          showHideMenuBarIcons(false);
        }
      }
      
      private void showHideMenuBarIcons(boolean hide) {
        if (hide) {
          mDrawerIsVisible = true;
          getSupportActionBar().setTitle(getString(R.string.filter_list));
          invalidateOptionsMenu();
        } else {
          mDrawerIsVisible = false;
          getSupportActionBar().setTitle("");
          invalidateOptionsMenu();
        }
      }
    };

    mDrawerLayout.setDrawerListener(mDrawerToggle);
    
    if (!YakoApp.hasMessages()) {
      toggleProgressDialog(true);
    }
  }

  
  @Override
  protected void onResume() {
    super.onResume();
    is_activity_visible = true;
    
    // setting filter adapter onResume, because it might change at settings panel
    TreeSet<Account> accounts = YakoApp.getAccounts(this);
    mDrawerFilterAdapter = new MainListDrawerFilterAdapter(this, accounts);
    mDrawerList.setAdapter(mDrawerFilterAdapter);
    int indexOfAccount = 0;
    if (actSelectedFilter != null) {
      // +1 needed because 0th element in adapter is "all account"
      indexOfAccount = AndroidUtils.getIndexOfAccount(accounts, actSelectedFilter);
      // the saved selected account is not available anymore...
      if (indexOfAccount == -1) {
        actSelectedFilter = null;
      }
      indexOfAccount++;
    }
    mDrawerList.setItemChecked(indexOfAccount, true);
    
    
    // notify the adapter if any change happened
    mFragment.notifyAdapterChange();
    
    
    // register broadcast receiver for new message load
    mMessageLoadedReceiver = new MessageLoadedReceiver();
    IntentFilter filter = new IntentFilter(MainService.BATCHED_MESSAGE_LIST_TASK_DONE_INTENT);
    filter.addAction(MessageListerHandler.MESSAGE_PACK_LOADED_INTENT);
    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageLoadedReceiver, filter);
    
    
    // loading messages
    if (!MainService.RUNNING) {
      reloadMessages(true);
    } else {
      long now = System.currentTimeMillis();
      if (YakoApp.lastFullMessageUpdate == null || YakoApp.lastFullMessageUpdate.getTime() + 1000l * Settings.MESSAGE_LOAD_INTERVAL < now) {
        reloadMessages(false);
      }
    }
    
    YakoApp.updateLastNotification(null, this);
    
    refreshLoadingIndicatorState();
  }
  
  @Override
  protected void onPause() {
    
    is_activity_visible = false;
    
    
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageLoadedReceiver);
    
    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName() + " - pause");
    t.send(new HitBuilders.AppViewBuilder().build());
    
// refreshing last notification date when closing activity
    YakoApp.updateLastNotification(null, this);
    Log.d("rgai", "on mainactivity pause");
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // logging
    if (screenReceiver != null) {
      unregisterReceiver(screenReceiver);
    }
  }
  
  
  
  @Override
  public void onBackPressed() {
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.MAINPAGE.BACKBUTTON_STR, true);
    super.onBackPressed();
  }
  
  
  public void loadMoreMessage() {
    Intent service = new Intent(this, MainScheduler.class);
    service.setAction(Context.ALARM_SERVICE);
    MainServiceExtraParams eParams = new MainServiceExtraParams();
    eParams.setLoadMore(true);
    if (actSelectedFilter != null) {
      eParams.setAccount(actSelectedFilter);
    }
    service.putExtra(IntentParamStrings.EXTRA_PARAMS, eParams);
    sendBroadcast(service);
  }
  
  
  /**
   * Returns true if the main activity is visible.
   * 
   * @return true if main activity visible, false otherwise
   */
  public static boolean isMainActivityVisible() {
    return is_activity_visible;
  }
  
  
  public synchronized void toggleProgressDialog(boolean show) {
    if (show) {
      if (pd == null) {
        pd = new ProgressDialog(this);
        pd.setCancelable(false);
      }
      pd.show();
      pd.setContentView(R.layout.progress_dialog);
    } else {
      if (pd != null) {
        pd.dismiss();
      }
    }
  }

  private MainActivityFragment loadFragment(boolean onCreateView) {
    
    MainActivityFragment fragment = null;
    
      FragmentManager fragmentManager = getSupportFragmentManager();
      boolean makeTransaction = false;
      if (onCreateView && fragmentManager.getFragments() != null && !fragmentManager.getFragments().isEmpty()) {
        makeTransaction = false;
      } else if (onCreateView && fragmentManager.getFragments() == null) {
        makeTransaction = true;
      } else if (!onCreateView && fragmentManager.getFragments() == null) {
        makeTransaction = true;
      } else if (!onCreateView && fragmentManager.getFragments() != null && !fragmentManager.getFragments().isEmpty()) {
        makeTransaction = false;
      }
      
      Log.d("rgai", "loading fragment?? -> " + makeTransaction);
      
      if (makeTransaction) {
//        Constructor constructor = fragmentClass.getConstructor();
//        FoodListFragment flf = (FoodListFragment) constructor.newInstance();
////        flf.setTitle(title);
//        mAttachedFragment = (Fragment) flf;
        fragment = MainActivityFragment.getInstance();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
      } else {
        fragment = (MainActivityFragment) fragmentManager.getFragments().get(0);
      }
      
//      mDrawerList.setItemChecked(position, true);
      mDrawerLayout.closeDrawer(mDrawerList);
      return fragment;
  }

  
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    for (int i = 0; i < menu.size(); i++) {
      menu.getItem(i).setVisible(!mDrawerIsVisible);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_settings_menu, menu);
    mMenu = menu;
    refreshLoadingIndicatorState();
    return true;
  }

  
  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    mDrawerToggle.syncState();
  }

  
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Pass any configuration change to the drawer toggls
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // The action bar home/up action should open or close the drawer.
    // ActionBarDrawerToggle will take care of this.
    if (mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }
    
    Intent intent;
    switch (item.getItemId()) {
      case R.id.accounts:
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_ACCOUNT_BTN, true);
        intent = new Intent(this, AccountSettingsListActivity.class);
        startActivity(intent);
        return true;
      case R.id.message_send_new:
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_MESSAGE_SEND_BTN, true);
        intent = new Intent(this, MessageReplyActivity.class);
        startActivity(intent);
        return true;
      case R.id.refresh_message_list:
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_REFRESH_BTN, true);

        reloadMessages(true);
        return true;
      case R.id.system_preferences:
        Intent i = new Intent(this, SystemPreferences.class);
        startActivityForResult(i, PREFERENCES_REQUEST_CODE);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  
  private void refreshLoadingIndicatorState() {
    if (!BatchedAsyncTaskExecutor.isProgressRunning(MainService.MESSAGE_LIST_QUERY_KEY)) {
      setRefreshActionButtonState(false);
    } else {
      setRefreshActionButtonState(true);
      if (mMenu != null) {
        MenuItem refreshItem = mMenu.findItem(R.id.refresh_message_list);
        if (refreshItem != null && refreshItem.getActionView() != null) {
          BatchedProcessState ps = BatchedAsyncTaskExecutor.getProgressState(MainService.MESSAGE_LIST_QUERY_KEY);
          ((TextView)refreshItem.getActionView().findViewById(R.id.refresh_stat)).setText(ps.getProcessDone()+"/"+ps.getTotalProcess());
        }
      }
    }
  }
  
  
  public void setRefreshActionButtonState(boolean refreshInProgress) {
    if (mMenu != null) {
      MenuItem refreshItem = mMenu.findItem(R.id.refresh_message_list);
      if (refreshItem != null) {
        if (refreshInProgress) {
          if (refreshItem.getActionView() == null) {
            refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
          } else {
            // do nothing, since we already displaying the progressbar
          }
        } else {
          refreshItem.setActionView(null);
        }
      }
    }
    mFragment.loadStateChanged(refreshInProgress);
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
                AccessToken.createFromExistingAccessToken(fbToken, expirationDate, new Date(2013, 1, 1),
                        AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, Settings.getFacebookPermissions()),
                new Session.StatusCallback() {
                  @Override
                  public void call(Session sn, SessionState ss, Exception excptn) {}
                }
        );
      }
    }
  }
  
  
  private void messegasArrivedToDisplay() {
    toggleProgressDialog(false);
    Log.d("rgai", "mFragment object: " + mFragment);
    mFragment.notifyAdapterChange();
  }


  /**
   * Sending request to load messages.
   * 
   * @param forceQuery true if load every message's account false otherwise
   */
  private void reloadMessages(boolean forceQuery) {
    refreshLoadingIndicatorState();
    Intent intent = new Intent(this, MainScheduler.class);
    intent.setAction(Context.ALARM_SERVICE);
    MainServiceExtraParams eParams = new MainServiceExtraParams();
    if (forceQuery) {
      eParams.setForceQuery(true);
    }
    if (actSelectedFilter != null) {
      eParams.setAccount(actSelectedFilter);
    }
    intent.putExtra(IntentParamStrings.EXTRA_PARAMS, eParams);
    this.sendBroadcast(intent);
  }
  
  
  private class MessageLoadedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      // this one is responsible for GUI loading indicator update
      if (intent.getAction().equals(MainService.BATCHED_MESSAGE_LIST_TASK_DONE_INTENT)) {
        MainActivity.this.refreshLoadingIndicatorState();
      }
      // this one is responsible for list/data updates
      else if (intent.getAction().equals(MessageListerHandler.MESSAGE_PACK_LOADED_INTENT)) {
        Log.d("rgai", "mainactivity.this: " + MainActivity.this);
        MainActivity.this.messegasArrivedToDisplay();
      }
    }
  }

  
  
  
  

  private class DrawerItemClickListener implements ListView.OnItemClickListener {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      mDrawerList.setItemChecked(position, true);
      mDrawerLayout.closeDrawer(mDrawerList);
      actSelectedFilter = (Account)parent.getItemAtPosition(position);
      StoreHandler.saveSelectedFilterAccount(MainActivity.this, actSelectedFilter);
      mFragment.notifyAdapterChange();
      reloadMessages(false);
    }
  }
  
  
  
  // LOGGING EVENTS
  public void appendVisibleElementToStringBuilder(StringBuilder builder, ListView lv, MainListAdapter adapter) {
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
  

  
  // logging stuff
  
  private void setUpAndRegisterScreenReceiver() {
    if (screenReceiver == null) {
      screenReceiver = new ScreenReceiver();
    }

    IntentFilter screenIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
    registerReceiver(screenReceiver, screenIntentFilter);
  }
  
  private static final String APPLICATION_START_STR = "application:start";
  private Thread.UncaughtExceptionHandler defaultUEH;
  private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
      EventLogger.INSTANCE.writeToLogFile("uncaughtException : " + ex.getMessage() + " " + ex.getLocalizedMessage(), true);
      // re-throw critical exception further to the os (important)
      defaultUEH.uncaughtException(thread, ex);
    }
  };
  
}
