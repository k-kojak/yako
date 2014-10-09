package hu.rgai.yako.smarttools;

import android.content.Context;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.config.Settings;

public interface MessagePredictionProvider {
  public double predictMessage(Context context, MessageListElement message);

  public class Helper {
    public static boolean isImportant(double pred) {
      return pred > Settings.IMPORTANT_LIMIT;
    }
  }

}
