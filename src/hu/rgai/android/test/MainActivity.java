package hu.rgai.android.test;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.Session;
import com.facebook.SessionState;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import com.google.android.gms.maps.model.LatLng;
import hu.rgai.yako.adapters.ZoneListAdapter;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.adapters.MainListAdapter;
import hu.rgai.yako.adapters.MainListDrawerFilterAdapter;

import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.eventlogger.LogUploadScheduler;
import hu.rgai.yako.eventlogger.ScreenReceiver;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.services.LocationService;
import hu.rgai.yako.services.MainService;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.view.activities.*;
import hu.rgai.yako.view.extensions.LinearListView;
import hu.rgai.yako.view.extensions.ZoneDisplayActionBarActivity;
import hu.rgai.yako.view.fragments.MainActivityFragment;
import hu.rgai.yako.workers.BatchedAsyncTaskExecutor;
import hu.rgai.yako.workers.SmartPredictionAsyncTask;

import java.util.*;


/**
 * @author Tamas Kojedzinszky
 */
public class MainActivity extends ZoneDisplayActionBarActivity {

  private static final long MY_LOCATION_LIFE_LENGTH = 5 * 60 * 1000; // in millisec

//  private static List<GpsZone> mGpsZones = null;
//  private Location mMyLastLocation = null;

  private int mPreviousDrawerColor = Settings.DEFAULT_ACTIONBAR_COLOR;

  private DrawerLayout mDrawerLayout;
  private LinearListView mAccountHolder;
  private CompoundButton mZonesToggle;
  private CompoundButton mFakeZoneToggle;
  private View mZonesContainer;
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
  private ZoneListChangedReceiver mLocationChangeReceiver = null;
  private ScreenReceiver screenReceiver;
  private TextView mEmptyListText = null;
  private TreeMap<Long, Account> mAccountsLongKey = null;
  
  
  public static LinkedList<Account> selectedAccounts = null ;
  private static volatile String fbToken = null;
  private static volatile boolean is_activity_visible = false;
  public static final String BATCHED_MESSAGE_MARKER_KEY = "batched_message_marker_key";

  private LayoutInflater mInflater;
  
  
  

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle, true, true, true);

    Tracker t = ((YakoApp) getApplication()).getTracker();
    t.setScreenName(((Object) this).getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());


    mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    // LOGGING
    setUpAndRegisterScreenReceiver();
    if (!EventLogger.INSTANCE.isLogFileOpen()) {
      EventLogger.INSTANCE.setContext(this);
      EventLogger.INSTANCE.openAllLogFile();
    }
    EventLogger.INSTANCE.writeToLogFile(LogFilePaths.FILE_TO_UPLOAD_PATH, APPLICATION_START_STR + " " + EventLogger.INSTANCE.getAppVersion() + " " + android.os.Build.VERSION.RELEASE, true);
    LogUploadScheduler.INSTANCE.setContext(this);
    if (!LogUploadScheduler.INSTANCE.isRunning) {
      LogUploadScheduler.INSTANCE.startRepeatingTask();
    }
