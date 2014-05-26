package hu.rgai.android.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.google.android.gms.analytics.GoogleAnalytics;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.MainServiceExtraParams;
import hu.rgai.android.beens.MainServiceExtraParams.ParamStrings;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.SmsAccount;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.LogUploadScheduler;
import hu.rgai.android.handlers.MessageListerHandler;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.BuildConfig;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.YakoApp;
import hu.rgai.android.tools.AndroidUtils;
import hu.rgai.android.workers.BatchedAsyncTaskExecutor;
import hu.rgai.android.workers.BatchedTimeoutAsyncTask;
import hu.rgai.android.workers.MessageListerAsyncTask;
import static hu.rgai.android.workers.MessageListerAsyncTask.NO_ACCOUNT_SET;
import static hu.rgai.android.workers.MessageListerAsyncTask.NO_INTERNET_ACCESS;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainService extends Service {
  
  public static boolean RUNNING = false;

  /**
   * This variable holds the ID of the actually displayed thread. That's why if
   * a new message comes from this thread id, we set it immediately to seen.
   */
//  public static volatile MessageListElement actViewingMessage = null;

  // flags for email account feedback
  
  
  private YakoApp mYakoApp;

//  private MyHandler handler = null;
  private final IBinder mBinder = new MyBinder();
  
  public static final String MESSAGE_LIST_QUERY = "message_list_query";
  
//  public static volatile TreeSet<MessageListElement> messages = null;
  public static volatile Date last_message_update = new Date();
//  public volatile static int currentRefreshedAccountCounter = 0;
//  public volatile static int currentNumOfAccountsToRefresh = 0;
//  public static volatile MessageListElement mLastNotifiedMessage = null;
  
//  private static 

  public MainService() {}

  @Override
  public void onCreate() {
    this.mYakoApp = (YakoApp)getApplication();
    
    if (BuildConfig.DEBUG) {
      Log.d("rgai", "#TURNING OFF GOOGLE ANALYTICS: WE ARE IN DEVELOPE MODE");
      GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(getApplicationContext());
      googleAnalytics.setAppOptOut(true);
    } else {
      Log.d("rgai", "#DO NOT TURN GOOGLE ANALYTICS OFF: WE ARE IN PRODUCTION MODE");
    }
    RUNNING = true;
//    handler = new MyHandler(this);

    // this loads the last notification dates from file
    MainActivity.initLastNotificationDates(this);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (LogUploadScheduler.INSTANCE.isRunning)
          LogUploadScheduler.INSTANCE.stopRepeatingTask();
        EventLogger.INSTANCE.closeLogFile();
      }
    });

    if (!EventLogger.INSTANCE.isLogFileOpen()) {
      EventLogger.INSTANCE.setContext(this);
      EventLogger.INSTANCE.openLogFile("logFile.txt", false);
    }

    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.APPLICATION.APPLICATION_START_STR
            + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + EventLogger.INSTANCE.getAppVersion()
            + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + android.os.Build.VERSION.RELEASE, true);
    LogUploadScheduler.INSTANCE.setContext(this);
    if (!LogUploadScheduler.INSTANCE.isRunning)
      LogUploadScheduler.INSTANCE.startRepeatingTask();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    RUNNING = false;
