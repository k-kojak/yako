package hu.rgai.android.services.schedulestarters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import hu.rgai.android.services.MainService;

public class MainServiceStarter extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("rgai", "MYservice-t futtatgatjuk x mp-nkent");
    Intent service = new Intent(context, MainService.class);
    context.startService(service);
  }

  
}
