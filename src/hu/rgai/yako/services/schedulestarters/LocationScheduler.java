package hu.rgai.yako.services.schedulestarters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import hu.rgai.yako.services.LocationService;

/**
 * Created by kojak on 9/26/2014.
 */
public class LocationScheduler extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
      context.startService(new Intent(context, LocationService.class));
    }
  }
}
