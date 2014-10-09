package hu.rgai.yako.smarttools;

import android.content.Context;
import android.util.Log;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.beens.MessageListElement;

import java.util.List;

/**
 * Created by kojak on 9/25/2014.
 */
public class DummyMessagePredictionProvider implements MessagePredictionProvider {
  @Override
  public double predictMessage(Context context, MessageListElement message) {
    /**
     * GpsZone might have an unitialized, default <code>distance</code> and <code>proximity</code> values.
     * That means location cannot be set, so there will be no active (NEAR, CLOSEST) locations.
     */
    List<GpsZone> gpsZones = YakoApp.getSavedGpsZones(context);
    for (GpsZone zone : gpsZones) {
//      Log.d("yako", zone.toString());
    }
//    if (message.getTitle().contains("i")) {
//      return 0.9;
//    } else {
      return Math.random();
//    }

  }
}
