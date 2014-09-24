package hu.rgai.android.test;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.Session;
import com.facebook.SessionState;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import hu.rgai.yako.adapters.ZoneListAdapter;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.eventlogger.AccelerometerListener;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.adapters.MainListAdapter;
import hu.rgai.yako.adapters.MainListDrawerFilterAdapter;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.eventlogger.LogUploadScheduler;
import hu.rgai.yako.eventlogger.ScreenReceiver;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.location.LocationChangeListener;
import hu.rgai.yako.services.MainService;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.GpsZoneDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.view.activities.AccountSettingsListActivity;
import hu.rgai.yako.view.activities.GoogleMapsActivity;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.yako.view.activities.SystemPreferences;
import hu.rgai.yako.view.extensions.LinearListView;
import hu.rgai.yako.view.fragments.MainActivityFragment;
import hu.rgai.yako.workers.BatchedAsyncTaskExecutor;

import java.util.*;


/**
 * @author Tamas Kojedzinszky
 */
public class MainActivity extends ActionBarActivity {

  private List<GpsZone> mGpsZones = null;
  private Location mMyLastLocation = null;

  private DrawerLayout mDrawerLayout;
  private LinearListView mAccountHolder;
  private LinearListView mZoneHolder;
  private MainListDrawerFilterAdapter mAccountFilterAdapter = null;
  private ZoneListAdapter mZoneListAdapter = null;

  private TextView mAddGpsZone;
  private View mDrawerWrapper;
  private ActionBarDrawerToggle mDrawerToggle;
  private MainActivityFragment mFragment = null;

  private boolean mDrawerIsVisible = false;
  private ProgressDialog pd = null;
  private MessageLoadedReceiver mMessageLoadedReceiver = null;
  private LocationChangeReceiver mLocationChangeReceiver = null;
  private Menu mMenu;
  private ScreenReceiver screenReceiver;
  private TextView mEmptyListText = null;
  private TreeMap<Long, Account> mAccountsLongKey = null;
  
  

  private static volatile String fbToken = null;
  public static Account actSelectedFilter = null;
  private static volatile boolean is_activity_visible = false;
  public static final String BATCHED_MESSAGE_MARKER_KEY = "batched_message_marker_key";

  public static final int PREFERENCES_REQUEST_CODE = 1;
  public static final int G_MAPS_ACTIVITY_REQUEST_CODE = 2;
  private LayoutInflater mInflater;
  
  
  

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());


    mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    
    // LOGGING
    setUpAndRegisterScreenReceiver();
    if (!EventLogger.INSTANCE.isLogFileOpen()) {
      EventLogger.INSTANCE.setContext(this);
      EventLogger.INSTANCE.openAllLogFile();
    }
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH , APPLICATION_START_STR + " " + EventLogger.INSTANCE.getAppVersion() + " " + android.os.Build.VERSION.RELEASE, true);
    LogUploadScheduler.INSTANCE.setContext(this);
    if (!LogUploadScheduler.INSTANCE.isRunning) {
      LogUploadScheduler.INSTANCE.startRepeatingTask();
    }
