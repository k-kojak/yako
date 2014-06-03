package hu.rgai.android.test;


import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.Session;
import com.facebook.SessionState;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.BatchedProcessState;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.LogUploadScheduler;
import hu.rgai.yako.eventlogger.ScreenReceiver;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.services.MainService;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.IntentParamStrings;
import hu.rgai.yako.view.activities.AccountSettingsListActivity;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.yako.view.activities.SystemPreferences;
import hu.rgai.yako.view.fragments.MainActivityFragment_F;
import hu.rgai.yako.workers.BatchedAsyncTaskExecutor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tamas Kojedzinszky
 */
public class MainActivity_A extends ActionBarActivity {

  public static final int PREFERENCES_REQUEST_CODE = 1;
  
  private DrawerLayout mDrawerLayout;
  private ListView mNavigationView;
  private String[] mMenuItems = null;
  private Class[] mFoodListFragments = null;
  private ActionBarDrawerToggle mDrawerToggle;
  private MainActivityFragment_F mMainActivityFragment = null;
  private ProgressDialog pd = null;
  private static String fbToken = null;
  private static volatile boolean is_activity_visible = false;
  private MessageLoadedReceiver mMessageLoadedReceiver = null;
  private ScreenReceiver screenReceiver;
  private Menu mMenu;
  public static Account actSelectedFilter = null;
  
  
  
  private static final String BATCHED_MESSAGE_MARKER_KEY = "batched_message_marker_key";
  
  
  
  private Thread.UncaughtExceptionHandler defaultUEH;
  private static final String APPLICATION_START_STR = "application:start";
  private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
      EventLogger.INSTANCE.writeToLogFile("uncaughtException : " + ex.getMessage() + " " + ex.getLocalizedMessage(), true);
      // re-throw critical exception further to the os (important)
      defaultUEH.uncaughtException(thread, ex);
    }
  };

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());
    
    setContentView(R.layout.mainlist_navigation_drawer);
    
    
    // logging events
    if (!EventLogger.INSTANCE.isLogFileOpen()) {
      EventLogger.INSTANCE.setContext(this);
      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
    }
    EventLogger.INSTANCE.writeToLogFile(APPLICATION_START_STR + " " + EventLogger.INSTANCE.getAppVersion() + " " + android.os.Build.VERSION.RELEASE, true);
    LogUploadScheduler.INSTANCE.setContext(this);
    if (!LogUploadScheduler.INSTANCE.isRunning) {
      LogUploadScheduler.INSTANCE.startRepeatingTask();
    }
    
    toggleProgressDialog(true);
    
    actSelectedFilter = StoreHandler.getSelectedFilterAccount(this);

    ////////////////////////////////
    
    mMenuItems = new String[]{"a", "b", "c", "d", "e", "f"};

    
    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    mNavigationView = (ListView) findViewById(R.id.left_drawer);

    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    mNavigationView.setAdapter(new ArrayAdapter<String>(this,
            R.layout.mainlist_navigation_drawer_list_item, mMenuItems));
    mNavigationView.setOnItemClickListener(new DrawerItemClickListener());


    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);

    // ActionBarDrawerToggle ties together the proper interactions
    // between the sliding drawer and the action bar app icon
    mDrawerToggle = new ActionBarDrawerToggle(
            this, /* host Activity */
            mDrawerLayout, /* DrawerLayout object */
            R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
            R.string.mainlist_open_drawer, /* "open drawer" description for accessibility */
            R.string.mainlist_close_drawer /* "close drawer" description for accessibility */) {
      @Override
      public void onDrawerClosed(View view) {
        updateTitle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
          invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        }
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        getSupportActionBar().setTitle(getString(R.string.filter_list));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
          invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        }
      }
    };

    mDrawerLayout.setDrawerListener(mDrawerToggle);

    loadFragment(true);
    
  }
  
  protected void onResume() {
    super.onResume();
    is_activity_visible = true;
    removeNotificationIfExists();
    refreshLoadingIndicatorState();
    
    
    // register broadcast receiver for new message load
    mMessageLoadedReceiver = new MessageLoadedReceiver();
    IntentFilter filter = new IntentFilter(MainService.BATCHED_MESSAGE_LIST_TASK_DONE_INTENT);
    filter.addAction(MessageListerHandler.MESSAGE_PACK_LOADED_INTENT);
    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageLoadedReceiver, filter);
    
    
    if (!MainService.RUNNING) {
      reloadMessages(true);
    } else {
      long now = System.currentTimeMillis();
      if (YakoApp.lastFullMessageUpdate == null || YakoApp.lastFullMessageUpdate.getTime() + 1000l * Settings.MESSAGE_LOAD_INTERVAL < now) {
        reloadMessages(false);
      }
    }
    
    YakoApp.updateLastNotification(null, this);
    setUpAndRegisterScreenReceiver();

    setContent();
    if (!EventLogger.INSTANCE.isLogFileOpen()) {
      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
    }
    LogUploadScheduler.INSTANCE.setContext(this);
    if (!LogUploadScheduler.INSTANCE.isRunning) {
      LogUploadScheduler.INSTANCE.startRepeatingTask();
    }
