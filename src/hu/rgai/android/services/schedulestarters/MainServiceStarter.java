package hu.rgai.android.services.schedulestarters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import hu.rgai.android.services.MainService;

public class MainServiceStarter extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
//    Log.d("rgai", "MYservice-t futtatgatjuk x mp-nkent");
    Intent service = new Intent(context, MainService.class);
    if (intent.getExtras() != null && intent.getExtras().containsKey(MainService.IntentParams.TYPE)) {
      service.putExtra(MainService.IntentParams.TYPE, intent.getExtras().getString(MainService.IntentParams.TYPE));
    }
    if (intent.getExtras() != null && intent.getExtras().containsKey(MainService.IntentParams.LOAD_MORE)) {
      service.putExtra(MainService.IntentParams.LOAD_MORE, intent.getExtras().getBoolean(MainService.IntentParams.LOAD_MORE));
    }
    if (intent.getExtras() != null && intent.getExtras().containsKey(MainService.IntentParams.FORCE_QUERY)) {
      service.putExtra(MainService.IntentParams.FORCE_QUERY, intent.getExtras().getBoolean(MainService.IntentParams.FORCE_QUERY));
    }
    context.startService(service);
  }

  
}
