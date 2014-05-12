package hu.rgai.android.services.schedulestarters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import hu.rgai.android.services.MainService;

public class MainServiceStarter extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
//    Log.d("rgai", "MYservice-t futtatgatjuk x mp-nkent");
    Intent service = new Intent(context, MainService.class);
    if (intent.getExtras() != null && intent.getExtras().containsKey("type")) {
      service.putExtra("type", intent.getExtras().getString("type"));
    }
    if (intent.getExtras() != null && intent.getExtras().containsKey("load_more")) {
      service.putExtra("load_more", intent.getExtras().getBoolean("load_more"));
    }
    if (intent.getExtras() != null && intent.getExtras().containsKey("force_query")) {
      service.putExtra("force_query", intent.getExtras().getBoolean("force_query"));
    }
    context.startService(service);
  }

  
}
