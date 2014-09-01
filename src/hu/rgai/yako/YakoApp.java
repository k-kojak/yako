/**
 * TODO:  1) messages DAO table's content column should contain only the preview text
 *          DONE: 1b) save and load email content to/with FullMessageDAO
 *        2) when loading email to main list, load only content for preview, be lazy and load content only on request
 *        3) save attachment info to database
 *        4) MainListAdapter should display if message has attachment
 */

package hu.rgai.yako;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.test.BuildConfig;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.store.StoreHandler;


import java.util.Date;
import java.util.HashMap;

/**
 * 
 * @author Tamas Kojedzinszky
 */
public class YakoApp extends Application {

  private Tracker tracker = null;

  private volatile static HashMap<Account, Date> lastNotificationDates = null;
  public volatile static MessageListElement mLastNotifiedMessage = null;
  public volatile static Boolean isRaedyForSms = null;
  public static volatile Date lastFullMessageUpdate = null;

  private static void initLastNotificationDates(Context c) {
    if (lastNotificationDates == null) {
      lastNotificationDates = StoreHandler.readLastNotificationObject(c);
      if (lastNotificationDates == null) {
        lastNotificationDates = new HashMap<Account, Date>();
      }
    }
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

  // TODO: this is just a quick hack for KitKat SMS handling
  private boolean isReadyForSmsProviding() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      String thisPackageName = getPackageName();
      if (!Telephony.Sms.getDefaultSmsPackage(this).equals(thisPackageName)) {
        Toast.makeText(this, "Yako is not the default SMS provider. Please set as default at default applications.", Toast.LENGTH_LONG).show();
        return false;
      }
    }
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
    isRaedyForSms = isReadyForSmsProviding();

    AccountDAO.getInstance(this).checkSMSAccount(this, isRaedyForSms);

    MessageListDAO.getInstance(this).purgeMessageList(this, 100);
  }
}
