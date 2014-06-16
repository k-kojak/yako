
package hu.rgai.yako.handlers;

import android.content.Context;
import android.os.Parcel;
import android.util.Log;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class SimpleMessageSendHandler extends MessageSendHandler {

  public SimpleMessageSendHandler(Context context) {
    super(context);
  }

  @Override
  public void success(String name) {
    Log.d("rgai2", "message sent successfully");
  }

  @Override
  public void fail(String name) {
    Log.d("rgai2", "message sent failed");
  }

  @Override
  public void delivered(String name) {
    Log.d("rgai2", "message delivered");
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel dest, int flags) {
    
  }

}
