package hu.rgai.yako.beens;

import android.content.Context;
import android.graphics.Bitmap;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface AsyncImageLoadProvider {
  public BitmapResult getBitmap(long id);
  public boolean isBitmapLoaded(long id);
  public Bitmap getDefaultBitmap(Context c);
}
