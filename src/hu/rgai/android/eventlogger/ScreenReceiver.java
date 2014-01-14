package hu.rgai.android.eventlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static hu.rgai.android.test.Constants.*;

public class ScreenReceiver extends BroadcastReceiver {

    public static boolean wasScreenOn = true;
 
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
          EventLogger.INSTANCE.writeToLogFile( SCREEN_OFF_STR );
          Log.d("willrgai", SCREEN_IS_OFF_STR);
          
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
          EventLogger.INSTANCE.writeToLogFile( SCREEN_ON_STR );
          Log.d("willrgai", SCREEN_IS_ON_STR);
        }
    }
 
}