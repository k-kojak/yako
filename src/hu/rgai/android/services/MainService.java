package hu.rgai.android.services;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.analytics.GoogleAnalytics;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.FullMessage;
import hu.rgai.android.beens.MainServiceExtraParams;
import hu.rgai.android.beens.MainServiceExtraParams.ParamStrings;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.MessageListResult;
import hu.rgai.android.beens.SmsAccount;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.LogUploadScheduler;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.YakoApp;
import hu.rgai.android.test.BuildConfig;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.MessageReply;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.AndroidUtils;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.rgai.android.workers.MessageListerAsyncTask;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainService extends Service {
  
  public static boolean RUNNING = false;

  /**
   * This variable holds the ID of the actually displayed thread. That's why if
   * a new message comes from this thread id, we set it immediately to seen.
   */
  public static volatile MessageListElement actViewingMessage = null;

  // flags for email account feedback
  public static final int OK = 0;
  public static final int UNKNOWN_HOST_EXCEPTION = 1;
  public static final int IOEXCEPTION = 2;
  public static final int CONNECT_EXCEPTION = 3;
  public static final int NO_SUCH_PROVIDER_EXCEPTION = 4;
  public static final int MESSAGING_EXCEPTION = 5;
  public static final int SSL_HANDSHAKE_EXCEPTION = 6;
  public static final int CERT_PATH_VALIDATOR_EXCEPTION = 7;
  public static final int NO_INTERNET_ACCESS = 8;
  public static final int NO_ACCOUNT_SET = 9;
  public static final int AUTHENTICATION_FAILED_EXCEPTION = 10;
  public static final int CANCELLED = 11;
  
  private YakoApp mYakoApp;

  private MyHandler handler = null;
  private final IBinder mBinder = new MyBinder();
  
//  public static volatile TreeSet<MessageListElement> messages = null;
  public static volatile Date last_message_update = new Date();
  public volatile static int currentRefreshedAccountCounter = 0;
  public volatile static int currentNumOfAccountsToRefresh = 0;
  public static volatile MessageListElement mLastNotifiedMessage = null;
  
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
    handler = new MyHandler(this);

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
    if (accounts.isEmpty() && !isPhone()) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putInt("result", NO_ACCOUNT_SET);
      msg.setData(bundle);
      handler.sendMessage(msg);
    } else {
      if (!accounts.isEmpty() && !isPhone() && !isNet) {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putInt("result", NO_INTERNET_ACCESS);
        msg.setData(bundle);
        handler.sendMessage(msg);
      } else {
        handler.setActViewingMessageAtThread(extraParams.getActViewingMessage());
        
        if (extraParams.getAccount() == null || extraParams.getAccount().equals(SmsAccount.account)) {
          accounts.add(SmsAccount.account);
        }
        
        boolean wasAnyFullUpdateCheck = false;
        if (currentNumOfAccountsToRefresh == currentRefreshedAccountCounter) {
        
          currentNumOfAccountsToRefresh = 0;
          currentRefreshedAccountCounter = 0;
          for (final Account acc : accounts) {
            if (extraParams.getAccount() == null || acc.equals(extraParams.getAccount())) {
              MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(acc, this);
  //              Log.d("rgai", forceQuery + " | " + loadMore + " | " + provider.isConnectionAlive() + " | " + provider.canBroadcastOnNewMessage());

              // checking if live connections are still alive, reconnect them if not
              boolean isConnectionAlive = provider.isConnectionAlive();
              AndroidUtils.checkAndConnectMessageProviderIfConnectable(provider, isConnectionAlive, this);
              if (extraParams.isForceQuery() || extraParams.isLoadMore() || !isConnectionAlive || !provider.canBroadcastOnNewMessage()
                      || (MainActivity.isMainActivityVisible() && !provider.canBroadcastOnMessageChange())) {
  //                Log.d("rgai", acc.isInternetNeededForLoad() + " | " + isNet);
                if (acc.isInternetNeededForLoad() && isNet || !acc.isInternetNeededForLoad()) {
  //                  Log.d("rgai", "igen, le kell kerdeznunk: " + provider);
  //                Log.d("rgai", "Igen, futtassunk betoltest...");
                  if (extraParams.getAccount() == null) {
                    wasAnyFullUpdateCheck = true;
                  }
                  // TODO: this Definitely should not be here
                  if (acc.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
                    MainActivity.openFbSession(this);
                  }
                  currentNumOfAccountsToRefresh++;
                  final MessageListerAsyncTask myThread = new MessageListerAsyncTask(mYakoApp, handler, acc, provider, extraParams.isLoadMore(),
                          extraParams.getQueryLimit(), extraParams.getQueryOffset());
                  AndroidUtils.<String, Integer, MessageListResult>startAsyncTask(myThread);
                  Handler handler = new Handler();
                  handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      if (myThread.getStatus() == AsyncTask.Status.RUNNING ) {
                        myThread.cancelRunningSetup();
                        myThread.cancel(true);
                        currentRefreshedAccountCounter++;
                        MainActivity.refreshLoadingStateRate();
                        Log.d("rgai", "THREAD CANCELLED");
                        if (extraParams.isForceQuery() || extraParams.isLoadMore()) {
                          Toast.makeText(MainService.this, "Connection timeout: " + acc.getDisplayName(), Toast.LENGTH_LONG).show();
                        }
                      }
                    }
                  }, 25000);
                }
              }
            }
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

  // TODO: switch back setMessageComment function
//  public static void setMessageContent(String id, Account account, FullMessage fullMessage) {
//
//    for (MessageListElement mlep : messages) {
//      if (mlep.getId().equals( id) && mlep.getAccount().equals( account)) {
//        mlep.setFullMessage( fullMessage);
//        break;
//      }
//    }
//  }
  
//  public static TreeSet<MessageListElement> getFilteredMessages(Account filterAcc) {
//    if (filterAcc == null) {
//      return messages;
//    } else {
//      TreeSet<MessageListElement> filterList = new TreeSet<MessageListElement>();
//      for (MessageListElement mlep : messages) {
//        if (mlep.getAccount().equals(filterAcc)) {
//          filterList.add(mlep);
//        }
//      }
//      return filterList;
//    }
//  }

  /**
   * Removes messages from message list where the account matches with the
   * parameter.
   * 
   * @param account
   */
//  public void removeMessagesToAccount(Account account) {
//    // Log.d("rgai", "removing messages to account -> " + account);
//    Iterator<MessageListElement> it = mYakoApp.getMessages().iterator();
//    while (it.hasNext()) {
//      MessageListElement mle = it.next();
//      if (mle.getAccount().equals(account)) {
//        it.remove();
//      }
//    }
//    Log.d("rgai", "messages removed to account -> " + account);
//  }

  /**
   * Sets the seen status to true, and the unreadCount to 0.
   * 
   * @param m
   *          the message to set
   * @return
   */
//  public static boolean setMessageSeenAndRead(MessageListElement m) {
//    boolean changed = false;
//    for (MessageListElement mlep : mYakoApp.getMessages()) {
//      if (mlep.equals(m) && !mlep.isSeen()) {
//        changed = true;
//        mlep.setSeen(true);
//        mlep.setUnreadCount(0);
//        break;
//      }
//    }
//    return changed;
//  }

//  public static MessageListElement getListElementById(String id, Account a) {
//    for (MessageListElement mle : messages) {
//      if (mle.getId().equals(id) && mle.getAccount().equals(a)) {
//        return mle;
//      }
//    }
//    return null;
//  }

//  public MessageListElement[] getMessages() {
//    if (messages != null) {
//      return messages.toArray(new MessageListElement[0]);
//    } else {
//      return null;
//    }
//  }

//  public void removeElementsFromList(Account acc) {
//    if (messages != null) {
//      for (MessageListElement mle : messages) {
//        if (mle.getAccount().equals(acc)) {
//          // Log.d("rgai", "removing message list element -> " + mle);
//          messages.remove(mle);
//          removeElementsFromList(acc);
//          break;
//        }
//      }
//    }
//  }
  
  private class MyHandler extends Handler {

    private final Context context;
    private MessageListElement actViewingMessageAtThread = null;

    //
    public MyHandler(Context context) {
      this.context = context;
    }

    @Override
    public void handleMessage(Message msg) {

      Bundle bundle = msg.getData();
      int newMessageCount = 0;
      if (bundle != null) {
        if (bundle.get(ParamStrings.RESULT) != null) {
//          Log.d("rgai", "MessageListerAsyncTaskResult: " + bundle.getInt(ParamStrings.RESULT));
          if (bundle.get(ParamStrings.ERROR_MESSAGE) != null) {
            MainActivity.showErrorMessage(bundle.getInt(ParamStrings.RESULT), bundle.getString(ParamStrings.ERROR_MESSAGE));
          }
//          Log.d("rgai", "##currentRefreshedAccountCounter++");
          MainService.currentRefreshedAccountCounter++;
          MainActivity.refreshLoadingStateRate();
          boolean loadMore = bundle.getBoolean(ParamStrings.LOAD_MORE);
          if (bundle.getInt(ParamStrings.RESULT) == OK && bundle.get("messages") != null) {
            MessageListElement[] newMessages = (MessageListElement[]) bundle.getParcelableArray("messages");
            MessageListResult.ResultType resultType = MessageListResult.ResultType.valueOf(bundle.getString("message_result_type"));
            
            // if NO_CHANGE or ERROR, then just return, we do not have to merge because messages
            // is probably empty anyway...
            if (resultType.equals(MessageListResult.ResultType.NO_CHANGE) || resultType.equals(MessageListResult.ResultType.ERROR)) {
              return;
            }

            /*
             * If new message packet comes from Facebook, and newMessages contains groupMessages,
             * send a broadcast so the group Facebook chat is notified about the new messages.
             */
            if (newMessages != null) {
              boolean sendBC = false;
              for (int i = 0; i < newMessages.length; i++) {
                MessageListElement m = newMessages[i];
//                Log.d("rgai", "m:" + m);
                if (!m.isUpdateFlags() && m.getMessageType().equals(MessageProvider.Type.FACEBOOK) && m.isGroupMessage()) {
                  sendBC = true;
                  break;
                }
              }
              if (sendBC) {
                Intent i = new Intent(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE);
                context.sendBroadcast(i);
              }
            }

            this.mergeMessages(newMessages, loadMore, resultType);
            MessageListElement lastUnreadMsg = null;

            Set<Account> accountsToUpdate = new HashSet<Account>();
            
            for (MessageListElement mle : mYakoApp.getMessages()) {
              if (mle.equals(MainService.actViewingMessage) || mle.equals(actViewingMessageAtThread)) {
                mle.setSeen(true);
                mle.setUnreadCount(0);
              }
              Date lastNotForAcc = MainActivity.getLastNotification(context, mle.getAccount());
              // Log.d("rgai", "LastNotForAccount: " + lastNotForAcc + " ("+ mle.getAccount() +")");
              if (!mle.isSeen() && mle.getDate().after(lastNotForAcc)) {
                if (lastUnreadMsg == null) {
                  lastUnreadMsg = mle;
                }
                newMessageCount++;
                accountsToUpdate.add(mle.getAccount());
              }
            }
            for (Account a : accountsToUpdate) {
              MainActivity.updateLastNotification(context, a);
            }
            if (newMessageCount != 0 && StoreHandler.SystemSettings.isNotificationTurnedOn(context)) {
              builNotification(newMessageCount, lastUnreadMsg);
            }
          }

          MainActivity.notifyMessageChange(loadMore);
        }
      }
    }
    
    private void setActViewingMessageAtThread(MessageListElement actViewingMessageAtThread) {
      this.actViewingMessageAtThread = actViewingMessageAtThread;
    }
    
    private void builNotification(int newMessageCount, MessageListElement lastUnreadMsg) {
      mLastNotifiedMessage = lastUnreadMsg;
      NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      if (lastUnreadMsg != null) {
        boolean soundNotification = StoreHandler.SystemSettings.isNotificationSoundTurnedOn(context);
        if (!MainActivity.isMainActivityVisible()) {
          String fromNameText = "?";
          if (lastUnreadMsg.getFrom() != null) {
            fromNameText = lastUnreadMsg.getFrom().getName();
          } else {
            if (lastUnreadMsg.getRecipientsList() != null) {
              fromNameText = "";
              for (int i = 0; i < lastUnreadMsg.getRecipientsList().size(); i++) {
                if (i > 0) {
                  fromNameText += ",";
                }
                fromNameText += lastUnreadMsg.getRecipientsList().get(i).getName();
              }
            }
          }

          Bitmap largeIcon;
          if (lastUnreadMsg.getFrom() != null) {
            largeIcon = ProfilePhotoProvider.getImageToUser(context, lastUnreadMsg.getFrom().getContactId()).getBitmap();
          } else {
            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.group_chat);
          }

          NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
              .setLargeIcon(largeIcon)
              .setSmallIcon(R.drawable.not_ic_action_email)
              .setWhen(lastUnreadMsg.getDate().getTime())
              .setTicker(fromNameText + ": " + lastUnreadMsg.getTitle())
              .setContentInfo(lastUnreadMsg.getAccount().getDisplayName())
              .setContentTitle(fromNameText).setContentText(lastUnreadMsg.getTitle());

          if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                  && lastUnreadMsg.getMessageType().equals(MessageProvider.Type.EMAIL)){
            notificationButtonHandling(lastUnreadMsg, mBuilder);
          }

          if (soundNotification) {
            Uri soundURI = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm);
            mBuilder.setSound(soundURI);
          }

          if (StoreHandler.SystemSettings.isNotificationVibrationTurnedOn(context)) {
            mBuilder.setVibrate(new long[] { 100, 150, 100, 150, 500, 150, 100, 150 });
          }

          Intent resultIntent;
          TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
          if (newMessageCount == 1) {
            Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(lastUnreadMsg.getAccount().getAccountType());
            resultIntent = new Intent(context, classToLoad);
//            resultIntent.putExtra("msg_list_element_id", lastUnreadMsg.getId());
            resultIntent.putExtra("message", lastUnreadMsg);
            stackBuilder.addParentStack(MainActivity.class);
          } else {
            resultIntent = new Intent(context, MainActivity.class);
          }
          resultIntent.putExtra(ParamStrings.FROM_NOTIFIER, true);
          stackBuilder.addNextIntent(resultIntent);
          PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
          mBuilder.setContentIntent(resultPendingIntent);
          mBuilder.setAutoCancel(true);
          KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
          mNotificationManager.notify(Settings.NOTIFICATION_NEW_MESSAGE_ID, mBuilder.build());
          EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.NOTIFICATION.NOTIFICATION_POPUP_STR
                  + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + km.inKeyguardRestrictedInputMode(), true);
        }
        // if main activity visible: only play sound
        else {
          if (soundNotification) {
            Uri soundURI = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.alarm);
            Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), soundURI);
            r.play();
          }
        }
      }
    }

	   private void notificationButtonHandling(MessageListElement lastUnreadMsg,
             NotificationCompat.Builder mBuilder) {

      Intent intent = new Intent(context, MessageReply.class);
      intent.putExtra("message", (Parcelable)lastUnreadMsg);
      intent.putExtra(ParamStrings.FROM_NOTIFIER, true);
//      intent.putExtra("account", (Parcelable) lastUnreadMsg.getAccount());
      //startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
      PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      mBuilder.addAction(R.drawable.ic_action_reply, "Reply", pIntent);

    }

    private void loggingNewMessageArrived(MessageListElement mle, boolean messageIsVisible) {
      if (mle.getDate().getTime() > EventLogger.INSTANCE.getLogfileCreatedTime()) {
        String fromID = mle.getFrom() == null ? mle.getRecipientsList() == null ? "NULL" : mle.getRecipientsList().get(0).getId() : mle.getFrom().getId();
        StringBuilder builder = new StringBuilder();
        builder.append(mle.getDate().getTime());
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.NEW_MESSAGE_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        builder.append(mle.getId());
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        builder.append(actViewingMessage == null ? "null" : actViewingMessage.getId());
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        builder.append(messageIsVisible);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        builder.append(mle.getMessageType());
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        builder.append(RSAENCODING.INSTANCE.encodingString(fromID));
        EventLogger.INSTANCE.writeToLogFile(builder.toString(), false);
      }
    }

    /**
     * 
     * @param newMessages the list of new messages
     * @param loadMoreRequest true if result of "load more" action, false otherwise, which
     * means this is a refresh action
     */
    private void mergeMessages(MessageListElement[] newMessages, boolean loadMoreRequest, MessageListResult.ResultType resultType) {
      for (MessageListElement newMessage : newMessages) {
        boolean contains = false;
        MessageListElement storedFoundMessage = null;
        // .contains not work, because the date of new item != date of old item
        // and
        // tree search does not return a valid value
        // causes problem at thread type messages like Facebook
        
        for (MessageListElement storedMessage : mYakoApp.getMessages()) {
          if (storedMessage.equals(newMessage)) {
            contains = true;
            storedFoundMessage = storedMessage;
          }
        }
        if (!contains) {
          mYakoApp.getMessages().add(newMessage);

          if ((actViewingMessage != null && newMessage.equals(actViewingMessage))
                  || (actViewingMessage == null && MainActivity.isMainActivityVisible())) {
            loggingNewMessageArrived(newMessage, true);
          } else {
            loggingNewMessageArrived(newMessage, false);
          }
        } else {
//          Log.d("rgai", "MESSAGE ALREADY IN LIST -> " + newMessage);
          // only update old messages' flags with the new one, and nothing else
          if (newMessage.isUpdateFlags()) {
//            Log.d("rgai", "JUST UPDATE SEEN INFO!");
            if (storedFoundMessage != null) {
              storedFoundMessage.setSeen(newMessage.isSeen());
              storedFoundMessage.setUnreadCount(newMessage.getUnreadCount());
            }
          } else {
//            Log.d("rgai", "HANDLE AS \"NEW\" MESSAGE -> " + newMessage);
            MessageListElement itemToRemove = null;
            for (MessageListElement oldMessage : mYakoApp.getMessages()) {
              if (newMessage.equals( oldMessage)) {
                // first updating person info anyway..
                oldMessage.setFrom( newMessage.getFrom());

                /*
                 * "Marking" FB message seen here. Do not change info of the item,
                 * if the date is the same, so the queried data will not override
                 * the displayed object. Facebook does not mark messages as seen
                 * when opening them, so we have to handle it at client side. OR
                 * if we check the message at FB, then turn it seen at the app
                 */
                if (newMessage.getDate().after(oldMessage.getDate()) || newMessage.isSeen() && !oldMessage.isSeen()) {
                  itemToRemove = oldMessage;
                  break;
                }
              }
            }
            if (itemToRemove != null) {
              mYakoApp.getMessages().remove(itemToRemove);
              mYakoApp.getMessages().add(newMessage);
            }
          }
        }
      }
      
      // checking for deleted mails here
      if (resultType == MessageListResult.ResultType.CHANGED && !loadMoreRequest) {
        deleteMergeMessages(newMessages);
      }
    }
  }
  
