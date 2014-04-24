
package hu.rgai.android.tools.view;

import android.os.AsyncTask;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import hu.rgai.android.beens.BitmapResult;
import hu.rgai.android.test.R;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AsyncImageLoader extends AsyncTask<Long, Void, BitmapResult> {

  private WeakReference<ImageView> mImageView;
  private AsyncImageLoadProvider mLoadProvider;
  private volatile boolean running = true;

  public AsyncImageLoader(ImageView imageView, AsyncImageLoadProvider loadProvider) {
    this.mImageView = new WeakReference<ImageView>(imageView);
    this.mLoadProvider = loadProvider;
  }
  
  @Override
  protected BitmapResult doInBackground(Long... ids) {
    // if task was stopped while waiting in thread pool
    if (!running) {
      return null;
    }
    return mLoadProvider.getBitmap(ids[0]);
  }

  @Override
  protected void onPostExecute(BitmapResult result) {
    if (running && !result.isDefaultBitmap() && mImageView != null && mImageView.get() != null) {
      mImageView.get().setImageBitmap(result.getBitmap());
      Animation myFadeInAnimation = AnimationUtils.loadAnimation(mImageView.get().getContext(), R.anim.image_fadein);
      mImageView.get().startAnimation(myFadeInAnimation);
    }
    running = false;
  }
  
  public synchronized boolean isRunning() {
    return running;
  }
  
  public synchronized void stop() {
    running = false;
  }
  
}
