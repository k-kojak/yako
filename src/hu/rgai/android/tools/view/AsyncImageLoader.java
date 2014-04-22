
package hu.rgai.android.tools.view;

import android.os.AsyncTask;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import hu.rgai.android.beens.BitmapResult;
import hu.rgai.android.test.R;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AsyncImageLoader extends AsyncTask<Long, Void, BitmapResult> {

  private ImageView mImageView;
  private AsyncImageLoadProvider mLoadProvider;
  private volatile boolean running = true;

  public AsyncImageLoader(ImageView imageView, AsyncImageLoadProvider loadProvider) {
    this.mImageView = imageView;
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
    if (running) {
      if (!result.isDefaultBitmap()) {
        mImageView.setImageBitmap(result.getBitmap());
        Animation myFadeInAnimation = AnimationUtils.loadAnimation(mImageView.getContext(), R.anim.image_fadein);
        mImageView.startAnimation(myFadeInAnimation);
      }
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
