package hu.rgai.android.services;

import hu.rgai.android.asynctasks.XmppConnector;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.LogUploadScheduler;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.intent.beens.account.GmailAccountAndr;
import hu.rgai.android.intent.beens.account.SmsAccountAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.MessageListElement;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.GmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullMessage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

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
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.TelephonyManager;
import android.util.Log;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.uszeged.inf.rgai.messagelog.beans.Person;

public class MainService extends Service {

  public static boolean RUNNING = false;
  private static int iterationCount = 0;

  /**
   * This variable holds the ID of the actually displayed thread. That's why if
   * a new message comes from this thread id, we set it immediately to seen.
   */
  public static String actViewingThreadId = null;

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

  private static final String APPLICATION_START_STR = "application:start";

  private Handler handler = null;
  // private LongOperation myThread = null;
  private final IBinder mBinder = new MyBinder();

  public static volatile Set<MessageListElementParc> messages = null;

  public MainService() {
    // super("valami nev");
    // Log.d("rgai", "myservice default constructor");
  }

  @Override
  public void onCreate() {
    // Log.d("rgai", "service oncreate");
    RUNNING = true;
    handler = new MyHandler( this);
    connectXmpp();
    Runtime.getRuntime().addShutdownHook( new Thread() {
      @Override
      public void run() {
        if (LogUploadScheduler.INSTANCE.isRunning)
          LogUploadScheduler.INSTANCE.stopRepeatingTask();
        EventLogger.INSTANCE.closeLogFile();
      }
    });
    // IntentFilter filter = new
    // IntentFilter(Constants.EMAIL_CONTENT_CHANGED_BC_MSG);
    // registerReceiver(emailContentChangeReceiver, filter);

    if (!EventLogger.INSTANCE.isLogFileOpen()) {
      EventLogger.INSTANCE.setContext( this);
      EventLogger.INSTANCE.openLogFile( "logFile.txt", false);
    }

    EventLogger.INSTANCE.writeToLogFile( APPLICATION_START_STR, true);
    LogUploadScheduler.INSTANCE.setContext( this);
    if (!LogUploadScheduler.INSTANCE.isRunning)
      LogUploadScheduler.INSTANCE.startRepeatingTask();
  }

  private void connectXmpp() {
    if (!FacebookMessageProvider.isXmppAlive()) {
      FacebookAccountAndr fba = StoreHandler.getFacebookAccount( this);
      if (fba != null) {
        XmppConnector xmppc = new XmppConnector( fba, this);
        xmppc.execute();
      }
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    RUNNING = false;
    // unregisterReceiver(emailContentChangeReceiver);
  }

  @Override
  public int onStartCommand( Intent intent, int flags, int startId) {
    // if (isNetworkAvailable()) {
    iterationCount++;
    MainActivity.openFbSession( this);
    // Log.d("rgai", "CURRENT MAINSERVICE ITERATION: " + iterationCount);
    MessageProvider.Type type = null;
    // if true, loading new messages at end of the lists, not checking for new
    // ones
    boolean loadMore = false;
    if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey( "type")) {
      type = MessageProvider.Type.valueOf( intent.getExtras().getString( "type"));
    }
    if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey( "load_more")) {
      loadMore = intent.getExtras().getBoolean( "load_more", false);
    }
    // Log.d("rgai","LOAD MORE AT MAINSERVICE -> " + loadMore);

