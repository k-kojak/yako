package hu.rgai.yako.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import hu.rgai.yako.intents.IntentStrings;

public class GlobalBroadcastReceiver extends BroadcastReceiver {

  public GlobalBroadcastReceiver() {
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    
    if (intent.getAction() != null) {
      
      // listening for internet access change
      if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
          
//          Log.d("rgai", "JUST CONNECTED TO WIFI NETWORK, MAKE A FULL QUERY");
          Intent service = new Intent(context, MainScheduler.class);
          service.setAction(Context.ALARM_SERVICE);
          MainServiceExtraParams eParams = new MainServiceExtraParams();
          eParams.setForceQuery(true);
          service.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
          context.sendBroadcast(service);
        } else {
//          Log.d("rgai", "JUST LOST WIFI NETWORK CONNECTION");
        }
      }
      
    }
  }
}