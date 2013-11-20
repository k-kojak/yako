package hu.rgai.android.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.intent.beens.account.GmailAccountAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.Constants;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.GmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.MessageListElement;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullMessage;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
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
  
  
  private Handler handler = null;
//  private LongOperation myThread = null;
  private final IBinder mBinder = new MyBinder();
  private Set<MessageListElementParc> messages = null;
  
  public MainService() {
//    super("valami nev");
//    Log.d("rgai", "myservice default constructor");
  }

  @Override
  public void onCreate() {
//    Log.d("rgai", "service oncreate");
    RUNNING = true;
    handler = new MyHandler(this);
    
//    IntentFilter filter = new IntentFilter(Constants.EMAIL_CONTENT_CHANGED_BC_MSG);
//    registerReceiver(emailContentChangeReceiver, filter);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    RUNNING = false;
//    unregisterReceiver(emailContentChangeReceiver);
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (isNetworkAvailable()) {
      List<AccountAndr> accounts = StoreHandler.getAccounts(this);
      if (accounts.isEmpty()) {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putInt("result", NO_ACCOUNT_SET);
        msg.setData(bundle);
        handler.sendMessage(msg);
      } else {
        for (AccountAndr acc : accounts) {
          LongOperation myThread = new LongOperation(handler, acc, this);
          myThread.execute();
        }
      }
//      myThread = new LongOperation(handler);
//      myThread.execute();
    } else {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putInt("result", NO_INTERNET_ACCESS);
      msg.setData(bundle);
      handler.sendMessage(msg);
    }
    
    return Service.START_STICKY;
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
  public void setMessageContent(String id, AccountAndr account, FullMessage fullMessage) {
  
    for (MessageListElementParc mlep : messages) {
      if (mlep.getId().equals(id) && mlep.getAccount().equals(account)) {
        mlep.setFullMessage(fullMessage);
        break;
      }
    }
  }
  
  /**
   * 
   * @param id
   * @return 
   * @deprecated use setMessageSeen instead
   */
  public boolean setMailSeen(String id) {
    boolean changed = false;
    for (MessageListElementParc mlep : messages) {
      if (mlep.getId().equals(id)) {
        if (mlep.isSeen()) {
          changed = true;
        }
        mlep.setSeen(true);
      }
    }
    return changed;
  }
  
  public boolean setMessageSeen(MessageListElementParc m) {
    boolean changed = false;
    for (MessageListElementParc mlep : messages) {
      if (mlep.equals(m) && !mlep.isSeen()) {
        changed = true;
        mlep.setSeen(true);
        break;
      }
    }
    return changed;
  }
  
  public MessageListElementParc getListElementById(String id, AccountAndr a) {
    for (MessageListElementParc mlep : messages) {
      if (mlep.getId().equals(id) && mlep.getAccount().equals(a)) {
        return mlep;
      } else {
//        Log.d("rgai", mlep.getId() + " != " + id + " && " + mlep.getAccount() + " != " + a);
      }
    }
    
    return null;
  }
  
  public MessageListElementParc[] getEmails() {
    if (messages != null) {
      return messages.toArray(new MessageListElementParc[0]);
    } else {
      return null;
    }
  }
  
  public void removeElementsFromList(AccountAndr acc) {
    if (messages != null) {
      for (MessageListElementParc mle : messages) {
        if (mle.getAccount().equals(acc)) {
          Log.d("rgai", "removing message list element -> " + mle);
          messages.remove(mle);
          removeElementsFromList(acc);
          break;
        }
      }
    }
  }

//  @Override
//  protected void onHandleIntent(Intent arg0) {
//    Log.d("rgai", );
//  }
  
  private class MyHandler extends Handler {
    
    private Context context;
//    
    public MyHandler(Context context) {
      this.context = context;
    }
    
    @Override
    public void handleMessage(Message msg) {
//      Log.d("rgai", "message arrived");
      Bundle bundle = msg.getData();
      int newMessageCount = 0;
      if (bundle != null) {
        if (bundle.get("result") != null) {
//          Log.d("rgai", bundle.getInt("result") + "");
          Intent intent = new Intent(Constants.MAIL_SERVICE_INTENT);
          intent.putExtra("result", bundle.getInt("result"));
          if (bundle.get("errorMessage") != null) {
            intent.putExtra("errorMessage", bundle.getString("errorMessage"));
          }
          if (bundle.getInt("result") == OK && bundle.get("messages") != null) {
//            Log.d("rgai", "message != null");
            MessageListElementParc[] newMessages = (MessageListElementParc[]) bundle.getParcelableArray("messages");
            // check if is there any new, unseen emails
            for (int i = 0; i < newMessages.length; i++) {
              if (!newMessages[i].isSeen()) {
                newMessageCount++;
              }
            }
//            if (emails != null) {
//              for (int i = 0; i < newMessages.length; i++) {
//                
//                for (int j = 0; j < emails.length; j++) {
//                  if (newMessages[i].getId() == emails[j].getId() && emails[j].getContent() != null) {
//                    newMessages[j].setContent(emails[i].getContent());
//                  }
//                }
//              }
//            }
            this.mergeMessages(newMessages);
//            messages = newMessages;
            intent.putExtra("messages", messages.toArray(new MessageListElementParc[0]));
//            Log.d("rgai", "intent.putExtra, size: " + newMessages.length);
            
            if (newMessageCount != 0) {
              NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                      .setSmallIcon(R.drawable.gmail_icon)
                      .setContentTitle(newMessageCount + " unread message" + (newMessageCount > 1 ? "s" : ""))
                      .setContentText(newMessageCount + " unread message" + (newMessageCount > 1 ? "s" : ""));
              Intent resultIntent = new Intent(context, MainActivity.class);
              TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
              stackBuilder.addParentStack(MainActivity.class);
              stackBuilder.addNextIntent(resultIntent);
              PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
              mBuilder.setContentIntent(resultPendingIntent);
              mBuilder.setAutoCancel(true);

              NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
              mNotificationManager.notify(10, mBuilder.build());
              
            }
          } else {
//            Log.d("rgai", "message == null");
          }
//          Log.d("rgai", "sending broadcast");
          sendBroadcast(intent);
        }
      }
    }
    
    private void mergeMessages(MessageListElementParc[] newMessages) {
      if (messages == null) {
        messages = new TreeSet<MessageListElementParc>();
      }
      for (MessageListElementParc newMessage : newMessages) {
        boolean contains = false;
        // .contains not work, because the date of new item != date of old item and 
        // tree search does not return a valid value
        for (MessageListElementParc storedMessage : messages) {
          if (storedMessage.equals(newMessage)) {
            contains = true;
          }
        }
        if (!contains) {
          messages.add(newMessage);
        } else {
          MessageListElementParc itemToRemove = null;
          for (MessageListElementParc oldMessage : messages) {
            if (newMessage.equals(oldMessage)) {
              /* "Marking" FB message seen here.
               * Do not change info of the item, if the date is the same, so the queried
               * data will not override the displayed object.
               * Facebook does not marks messages as seen when opening them, so we have to
               * handle it at client side.
               * OR if we check the message at FB, then turn it seen at the app
               */
              if (newMessage.getDate().after(oldMessage.getDate())
                      || newMessage.isSeen() && !oldMessage.isSeen()) {
                itemToRemove = oldMessage;
                break;
              }
            }
          }
          if (itemToRemove != null) {
            boolean removed = messages.remove(itemToRemove);
            // updating seen status
//            itemToRemove.setSeen(newMessage.isSeen());
            // updating date
//            itemToRemove.setDate(newMessage.getDate());
            // updating title
//            itemToRemove.setTitle(newMessage.getTitle());
            // update item count
//            itemToRemove.setUnreadCount(newMessage.getUnreadCount());
            messages.add(newMessage);
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
  
  private class LongOperation extends AsyncTask<String, Integer, List<MessageListElementParc> > {

    
    private int result;
    private String errorMessage = null;
    private Handler handler;
    private AccountAndr acc;
    private Context context;
    
    
    public LongOperation(Handler handler, AccountAndr acc, Context context) {
      this.handler = handler;
      this.acc = acc;
      this.context = context;
    }
    
    @Override
    protected List<MessageListElementParc> doInBackground(String... params) {
      
      List<MessageListElementParc> messages = new LinkedList<MessageListElementParc>();
      String accountName = "";
      try {
        if (acc instanceof GmailAccountAndr) {
          accountName = ((GmailAccount)acc).getEmail();
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((GmailAccount)acc);
          List<MessageListElement> mle = semp.getMessageList(0, acc.getMessageLimit());
          
          messages.addAll(nonParcToParc(mle));
        } else if (acc instanceof EmailAccountAndr) {
          
          accountName = ((EmailAccount)acc).getEmail();
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((EmailAccount)acc);
          messages.addAll(nonParcToParc(semp.getMessageList(0, acc.getMessageLimit())));
        } else if (acc instanceof FacebookAccountAndr) {
          accountName = ((FacebookAccountAndr)acc).getDisplayName();
          FacebookMessageProvider semp = new FacebookMessageProvider((FacebookAccount)acc);
          messages.addAll(nonParcToParc(semp.getMessageList(0, acc.getMessageLimit())));
        }
        SmsMessageProvider smsmp = new SmsMessageProvider(this.context);
        messages.addAll(nonParcToParc(smsmp.getMessageList(0, 10)));
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
    
    private List<MessageListElementParc> nonParcToParc(List<MessageListElement> origi) {
      List<MessageListElementParc> parc = new LinkedList<MessageListElementParc>();
      for (MessageListElement mle : origi) {
        parc.add(new MessageListElementParc(mle, acc));
      }
      
      return parc;
    }
    
    @Override
    protected void onPostExecute(List<MessageListElementParc> messages) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      if (this.result == OK) {
        bundle.putParcelableArray("messages", messages.toArray(new MessageListElementParc[messages.size()]));
//        Log.d("rgai", "put messages("+ messages.size() + ") to bundle -> ");
      }
      bundle.putInt("result", this.result);
      bundle.putString("errorMessage", this.errorMessage);
      
      msg.setData(bundle);
      handler.sendMessage(msg);
    }

    @Override
    protected void onPreExecute() {
      Log.d(Constants.LOG, "onPreExecute");
    }

//    @Override
//    protected void onProgressUpdate(Integer... values) {
//      Log.d(Constants.LOG, "onProgressUpdate");
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//
//      bundle.putInt("progress", values[0]);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
//    }
  }
  
}
