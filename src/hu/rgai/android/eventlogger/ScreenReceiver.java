package hu.rgai.android.eventlogger;

import static hu.rgai.android.test.Constants.SCREEN_IS_OFF_STR;
import static hu.rgai.android.test.Constants.SCREEN_IS_ON_STR;
import static hu.rgai.android.test.Constants.SCREEN_OFF_STR;
import static hu.rgai.android.test.Constants.SCREEN_ON_STR;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    public static boolean wasScreenOn = true;
 
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
          EventLogger.INSTANCE.writeToLogFile( SCREEN_OFF_STR, true );
          Log.d("willrgai", SCREEN_IS_OFF_STR);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
          EventLogger.INSTANCE.writeToLogFile( SCREEN_ON_STR, true );
          Log.d("willrgai", SCREEN_IS_ON_STR);
        }
    }
 
}