//    logActivityEvent(EventLogger.LOGGER_STRINGS.MAINPAGE.RESUME_STR);
  }

  /**
   * Removes the notification from statusbar if exists.
   */
  private void removeNotificationIfExists() {
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
  }
  
  private void setUpAndRegisterScreenReceiver() {
    if (screenReceiver == null) {
      screenReceiver = new ScreenReceiver();
    }

    IntentFilter screenIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
    registerReceiver(screenReceiver, screenIntentFilter);
  }
  
  public void notifyAdapterChange() {}
  
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
  
  private void messegasArrivedToDisplay() {
    toggleProgressDialog(false);
    setContent();
  }
  
  /**
   * Returns true if the main activity is visible.
   * 
   * @return true if main activity visible, false otherwise
   */
  public static boolean isMainActivityVisible() {
    return is_activity_visible;
  }
  
  private void setContent() {
    if (!YakoApp.getMessages().isEmpty()) {
      toggleProgressDialog(false);
    }
    mMainActivityFragment.setContent();
  }
  
  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (screenReceiver != null) {
      unregisterReceiver(screenReceiver);
    }
  }
  
  
  private void reloadMessages(boolean forceQuery) {
//    Log.d("rgai", "RELOAD messages");
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
  
  private void loadFragment(boolean onCreateView) {
    
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

    if (makeTransaction) {
      MainActivityFragment_F maf = MainActivityFragment_F.newInstance();
//        maf.setTitle(title);
      mMainActivityFragment = maf;
      fragmentManager.beginTransaction().replace(R.id.content_frame, mMainActivityFragment).commit();
    }

    mDrawerLayout.closeDrawer(mNavigationView);
    
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_settings_menu, menu);
    mMenu = menu;
    refreshLoadingIndicatorState();
    return true;
  }
  
  private void refreshLoadingIndicatorState() {
    if (!BatchedAsyncTaskExecutor.isProgressRunning(MainService.MESSAGE_LIST_QUERY_KEY)) {
//      mMainActivityFragment.setRefreshActionButtonState(false);
    } else {
//      mMainActivityFragment.setRefreshActionButtonState(true);
      if (mMenu != null) {
        MenuItem refreshItem = mMenu.findItem(R.id.refresh_message_list);
        if (refreshItem != null && refreshItem.getActionView() != null) {
          BatchedProcessState ps = BatchedAsyncTaskExecutor.getProgressState(MainService.MESSAGE_LIST_QUERY_KEY);
          ((TextView)refreshItem.getActionView().findViewById(R.id.refresh_stat)).setText(ps.getProcessDone()+"/"+ps.getTotalProcess());
        }
      }
    }
  }
  
  private synchronized void toggleProgressDialog(boolean show) {
    if (show) {
      pd = new ProgressDialog(this);
      pd.setMessage(this.getString(R.string.loading));
      pd.setCancelable(false);
      pd.show();
    } else {
      if (pd != null) {
        pd.dismiss();
      }
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    mDrawerToggle.syncState();
  }
  
  @Override
  public void onBackPressed() {
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.MAINPAGE.BACKBUTTON_STR, true);
    super.onBackPressed();
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
//      case R.id.filter_list:
//        showListFilter();
//        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

//  public static String getCartTitleText(Context c, boolean withCartText) {
//    
//  }
  public static String getCartTitleText(Context c, boolean withCartText) {
    String title = null;

    int pizzaNum = 0;
    int price = 0;
//    for (PizzaOrderItem poi : cart.values()) {
//      pizzaNum += poi.getAmount();
//      price += poi.getAmount() * poi.getSizePrice().price;
//    }
    if (pizzaNum > 0) {
//      String pizzaStr = pizzaNum == 1 ? c.getString(R.string.one_item) : c.getString(R.string.multiple_items);
//      title = pizzaNum + " " + pizzaStr + ", " + price + " Ft";
//    } else {
//      title = c.getString(R.string.empty);
    }

    if (withCartText) {
//      title = c.getString(R.string.cart) + ": " + title;
    }

    return title;
  }

  public void updateTitle() {
    getSupportActionBar().setTitle(getCartTitleText(this, true));
  }

  public void removeCurrentFragment() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    if (mMainActivityFragment != null) {
      fragmentManager.beginTransaction().remove(mMainActivityFragment).commit();
    }
  }

  private class MessageLoadedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      // this one is responsible for GUI loading indicator update
//      if (intent.getAction().equals(MainService.BATCHED_MESSAGE_LIST_TASK_DONE_INTENT)) {
//        MainActivity_A.this.refreshLoadingIndicatorState();
//      }
      // this one is responsible for list/data updates
//      else if (intent.getAction().equals(MessageListerHandler.MESSAGE_PACK_LOADED_INTENT)) {
//        MainActivity_A.this.messegasArrivedToDisplay();
//      }
    }
  }
  
  private class DrawerItemClickListener implements ListView.OnItemClickListener {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      mNavigationView.setItemChecked(position, true);
      mDrawerLayout.closeDrawer(mNavigationView);
      loadFragment(false);
    }
  }
}
