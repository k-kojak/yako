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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Telephony;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.model.LatLng;
import hu.rgai.android.test.BuildConfig;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.GpsZoneDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.workers.MyAsyncTask;


import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 
 * @author Tamas Kojedzinszky
 */
public class YakoApp extends Application {

  private Tracker tracker = null;

  private static LatLng fakeLocation = null;

  private volatile static HashMap<Account, Date> lastNotificationDates = null;
  public volatile static MessageListElement mLastNotifiedMessage = null;
  public volatile static Boolean isRaedyForSms = null;
  public static volatile Date lastFullMessageUpdate = null;
  public static volatile List<GpsZone> gpsZones = null;

  private static int mPreviousActionbarColor = Settings.DEFAULT_ACTIONBAR_COLOR;

  private static void initLastNotificationDates(Context c) {
    if (lastNotificationDates == null) {
      lastNotificationDates = StoreHandler.readLastNotificationObject(c);
      if (lastNotificationDates == null) {
        lastNotificationDates = new HashMap<Account, Date>();
      }
    }
  }

  public static LatLng getFakeLocation() {
    return fakeLocation;
  }

  public static void setFakeLocation(LatLng location) {
    fakeLocation = location;
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

  public static void printAsyncTasks(boolean printTasks) {
    ThreadPoolExecutor tpe = (ThreadPoolExecutor)MyAsyncTask.THREAD_POOL_EXECUTOR;
    Log.d("yako", tpe.toString());
    if (printTasks) {
      BlockingQueue<Runnable> rq = tpe.getQueue();
      for (Runnable r : rq) {
        Log.d("yako", r.toString());
      }
      Log.d("yako", " - - - - - - - ");
    }
  }


  /**
   * Returns all of the gps zones with distance and proximity information (if have).
   * @param context
   * @return
   */
  public static List<GpsZone> getSavedGpsZones (Context context) {
    return getSavedGpsZones(context, false);
  }


  /**
   * Returns all of the gps zones with distance and proximity information (if have).
   * @param context
   * @param forceLoadFromDatabase if true, function will load zone values from database
   * @return
   */
  public static List<GpsZone> getSavedGpsZones (Context context, boolean forceLoadFromDatabase) {
    if (gpsZones == null || forceLoadFromDatabase) {
      gpsZones = GpsZoneDAO.getInstance(context).getAllZones();
    }
    return gpsZones;
  }

  public static GpsZone getClosestZone(Context context, boolean forceLoadFromDatabase) {
    GpsZone closest = null;
    if (StoreHandler.isZoneStateActivated(context)) {
      List<GpsZone> gpsZones = YakoApp.getSavedGpsZones(context, forceLoadFromDatabase);
      closest = GpsZone.getClosest(gpsZones);
    }
    return closest;
  }

  public static void setActionBarColor(ActionBarActivity aba, GpsZone closest) {
    int targetColor;

    if (closest != null) {
      targetColor = 0xff << 24 | closest.getZoneType().getColor();
    } else {
      targetColor =  Settings.DEFAULT_ACTIONBAR_COLOR;
    }

    String device = getDeviceName();
    ColorDrawable targetColorDr = new ColorDrawable(targetColor);
    if (device.startsWith("htc")) {
      aba.getSupportActionBar().setBackgroundDrawable(targetColorDr);
    } else {
      TransitionDrawable titleAnimation = new TransitionDrawable(
              new ColorDrawable[]{new ColorDrawable(mPreviousActionbarColor), targetColorDr});
      aba.getSupportActionBar().setBackgroundDrawable(titleAnimation);
      titleAnimation.startTransition(1000);
    }


    mPreviousActionbarColor = targetColor;
  }

  public static String getDeviceName() {
    String manufacturer = Build.MANUFACTURER.toLowerCase();
    String model = Build.MODEL.toLowerCase();
    if (model.startsWith(manufacturer)) {
      return model;
    } else {
      return manufacturer + " " + model;
    }
  }

  public static void setActionBarTitle(ActionBarActivity aba, GpsZone closest, boolean setTitle, boolean setSubtitle) {
    String title = "";
    String subTitle = "";
    if (closest != null) {
      title = "@ " + closest.getAlias();
      subTitle = closest.getZoneType().getDisplayName(aba);
    }
    if (setTitle) {
      aba.getSupportActionBar().setTitle(title);
    }
    if (setSubtitle) {
      aba.getSupportActionBar().setSubtitle(subTitle.toUpperCase());
    }
  }


  @Override
  public void onCreate() {
    super.onCreate();

    try {
      AsyncTask.class.getMethod("setDefaultExecutor", Executor.class).invoke(null, AsyncTask.SERIAL_EXECUTOR);
    } catch (IllegalAccessException e) {
      Log.d("yako", "", e);
    } catch (InvocationTargetException e) {
      Log.d("yako", "", e);
    } catch (NoSuchMethodException e) {
      Log.d("yako", "", e);
    } catch (Exception e) {
      Log.d("yako", "", e);
    }

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
