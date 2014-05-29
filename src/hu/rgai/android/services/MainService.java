package hu.rgai.android.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.BatchedProcessState;
import hu.rgai.android.beens.MainServiceExtraParams;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.SmsAccount;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.LogUploadScheduler;
import hu.rgai.android.handlers.BatchedAsyncTaskHandler;
import hu.rgai.android.handlers.MessageListerHandler;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.YakoApp;
import hu.rgai.android.tools.AndroidUtils;
import hu.rgai.android.tools.ParamStrings;
import hu.rgai.android.workers.BatchedAsyncTaskExecutor;
import hu.rgai.android.workers.BatchedTimeoutAsyncTask;
import hu.rgai.android.workers.MessageListerAsyncTask;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainService extends Service {
  
  public static boolean RUNNING = false;

  private final IBinder mBinder = new MyBinder();
  
  
  public static final String MESSAGE_LIST_QUERY_KEY = "message_list_query_key";
  public static final String BATCHED_MESSAGE_LIST_TASK_DONE_INTENT = "batched_message_list_task_done_intent";

  public MainService() {}

  @Override
  public void onCreate() {
    RUNNING = true;

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
    if (!LogUploadScheduler.INSTANCE.isRunning) {
      LogUploadScheduler.INSTANCE.startRepeatingTask();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    RUNNING = false;
    
  }
  
  private void updateMessagesPrettyDate() {
    if (YakoApp.getMessages() != null) {
      SimpleDateFormat sdf = new SimpleDateFormat();
      for (MessageListElement mlep : YakoApp.getMessages()) {
        mlep.updatePrettyDateString(sdf);
      }
    }
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
//    Log.d("rgai", "Service:onStartCommand");
    MessageListElement.refreshCurrentDates();
    updateMessagesPrettyDate();
    
    TreeSet<Account> accounts = YakoApp.getAccounts(this);
    
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
    boolean isPhone = YakoApp.isPhone;
    MessageListerHandler preHandler = new MessageListerHandler(this, extraParams, null);
    if (accounts.isEmpty() && !isPhone) {
      preHandler.finished(null, false, MessageListerAsyncTask.NO_ACCOUNT_SET, null);
    } else {
      if (!accounts.isEmpty() && !isPhone && !isNet) {
        preHandler.finished(null, false, MessageListerAsyncTask.NO_INTERNET_ACCESS, null);
      } else {
        
//        if (isPhone && (extraParams.getAccount() == null || extraParams.getAccount().equals(SmsAccount.account))) {
//          accounts.add(SmsAccount.account);
//        }
        
        if (!BatchedAsyncTaskExecutor.isProgressRunning(MESSAGE_LIST_QUERY_KEY)) {
          List<BatchedTimeoutAsyncTask> tasks = new LinkedList<BatchedTimeoutAsyncTask>();
          boolean wasAnyFullUpdateCheck = false;
          for (Account acc : accounts) {
            
            if (extraParams.getAccount() == null || acc.equals(extraParams.getAccount())) {
              MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(acc, this);

              
              // checking if live connections are still alive, reconnect them if not
              boolean isConnectionAlive = provider.isConnectionAlive();
              AndroidUtils.checkAndConnectMessageProviderIfConnectable(provider, isConnectionAlive, this);
              
              if (extraParams.isForceQuery() || extraParams.isLoadMore() || !isConnectionAlive
                      || !provider.canBroadcastOnNewMessage()
                      || (MainActivity.isMainActivityVisible() && !provider.canBroadcastOnMessageChange())) {
                
                if (acc.isInternetNeededForLoad() && isNet || !acc.isInternetNeededForLoad()) {
                  if (extraParams.getAccount() == null) {
                    wasAnyFullUpdateCheck = true;
                  }
                  
                  // TODO: this Definitely should not be here
                  if (acc.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
                    MainActivity.openFbSession(this);
                  }

                  MessageListerHandler th = new MessageListerHandler(this, extraParams, acc.getDisplayName());
                  MessageListerAsyncTask myThread = new MessageListerAsyncTask(this, acc,
                          provider, extraParams.isLoadMore(), extraParams.getQueryLimit(),
                          extraParams.getQueryOffset(), th);
                  myThread.setTimeout(25000);
                  
                  tasks.add(myThread);
                }
              }
            }
          }
          try {
            BatchedAsyncTaskExecutor executor = new BatchedAsyncTaskExecutor(tasks, MESSAGE_LIST_QUERY_KEY, new BatchedAsyncTaskHandler() {
              public void batchedTaskDone(boolean cancelled, String progressKey, BatchedProcessState processState) {
                if (processState.isDone()) {
//                  Log.d("rgai2", "@@@@@@BATCH DONE");
                }
                Intent i = new Intent(BATCHED_MESSAGE_LIST_TASK_DONE_INTENT);
                LocalBroadcastManager.getInstance(MainService.this).sendBroadcast(i);
              }
            });
            executor.execute();
          } catch (Exception ex) {
            Logger.getLogger(MainService.class.getName()).log(Level.SEVERE, null, ex);
          }
          if (wasAnyFullUpdateCheck) {
            YakoApp.lastFullMessageUpdate = new Date();
          }
        } else {
//          Log.d("rgai2", "ITT MOST MEGGATOLTUK A TOBBSZORI INDITAST!!!!!!!!!!!!!!!!!!!!!!");
        }
      }
    }
    return Service.START_STICKY;
  }

  private boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  
  public IBinder onBind(Intent arg0) {
    return mBinder;
  }


  public class MyBinder extends Binder {
    public MainService getService() {
      return MainService.this;
    }
  }

}