//    Log.d("rgai", "MAIN SERVICE ON DESTROY CALLED!");
    
  }
  
  private void updateMessagesPrettyDate() {
    if (mYakoApp.getMessages() != null) {
      SimpleDateFormat sdf = new SimpleDateFormat();
      for (MessageListElement mlep : mYakoApp.getMessages()) {
        mlep.updatePrettyDateString(sdf);
      }
    }
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // if (isNetworkAvailable()) {
    Log.d("rgai", "Service:onStartCommand");
    MessageListElement.refreshCurrentDates();
    updateMessagesPrettyDate();
    
    List<Account> accounts = StoreHandler.getAccounts(this);
    
    final MainServiceExtraParams extraParams;
    if (intent != null && intent.getExtras() != null) {
      if (intent.getExtras().containsKey(ParamStrings.EXTRA_PARAMS)) {
        extraParams = intent.getExtras().getParcelable(ParamStrings.EXTRA_PARAMS);
      } else {
        extraParams = new MainServiceExtraParams();
      }
    } else {
      extraParams = new MainServiceExtraParams();
    }
    
    boolean isNet = isNetworkAvailable();
    boolean isPhone = isPhone();
    MessageListerHandler preHandler = new MessageListerHandler(mYakoApp, extraParams, null);
    if (accounts.isEmpty() && !isPhone) {
      preHandler.finished(null, false, NO_ACCOUNT_SET, null);
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//      bundle.putInt("result", NO_ACCOUNT_SET);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
    } else {
      if (!accounts.isEmpty() && !isPhone && !isNet) {
        preHandler.finished(null, false, NO_INTERNET_ACCESS, null);
      } else {
//        handler.setActViewingMessageAtThread(extraParams.getActViewingMessage());
        
        if (isPhone && (extraParams.getAccount() == null || extraParams.getAccount().equals(SmsAccount.account))) {
          accounts.add(SmsAccount.account);
        }
        
        
        if (!BatchedAsyncTaskExecutor.isProgressRunning(MESSAGE_LIST_QUERY)) {
          List<BatchedTimeoutAsyncTask> tasks = new LinkedList<BatchedTimeoutAsyncTask>();
          boolean wasAnyFullUpdateCheck = false;
//          currentNumOfAccountsToRefresh = 0;
//          currentRefreshedAccountCounter = 0;
          for (Account acc : accounts) {
            
            if (extraParams.getAccount() == null || acc.equals(extraParams.getAccount())) {
              MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(acc, this);

              
              // checking if live connections are still alive, reconnect them if not
              boolean isConnectionAlive = provider.isConnectionAlive();
              AndroidUtils.checkAndConnectMessageProviderIfConnectable(provider, isConnectionAlive, this);
              if (extraParams.isForceQuery() || extraParams.isLoadMore() || !isConnectionAlive || !provider.canBroadcastOnNewMessage()
                      || (MainActivity.isMainActivityVisible() && !provider.canBroadcastOnMessageChange())) {
                if (acc.isInternetNeededForLoad() && isNet || !acc.isInternetNeededForLoad()) {
                  if (extraParams.getAccount() == null) {
                    wasAnyFullUpdateCheck = true;
                  }
                  
                  // TODO: this Definitely should not be here
                  if (acc.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
                    MainActivity.openFbSession(this);
                  }

                  MessageListerHandler th = new MessageListerHandler(mYakoApp, extraParams, acc.getDisplayName());
                  MessageListerAsyncTask myThread = new MessageListerAsyncTask(mYakoApp, acc,
                          provider, extraParams.isLoadMore(), extraParams.getQueryLimit(),
                          extraParams.getQueryOffset(), th);
                  myThread.setTimeout(25000);
                  
                  tasks.add(myThread);
                }
              }
            }
          }
          try {
            BatchedAsyncTaskExecutor executor = new BatchedAsyncTaskExecutor(tasks);
            executor.execute();
          } catch (Exception ex) {
            Logger.getLogger(MainService.class.getName()).log(Level.SEVERE, null, ex);
          }
          MainActivity.refreshLoadingStateRate();
          if (wasAnyFullUpdateCheck) {
            last_message_update = new Date();
            MainActivity.notifyMessageChange(false);
          }
  //          Log.d("rgai", " . ");
        } else {
          Log.d("rgai2", "ITT MOST MEGGATOLTUK A TOBBSZORI INDITAST!!!!!!!!!!!!!!!!!!!!!!");
        }
      }
    }
    return Service.START_STICKY;
  }

  private boolean isPhone() {
    TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    int simState = telMgr.getSimState();
    if (simState == TelephonyManager.SIM_STATE_READY) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return mBinder;
  }


  public class MyBinder extends Binder {
    public MainService getService() {
      return MainService.this;
    }
  }

}
