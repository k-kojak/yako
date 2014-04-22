
package hu.rgai.android.tools.view;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import java.lang.ref.WeakReference;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AsyncImageView extends ImageView {

  private long mContactId = -1;
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
      Log.d("rgai", "mLoader != null");
      mLoader.get().stop();
    }
    // if bitmap in cache, just display it
    if (loadProvider.isBitmapLoaded(contactId)) {
//      Log.d("rgai", "loading image from cache");
      this.setImageBitmap(loadProvider.getBitmap(contactId).getBitmap());
    } else {
      this.setImageBitmap(loadProvider.getDefaultBitmap(this.getContext()));
      // stopping current loader
//      if (mLoader != null && mLoader.get() != null && mLoader.get().isRunning() && mContactId != contactId) {
//        Log.d("rgai", "cancel current asyncTask");
//        mLoader.get().stop();
//      }
      // image load is still in progress...
//      else if (mLoader != null && mLoader.get() != null && mLoader.get().isRunning() && mContactId == contactId) {
//        Log.d("rgai", "let current asynctask to finish run");
//        return;
//      }
      this.mContactId = contactId;
      AsyncImageLoader loader = new AsyncImageLoader(this, loadProvider);
      loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contactId);
      mLoader = new WeakReference<AsyncImageLoader>(loader);
    }
  }
  
}
