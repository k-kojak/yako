package hu.rgai.yako.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import hu.rgai.android.test.MainActivity;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.BatchedProcessState;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.LogUploadScheduler;
import hu.rgai.yako.handlers.BatchedAsyncTaskHandler;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.workers.BatchedAsyncTaskExecutor;
import hu.rgai.yako.workers.BatchedTimeoutAsyncTask;
import hu.rgai.yako.workers.MessageListerAsyncTask;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainService extends Service {
  
  public static boolean RUNNING = false;
  private static final LinkedList<MainServiceExtraParams> asyncTaskQueue = new LinkedList<MainServiceExtraParams>();
  private final IBinder mBinder = new MyBinder();
  
  
  public static final String MESSAGE_LIST_QUERY_KEY = "message_list_query_key";
  public static final String BATCHED_MESSAGE_LIST_TASK_DONE_INTENT = "batched_message_list_task_done_intent";
  public static final String NO_TASK_AVAILABLE_TO_PROCESS = "no_task_available_to_process";

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
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    YakoApp.updateMessagesPrettyDateStrings();

    TreeSet<Account> accounts = AccountDAO.getInstance(this).getAllAccounts();

    final MainServiceExtraParams extraParams;
    
    // if true that means android system restarted the mainservice without sending any parameter
    // in this case we have to make a full query
    boolean startedByAndroid = true;
    if (intent != null && intent.getExtras() != null) {
      if (intent.getExtras().containsKey(IntentStrings.Params.EXTRA_PARAMS)) {
        // alarm manager cannot send extra params via intent.putextra, we have
        // to use bundles, so if the request comes from there, we have a bundle
        // if not, we have the data directly in the intent
        if (intent.getExtras().get(IntentStrings.Params.EXTRA_PARAMS) instanceof Bundle) {
          extraParams = intent.getExtras().getBundle(IntentStrings.Params.EXTRA_PARAMS).getParcelable(IntentStrings.Params.EXTRA_PARAMS);
        } else {
          extraParams = intent.getExtras().getParcelable(IntentStrings.Params.EXTRA_PARAMS);
        }
        startedByAndroid = false;
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
        
        if (!BatchedAsyncTaskExecutor.isProgressRunning(MESSAGE_LIST_QUERY_KEY)) {
          List<BatchedTimeoutAsyncTask> tasks = new LinkedList<BatchedTimeoutAsyncTask>();
          boolean wasAnyFullUpdateCheck = false;
          for (Account acc : accounts) {
            
            if (extraParams.getAccount() == null || acc.equals(extraParams.getAccount())) {
              MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(acc, this);

              
              // checking if live connections are still alive, reconnect them if not
              boolean isConnectionAlive = provider.isConnectionAlive();
              AndroidUtils.checkAndConnectMessageProviderIfConnectable(provider, isConnectionAlive, this);
              
              if (startedByAndroid || extraParams.isForceQuery() || extraParams.isLoadMore()
                      || !isConnectionAlive || !provider.canBroadcastOnNewMessage()
                      || MainActivity.isMainActivityVisible()) {
                
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
            // this means there is no available task to process
            if (tasks.isEmpty()) {
              Intent i = new Intent(NO_TASK_AVAILABLE_TO_PROCESS);
              LocalBroadcastManager.getInstance(MainService.this).sendBroadcast(i);
            } else {
              BatchedAsyncTaskExecutor executor = new BatchedAsyncTaskExecutor(tasks, MESSAGE_LIST_QUERY_KEY, new BatchedAsyncTaskHandler() {
                public void batchedTaskDone(boolean cancelled, String progressKey, BatchedProcessState processState) {
                  if (processState.isDone()) {
                    // store current message list to disk!
                    synchronized (YakoApp.getMessages()) {
                      Log.i("rgai", "saving message list to disk");
                      AccountDAO accDAO = AccountDAO.getInstance(MainService.this);
                      TreeMap<Account, Integer> accounts = accDAO.getAccountToIdMap();

                      // this means the database does not contains any accounts
//                      if (accounts.isEmpty()) {
//                        accDAO.insertAccounts(StoreHandler.getAccounts(MainService.this));
//                        accounts = accDAO.getAccountToIdMap();
//                      }
                      accDAO.close();

                      Log.d("rgai", "accountsMap: " + accounts);
                      MessageListDAO msgDAO = MessageListDAO.getInstane(MainService.this);
                      msgDAO.insertMessages(YakoApp.getMessages(), accounts);
                      Log.i("rgai", "saved");
                    }
                    // if we have tasks in queue, then execute the next one
                    if (!asyncTaskQueue.isEmpty()) {
                      MainServiceExtraParams next = asyncTaskQueue.pollFirst();
                      Intent intent = new Intent(MainService.this, MainService.class);
                      intent.putExtra(IntentStrings.Params.EXTRA_PARAMS, next);
                      MainService.this.startService(intent);
                    }
                  }
                  Intent i = new Intent(BATCHED_MESSAGE_LIST_TASK_DONE_INTENT);
                  LocalBroadcastManager.getInstance(MainService.this).sendBroadcast(i);
                }
              });
              executor.execute(this);
            }
          } catch (Exception ex) {
            Logger.getLogger(MainService.class.getName()).log(Level.SEVERE, null, ex);
          }
          if (wasAnyFullUpdateCheck) {
            YakoApp.lastFullMessageUpdate = new Date();
          }
        } else {
          asyncTaskQueue.add(extraParams);
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
