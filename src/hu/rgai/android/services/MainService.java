package hu.rgai.android.services;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.TelephonyManager;
import android.util.Log;
import hu.rgai.android.beens.FullMessage;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.HtmlContent;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.LogUploadScheduler;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.beens.Person;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.EmailAccount;
import hu.rgai.android.beens.FacebookAccount;
import hu.rgai.android.beens.GmailAccount;
import hu.rgai.android.beens.SmsAccount;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.AndroidUtils;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.rgai.android.workers.ActiveConnectionConnector;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

public class MainService extends Service {

  public static boolean RUNNING = false;
  private static int iterationCount = 0;

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

  private MyHandler handler = null;
  private final IBinder mBinder = new MyBinder();

  public static volatile Set<MessageListElement> messages = null;
  public static volatile MessageListElement mLastNotifiedMessage = null;

  public MainService() {}

  @Override
  public void onCreate() {
    RUNNING = true;
    handler = new MyHandler(this);

    AndroidUtils.connectConnectableMessageProviders(this);
    
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
    Log.d("rgai", "MAIN SERVICE ON DESTROY CALLED!");
    
    // unregisterReceiver(emailContentChangeReceiver);
  }

  private void updateMessagesPrettyDate() {
    if (messages != null) {
      SimpleDateFormat sdf = new SimpleDateFormat();
      for (MessageListElement mlep : messages) {
        mlep.updatePrettyDateString(sdf);
      }
    }
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // if (isNetworkAvailable()) {
    
    MessageListElement.refreshCurrentDates();
    updateMessagesPrettyDate();
    
    List<Account> accounts = StoreHandler.getAccounts(this);
    
//    if (iterationCount == 1) {
//      Debug.startMethodTracing("calc_store_connect_repaired");
//      Log.d("rgai", "STARTING DEBUG NOW!!!");
//    }
    iterationCount++;
    MainActivity.openFbSession(this);
    // Log.d("rgai", "CURRENT MAINSERVICE ITERATION: " + iterationCount);
    MessageProvider.Type type = null;
    MessageListElement actViewingMessageAtThread = null;
    // if true, loading new messages at end of the lists, not checking for new
    // ones
    boolean loadMore = false;
    boolean forceQuery = false;
    if (intent != null && intent.getExtras() != null) {
      if (intent.getExtras().containsKey("type")) {
        type = MessageProvider.Type.valueOf(intent.getExtras().getString("type"));
      }
      if (intent.getExtras().containsKey("load_more")) {
        loadMore = intent.getExtras().getBoolean("load_more", false);
      }
      if (intent.getExtras().containsKey("force_query")) {
        forceQuery = intent.getExtras().getBoolean("force_query", false);
      }
      if (intent.getExtras().containsKey("act_viewing_message")) {
        actViewingMessageAtThread = (MessageListElement)intent.getExtras().getParcelable("act_viewing_message");
      }
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
        handler.setActViewingMessageAtThread(actViewingMessageAtThread);
        
        if (type == null || type.equals(MessageProvider.Type.SMS)) {
          accounts.add(SmsAccount.account);
//          MessageProvider provider = getMessageProviderByAccount(SmsAccount.account, this);
//          LongOperation myThread = new LongOperation(this, handler, SmsAccount.account, provider, loadMore);
//          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            myThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//          } else {
//            myThread.execute();
//          }
        }
        
//        if (isNet) {
          for (Account acc : accounts) {
            
            if (type == null || acc.getAccountType().equals(type)) {
              MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(acc, this);
              Log.d("rgai", forceQuery + " | " + loadMore + " | " + provider.isConnectionAlive() + " | " + provider.canBroadcastOnNewMessage());
              
              if (forceQuery || loadMore || !provider.isConnectionAlive() || !provider.canBroadcastOnNewMessage()) {
                Log.d("rgai", acc.isInternetNeededForLoad() + " | " + isNet);
                if (acc.isInternetNeededForLoad() && isNet || !acc.isInternetNeededForLoad()) {
                  
                  Log.d("rgai", "igen, le kell kerdeznunk: " + provider);
                
                  LongOperation myThread = new LongOperation(this, handler, acc, provider, loadMore);
                  AndroidUtils.<String, Integer, List<MessageListElement>>startAsyncTask(myThread);
                } else {
                  Log.d("rgai", "nem, nem kell lekerdeznunk: " + provider);
                }
              } else {
                Log.d("rgai", "nem, nem kell lekerdeznunk: " + provider);
              }
            }
          }
          Log.d("rgai", " . ");
//        }
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
  public static void setMessageContent(String id, Account account, FullMessage fullMessage) {

    for (MessageListElement mlep : messages) {
      if (mlep.getId().equals( id) && mlep.getAccount().equals( account)) {
        mlep.setFullMessage( fullMessage);
        break;
      }
    }
  }
  
  public static Set<MessageListElement> getFilteredMessages(Account filterAcc) {
    if (filterAcc == null) {
      return messages;
    } else {
      Set<MessageListElement> filterList = new TreeSet<MessageListElement>();
      for (MessageListElement mlep : messages) {
        if (mlep.getAccount().equals(filterAcc)) {
          filterList.add(mlep);
        }
      }
      return filterList;
    }
  }

  /**
   * Removes messages from message list where the account matches with the
   * parameter.
   * 
   * @param account
   */
  public void removeMessagesToAccount( Account account) {
    // Log.d("rgai", "removing messages to account -> " + account);
    Iterator<MessageListElement> it = messages.iterator();
    while (it.hasNext()) {
      MessageListElement mle = it.next();
      if (mle.getAccount().equals(account)) {
        it.remove();
      }
    }
    Log.d("rgai", "messages removed to account -> " + account);
  }

  /**
   * Sets the seen status to true, and the unreadCount to 0.
   * 
   * @param m
   *          the message to set
   * @return
   */
  public static boolean setMessageSeenAndRead(MessageListElement m) {
    boolean changed = false;
    for (MessageListElement mlep : messages) {
      if (mlep.equals(m) && !mlep.isSeen()) {
        changed = true;
        mlep.setSeen(true);
        mlep.setUnreadCount(0);
        break;
      }
    }
    return changed;
  }

  public static void initMessages() {
    messages = new TreeSet<MessageListElement>();
  }

  public void setAllMessagesToSeen() {
    for (MessageListElement mle : messages) {
      mle.setSeen(true);
    }
  }

  public static MessageListElement getListElementById(String id, Account a) {
    // TODO: if we display a notification, and later..much later we open the message from notification bar
    //then it is possible that messages variable is unitialized..so null..
    for (MessageListElement mle : messages) {
      if (mle.getId().equals(id) && mle.getAccount().equals(a)) {
        return mle;
      }
    }
    return null;
  }

  public MessageListElement[] getMessages() {
    if (messages != null) {
      return messages.toArray(new MessageListElement[0]);
    } else {
      return null;
    }
  }

  public void removeElementsFromList(Account acc) {
    if (messages != null) {
      for (MessageListElement mle : messages) {
        if (mle.getAccount().equals(acc)) {
          // Log.d("rgai", "removing message list element -> " + mle);
          messages.remove(mle);
          removeElementsFromList(acc);
          break;
        }
      }
    }
  }
  
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
        if (bundle.get("result") != null) {
          if (bundle.get("errorMessage") != null) {
            MainActivity.showErrorMessage(bundle.getInt("result"), bundle.getString("errorMessage"));
          }
          boolean loadMore = bundle.getBoolean("load_more");
          if (bundle.getInt( "result") == OK && bundle.get( "messages") != null) {
            MessageListElement[] newMessages = (MessageListElement[]) bundle.getParcelableArray( "messages");

            /*
             * If new message packet comes from Facebook, and newMessages contains groupMessages,
             * send a broadcast so the group Facebook chat is notified about the new messages.
             */
            if (newMessages != null) {
              boolean sendBC = false;
              for (int i = 0; i < newMessages.length; i++) {
                MessageListElement m = newMessages[i];
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

            this.mergeMessages(newMessages);
            MessageListElement lastUnreadMsg = null;

            Set<Account> accountsToUpdate = new HashSet<Account>();
            
            for (MessageListElement mle : messages) {
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
      if (!MainActivity.isMainActivityVisible() && lastUnreadMsg != null) {
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
        
        if (StoreHandler.SystemSettings.isNotificationSoundTurnedOn(context)) {
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
          resultIntent.putExtra("msg_list_element_id", lastUnreadMsg.getId());
          resultIntent.putExtra("account", (Parcelable) lastUnreadMsg.getAccount());
          stackBuilder.addParentStack(MainActivity.class);
        } else {
          resultIntent = new Intent(context, MainActivity.class);
        }
        resultIntent.putExtra("from_notifier", true);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mNotificationManager.notify(Settings.NOTIFICATION_NEW_MESSAGE_ID, mBuilder.build());
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.NOTIFICATION.NOTIFICATION_POPUP_STR
                + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + km.inKeyguardRestrictedInputMode(), true);
      }
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

    private void mergeMessages(MessageListElement[] newMessages) {
      if (messages == null) {
        initMessages();
      }
      for (MessageListElement newMessage : newMessages) {
        boolean contains = false;
        MessageListElement storedFoundMessage = null;
        // .contains not work, because the date of new item != date of old item
        // and
        // tree search does not return a valid value
        // causes problem at thread type messages like Facebook
        for (MessageListElement storedMessage : messages) {
          if (storedMessage.equals(newMessage)) {
            contains = true;
            storedFoundMessage = storedMessage;
          }
        }
        if (!contains) {
          messages.add(newMessage);

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
            for (MessageListElement oldMessage : messages) {
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
              messages.remove(itemToRemove);
              messages.add(newMessage);
            }
          }
        }
      }
    }
  }

  public class MyBinder extends Binder {
    public MainService getService() {
      return MainService.this;
    }
  }

  private class LongOperation extends AsyncTask<String, Integer, List<MessageListElement>> {

    private Context context;
    private int result;
    private String errorMessage = null;
    private final Handler handler;
    private final Account acc;
    private final MessageProvider messageProvider;
    private boolean loadMore = false;

    public LongOperation(Context context, Handler handler, Account acc, MessageProvider messageProvider,
            boolean loadMore) {
      this.context = context;
      this.handler = handler;
      this.acc = acc;
      this.messageProvider = messageProvider;
      this.context = context;
      this.loadMore = loadMore;
    }

    @Override
    protected List<MessageListElement> doInBackground(String... params) {
      List<MessageListElement> messages = null;
      try {
        if (messageProvider != null) {
          int currentMessagesToAccount = 0;
          if (loadMore) {
            if (MainService.messages != null) {
              for (MessageListElement m : MainService.messages) {
                if (m.getAccount().equals(acc)) {
                  currentMessagesToAccount++;
                }
              }
            }
          }
          
          // the already loaded messages to the specific content type...
          Set<MessageListElement> loadedMessages = getLoadedMessages(acc, MainService.messages);
          
          messages = messageProvider.getMessageList(currentMessagesToAccount,
                  acc.getMessageLimit(), loadedMessages, Settings.MAX_SNIPPET_LENGTH);
          
          // searching for android contacts
          extendPersonObject(messages);
          
//          List<MessageListElement> parcelableMessages = extendPersonObject();
          
//          for (MessageListElement mlep : parcelableMessages) {
//            if (!messages.contains(mlep)) {
//              messages.add(mlep);
//            } else {
//              if (!mlep.isUpdateFlags()) {
//                messages.get(messages.indexOf(mlep)).setFrom(mlep.getFrom());
//              }
//            }
//          }
        }

      } catch (AuthenticationFailedException ex) {
        ex.printStackTrace();
        this.result = AUTHENTICATION_FAILED_EXCEPTION;
        this.errorMessage = acc.getDisplayName();
        return null;
      } catch (CertPathValidatorException ex) {
        ex.printStackTrace();
        this.result = CERT_PATH_VALIDATOR_EXCEPTION;
        return null;
      } catch (SSLHandshakeException ex) {
        ex.printStackTrace();
        this.result = SSL_HANDSHAKE_EXCEPTION;
        return null;
      } catch (NoSuchProviderException ex) {
        ex.printStackTrace();
        this.result = NO_SUCH_PROVIDER_EXCEPTION;
        return null;
      } catch (ConnectException ex) {
        ex.printStackTrace();
        this.result = CONNECT_EXCEPTION;
        return null;
      } catch (UnknownHostException ex) {
        ex.printStackTrace();
        this.result = UNKNOWN_HOST_EXCEPTION;
        return null;
      } catch (MessagingException ex) {
        ex.printStackTrace();
        this.result = MESSAGING_EXCEPTION;
        return null;
      } catch (IOException ex) {
        ex.printStackTrace();
        this.result = IOEXCEPTION;
        return null;
      }
      this.result = OK;
      return messages;
    }
    
    private Set<MessageListElement> getLoadedMessages(Account account, Set<MessageListElement> messages) {
      Set<MessageListElement> selected = new HashSet<MessageListElement>();
      if (messages != null) {
        for (MessageListElement mle : messages) {
          if (mle.getAccount().equals(account)) {
            selected.add(mle);
          }
        }
      }
      return selected;
    }

    private void extendPersonObject(List<MessageListElement> origi) {
      Person p;
      for (MessageListElement mle : origi) {
        p = Person.searchPersonAndr(context, mle.getFrom());
        mle.setFrom(p);
        if (!mle.isUpdateFlags()) {
          if (mle.getFullMessage() != null && mle.getFullMessage() instanceof FullSimpleMessage) {
            ((FullSimpleMessage)mle.getFullMessage()).setFrom(p);
          }
          
          if (mle.getRecipientsList() != null) {
            for (int i = 0; i < mle.getRecipientsList().size(); i++) {
              p = Person.searchPersonAndr(context, mle.getRecipientsList().get(i));
              mle.getRecipientsList().set(i, p);
            }
          }
        }
      }
    }

    @Override
    protected void onPostExecute(List<MessageListElement> messages) {
      
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      if (this.result == OK) {
        bundle.putParcelableArray("messages", messages.toArray(new MessageListElement[messages.size()]));
        // Log.d("rgai", "put messages("+ messages.size() + ") to bundle -> ");
      }
      bundle.putBoolean("load_more", loadMore);
      bundle.putInt("result", this.result);
      bundle.putString("errorMessage", this.errorMessage);

      msg.setData(bundle);
      handler.sendMessage(msg);
    }

    @Override
    protected void onPreExecute() {
      // Log.d(Constants.LOG, "onPreExecute");
    }

  }

}
