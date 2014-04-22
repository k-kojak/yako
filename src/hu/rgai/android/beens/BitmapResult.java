
package hu.rgai.android.beens;

import android.graphics.Bitmap;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class BitmapResult {
  private Bitmap bitmap;
  private boolean defaultBitmap;

  public BitmapResult(Bitmap bitmap, boolean defaultBitmap) {
    this.bitmap = bitmap;
    this.defaultBitmap = defaultBitmap;
  }

  public Bitmap getBitmap() {
    return bitmap;
  }

  public boolean isDefaultBitmap() {
    return defaultBitmap;
  }
  
}
