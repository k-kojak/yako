package hu.rgai.android.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartService extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("rgai", "MYservice-t futtatgatjuk x mp-nkent");
    Intent service = new Intent(context, MyService.class);
    context.startService(service);
  }

  
}
