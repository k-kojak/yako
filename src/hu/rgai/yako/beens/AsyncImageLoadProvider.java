package hu.rgai.yako.beens;

import android.content.Context;
import android.graphics.Bitmap;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface AsyncImageLoadProvider {
  public BitmapResult getBitmap(Person p);
  public boolean isBitmapLoaded(Person p);
  public Bitmap getDefaultBitmap(Context c);
}
