package hu.rgai.yako.eventlogger;

import android.location.Criteria;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationLogger implements LocationListener {

  private LocationManager locationManager;

  private final Context context;

  public LocationLogger(Context context) {
    super();
    this.context = context;
  }

  public void updateLocation() {
    Log.d("willrgai", "updating location info...");
    try {
      locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      if (locationManager == null)
        return;

      loggingLocation();
    } catch (Exception e) {
      Log.d("willrgai", "", e);
    }
  }

  private void loggingLocation() {
    Log.d("willrgai", "request single location update");
    Criteria crit = new Criteria();
    crit.setPowerRequirement(Criteria.POWER_MEDIUM);
    crit.setCostAllowed(false);
    locationManager.requestSingleUpdate(crit, this, null);
  }

//  private String getFormatedLocationString(Location location) {
//    return "coordinates latitude " + String.valueOf(location.getLatitude()) + " longitude " + String.valueOf(location.getLongitude());
//  }

  @Override
  public void onLocationChanged(Location location) {
//    Log.d("willrgai", "logging location: " + location.toString());
//    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, getFormatedLocationString(location), true);
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
