
package hu.rgai.android.tools.view;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.widget.ImageView;
import hu.rgai.android.beens.BitmapResult;
import hu.rgai.android.tools.AndroidUtils;
import java.lang.ref.WeakReference;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AsyncImageView extends ImageView {

  private WeakReference<AsyncImageLoader> mLoader = null;
  
  
  public AsyncImageView(Context context) {
    super(context);
  }

  public AsyncImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AsyncImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setImageBitmap(AsyncImageLoadProvider loadProvider, long contactId) {
    // stop current worker thread anyway!
    if (mLoader != null && mLoader.get() != null) {
      mLoader.get().stop();
    }
    // if bitmap in cache, just display it
    if (loadProvider.isBitmapLoaded(contactId)) {
      this.setImageBitmap(loadProvider.getBitmap(contactId).getBitmap());
    } else {
      this.setImageBitmap(loadProvider.getDefaultBitmap(this.getContext()));
      
      AsyncImageLoader loader = new AsyncImageLoader(this, loadProvider);
      AndroidUtils.<Long, Void, BitmapResult>startAsyncTask(loader, contactId);
      mLoader = new WeakReference<AsyncImageLoader>(loader);
    }
  }
  
}
