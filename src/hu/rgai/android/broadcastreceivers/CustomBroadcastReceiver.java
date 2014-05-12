package hu.rgai.android.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
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
          Log.d("rgai", "JUST CONNECTED TO WIFI NETWORK, CONNECTING AVAILABLE PROVIDERS AGAIN");
          AndroidUtils.connectConnectableMessageProviders(context);
        } else {
          Log.d("rgai", "JUST LOST WIFI NETWORK CONNECTION");
        }
      }
      
    }
  }
}