    // Log.d("rgai", "MainService acc type -> " + (type == null ? "NULL" :
    // type.toString()));
    List<AccountAndr> accounts = StoreHandler.getAccounts( this);
    boolean isNet = isNetworkAvailable();
    if (accounts.isEmpty() && !isPhone()) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putInt( "result", NO_ACCOUNT_SET);
      msg.setData( bundle);
      handler.sendMessage( msg);
    } else {
      if (!accounts.isEmpty() && !isPhone() && !isNet) {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putInt( "result", NO_INTERNET_ACCESS);
        msg.setData( bundle);
        handler.sendMessage( msg);
      } else {
        if (isNet) {
          for (AccountAndr acc : accounts) {
            if (type == null || acc.getAccountType().equals( type)) {
              LongOperation myThread = new LongOperation( this, handler, acc, loadMore);
              myThread.execute();
            }
          }
        }

        if (type == null || type.equals( MessageProvider.Type.SMS)) {
          AccountAndr smsAcc = new SmsAccountAndr();
          LongOperation myThread = new LongOperation( this, handler, smsAcc, loadMore);
          myThread.execute();
        }
      }
    }
    // myThread = new LongOperation(handler);
    // myThread.execute();
    // } else {
    // Message msg = handler.obtainMessage();
    // Bundle bundle = new Bundle();
    // bundle.putInt("result", NO_INTERNET_ACCESS);
    // msg.setData(bundle);
    // handler.sendMessage(msg);
    // }

    return Service.START_STICKY;
  }

  private boolean isPhone() {
    TelephonyManager telMgr = (TelephonyManager) getSystemService( Context.TELEPHONY_SERVICE);
    int simState = telMgr.getSimState();
    if (simState == TelephonyManager.SIM_STATE_READY) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  @Override
  public IBinder onBind( Intent arg0) {
    return mBinder;
  }

  // TODO: switch back setMessageComment function
  public static void setMessageContent( String id, AccountAndr account, FullMessage fullMessage) {

    for (MessageListElementParc mlep : messages) {
      if (mlep.getId().equals( id) && mlep.getAccount().equals( account)) {
        mlep.setFullMessage( fullMessage);
        break;
      }
    }
  }

  /**
   * Removes messages from message list where the account matches with the
   * parameter.
   * 
   * @param account
   */
  public void removeMessagesToAccount( AccountAndr account) {
    // Log.d("rgai", "removing messages to account -> " + account);
    Iterator<MessageListElementParc> it = messages.iterator();
    while (it.hasNext()) {
      MessageListElementParc mle = it.next();
      if (mle.getAccount().equals( account)) {
        it.remove();
      }
    }
    Log.d( "rgai", "messages removed to account -> " + account);
  }

  // /**
  // *
  // * @param id
  // * @return
  // * @deprecated use setMessageSeen instead
  // */
  // @Deprecated
  // public boolean setMailSeen(String id) {
  // boolean changed = false;
  // for (MessageListElementParc mlep : messages) {
  // if (mlep.getId().equals(id)) {
  // if (mlep.isSeen()) {
  // changed = true;
  // }
  // mlep.setSeen(true);
  // }
  // }
  // return changed;
  // }

  /**
   * Sets the seen status to true, and the unreadCount to 0.
   * 
   * @param m
   *          the message to set
   * @return
   */
  public static boolean setMessageSeenAndRead( MessageListElementParc m) {
    boolean changed = false;
    for (MessageListElementParc mlep : messages) {
      if (mlep.equals( m) && !mlep.isSeen()) {
        changed = true;
        mlep.setSeen( true);
        mlep.setUnreadCount( 0);
        break;
      }
    }
    return changed;
  }

  public static void initMessages() {
    messages = new TreeSet<MessageListElementParc>();
  }

  public void setAllMessagesToSeen() {
    for (MessageListElementParc mlep : messages) {
      mlep.setSeen( true);
    }
  }

  public MessageListElementParc getListElementById( String id, AccountAndr a) {
    for (MessageListElementParc mlep : messages) {
      if (mlep.getId().equals( id) && mlep.getAccount().equals( a)) {
        return mlep;
      } else {
        // Log.d("rgai", mlep.getId() + " != " + id + " && " + mlep.getAccount()
        // + " != " + a);
      }
    }

    return null;
  }

  public MessageListElementParc[] getMessages() {
    if (messages != null) {
      return messages.toArray( new MessageListElementParc[0]);
    } else {
      return null;
    }
  }

  public void removeElementsFromList( AccountAndr acc) {
    if (messages != null) {
      for (MessageListElementParc mle : messages) {
        if (mle.getAccount().equals( acc)) {
          // Log.d("rgai", "removing message list element -> " + mle);
          messages.remove( mle);
          removeElementsFromList( acc);
          break;
        }
      }
    }
  }

  // @Override
  // protected void onHandleIntent(Intent arg0) {
  // Log.d("rgai", );
  // }

  private class MyHandler extends Handler {

    private static final String NOTIFICATION_POPUP_STR = "notification:popup";
    private static final String NEW_MESSAGE_STR = "newMessage";
    private static final String UNDERLINE_SIGN_STR = "_";
    private static final String MESSAGE_ARRIVED_STR = "message_arrived";
    private static final String SPACE_STR = " ";
    private final Context context;

    //
    public MyHandler( Context context) {
      this.context = context;
    }

    @Override
    public void handleMessage( Message msg) {

      Bundle bundle = msg.getData();
      int newMessageCount = 0;
      if (bundle != null) {
        if (bundle.get( "result") != null) {
          if (bundle.get( "errorMessage") != null) {
            MainActivity.showErrorMessage( bundle.getInt( "result"), bundle.getString( "errorMessage"));
          }
          boolean loadMore = bundle.getBoolean( "load_more");
          if (bundle.getInt( "result") == OK && bundle.get( "messages") != null) {
            MessageListElementParc[] newMessages = (MessageListElementParc[]) bundle.getParcelableArray( "messages");

            /*
             * If new message packet comes from Facebook, and newMessages contains groupMessages,
             * send a broadcast so the group Facebook chat is notified about the new messages.
             */
            if (newMessages != null) {
              boolean sendBC = false;
              for (int i = 0; i < newMessages.length; i++) {
                MessageListElementParc m = newMessages[i];
                if (m.getMessageType().equals(MessageProvider.Type.FACEBOOK) && m.isGroupMessage()) {
                  sendBC = true;
                  break;
                }
              }
              if (sendBC) {
                Log.d("rgai", "SENDING NOTIFY BROADCAST FROM MAIN SERVICE");
                Intent i = new Intent(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE);
                context.sendBroadcast(i);
              }
            }
            
            this.mergeMessages( newMessages);
            MessageListElementParc lastUnreadMsg = null;

            for (MessageListElementParc mle : messages) {
              if (mle.getId().equals( actViewingThreadId)) {
                mle.setSeen( true);
                mle.setUnreadCount( 0);
              }
              Date lastNotForAcc = MainActivity.getLastNotification(mle.getAccount());
              Log.d("rgai", "LastNotForAccount: " + lastNotForAcc + " ("+ mle.getAccount() +")");
              if (!mle.isSeen() && mle.getDate().after( lastNotForAcc)) {
                if (lastUnreadMsg == null) {
                  lastUnreadMsg = mle;
                }
                newMessageCount++;
                MainActivity.updateLastNotification(mle.getAccount());
              }
            }

            NotificationManager mNotificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE);
            if (newMessageCount != 0) {
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
                  largeIcon = ProfilePhotoProvider.getImageToUser(context, lastUnreadMsg.getFrom().getContactId());
                } else {
                  largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.group_chat);
                }
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder( context)
                        .setLargeIcon(largeIcon)
                        .setSmallIcon(R.drawable.not_ic_action_email)
                        .setWhen( lastUnreadMsg.getDate().getTime())
                        .setTicker(fromNameText + ": " + lastUnreadMsg.getTitle())
                        .setContentInfo( lastUnreadMsg.getAccount().getDisplayName())
                        
                        .setContentTitle(fromNameText).setContentText(lastUnreadMsg.getTitle())
                        .setVibrate(new long[]{100,150,100,150,500,150,100,150});
                Intent resultIntent;
                if (newMessageCount == 1) {
                  Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get( lastUnreadMsg.getAccount().getAccountType());
                  resultIntent = new Intent( context, classToLoad);
                  resultIntent.putExtra( "msg_list_element", lastUnreadMsg);
                  resultIntent.putExtra( "account", (Parcelable) lastUnreadMsg.getAccount());
                } else {
                  resultIntent = new Intent( context, MainActivity.class);
                }
                resultIntent.putExtra( "from_notifier", true);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create( context);
                stackBuilder.addParentStack( MainActivity.class);
                stackBuilder.addNextIntent( resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent( 0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent( resultPendingIntent);
                mBuilder.setAutoCancel( true);
                KeyguardManager km = (KeyguardManager) getSystemService( Context.KEYGUARD_SERVICE);
                mNotificationManager.notify( Settings.NOTIFICATION_NEW_MESSAGE_ID, mBuilder.build());
                EventLogger.INSTANCE.writeToLogFile( NOTIFICATION_POPUP_STR + SPACE_STR + km.inKeyguardRestrictedInputMode(), true);
                Log.d( "rgai", "DISPLAY NOTIFICATION...");
//                if (newMessageCount == 1) {
//                  MainActivity.updateLastNotification( lastUnreadMsg.getAccount());
//                } else {
//                  MainActivity.updateLastNotification( null);
//                }
              } else {

              }

            }
          }

          MainActivity.notifyMessageChange( loadMore);
        }
      }
    }

    private void loggingNewMessageArrived( MessageListElementParc mle, boolean messageIsVisible) {
      if (mle.getDate().getTime() > EventLogger.INSTANCE.getLogfileCreatedTime()) {
        String fromID = mle.getFrom() == null ? mle.getRecipientsList() == null ? "NULL" : mle.getRecipientsList().get(0).getId() : mle.getFrom().getId();
        StringBuilder builder = new StringBuilder();
        builder.append( mle.getDate().getTime());
        builder.append( SPACE_STR);
        builder.append( NEW_MESSAGE_STR);
        builder.append( SPACE_STR);
        builder.append( mle.getId());
        builder.append( SPACE_STR);
        builder.append( actViewingThreadId);
        builder.append( SPACE_STR);
        builder.append( messageIsVisible);
        builder.append( SPACE_STR);
        builder.append( mle.getMessageType());
        builder.append( SPACE_STR);
        builder.append( RSAENCODING.INSTANCE.encodingString( fromID));
        EventLogger.INSTANCE.writeToLogFile( builder.toString(), false);
      }
    }

    private void mergeMessages( MessageListElementParc[] newMessages) {
      if (messages == null) {
        initMessages();
      }
      for (MessageListElementParc newMessage : newMessages) {
        boolean contains = false;
        // .contains not work, because the date of new item != date of old item
        // and
        // tree search does not return a valid value
        // causes problem at thread type messages like Facebook
        for (MessageListElementParc storedMessage : messages) {
          if (storedMessage.equals( newMessage)) {
            contains = true;
          }
        }
        if (!contains) {
          messages.add( newMessage);

          if (((actViewingThreadId != null) && (newMessage.getId().contains( actViewingThreadId + UNDERLINE_SIGN_STR))) || ((actViewingThreadId == null) && (MainActivity.isMainActivityVisible()))) {
            loggingNewMessageArrived( newMessage, true);
          } else {
            loggingNewMessageArrived( newMessage, false);
          }
          // searching PersonAndr here for new messages
          // newMessage.setFrom(PersonAndr.searchPersonAndr(context,
          // newMessage.getFrom()));
        } else {
          MessageListElementParc itemToRemove = null;
          for (MessageListElementParc oldMessage : messages) {
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
              if (newMessage.getDate().after( oldMessage.getDate()) || newMessage.isSeen() && !oldMessage.isSeen()) {
                itemToRemove = oldMessage;
                break;
              }
            }
          }
          if (itemToRemove != null) {
            boolean removed = messages.remove( itemToRemove);
            // updating seen status
            // itemToRemove.setSeen(newMessage.isSeen());
            // updating date
            // itemToRemove.setDate(newMessage.getDate());
            // updating title
            // itemToRemove.setTitle(newMessage.getTitle());
            // update item count
            // itemToRemove.setUnreadCount(newMessage.getUnreadCount());
            messages.add( newMessage);
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

  private class LongOperation extends AsyncTask<String, Integer, List<MessageListElementParc>> {

    private Context context;
    private int result;
    private String errorMessage = null;
    private final Handler handler;
    private final AccountAndr acc;
    private boolean loadMore = false;

    public LongOperation( Context context, Handler handler, AccountAndr acc, boolean loadMore) {
      this.context = context;
      this.handler = handler;
      this.acc = acc;
      this.context = context;
      this.loadMore = loadMore;
    }

    @Override
    protected List<MessageListElementParc> doInBackground( String... params) {

      List<MessageListElementParc> messages = new LinkedList<MessageListElementParc>();
      String accountName = "";
      try {
        MessageProvider mp = null;
        if (acc instanceof GmailAccountAndr) {
          accountName = ((GmailAccount) acc).getEmail();
          mp = new SimpleEmailMessageProvider( (GmailAccount) acc);
          // List<MessageListElement> mle = semp.getMessageList(0,
          // acc.getMessageLimit());
          // messages.addAll(nonParcToParc(mle));
        } else if (acc instanceof EmailAccountAndr) {
          if (iterationCount % 4 == 1) {
            // Log.d("rgai", "GETTING MAIL WITH ACCOUNT: " + acc);
            accountName = ((EmailAccount) acc).getEmail();
            mp = new SimpleEmailMessageProvider( (EmailAccount) acc);
          } else {
            // Log.d("rgai", "SKIP ACCOUNT B. OF iteration count: " + acc);
          }
          // messages.addAll(nonParcToParc(semp.getMessageList(0,
          // acc.getMessageLimit())));
        } else if (acc instanceof FacebookAccountAndr) {
          accountName = ((FacebookAccountAndr) acc).getDisplayName();
          mp = new FacebookMessageProvider( (FacebookAccount) acc);
          // messages.addAll(nonParcToParc(semp.getMessageList(0,
          // acc.getMessageLimit())));
        } else if (acc instanceof SmsAccountAndr) {
          accountName = "SMS";
          mp = new SmsMessageProvider( this.context);
          // messages.addAll(nonParcToParc(smsmp.getMessageList(0,
          // acc.getMessageLimit())));
        }
        if (mp != null) {
          int currentMessagesToAccount = 0;
          if (loadMore) {
            if (MainService.this.messages != null) {
              for (MessageListElementParc m : MainService.this.messages) {
                if (m.getAccount().equals( acc)) {
                  currentMessagesToAccount++;
                }
              }
            }
            // Log.d("rgai","current messages to account " + acc + " is: " +
            // currentMessagesToAccount);
          }
          List<MessageListElementParc> parcelableMessages = nonParcToParc( mp.getMessageList( currentMessagesToAccount, acc.getMessageLimit()));
          for (MessageListElementParc mlep : parcelableMessages) {
            if (!messages.contains( mlep)) {
              messages.add( mlep);
            } else {
              messages.get( messages.indexOf( mlep)).setFrom( mlep.getFrom());
            }
          }
        }

      } catch (AuthenticationFailedException ex) {
        ex.printStackTrace();
        this.result = AUTHENTICATION_FAILED_EXCEPTION;
        this.errorMessage = accountName;
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

    private List<MessageListElementParc> nonParcToParc( List<MessageListElement> origi) {
      List<MessageListElementParc> parc = new LinkedList<MessageListElementParc>();
      for (MessageListElement mle : origi) {
        MessageListElementParc mlep = new MessageListElementParc( mle, acc);
        // Log.d("rgai", "@A message from user -> " + mle.getFrom());
        mlep.setFrom( PersonAndr.searchPersonAndr( context, mle.getFrom()));
        if (mlep.getRecipientsList() != null) {
          for (int i = 0; i < mlep.getRecipientsList().size(); i++) {
            PersonAndr pa = PersonAndr.searchPersonAndr(context, mlep.getRecipientsList().get(i));
            mlep.getRecipientsList().set(i, pa);
            
          }
        }
        // Log.d("rgai", "@A message from REPLACED user -> " + mlep.getFrom());
        parc.add( mlep);
      }

      return parc;
    }

    @Override
    protected void onPostExecute( List<MessageListElementParc> messages) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      if (this.result == OK) {
        bundle.putParcelableArray( "messages", messages.toArray( new MessageListElementParc[messages.size()]));
        // Log.d("rgai", "put messages("+ messages.size() + ") to bundle -> ");
      }
      bundle.putBoolean( "load_more", loadMore);
      bundle.putInt( "result", this.result);
      bundle.putString( "errorMessage", this.errorMessage);

      msg.setData( bundle);
      handler.sendMessage( msg);
    }

    @Override
    protected void onPreExecute() {
      // Log.d(Constants.LOG, "onPreExecute");
    }

  }

}
