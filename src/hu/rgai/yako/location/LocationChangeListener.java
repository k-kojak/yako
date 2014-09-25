package hu.rgai.yako.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

public enum LocationChangeListener {
  INSTANCE;

  public static final String EXTRA_LOCATION = "location";
  public static final String ACTION_LOCATION_CHANGED = "hu.rgai.yako.action_location_changed";
  public static final int REQ_CODE_LOCATION_CHANGED = 1;
  private static final long REQUEST_INTERVAL = 1 * 60 * 1000;
  private static final Criteria REQUEST_CRITERIA = new Criteria();
  static {
    REQUEST_CRITERIA.setCostAllowed(false);
    REQUEST_CRITERIA.setPowerRequirement(Criteria.POWER_HIGH);
  }

  private LocationManager locationManager = null;


  public synchronized void initLocationManager(Context context) {
    if (locationManager == null) {
      locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      PendingIntent pi = getLocationPendingIntent(context);
      locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, REQUEST_INTERVAL, 0.0f, pi);
    }
    String bestProvider = locationManager.getBestProvider(REQUEST_CRITERIA, false);
    Location loc = locationManager.getLastKnownLocation(bestProvider);
    Intent i = new Intent(ACTION_LOCATION_CHANGED);
    if (Math.random() < 0.8) {
      loc = null;
    }
    i.putExtra(EXTRA_LOCATION, loc);
    context.sendBroadcast(i);
  }

  private PendingIntent getLocationPendingIntent(Context context) {
    Intent i = new Intent(ACTION_LOCATION_CHANGED);
    PendingIntent pi = PendingIntent.getBroadcast(context, REQ_CODE_LOCATION_CHANGED, i, PendingIntent.FLAG_UPDATE_CURRENT);
    return pi;
  }

}
