/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hu.rgai.android.test;

import android.app.Application;
import android.util.Log;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.FullMessage;
import hu.rgai.android.beens.MessageListElement;
import java.util.Iterator;
import java.util.TreeSet;


/**
 *
 * @author k
 */
public class YakoApp extends Application {
  
  private Tracker tracker = null;
  
  private volatile TreeSet<MessageListElement> messages;

  public TreeSet<MessageListElement> getMessages() {
    if (messages == null) {
      messages = new TreeSet<MessageListElement>();
    }
    return messages;
  }
  
  public void setMessageContent(MessageListElement message, FullMessage fullMessage) {
    for (MessageListElement m : getMessages()) {
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
    Iterator<MessageListElement> it = getMessages().iterator();
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
    for (MessageListElement mlep : getMessages()) {
      if (mlep.equals(m) && !mlep.isSeen()) {
        changed = true;
        mlep.setSeen(true);
        mlep.setUnreadCount(0);
        break;
      }
    }
    return changed;
  }
  
  public TreeSet<MessageListElement> getFilteredMessages(Account filterAcc) {
    if (filterAcc == null) {
      return getMessages();
    } else {
      TreeSet<MessageListElement> filterList = new TreeSet<MessageListElement>();
      for (MessageListElement mlep : getMessages()) {
        if (mlep.getAccount().equals(filterAcc)) {
          filterList.add(mlep);
        }
      }
      return filterList;
    }
  }
  
  public MessageListElement getListElementById(String id, Account a) {
    for (MessageListElement mle : getMessages()) {
      if (mle.getId().equals(id) && mle.getAccount().equals(a)) {
        return mle;
      }
    }
    return null;
  }
  
  public TreeSet<MessageListElement> getLoadedMessages(Account account) {
    TreeSet<MessageListElement> selected = new TreeSet<MessageListElement>();
    if (getMessages() != null) {
      for (MessageListElement mle : getMessages()) {
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
