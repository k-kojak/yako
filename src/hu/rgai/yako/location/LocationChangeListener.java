package hu.rgai.yako.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

/**
 * Created by kojak on 9/22/2014.
 */
public enum LocationChangeListener {
  INSTANCE;

  public static final String ACTION_LOCATION_CHANGED = "action_location_changed";
  public static final int REQ_CODE_LOCATION_CHANGED = 1;
  private static final long REQUEST_INTERVAL = 2 * 60 * 1000;
  private static final Criteria REQUEST_CRITERIA = new Criteria();
  static {
    REQUEST_CRITERIA.setCostAllowed(false);
    REQUEST_CRITERIA.setPowerRequirement(Criteria.POWER_LOW);
  }

  private LocationManager locationManager = null;
  public static final float[] WORK_COORDINATES = new float[2];
  public static final float[] HOME_COORDINATES = new float[2];
  static {
    WORK_COORDINATES[0] = 46.246755f;
    WORK_COORDINATES[1] = 20.146554f;
    HOME_COORDINATES[0] = 46.26704f;
    HOME_COORDINATES[1] = 20.143075f;
  }


  public synchronized void initLocationManager(Context context) {
    if (locationManager == null) {
      locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      PendingIntent pi = getLocationPendingIntent(context);
      locationManager.requestLocationUpdates(REQUEST_INTERVAL, 0.0f, REQUEST_CRITERIA, pi);
    } else {
      Location loc = locationManager.getLastKnownLocation(locationManager.getBestProvider(REQUEST_CRITERIA, true));
      Intent i = new Intent(ACTION_LOCATION_CHANGED);
      i.putExtra("location", loc);
      context.sendBroadcast(i);
      Log.d("yako", "send location broadcast...");
    }

  }

  private PendingIntent getLocationPendingIntent(Context context) {
    Intent i = new Intent(ACTION_LOCATION_CHANGED);
    PendingIntent pi = PendingIntent.getBroadcast(context, REQ_CODE_LOCATION_CHANGED, i, PendingIntent.FLAG_UPDATE_CURRENT);
    return pi;
  }

}
