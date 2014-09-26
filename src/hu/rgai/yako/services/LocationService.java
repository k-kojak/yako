package hu.rgai.yako.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.GpsZone;

import java.util.*;

public class LocationService extends Service {

  private static final long MY_LOCATION_LIFE_LENGTH = 5 * 60 * 1000; // in millisec
  public static final String EXTRA_LOCATION = "location";
  public static final String EXTRA_FORCE_UPDATE_ZONES = "hu.rgai.yako.force_update_zones";
  public static final String ACTION_NEW_LOCATION_ARRIVED = "hu.rgai.yako.action_new_location_arrived";
  public static final String ACTION_ZONE_LIST_MUST_REFRESH = "hu.rgai.yako.action_zone_list_must_refresh";
  public static final int REQ_CODE_LOCATION_CHANGED = 1;
  private static final long REQUEST_INTERVAL = 1 * 60 * 1000;
  private static final Criteria REQUEST_CRITERIA = new Criteria();
  static {
    REQUEST_CRITERIA.setCostAllowed(false);
    REQUEST_CRITERIA.setPowerRequirement(Criteria.POWER_HIGH);
  }

  private LocationManager mLocationManager = null;
  private LocationUpdateReceiver mLocationReceiver;
  public static Location mMyLastLocation = null;

