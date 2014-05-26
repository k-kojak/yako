/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hu.rgai.android.test;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.FullMessage;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.store.StoreHandler;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;


/**
 *
 * @author k
 */
public class YakoApp extends Application {
  
  private Tracker tracker = null;
  
  private volatile static TreeSet<MessageListElement> messages = new TreeSet<MessageListElement>();
  private volatile static  HashMap<Account, Date> last_notification_dates = new HashMap<Account, Date>();
  public volatile static MessageListElement mLastNotifiedMessage = null;

  public TreeSet<MessageListElement> getMessages() {
    return messages;
  }
  
  public void setMessageContent(MessageListElement message, FullMessage fullMessage) {
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
  public void removeMessagesToAccount(Account account) {
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
  public boolean setMessageSeenAndReadLocally(MessageListElement m) {
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
  
  /**
   * Updates the last notification date of the given account. Sets all of the
   * accounts last notification date to the current date if null given.
   * 
   * @param acc
   *          the account to update, or null if update all account's last event
   *          time
   */
  public void updateLastNotification(Account acc) {
    if (acc != null) {
      last_notification_dates.put(acc, new Date());
    } else {
      for (Account a : last_notification_dates.keySet()) {
        last_notification_dates.get(a).setTime(new Date().getTime());
      }
    }
    StoreHandler.writeLastNotificationObject(this, last_notification_dates);
  }

  /**
   * Returns the last notification of the given account.
   * 
   * @param acc
   *          last notification time will be set to this account
   * @return
   */
  public Date getLastNotification(Account acc) {
    Date ret = null;
    if (last_notification_dates == null || acc == null) {
      ret = new Date(new Date().getTime() - 86400L * 365 * 1000);
    } else {
      ret = last_notification_dates.get(acc);
    }
    if (ret == null) {
      ret = new Date(new Date().getTime() - 86400L * 365 * 1000);
    }
    return ret;
  }
  
  public void setLastNotifiedMessage(MessageListElement lastNotifiedMessage) {
    mLastNotifiedMessage = lastNotifiedMessage;
  }

  public MessageListElement getmLastNotifiedMessage() {
    return mLastNotifiedMessage;
  }
  
  public TreeSet<MessageListElement> getFilteredMessages(Account filterAcc) {
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
  
  public MessageListElement getListElementById(String id, Account a) {
    for (MessageListElement mle : messages) {
      if (mle.getId().equals(id) && mle.getAccount().equals(a)) {
        return mle;
      }
    }
    return null;
  }
  
  public TreeSet<MessageListElement> getLoadedMessages(Account account) {
    TreeSet<MessageListElement> selected = new TreeSet<MessageListElement>();
    if (messages != null) {
      for (MessageListElement mle : messages) {
        if (mle.getAccount().equals(account)) {
          selected.add(mle);
        }
      }
    }
    return selected;
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
  }
 
  
  
}
