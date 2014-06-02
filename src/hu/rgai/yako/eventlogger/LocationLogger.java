package hu.rgai.yako.eventlogger;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationLogger implements LocationListener {

  private LocationManager locationManager;

  private String provider;

  private final Context context;

  boolean isGPSEnabled = false;

  boolean isNetworkEnabled = false;

  Location gpsLocation;

  Location networkLocation;

  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

  private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

  public LocationLogger(Context context) {
    super();
    this.context = context;

  }

  public void updateLocation() {
    try {
      locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      if (locationManager == null)
        return;

      // getting GPS status
      isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

      // getting network status
      isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

      if (!isGPSEnabled && !isNetworkEnabled) {
//        Log.d("willrgai", " no provider is enabled");
      } else {
        // First get location from Network Provider
        loggingLocation();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loggingLocation() {
    if (isNetworkEnabled) {
      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
      networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }
    // if GPS Enabled get lat/long using GPS Services
    if (isGPSEnabled) {
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
      gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }
    if ((gpsLocation != null) && (networkLocation != null)) {
      if (gpsLocation.getAccuracy() > networkLocation.getAccuracy())
        EventLogger.INSTANCE.writeToLogFile(getFormatedLocationString(gpsLocation), true);
      else
        EventLogger.INSTANCE.writeToLogFile(getFormatedLocationString(networkLocation), true);
    } else {
      if (gpsLocation != null) {
        EventLogger.INSTANCE.writeToLogFile(getFormatedLocationString(gpsLocation), true);
      }
      if (networkLocation != null) {
        EventLogger.INSTANCE.writeToLogFile(getFormatedLocationString(networkLocation), true);
      }
    }
  }

  private String getFormatedLocationString(Location location) {
    return "coordinates latitude " + String.valueOf(location.getLatitude()) + " longitude " + String.valueOf(location.getLongitude());
  }

  @Override
  public void onLocationChanged(Location location) {
    Log.d("willrgai", "onLocationChanged location");
    if (location != null) {
      int lat = (int) (location.getLatitude());
      int lng = (int) (location.getLongitude());
      Log.d("willrgai", "onLocationChanged lat " + lat + " lng " + lng);
    }

  }

  @Override
  public void onProviderDisabled(String provider) {
  }

  @Override
  public void onProviderEnabled(String provider) {
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
  }

}
