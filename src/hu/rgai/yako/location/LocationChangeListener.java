package hu.rgai.yako.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public enum LocationChangeListener {
  INSTANCE;

  public static final String ACTION_LOCATION_CHANGED = "action_location_changed";
  public static final int REQ_CODE_LOCATION_CHANGED = 1;
  private static final long REQUEST_INTERVAL = 2 * 10 * 1000;
  private static final Criteria REQUEST_CRITERIA = new Criteria();
  static {
    REQUEST_CRITERIA.setCostAllowed(false);
    REQUEST_CRITERIA.setPowerRequirement(Criteria.POWER_HIGH);
  }

  private LocationManager locationManager = null;
  public static final float[] WORK_COORDINATES = new float[2];
  public static final float[] HOME_COORDINATES = new float[2];
  public static final float[] BEKE_COORDINATES = new float[2];
  static {
    WORK_COORDINATES[0] = 46.246755f;
    WORK_COORDINATES[1] = 20.146554f;
    HOME_COORDINATES[0] = 46.26704f;
    HOME_COORDINATES[1] = 20.143075f;
    BEKE_COORDINATES[0] = 46.246061f;
    BEKE_COORDINATES[1] = 20.145406f;
  }


  public synchronized void initLocationManager(Context context) {
    if (locationManager == null) {
      locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      PendingIntent pi = getLocationPendingIntent(context);
      locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, REQUEST_INTERVAL, 0.0f, pi);
    } else {
      String bestProvider = locationManager.getBestProvider(REQUEST_CRITERIA, false);
//      String bestProvider = locationManager.getProvider(LocationManager.PASSIVE_PROVIDER).getName();
      Location loc = locationManager.getLastKnownLocation(bestProvider);
      Intent i = new Intent(ACTION_LOCATION_CHANGED);
      i.putExtra("location", loc);
      i.putExtra("best_provider", bestProvider);
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
