package hu.rgai.android.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import hu.rgai.android.config.Settings;

public class CustomBroadcastReceiver extends BroadcastReceiver {

  public CustomBroadcastReceiver() {
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    // listening for internet access change
    Log.d("rgai", "NEW MESSAGE ARRIVED RECEIVER");
    if (intent.getAction() != null) {
      Log.d("rgai", "CUSTOM BROADCAAST RECEIVER's ACTION: " + intent.getAction());
    }
    if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
      NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
      String typeName = info.getTypeName();
      String subtypeName = info.getSubtypeName();
//        System.out.println("Network is up ******** " + typeName + ":::" + subtypeName);

//        activity.setContent("onInternetBroadcast receive");
    } else if (intent.getAction().equals(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST)) {
//        Log.d("rgai", "NEW MESSAGE ARRIVED RECEIVER");
//      Intent i = new Intent(context, MainScheduler.class);
//      if (intent.getExtras().containsKey("type")) {
//        i.putExtra("type", intent.getExtras().getString("type"));
//      }
//      i.setAction(Context.ALARM_SERVICE);
//      context.sendBroadcast(i);
    }
  }
}