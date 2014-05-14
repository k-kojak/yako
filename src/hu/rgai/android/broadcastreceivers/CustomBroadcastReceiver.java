package hu.rgai.android.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import hu.rgai.android.services.MainService;
import hu.rgai.android.tools.AndroidUtils;

public class CustomBroadcastReceiver extends BroadcastReceiver {

  public CustomBroadcastReceiver() {
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    
    if (intent.getAction() != null) {
      
      // listening for internet access change
      if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
//          Log.d("rgai", "JUST CONNECTED TO WIFI NETWORK, CONNECTING AVAILABLE PROVIDERS AGAIN");
//          AndroidUtils.connectConnectableMessageProviders(context);
          
          Log.d("rgai", "JUST CONNECTED TO WIFI NETWORK, MAKE A FULL QUERY");
          Intent service = new Intent(context, MainService.class);
          service.putExtra(MainService.IntentParams.FORCE_QUERY, true);
          context.startService(service);
        } else {
          Log.d("rgai", "JUST LOST WIFI NETWORK CONNECTION");
        }
      }
      
    }
  }
}