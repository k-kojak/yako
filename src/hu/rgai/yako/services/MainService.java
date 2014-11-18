package hu.rgai.yako.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import hu.rgai.yako.eventlogger.AccelerometerListener;
import hu.rgai.android.test.MainActivity;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.BatchedProcessState;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.eventlogger.LogUploadScheduler;
import hu.rgai.yako.handlers.BatchedAsyncTaskHandler;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.workers.BatchedAsyncTaskExecutor;
import hu.rgai.yako.workers.BatchedTimeoutAsyncTask;
import hu.rgai.yako.workers.MessageListerAsyncTask;

import java.util.*;

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
      EventLogger.INSTANCE.openAllLogFile();
    }
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH,EventLogger.LOGGER_STRINGS.APPLICATION.APPLICATION_START_STR
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

    TreeSet<Account> accounts = AccountDAO.getInstance(this).getAllAccounts();

    ExtraProcessResult pr = getExtraParams(intent);
    final MainServiceExtraParams extraParams = pr.mServExtraParams;

    /**
     * if true that means android system restarted the mainservice without sending any parameter
     * if that is the case, we have to make a full query
     */
    boolean startedByAndroid = pr.startedByAndroid;
    boolean isNet = isNetworkAvailable();
    boolean isPhone = YakoApp.isRaedyForSms;

    MessageListerHandler preHandler = new MessageListerHandler(this, extraParams, null);

    if (accounts.isEmpty() && !isPhone) {
      preHandler.finished(null, MessageListerAsyncTask.NO_ACCOUNT_SET, null);
    } else {
      if (!accounts.isEmpty() && !isPhone && !isNet) {
        preHandler.finished(null, MessageListerAsyncTask.NO_INTERNET_ACCESS, null);
      } else {
        runMessageQuery(this, accounts, startedByAndroid, isNet, extraParams);

      }
    }
    return Service.START_STICKY;
  }



  private static void runMessageQuery(final Context context, TreeSet<Account> accounts, boolean startedByAndroid,
                                      boolean isNet, MainServiceExtraParams extraParams) {

    if (!BatchedAsyncTaskExecutor.isProgressRunning(MESSAGE_LIST_QUERY_KEY)) {

      List<BatchedTimeoutAsyncTask> tasks = new LinkedList<>();
      boolean wasAnyFullUpdateCheck = buildQueryTasks(context, accounts, extraParams, startedByAndroid, isNet, tasks);

      try {
        // this means there is no available task to process
        if (tasks.isEmpty()) {
          Intent i = new Intent(NO_TASK_AVAILABLE_TO_PROCESS);
          LocalBroadcastManager.getInstance(context).sendBroadcast(i);
        } else {
          BatchedAsyncTaskHandler batchHandler = new MyBatchedTaskHandler(context);
          BatchedAsyncTaskExecutor executor = new BatchedAsyncTaskExecutor(tasks, MESSAGE_LIST_QUERY_KEY, batchHandler);
          executor.execute(context);
        }
      } catch (Exception ex) {
        Log.d("rgai", "start command exception", ex);
      }
      if (wasAnyFullUpdateCheck) {
        YakoApp.lastFullMessageUpdate = new Date();
      }
    } else {
      asyncTaskQueue.add(extraParams);
    }
  }



  /**
   * Returns true if this was a full update check (all accounts were checked, not only 1), false otherwise.
   * @param context
   * @param accounts
   * @param extraParams
   * @param startedByAndroid
   * @param isNet
   * @param tasks
   * @return
   */
  private static boolean buildQueryTasks(Context context, TreeSet<Account> accounts,
                                                               MainServiceExtraParams extraParams,
                                                               boolean startedByAndroid, boolean isNet,
                                                               List<BatchedTimeoutAsyncTask> tasks) {
    boolean wasAnyFullUpdateCheck = false;
    TreeMap<Account, Long> accountsAccountKey = null;
    TreeMap<Long, Account> accountsIntegerKey = null;

    for (Account acc : accounts) {
      if (extraParams.isAccountsEmpty() || extraParams.accountsContains(acc)) {
        MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(acc, context);

        // checking if live connections are still alive, reconnect them if not
        boolean isConnectionAlive = provider.isConnectionAlive();
        AndroidUtils.checkAndConnectMessageProviderIfConnectable(provider, isConnectionAlive, context);

        boolean shouldLoadData = startedByAndroid || extraParams.isForceQuery() || extraParams.isLoadMore()
                || !isConnectionAlive || !provider.canBroadcastOnNewMessage()
                || MainActivity.isMainActivityVisible();

        boolean canLoadData = acc.isInternetNeededForLoad() && isNet || !acc.isInternetNeededForLoad();

        if (shouldLoadData && canLoadData) {
          if (extraParams.isAccountsEmpty()) {
            wasAnyFullUpdateCheck = true;
          }

          // TODO: this Definitely should not be here
          if (acc.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
            MainActivity.openFbSession(context);
          }
          if (accountsAccountKey == null || accountsIntegerKey == null) {
            accountsAccountKey = AccountDAO.getInstance(context).getAccountToIdMap();
            accountsIntegerKey = AccountDAO.getInstance(context).getIdToAccountsMap();
          }

          MessageListerHandler th = new MessageListerHandler(context, extraParams, acc.getDisplayName());
          MessageListerAsyncTask myThread = new MessageListerAsyncTask(context, accountsAccountKey,
                  accountsIntegerKey, acc, provider, extraParams.isLoadMore(),
                  extraParams.isMessagesRemovedAtServer(), extraParams.getQueryLimit(),
                  extraParams.getQueryOffset(), th);
          myThread.setTimeout(25000);

          tasks.add(myThread);
        }
      }
    }

    return wasAnyFullUpdateCheck;
  }

  private static ExtraProcessResult getExtraParams(Intent intent) {

    boolean startedByAndroid = true;
    MainServiceExtraParams extraParams;

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

    return new ExtraProcessResult(extraParams, startedByAndroid);
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

  private static class MyBatchedTaskHandler implements BatchedAsyncTaskHandler {

    final Context mContext;

    public MyBatchedTaskHandler(Context context) {
      mContext = context;
    }

    @Override
    public void batchedTaskDone(boolean cancelled, String progressKey, BatchedProcessState processState) {
      if (processState.isDone()) {
        // if we have tasks in queue, then execute the next one
        if (!asyncTaskQueue.isEmpty()) {
          MainServiceExtraParams next = asyncTaskQueue.pollFirst();
          Intent intent = new Intent(mContext, MainService.class);
          intent.putExtra(IntentStrings.Params.EXTRA_PARAMS, next);
          mContext.startService(intent);
        }
      }
      Intent i = new Intent(BATCHED_MESSAGE_LIST_TASK_DONE_INTENT);
      LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
    }
  }

  private static class ExtraProcessResult {
    final MainServiceExtraParams mServExtraParams;
    final boolean startedByAndroid;

    public ExtraProcessResult(MainServiceExtraParams mServExtraParams, boolean startedByAndroid) {
      this.mServExtraParams = mServExtraParams;
      this.startedByAndroid = startedByAndroid;
    }
  }

}
