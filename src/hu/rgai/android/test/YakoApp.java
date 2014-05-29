
package hu.rgai.android.test;

import android.app.Application;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.FacebookAccount;
import hu.rgai.android.beens.FullMessage;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.store.StoreHandler;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;


/**
 *
 * @author Tamas Kojedzinszky
 */
public class YakoApp extends Application {
  
  private Tracker tracker = null;
  
  private volatile static TreeSet<MessageListElement> messages = new TreeSet<MessageListElement>();
  private volatile static  HashMap<Account, Date> lastNotificationDates = new HashMap<Account, Date>();
  public volatile static MessageListElement mLastNotifiedMessage = null;
  public volatile static boolean isPhone;
  public static volatile Date lastMessageUpdate = new Date();
  private volatile static TreeSet<Account> accounts;

  public static TreeSet<MessageListElement> getMessages() {
    return messages;
  }
  
  public static TreeSet<Account> getAccounts(Context context) {
    if (accounts == null) {
      loadAccountsFromStore(context);
    }
    return accounts;
  }
  
  private static void loadAccountsFromStore(Context context) {
    accounts = StoreHandler.getAccounts(context);
  }
  
  public static void setAccounts(TreeSet<Account> newAccs) {
    accounts = newAccs;
  }
  
  public static boolean isFacebookAccountAdded(Context context) {
    for (Account a : getAccounts(context)) {
      if (a.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
        return true;
      }
    }
    return false;
  }
  
  
  public static MessageListElement getMessageById_Account_Date(String id, Account acc) {
    MessageListElement compareElement = new MessageListElement(id, acc);
    for (MessageListElement mle : messages) {
      if (mle.equals(compareElement)) {
        return mle;
      }
    }
    return null;
  }
  
  public static void setMessageContent(MessageListElement message, FullMessage fullMessage) {
    for (MessageListElement m : messages) {
      if (m.equals(message)) {
        m.setFullMessage(fullMessage);
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
  public static void removeMessagesToAccount(Account account) {
    Iterator<MessageListElement> it = messages.iterator();
    while (it.hasNext()) {
      MessageListElement mle = it.next();
      if (mle.getAccount().equals(account)) {
        it.remove();
      }
    }
  }
  
  
  /**
   * Sets the seen status to true, and the unreadCount to 0.
   * 
   * @param m  the message to set
   * @return true if status changed, false otherwise
   * 
   */
  public static boolean setMessageSeenAndReadLocally(MessageListElement m) {
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

  
  private static void initLastNotificationDates(Context c) {
    if (lastNotificationDates == null) {
      lastNotificationDates = StoreHandler.readLastNotificationObject(c);
      if (lastNotificationDates == null) {
        lastNotificationDates = new HashMap<Account, Date>();
      }
    }
  }
  
  
  /**
   * Updates the last notification date of the given account.
   * Sets all of the accounts last notification date to the current date if null given.
   * 
   * @param acc the account to update, or null if update all account's last event time
   * @param c
   */
  public static void updateLastNotification(Account acc, Context c) {
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
  
  
  private boolean setIsPhone() {
    TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    int simState = telMgr.getSimState();
    if (simState != TelephonyManager.SIM_STATE_ABSENT) {
      return true;
    } else {
      return false;
    }
  }
  
  
  /**
   * Returns the last notification of the given account.
   * 
   * @param acc last notification time will be set to this account
   * @return
   */
  public static Date getLastNotification(Account acc) {
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
  
  
  public static void setLastNotifiedMessage(MessageListElement lastNotifiedMessage) {
    mLastNotifiedMessage = lastNotifiedMessage;
  }

  
  public static MessageListElement getLastNotifiedMessage() {
    return mLastNotifiedMessage;
  }
  
  
  public static TreeSet<MessageListElement> getFilteredMessages(Account filterAcc) {
    if (filterAcc == null) {
      return messages;
    } else {
      TreeSet<MessageListElement> filterList = new TreeSet<MessageListElement>();
      for (MessageListElement mlep : messages) {
        if (mlep.getAccount().equals(filterAcc)) {
          filterList.add(mlep);
        }
      }
      return filterList;
    }
  }
  
  
  public synchronized Tracker getTracker() {
    if (tracker == null) {
      GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
      tracker = analytics.newTracker(R.xml.analytics);
    }
    return tracker;
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
    
  }
  
}