//    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//    sensorManager.registerListener(new AccelerometerListener(),
//    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    mLocationChangeReceiver = new ZoneListChangedReceiver();


    setContentView(R.layout.mainlist_navigation_drawer);

    mEmptyListText = new TextView(this);
    mEmptyListText.setText(this.getString(R.string.empty_list));
    mEmptyListText.setGravity(Gravity.CENTER);
    mEmptyListText.setVisibility(View.GONE);
    ((FrameLayout) findViewById(R.id.content_frame)).addView(mEmptyListText);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);


    selectedAccounts = StoreHandler.getSelectedFilterAccount(this);

    if (selectedAccounts == null) {
      selectedAccounts = new LinkedList<Account>();
    }

    mDrawerWrapper = findViewById(R.id.drawer_wrapper);
    mAccountHolder = (LinearListView) findViewById(R.id.account_holder);
    mAccountHolder.setIsSingleSelect(false);

    mZonesToggle = (CompoundButton) findViewById(R.id.zone_on_off);
    mFakeZoneToggle = (CompoundButton) findViewById(R.id.fake_zone);
    mZoneHolder = (LinearListView) findViewById(R.id.zone_holder);
    mZonesContainer = findViewById(R.id.zones_container);
    mAddGpsZone = (TextView) findViewById(R.id.add_gps_zone);
    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    setContent(MessageListDAO.getInstance(this).getAllMessagesCount() != 0 ? true : null);
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);


    mAccountHolder.setOnItemClickListener(new AccountFilterClickListener());
    mZoneHolder.setOnItemClickListener(new ZoneListClickListener());

    boolean isZonesActivated = StoreHandler.isZoneStateActivated(this);
    if (!isZonesActivated) {
      mZonesContainer.setVisibility(View.GONE);
    }
    mZonesToggle.setChecked(isZonesActivated);
    mZonesToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        animateZoneList(isChecked);
        boolean isOn = mZonesToggle.isChecked();
        StoreHandler.setZoneActivityState(MainActivity.this, isOn);
        redisplayMessages();
        loadZoneListAdapter(false);
      }
    });

    mFakeZoneToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) {
          YakoApp.setFakeLocation(null);
          startLocationService(true);
          SmartPredictionAsyncTask smartPred = new SmartPredictionAsyncTask(MainActivity.this, false);
          AndroidUtils.startTimeoutAsyncTask(smartPred);
        } else {
          startFakeMapActivity();
        }
      }
    });

    mAddGpsZone.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startMapActivity(null);
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
//          getSupportActionBar().setTitle(getString(R.string.filter_list));
          invalidateOptionsMenu();
        } else {
          mDrawerIsVisible = false;
//          setActionbar();
          invalidateOptionsMenu();
        }
      }
    };

    mDrawerLayout.setDrawerListener(mDrawerToggle);

  }

  private void animateZoneList(final boolean isActivated) {
    Animation fadeAnim = AnimationUtils.loadAnimation(this, isActivated ? R.anim.fade_in : R.anim.fade_out);
    fadeAnim.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
        if (isActivated) {
          mZonesContainer.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        if (!isActivated) {
          mZonesContainer.setVisibility(View.GONE);
        }
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }
    });
    mZonesContainer.startAnimation(fadeAnim);
  }


  @Override
  protected void onResume() {
    super.onResume();

//    YakoApp.printAsyncTasks(false);

    is_activity_visible = true;
    removeNotificationIfExists();

    mAccountsLongKey = AccountDAO.getInstance(this).getIdToAccountsMap();
    
    // setting zone list
    loadZoneListAdapter(false);
//    setZoneActivityStates();


//    // setting filter adapter onResume, because it might change at settings panel
    setAccountList();


    // setting title
    setActionBar();
    

    // register broadcast receiver for new message load
    LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
    mMessageLoadedReceiver = new MessageLoadedReceiver();
    IntentFilter filter = new IntentFilter(MainService.BATCHED_MESSAGE_LIST_TASK_DONE_INTENT);
    filter.addAction(MessageListerHandler.MESSAGE_PACK_LOADED_INTENT);
    filter.addAction(MainService.NO_TASK_AVAILABLE_TO_PROCESS);
    localManager.registerReceiver(mMessageLoadedReceiver, filter);



    // register broadcast for receive location change pending intents
    IntentFilter locFilter = new IntentFilter(LocationService.ACTION_ZONE_LIST_MUST_REFRESH);
    localManager.registerReceiver(mLocationChangeReceiver, locFilter);
    startLocationService(false);

    
    // loading messages
    if (!MainService.RUNNING) {
      reloadMessages(true);
    } else {
      long now = System.currentTimeMillis();
      if (selectedAccounts.isEmpty()
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
//    mAccountHolder.clearChoices();

    LinkedList<Integer> indexOfAccounts = null;
    if (!selectedAccounts.isEmpty()) {

      indexOfAccounts = AndroidUtils.getIndexOfAccount(accounts, selectedAccounts);
      // the saved selected instance is not available anymore...
      if (indexOfAccounts.isEmpty()) {
        selectedAccounts.clear();
      }
    }

    if (selectedAccounts.isEmpty()) {
      mAccountHolder.setItemChecked(0, true);
    } else {
      for (int i=0; i < indexOfAccounts.size(); i++) {
        // +1 needed because 0th element in adapter is "all instance"
        mAccountHolder.setItemChecked(indexOfAccounts.get(i) + 1, true);
      }
    }
  }

  public void loadZoneListAdapter(boolean refreshFromDatabase) {
    List<GpsZone> zones = YakoApp.getSavedGpsZones(this, refreshFromDatabase);
    boolean zoneActivated = StoreHandler.isZoneStateActivated(this);
    mZoneListAdapter = new ZoneListAdapter(this, zones, zoneActivated);
    mZoneHolder.setAdapter(mZoneListAdapter);
    setActionBar();
  }

  public void startLocationService(boolean forceUpdateZones) {
    Intent i = new Intent(this, LocationService.class);
    if (forceUpdateZones) {
      i.putExtra(LocationService.EXTRA_FORCE_UPDATE_ZONES, true);
    }
    startService(i);
  }

  @Override
  protected void onPause() {
    
    is_activity_visible = false;
    logActivityEvent(EventLogger.LOGGER_STRINGS.MAINPAGE.PAUSE_STR);

    LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
    localManager.unregisterReceiver(mMessageLoadedReceiver);
    localManager.unregisterReceiver(mLocationChangeReceiver);

    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(((Object)this).getClass().getName() + " - pause");
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
    EventLogger.INSTANCE.writeToLogFile(LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.MAINPAGE.BACKBUTTON_STR, true);
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
          List<Account> a = data.getParcelableArrayListExtra(IntentStrings.Params.ACCOUNT);

          Intent intent = new Intent(this, MainScheduler.class);
          intent.setAction(Context.ALARM_SERVICE);
          MainServiceExtraParams eParams = new MainServiceExtraParams();
          eParams.setForceQuery(true);
          eParams.setAccounts(a);
          eParams.setQueryOffset(0);

          LinkedList<Long> accountIds = new LinkedList<Long>();
          TreeMap<Account, Long> accountsAccountKey = AccountDAO.getInstance(this).getAccountToIdMap();          
          for (Account acc: selectedAccounts) {
            accountIds.add(accountsAccountKey.get(acc));
          }
          eParams.setQueryLimit(MessageListDAO.getInstance(this).getAllMessagesCount(accountIds));
          intent.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
          this.sendBroadcast(intent);
        } else {
//          Log.d("rgai", "refresh not .... needed!!!!!");
        }
      }
    } else if (requestCode == Settings.ActivityRequestCodes.GOOGLE_MAPS_ACTIVITY_RESULT) {
      if (data != null && data.getAction() != null) {
        if (data.getAction().equals(GoogleMapsActivity.ACTION_REFRESH_ZONE_LIST)) {
          loadZoneListAdapter(true);
          startLocationService(true);

          // run prediction again, since the zone type might changed...and need to recategorize messages...
          SmartPredictionAsyncTask smartPred = new SmartPredictionAsyncTask(this, false);
          AndroidUtils.startTimeoutAsyncTask(smartPred);

          Toast.makeText(this, "Zone saved.", Toast.LENGTH_SHORT).show();
        }
      }
    } else if (requestCode == Settings.ActivityRequestCodes.FAKE_GOOGLE_MAPS_ACTIVITY_RESULT) {
      if (data != null && data.getAction() != null) {
        if (data.getAction().equals(FakeGoogleMapsActivity.ACTION_FAKE_ZONE_SET)) {
          startLocationService(true);
//          loadZoneListAdapter(false);
          SmartPredictionAsyncTask smartPred = new SmartPredictionAsyncTask(this, false);
          AndroidUtils.startTimeoutAsyncTask(smartPred);
          mFakeZoneToggle.setChecked(true);
        }
      } else {
        mFakeZoneToggle.setChecked(false);
      }
    }
  }
  
  
  protected void setActionBar() {
    super.setActionBar();

    GpsZone closest = YakoApp.getClosestZone(this, false);
    setNavigationDrawerColor(closest);
  }


  private void setNavigationDrawerColor(GpsZone closest) {
    int drawerBgColor;

    if (closest != null) {
      drawerBgColor = 0xff << 24 | halfOfColor(closest.getZoneType().getColor());
    } else {
      drawerBgColor =  Settings.DEFAULT_ACTIONBAR_COLOR;
    }

    TransitionDrawable drawerAnimation = new TransitionDrawable(
            new Drawable[]{new ColorDrawable(mPreviousDrawerColor), new ColorDrawable(drawerBgColor)});
    mDrawerWrapper.setBackground(drawerAnimation);
    drawerAnimation.startTransition(1000);

    mPreviousDrawerColor = drawerBgColor;
  }

  private static int halfOfColor(int c) {
    int redMask   = 0xff0000;
    int greenMask = 0x00ff00;
    int blueMask  = 0x0000ff;
    int shift = 1;

    int r = ((c & redMask) >> shift) & redMask;
    int g = ((c & greenMask) >> shift) & greenMask;
    int b = ((c & blueMask) >> shift) & blueMask;

    return r | g | b;
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
    if (!selectedAccounts.isEmpty()) {
        eParams.setAccounts(selectedAccounts);
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
        startActivityForResult(i, Settings.ActivityRequestCodes.PREFERENCES_REQUEST_CODE);
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
  
  
  private void redisplayMessages() {
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
    if (!selectedAccounts.isEmpty()) {
      eParams.setAccounts(selectedAccounts);
    }
    intent.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
    this.sendBroadcast(intent);
  }

//  private void setZoneActivityStates() {
//    boolean zoneChanged = true;
//    if (mMyLastLocation != null) {
//      Log.d("yako", "lat, long: " + mMyLastLocation.getLatitude() + ", " + mMyLastLocation.getLongitude());
//      Log.d("yako", "time: " + new Date(mMyLastLocation.getTime()));
//
//      Set<String> nearLocationList = new TreeSet<String>();
//      String closestLoc = null;
//      float closest = Float.MAX_VALUE;
//      for (GpsZone zone : YakoApp.getSavedGpsZones(this)) {
//        int distance = Math.round(getDist((float) zone.getLat(), (float) zone.getLong(),
//                (float) mMyLastLocation.getLatitude(), (float) mMyLastLocation.getLongitude()));
//        zone.setDistance(distance);
//        zone.setProximity(GpsZone.Proximity.UNKNOWN);
//        if (distance <= zone.getRadius()) {
//          nearLocationList.add(zone.getAlias());
//          if (distance < closest) {
//            closest = distance;
//            closestLoc = zone.getAlias();
//          }
//        }
//      }
//
//      for (GpsZone zone : YakoApp.getSavedGpsZones(this)) {
//        if (zone.getAlias().equals(closestLoc)) {
//          zone.setProximity(GpsZone.Proximity.CLOSEST);
//        } else if (nearLocationList.contains(zone.getAlias())) {
//          zone.setProximity(GpsZone.Proximity.NEAR);
//        } else {
//          zone.setProximity(GpsZone.Proximity.FAR);
//        }
//      }
//    }
//    if (zoneChanged) {
//      predictMessages();
//    }
//  }

//  private void predictMessages() {
//    TreeMap<Long, Account> accounts = AccountDAO.getInstance(this).getIdToAccountsMap();
//    TreeSet<MessageListElement> msgs = MessageListDAO.getInstance(this).getAllMessages(accounts);
//    if (msgs != null && !msgs.isEmpty()) {
//      MessagePredictionProvider msgPredProvider = new DummyMessagePredictionProvider();
//      double val = msgPredProvider.predictMessage(this, msgs.first());
//      Toast.makeText(this, "predicted dummy value: " + val, Toast.LENGTH_LONG).show();
//    }
//  }

//  private float getDist(float x1, float y1, float x2, float y2) {
//    float[] dist = new float[2];
//    Location.distanceBetween(x1, y1, x2, y2, dist);
//    return dist[0];
//  }

  private void startFakeMapActivity() {
    Intent i = new Intent(MainActivity.this, FakeGoogleMapsActivity.class);
    LatLng latLng = LocationService.mMyLastLocation != null
            ? new LatLng(LocationService.mMyLastLocation.getLatitude(), LocationService.mMyLastLocation.getLongitude()) : null;
    i.putExtra(FakeGoogleMapsActivity.EXTRA_START_LOC, latLng);
    startActivityForResult(i, Settings.ActivityRequestCodes.FAKE_GOOGLE_MAPS_ACTIVITY_RESULT);
  }

  private void startMapActivity(GpsZone zone) {
    Intent i = new Intent(MainActivity.this, GoogleMapsActivity.class);
    if (zone != null) {
      i.putExtra(GoogleMapsActivity.EXTRA_GPS_ZONE_DATA, zone);
    } else {
      LatLng latLng = LocationService.mMyLastLocation != null
              ? new LatLng(LocationService.mMyLastLocation.getLatitude(), LocationService.mMyLastLocation.getLongitude()) : null;
      i.putExtra(GoogleMapsActivity.EXTRA_START_LOC, latLng);
    }
    startActivityForResult(i, Settings.ActivityRequestCodes.GOOGLE_MAPS_ACTIVITY_RESULT);
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
        MainActivity.this.redisplayMessages();
      }
      // if no task available to do at service
      else if (intent.getAction().equals(MainService.NO_TASK_AVAILABLE_TO_PROCESS)) {
        MainActivity.this.setContent(MessageListDAO.getInstance(MainActivity.this).getAllMessagesCount() != 0);
      }
    }
  }


  private class ZoneListChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(LocationService.ACTION_ZONE_LIST_MUST_REFRESH)) {
        boolean reloadMainList = intent.getBooleanExtra(LocationService.EXTRA_RELOAD_MAINLIST, true);
        loadZoneListAdapter(false);
        if (reloadMainList) {
          MainActivity.this.redisplayMessages();
        } else {
          // skipping main list load, because with this change we started a new prediction to messages, so
          // the end of that process will result a list reload anyway...

          // do nothing...wait for async task to finish and refresh message list at the end of that process
        }
      }
    }
  }



  private class AccountFilterClickListener implements LinearListView.OnItemClickListener {

    @Override
    public void onItemClick(Object item, int position) {
      Account a = (Account)item;
      mAccountHolder.setItemChecked(position, !mAccountHolder.isItemChecked(position));


        if (position == 0 || mAccountHolder.getCheckedItemCount() == 0
            || mAccountHolder.getCheckedItemCount() == mAccountHolder.getCount() - 1
            && !mAccountHolder.isItemChecked(0)) {

          mAccountHolder.clearChoices();
          //mDrawerList.requestLayout();
          mAccountHolder.setItemChecked(0, true);
          selectedAccounts.clear();

        } else {
          mAccountHolder.setItemChecked(0, false);
          if (mAccountHolder.isItemChecked(position)) {
            selectedAccounts.add(a);
          } else {
            selectedAccounts.remove(a);
          }
        }


      StoreHandler.saveSelectedFilterAccount(MainActivity.this, selectedAccounts);
      if (mFragment != null) {
        mFragment.hideContextualActionbar();
        mFragment.notifyAdapterChange();
      }


      LinkedList<Long> accountIds = new LinkedList<Long>();

      if (!selectedAccounts.isEmpty()) {
        TreeMap<Account, Long> accountsAccountKey = AccountDAO.getInstance(MainActivity.this).getAccountToIdMap();
       
        for (Account acc: selectedAccounts) {
          accountIds.add(accountsAccountKey.get(acc));
        }
      }

      // run query for selected filter only if list is empty OR all accounts are selected
      if (selectedAccounts.isEmpty()
              || !selectedAccounts.isEmpty() && MessageListDAO.getInstance(MainActivity.this).getAllMessagesCount(accountIds) == 0) {
        reloadMessages(false);
      }
    }
  }

  private class ZoneListClickListener implements LinearListView.OnItemClickListener {

    @Override
    public void onItemClick(Object item, int position) {
      GpsZone zone = (GpsZone)item;
      startMapActivity(zone);
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
      if (selectedAccounts.isEmpty()) {
        builder.append(EventLogger.LOGGER_STRINGS.MAINPAGE.ALL_STR);
      } else {        
        for (int i=0; i<selectedAccounts.size(); i++ ) {
          builder.append(selectedAccounts.get(i).getDisplayName());
        }
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