//    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//    sensorManager.registerListener(new AccelerometerListener(),
//    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    mLocationChangeReceiver = new LocationChangeReceiver();
    
    
    setContentView(R.layout.mainlist_navigation_drawer);
    
    mEmptyListText = new TextView(this);
    mEmptyListText.setText(this.getString(R.string.empty_list));
    mEmptyListText.setGravity(Gravity.CENTER);
    mEmptyListText.setVisibility(View.GONE);
    ((FrameLayout)findViewById(R.id.content_frame)).addView(mEmptyListText);
    
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);

    
    actSelectedFilter = StoreHandler.getSelectedFilterAccount(this);
    
    mDrawerWrapper = findViewById(R.id.drawer_wrapper);
    mAccountHolder = (LinearListView) findViewById(R.id.account_holder);
    mAccountHolder.setIsSingleSelect(true);

    mZoneHolder = (LinearListView) findViewById(R.id.zone_holder);
    mAddGpsZone = (TextView) findViewById(R.id.add_gps_zone);
    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    setContent(MessageListDAO.getInstance(this).getAllMessagesCount() != 0 ? true : null);
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    
    
    mAccountHolder.setOnItemClickListener(new DrawerItemClickListener());

    mAddGpsZone.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivityForResult(new Intent(MainActivity.this, GoogleMapsActivity.class), G_MAPS_ACTIVITY_REQUEST_CODE);
      }
    });

    

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
          setTitleByFilter();
          invalidateOptionsMenu();
        }
      }
    };

    mDrawerLayout.setDrawerListener(mDrawerToggle);
    
  }


  @Override
  protected void onResume() {
    super.onResume();
    float[] f = new float[3];
    Location.distanceBetween(0.0000f, 0.00000f, 1.00000f, 0.5f, f);
    Log.d("yako", Arrays.toString(f));
    is_activity_visible = true;
    removeNotificationIfExists();

    mAccountsLongKey = AccountDAO.getInstance(this).getIdToAccountsMap();
    

    // setting zone list
    loadZoneListAdapter(true);
    setActiveLocation();


//    // setting filter adapter onResume, because it might change at settings panel
    setAccountList();


    // setting title
    setTitleByFilter();
    
    

    // register broadcast receiver for new message load
    mMessageLoadedReceiver = new MessageLoadedReceiver();
    IntentFilter filter = new IntentFilter(MainService.BATCHED_MESSAGE_LIST_TASK_DONE_INTENT);
    filter.addAction(MessageListerHandler.MESSAGE_PACK_LOADED_INTENT);
    filter.addAction(MainService.NO_TASK_AVAILABLE_TO_PROCESS);
    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageLoadedReceiver, filter);



    // register broadcast for receive location change pending intents
    IntentFilter locFilter = new IntentFilter(LocationChangeListener.ACTION_LOCATION_CHANGED);
    registerReceiver(mLocationChangeReceiver, locFilter);
    LocationChangeListener.INSTANCE.initLocationManager(this);
    
    
    // loading messages
    if (!MainService.RUNNING) {
      reloadMessages(true);
    } else {
      long now = System.currentTimeMillis();
      if (actSelectedFilter == null
              && (YakoApp.lastFullMessageUpdate == null || YakoApp.lastFullMessageUpdate.getTime() + 1000l * Settings.MESSAGE_LOAD_INTERVAL < now)) {
        reloadMessages(false);
      }
    }
    
    YakoApp.updateLastNotification(null, this);
    
    refreshLoadingIndicatorState();
    
    logActivityEvent(EventLogger.LOGGER_STRINGS.MAINPAGE.RESUME_STR);


    if (!StoreHandler.isMessageForDatabaseSorryDisplayed(this)) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          StoreHandler.setIsMessageForDatabaseSorryDisplayed(MainActivity.this);
        }
      });
      builder.setTitle("Backend changes");
      builder.setMessage("Due to a bigger code refactor, your account settings are lost.\n" +
              "Please set them again and apologize for the inconvenience.").show();
    }
  }

  private void setAccountList() {
    TreeSet<Account> accounts = new TreeSet<Account>(mAccountsLongKey.values());
    mAccountFilterAdapter = new MainListDrawerFilterAdapter(this, accounts);
    mAccountHolder.setAdapter(mAccountFilterAdapter);
    int indexOfAccount = 0;
    if (actSelectedFilter != null) {
      // +1 needed because 0th element in adapter is "all instance"
      indexOfAccount = AndroidUtils.getIndexOfAccount(accounts, actSelectedFilter);
      // the saved selected instance is not available anymore...
      if (indexOfAccount == -1) {
        actSelectedFilter = null;
      }
      indexOfAccount++;
    }
    mAccountHolder.setItemChecked(indexOfAccount, true);
  }

  public void loadZoneListAdapter(boolean refreshFromDatabase) {
    if (refreshFromDatabase) {
      mGpsZones = GpsZoneDAO.getInstance(this).getAllZones();
    }
    setActiveLocation();
    mZoneListAdapter = new ZoneListAdapter(this, mGpsZones);
    mZoneHolder.setAdapter(mZoneListAdapter);
  }

  @Override
  protected void onPause() {
    
    is_activity_visible = false;
    logActivityEvent(EventLogger.LOGGER_STRINGS.MAINPAGE.PAUSE_STR);
    
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageLoadedReceiver);
    unregisterReceiver(mLocationChangeReceiver);
    
    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName() + " - pause");
    t.send(new HitBuilders.AppViewBuilder().build());
    
