package hu.rgai.yako.smarttools;

import android.content.Context;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.util.List;

/**
 * Created by kojak on 9/25/2014.
 */
public class DummyMessagePredictionProvider implements MessagePredictionProvider {
  @Override
  public double predictMessage(Context context, MessageListElement message) {
    FullMessage fullMessage;
    // this is how you can get the actual content...
    if (message.getMessageType().equals(MessageProvider.Type.EMAIL)
            || message.getMessageType().equals(MessageProvider.Type.GMAIL)) {
      // this case the fullMessage is a single FullSimple message with infos you need
      fullMessage = (FullSimpleMessage) message.getFullMessage();
    }
    // SMS or Facebook
    else {
      // this case the fullMessage is a TreeSet of FullSimple messages with infos you need (thread items)
      fullMessage = (FullThreadMessage) message.getFullMessage();
    }
    /**
     * GpsZone might have an unitialized, default <code>distance</code> and <code>proximity</code> values.
     * That means location cannot be set, so there will be no active (NEAR, CLOSEST) locations.
     */
    List<GpsZone> gpsZones = YakoApp.getSavedGpsZones(context);
    GpsZone closest = GpsZone.getClosest(gpsZones);
    for (GpsZone zone : gpsZones) {
//      Log.d("yako", zone.toString());
    }
    if (closest != null) {
      String s = "";
      if (closest.getZoneType().equals(GpsZone.ZoneType.REST)) {
        s = "cs";
      } else if (closest.getZoneType().equals(GpsZone.ZoneType.WORK)) {
        s = "sc";
      } else if (closest.getZoneType().equals(GpsZone.ZoneType.SILENT)) {
        s = "k";
      }
      if (message.getTitle().contains(s)) {
        return 0.99;
      } else {
        return 0;
      }
    } else {
      return 0;
    }
  }
}