//  public static TreeSet<MessageListElement> getLoadedMessages(Account account, TreeSet<MessageListElement> messages) {
//    TreeSet<MessageListElement> selected = new TreeSet<MessageListElement>();
//    if (messages != null) {
//      for (MessageListElement mle : messages) {
//        if (mle.getAccount().equals(account)) {
//          selected.add(mle);
//        }
//      }
//    }
//    return selected;
//  }
  
  private synchronized void deleteMergeMessages(MessageListElement[] newMessages) {
    if (newMessages.length > 0) {
      TreeSet<MessageListElement> msgs = mYakoApp.getLoadedMessages(newMessages[0].getAccount());
      SortedSet<MessageListElement> messagesToRemove = msgs.headSet(newMessages[newMessages.length - 1]);
//      Log.d("rgai", "head set list: " + messagesToRemove);
      
      for (int i = 0; i < newMessages.length; i++) {
        if (messagesToRemove.contains(newMessages[i])) {
          messagesToRemove.remove(newMessages[i]);
        }
      }
//      Log.d("rgai", "THESE ELEMENTS MUST BE REMOVED FROM MY LIST: " + messagesToRemove);
      for (MessageListElement mle : messagesToRemove) {
        mYakoApp.getMessages().remove(mle);
      }
      
    }
  }

  public class MyBinder extends Binder {
    public MainService getService() {
      return MainService.this;
    }
  }

}
