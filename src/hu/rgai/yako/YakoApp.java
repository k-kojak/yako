
package hu.rgai.yako;

import android.app.Application;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.test.BuildConfig;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.FullMessage;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.store.StoreHandler;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 *
 * @author Tamas Kojedzinszky
 */
public class YakoApp extends Application {
  
  private Tracker tracker = null;
  
//  private volatile static TreeSet<MessageListElement> messages = null;
  private volatile static  HashMap<Account, Date> lastNotificationDates = null;
  public volatile static MessageListElement mLastNotifiedMessage = null;
  public volatile static Boolean isPhone = null;
  public static volatile Date lastFullMessageUpdate = null;
//  private volatile static TreeSet<Account> accounts;



//  public static boolean hasMessages() {
//    return messages != null && !messages.isEmpty();
//  }


//  public static void setAccounts(TreeSet<Account> newAccs) {
//    accounts = newAccs;
//  }
  

//  public static MessageListElement getMessageById_Account_Date(String id, Account acc) {
//    MessageListElement compareElement = new MessageListElement(id, acc);
//    for (MessageListElement mle : messages) {
//      if (mle.equals(compareElement)) {
//        return mle;
//      }
//    }
//    return null;
//  }
  
//  public static void setMessageContent(MessageListElement message, FullMessage fullMessage) {
//    for (MessageListElement m : messages) {
//      if (m.equals(message)) {
//        m.setFullMessage(fullMessage);
//        break;
//      }
//    }
//  }
  
//  /**
//   * Removes messages from message list where the instance matches with the
//   * parameter.
//   *
//   * @param account
//   */
//  public static void removeMessagesToAccount(Account account) {
//    Iterator<MessageListElement> it = messages.iterator();
//    while (it.hasNext()) {
//      MessageListElement mle = it.next();
//      if (mle.getAccount().equals(account)) {
//        it.remove();
//      }
//    }
//  }
  
  
//  /**
//   * Sets the seen status to true, and the unreadCount to 0.
//   *
//   * @param m  the message to set
//   * @return true if status changed, false otherwise
//   *
//   */
//  public static boolean setMessageSeenAndReadLocally(MessageListElement m) {
//    boolean changed = false;
//    for (MessageListElement mlep : messages) {
//      if (mlep.equals(m) && !mlep.isSeen()) {
//        changed = true;
//        mlep.setSeen(true);
//        mlep.setUnreadCount(0);
//        break;
//      }
//    }
//    return changed;
//  }

  
  private static void initLastNotificationDates(Context c) {
    if (lastNotificationDates == null) {
      lastNotificationDates = StoreHandler.readLastNotificationObject(c);
      if (lastNotificationDates == null) {
        lastNotificationDates = new HashMap<Account, Date>();
      }
    }
//    Log.d("rgai2", "read in last notification dates: " + lastNotificationDates);
  }
  
  
  /**
   * Updates the last notification date of the given instance.
   * Sets all of the accounts last notification date to the current date if null given.
   * 
   * @param acc the instance to update, or null if update all instance's last event time
   * @param c
   */
  public synchronized static void updateLastNotification(Account acc, Context c) {
    initLastNotificationDates(c);
    if (acc != null) {
      lastNotificationDates.put(acc, new Date());
    } else {
      for (Account a : lastNotificationDates.keySet()) {
        lastNotificationDates.get(a).setTime(new Date().getTime());
      }
    }
    StoreHandler.writeLastNotificationObject(c, lastNotificationDates);
  }
  

  /**
   * Returns the last notification of the given instance.
   * 
   * @param acc last notification time will be set to this instance
   * @return
   */
  public static Date getLastNotification(Account acc, Context c) {
    initLastNotificationDates(c);
    Date ret = null;
    if (lastNotificationDates == null || acc == null) {
      ret = new Date(new Date().getTime() - 86400L * 365 * 1000);
    } else {
      ret = lastNotificationDates.get(acc);
    }
    if (ret == null) {
      ret = new Date(new Date().getTime() - 86400L * 365 * 1000);
    }
    return ret;
  }
  

  private boolean setIsPhone() {
    TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    int simState = telMgr.getSimState();
    if (simState == TelephonyManager.SIM_STATE_READY) {
      return true;
    } else {
      return false;
    }
  }
  
  
  public static void setLastNotifiedMessage(MessageListElement lastNotifiedMessage) {
    mLastNotifiedMessage = lastNotifiedMessage;
  }

  
  public static MessageListElement getLastNotifiedMessage() {
    return mLastNotifiedMessage;
  }
  
  
//  public static TreeSet<MessageListElement> getFilteredMessages(Account filterAcc) {
//    if (filterAcc == null) {
//      return messages;
//    } else {
//      TreeSet<MessageListElement> filterList = new TreeSet<MessageListElement>();
//      synchronized (messages) {
//        for (MessageListElement mlep : messages) {
//          if (mlep.getAccount().equals(filterAcc)) {
//            filterList.add(mlep);
//          }
//        }
//      }
//      return filterList;
//    }
//  }
  
  
  public synchronized Tracker getTracker() {
    if (tracker == null) {
      GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
      tracker = analytics.newTracker(R.xml.analytics);
    }
    return tracker;
  }
  
  public void sendAnalyticsError(int code) {
     Tracker t = getTracker();
     t.send(new HitBuilders.ExceptionBuilder()
             .setDescription("Custom, own catched error. Error code: " + code)
             .setFatal(false)
             .build());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    // IMPORTANT: SPEND AS LESS TIME HERE AS POSSIBLE
    if (BuildConfig.DEBUG) {
      Log.d("rgai", "#TURNING OFF GOOGLE ANALYTICS: WE ARE IN DEVELOPE MODE");
      GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(this);
      googleAnalytics.setAppOptOut(true);
    } else {
      Log.d("rgai", "#DO NOT TURN GOOGLE ANALYTICS OFF: WE ARE IN PRODUCTION MODE");
    }
    
    // TODO: we may have to update it on network state change!
    isPhone = setIsPhone();

    AccountDAO.getInstance(this).checkSMSAccount(this, isPhone);

    // read in message list
    long start = System.currentTimeMillis();
    AccountDAO accountDAO = AccountDAO.getInstance(this);
    TreeMap<Long, Account> accounts = accountDAO.getIdToAccountsMap();
    accountDAO.close();

//    MessageListDAO msgDAO = MessageListDAO.getInstance(this).getAllMessages(accounts);
//    messages = msgDAO.getAllMessages(accounts);
//    msgDAO.close();
//    Log.d("rgai", "time to read "+ messages.size() +" items from db: " + (System.currentTimeMillis() - start) + " ms");
//    if (messages == null) {
//      messages = new TreeSet<MessageListElement>();
//    }
  }
}