// refreshing last notification date when closing activity
    YakoApp.updateLastNotification(null, this);
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
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.MAINPAGE.BACKBUTTON_STR, true);
    super.onBackPressed();
  }

  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == Settings.ActivityRequestCodes.FULL_MESSAGE_RESULT) {
      if (data != null && data.hasExtra(IntentStrings.Params.MESSAGE_THREAD_CHANGED)) {
        boolean refreshNeeded = data.getBooleanExtra(IntentStrings.Params.MESSAGE_THREAD_CHANGED, false);
        if (refreshNeeded) {
          Log.d("rgai", "REFRESH NEEDED!!!!!");
          Account a = data.getParcelableExtra(IntentStrings.Params.ACCOUNT);
          Intent intent = new Intent(this, MainScheduler.class);
          intent.setAction(Context.ALARM_SERVICE);
          MainServiceExtraParams eParams = new MainServiceExtraParams();
          eParams.setForceQuery(true);
          eParams.setAccount(a);
          eParams.setQueryOffset(0);

          TreeMap<Account, Long> accountsAccountKey = AccountDAO.getInstance(this).getAccountToIdMap();
          long accountId = accountsAccountKey.get(a);
          eParams.setQueryLimit(MessageListDAO.getInstance(this).getAllMessagesCount(accountId));
          intent.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
          this.sendBroadcast(intent);
        } else {
//          Log.d("rgai", "refresh not .... needed!!!!!");
        }
      }
    }
  }
  
  
  private void setTitleByFilter() {
    if (actSelectedFilter == null) {
      getSupportActionBar().setTitle("");
    } else {
      getSupportActionBar().setTitle("Filter on");
    }

    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    sensorManager.registerListener(new AccelerometerListener(),
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    
    
  }
  
  
  private void setContent(Boolean hasData) {
    // null means we dont know yet: called on onCreate
    if (hasData == null) {
      toggleProgressDialog(true);
    } else {
      toggleProgressDialog(false);
      if (hasData) {
        mEmptyListText.setVisibility(View.GONE);
        mFragment = loadFragment();
      } else {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getFragments() != null && !fragmentManager.getFragments().isEmpty()) {
          FragmentTransaction ft = fragmentManager.beginTransaction();
          for (Fragment f : fragmentManager.getFragments()) {
            if (f != null) {
              ft.remove(f);
            }
          }
          ft.commit();
          fragmentManager.executePendingTransactions();
        }
        mEmptyListText.setVisibility(View.VISIBLE);
        mFragment = null;
      }
    }
  }
  
  
  public void loadMoreMessage() {
    Intent service = new Intent(this, MainScheduler.class);
    service.setAction(Context.ALARM_SERVICE);
    MainServiceExtraParams eParams = new MainServiceExtraParams();
    eParams.setLoadMore(true);
    if (actSelectedFilter != null) {
      eParams.setAccount(actSelectedFilter);
    }
    service.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
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
  
  
  /**
   * Removes the notification from statusbar if exists.
   */
  private void removeNotificationIfExists() {
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);
  }
  
  
  public synchronized void toggleProgressDialog(boolean show) {
    if (show) {
      if (pd == null) {
        pd = new ProgressDialog(this);
        pd.setCancelable(true);
      }
      pd.show();
      pd.setContentView(R.layout.progress_dialog);
    } else {
      if (pd != null) {
        pd.dismiss();
      }
    }
  }

  private MainActivityFragment loadFragment() {
    
    MainActivityFragment fragment = null;
    
      FragmentManager fragmentManager = getSupportFragmentManager();
      boolean makeTransaction = false;
      if (fragmentManager.getFragments() != null && !fragmentManager.getFragments().isEmpty()) {
        makeTransaction = false;
      } else {
        makeTransaction = true;
      }
      
      if (makeTransaction) {
        fragment = MainActivityFragment.getInstance();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        fragmentManager.executePendingTransactions();
      } else {
        fragment = (MainActivityFragment) fragmentManager.getFragments().get(0);
      }
      
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
        EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.CLICK.CLICK_ACCOUNT_BTN, true);
        intent = new Intent(this, AccountSettingsListActivity.class);
        startActivity(intent);
        return true;
      case R.id.message_send_new:
        EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.CLICK.CLICK_MESSAGE_SEND_BTN, true);
        intent = new Intent(this, MessageReplyActivity.class);
        startActivity(intent);
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
//      if (mMenu != null) {
//        MenuItem refreshItem = mMenu.findItem(R.id.refresh_message_list);
//        if (refreshItem != null && refreshItem.getActionView() != null) {
//          BatchedProcessState ps = BatchedAsyncTaskExecutor.getProgressState(MainService.MESSAGE_LIST_QUERY_KEY);
//          ((TextView)refreshItem.getActionView().findViewById(R.id.refresh_stat)).setText(ps.getProcessDone()+"/"+ps.getTotalProcess());
//        }
//      }
    }
  }
  
  
  public void setRefreshActionButtonState(boolean refreshInProgress) {
//    if (mMenu != null) {
//      MenuItem refreshItem = mMenu.findItem(R.id.refresh_message_list);
//      if (refreshItem != null) {
//        if (refreshInProgress) {
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
    if (mFragment != null) {
      mFragment.loadStateChanged(refreshInProgress);
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
    setContent(true);
    toggleProgressDialog(false);
    mFragment.notifyAdapterChange();
  }


  /**
   * Sending request to load messages.
   * 
   * @param forceQuery true if load every message's instance false otherwise
   */
  public void reloadMessages(boolean forceQuery) {
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
    intent.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
    this.sendBroadcast(intent);
  }

  private void setActiveLocation() {
    if (mMyLastLocation != null) {
      Log.d("yako", "lat, long: " + mMyLastLocation.getLatitude() + ", " + mMyLastLocation.getLongitude());
      Log.d("yako", "time: " + new Date(mMyLastLocation.getTime()));

      Set<String> nearLocationList = new TreeSet<String>();
      String closestLoc = null;
      float closest = Float.MAX_VALUE;
      for (GpsZone zone : mGpsZones) {
        float distance = getDist((float) zone.getLat(), (float) zone.getLong(),
                (float) mMyLastLocation.getLatitude(), (float) mMyLastLocation.getLongitude());
        zone.setProximity(GpsZone.Proximity.FAR);
        if (distance <= zone.getRadius()) {
          nearLocationList.add(zone.getAlias());
          if (distance < closest) {
            closest = distance;
            closestLoc = zone.getAlias();
          }
        }
      }

      for (GpsZone zone : mGpsZones) {
        if (zone.getAlias().equals(closestLoc)) {
          zone.setProximity(GpsZone.Proximity.CLOSEST);
        } else if (nearLocationList.contains(zone.getAlias())) {
          zone.setProximity(GpsZone.Proximity.NEAR);
        }
      }
    }
  }

  private float getDist(float x1, float y1, float x2, float y2) {
    float[] dist = new float[2];
    Location.distanceBetween(x1, y1, x2, y2, dist);
    return dist[0];
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
        MainActivity.this.messegasArrivedToDisplay();
      }
      // if no task available to do at service
      else if (intent.getAction().equals(MainService.NO_TASK_AVAILABLE_TO_PROCESS)) {
        MainActivity.this.setContent(MessageListDAO.getInstance(MainActivity.this).getAllMessagesCount() != 0);
      }
    }
  }


  private class LocationChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      // this one is responsible for GUI loading indicator update
      if (intent.getAction().equals(LocationChangeListener.ACTION_LOCATION_CHANGED)) {
        mMyLastLocation = (Location) intent.getExtras().get("location");
        loadZoneListAdapter(false);
      }
    }

  }



  private class DrawerItemClickListener implements LinearListView.OnItemClickListener {

    @Override
    public void onItemClick(Object item, int position) {
      mAccountHolder.setItemChecked(position, true);
      mDrawerLayout.closeDrawer(mDrawerWrapper);
      actSelectedFilter = (Account)item;
      StoreHandler.saveSelectedFilterAccount(MainActivity.this, actSelectedFilter);
      if (mFragment != null) {
        mFragment.hideContextualActionbar();
        mFragment.notifyAdapterChange();
      }

      // run query for selected filter only if list is empty OR selected all accounts

      long accountId = -1;
      if (actSelectedFilter != null) {
        TreeMap<Account, Long> accountsAccountKey = AccountDAO.getInstance(MainActivity.this).getAccountToIdMap();
        accountId = accountsAccountKey.get(actSelectedFilter);
      }

      if (actSelectedFilter == null
              || actSelectedFilter != null && MessageListDAO.getInstance(MainActivity.this).getAllMessagesCount(accountId) == 0) {
        reloadMessages(false);
      }
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
      if (actSelectedFilter == null) {
        builder.append(EventLogger.LOGGER_STRINGS.MAINPAGE.ALL_STR);
      } else {
        builder.append(actSelectedFilter.getDisplayName());
      }

      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      for (int actualVisiblePosition = firstVisiblePosition; actualVisiblePosition < lastVisiblePosition; actualVisiblePosition++) {
        if (adapter.getItem(actualVisiblePosition) != null) {
          Cursor cursor = (Cursor)adapter.getItem(actualVisiblePosition);
          MessageListElement mle = MessageListDAO.cursorToMessageListElement(cursor, mAccountsLongKey);
          builder.append(mle.getId());
          builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        }
      }
    } catch (Exception ex) {
      Log.d("rgai", "NULL POINTER EXCEPTION CATCHED", ex);
    }

  }
  
  private void logActivityEvent(String event) {
    StringBuilder builder = new StringBuilder();
    builder.append(event);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    if (mFragment != null) {
      appendVisibleElementToStringBuilder(builder, mFragment.getListView(), mFragment.getAdapter());
    }
    Log.d("willrgai", builder.toString());
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, builder.toString(), true);
  }
  
  private void setUpAndRegisterScreenReceiver() {
    if (screenReceiver == null) {
      screenReceiver = new ScreenReceiver();
    }

    IntentFilter screenIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
    registerReceiver(screenReceiver, screenIntentFilter);
  }

  private SensorManager sensorManager;


  
  private static final String APPLICATION_START_STR = "application:start";
  private Thread.UncaughtExceptionHandler defaultUEH;
  private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
      EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, "uncaughtException : " + ex.getMessage() + " " + ex.getLocalizedMessage(), true);
      // re-throw critical exception further to the os (important)
      defaultUEH.uncaughtException(thread, ex);
    }
  };
  
}