  @Override
  public void onCreate() {
    super.onCreate();
    mLocationReceiver = new LocationUpdateReceiver();
    registerReceiver(mLocationReceiver, new IntentFilter(ACTION_NEW_LOCATION_ARRIVED));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && intent.hasExtra(EXTRA_FORCE_UPDATE_ZONES)
            && intent.getBooleanExtra(EXTRA_FORCE_UPDATE_ZONES, false)) {
      List<GpsZone> zones = YakoApp.getSavedGpsZones(this);
      calcActivityState(zones);
      sendZoneListRefreshBroadcast(this);
    } else {
      initLocationManager(this);
    }
    return Service.START_STICKY;
  }

  private void initLocationManager(Context context) {
    if (mLocationManager == null) {
      mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      PendingIntent pi = getLocationPendingIntent(context);
      mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, REQUEST_INTERVAL, 0.0f, pi);
    }
    String bestProvider = mLocationManager.getBestProvider(REQUEST_CRITERIA, false);
    Location loc = mLocationManager.getLastKnownLocation(bestProvider);
    Intent i = new Intent(ACTION_NEW_LOCATION_ARRIVED);
    i.putExtra(EXTRA_LOCATION, loc);
    context.sendBroadcast(i);
  }

  private PendingIntent getLocationPendingIntent(Context context) {
    Intent i = new Intent(ACTION_NEW_LOCATION_ARRIVED);
    PendingIntent pi = PendingIntent.getBroadcast(context, REQ_CODE_LOCATION_CHANGED, i, PendingIntent.FLAG_UPDATE_CURRENT);
    return pi;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private class LocationUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent != null && intent.getAction() != null) {
        if (intent.getAction().equals(ACTION_NEW_LOCATION_ARRIVED)) {
          // saving MyLocation...
          Log.d("yako", "## new location");
          Location newLocation = (Location) intent.getExtras().get(LocationService.EXTRA_LOCATION);
          if (newLocation != null) {
            double birizgaLat = Math.random() / 100000;
            double birizgaLong = Math.random() / 100000;
            Log.d("yako", " #birizga: "+birizgaLat + " , " + birizgaLong);
            newLocation.setLatitude(newLocation.getLatitude() + birizgaLat);
            newLocation.setLongitude(newLocation.getLongitude() + birizgaLong);
          }
          if (newLocation != null
                  || mMyLastLocation == null
                  || (mMyLastLocation.getTime() + MY_LOCATION_LIFE_LENGTH < System.currentTimeMillis())) {

            boolean locationChanged = false;
            if ((mMyLastLocation != null && newLocation == null)
                    || (mMyLastLocation == null && newLocation != null)
                    || (mMyLastLocation != null && newLocation != null
                      && (mMyLastLocation.getLatitude() != newLocation.getLatitude()
                      || mMyLastLocation.getLongitude() != newLocation.getLongitude() ) ) ) {
              locationChanged = true;
            }
            Log.d("yako", " # new loc: " + newLocation);
            Log.d("yako", " # old loc: " + mMyLastLocation);
            mMyLastLocation = newLocation;
            if (locationChanged) {
              Log.d("yako", " # location changed");
              locationChanged(context);
            } else {
              Log.d("yako", " # location not changed");
            }
          }
        }
      }
    }

    private void locationChanged(Context context) {
      List<GpsZone> zones = YakoApp.getSavedGpsZones(context);
      ZoneActivityCalcResult zoneActivityStateResult = calcActivityState(zones);
      Log.d("yako", " # zone act calc result: " + zoneActivityStateResult.toString());
      if (zoneActivityStateResult.zoneActivityChanged) {
        /**
         * TODO: run prediction to all messages;
         * TODO: send a broadcast about new message order AFTER prediction;
         * TODO: refresh zone list
         */
        sendZoneListRefreshBroadcast(context);
      } else if (zoneActivityStateResult.distanceChanged) {
        // Just refresh zone list
        sendZoneListRefreshBroadcast(context);
      }
    }

  }

  private void sendZoneListRefreshBroadcast(Context context) {
    LocalBroadcastManager bcManager = LocalBroadcastManager.getInstance(context);
    bcManager.sendBroadcast(new Intent(ACTION_ZONE_LIST_MUST_REFRESH));
  }

  private ZoneActivityCalcResult calcActivityState(List<GpsZone> zones) {
    boolean distanceChanged = true;
    boolean zoneActivityChanged = true;

    if (mMyLastLocation != null) {
      distanceChanged = false;
      zoneActivityChanged = false;

      TreeSet<GpsZone> nearLocationList = new TreeSet<GpsZone>();
      String closestLoc = null;
      float closest = Float.MAX_VALUE;

      for (GpsZone zone : zones) {
        int newDistance = Math.round(getDist(
                (float) zone.getLat(),
                (float) zone.getLong(),
                (float) mMyLastLocation.getLatitude(),
                (float) mMyLastLocation.getLongitude()));

        if (newDistance != zone.getDistance()) {
          distanceChanged = true;
        }
        zone.setDistance(newDistance);
        if (newDistance <= zone.getRadius()) {
          nearLocationList.add(zone);
          if (newDistance < closest) {
            closest = newDistance;
            closestLoc = zone.getAlias();
          }
        }
      }



      for (GpsZone zone : zones) {
        GpsZone.Proximity newProximity;
        if (zone.getAlias().equals(closestLoc)) {
          newProximity = GpsZone.Proximity.CLOSEST;
        } else if (nearLocationList.contains(zone)) {
          newProximity = GpsZone.Proximity.NEAR;
        } else {
          newProximity = GpsZone.Proximity.FAR;
        }

        if (!zone.getProximity().equals(newProximity)) {
          zoneActivityChanged = true;
        }
        zone.setProximity(newProximity);
      }
    }

    return new ZoneActivityCalcResult(distanceChanged, zoneActivityChanged);
  }

  private static float getDist(float x1, float y1, float x2, float y2) {
    float[] dist = new float[2];
    Location.distanceBetween(x1, y1, x2, y2, dist);
    return dist[0];
  }

  private class ZoneActivityCalcResult {
    private final boolean distanceChanged;
    private final boolean zoneActivityChanged;

    private ZoneActivityCalcResult(boolean distanceChanged, boolean zoneActivityChanged) {
      this.distanceChanged = distanceChanged;
      this.zoneActivityChanged = zoneActivityChanged;
    }

    @Override
    public String toString() {
      return "ZoneActivityCalcResult{" +
              "distanceChanged=" + distanceChanged +
              ", zoneActivityChanged=" + zoneActivityChanged +
              '}';
    }
  }

}
