package hu.rgai.yako.smarttools;

import android.content.Context;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.beens.MessageListElement;

public interface MessagePredictionProvider {
  public double predictMessage(Context context, MessageListElement message);
}
