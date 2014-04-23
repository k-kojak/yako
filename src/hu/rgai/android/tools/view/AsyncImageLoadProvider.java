package hu.rgai.android.tools.view;

import android.content.Context;
import android.graphics.Bitmap;
import hu.rgai.android.beens.BitmapResult;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface AsyncImageLoadProvider {
  public BitmapResult getBitmap(long id);
  public boolean isBitmapLoaded(long id);
  public Bitmap getDefaultBitmap(Context c);
}
