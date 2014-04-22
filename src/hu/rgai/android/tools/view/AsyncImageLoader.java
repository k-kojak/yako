
package hu.rgai.android.tools.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import hu.rgai.android.beens.BitmapResult;
import hu.rgai.android.test.R;
import java.util.logging.Level;
import java.util.logging.Logger;

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
      Log.d("rgai", "STOPPED WHILE WAITING IN THREAD POOL");
      return null;
    }
    Log.d("rgai", "    async started");
//    try {
//      Thread.sleep(500 + (int)(Math.random() * 1500));
//    } catch (InterruptedException ex) {
//      Logger.getLogger(AsyncImageLoader.class.getName()).log(Level.SEVERE, null, ex);
//    }
    return mLoadProvider.getBitmap(ids[0]);
  }

  @Override
  protected void onPostExecute(BitmapResult result) {
    Log.d("rgai", "        async finished");
    if (running) {
      if (!result.isDefaultBitmap()) {
        mImageView.setImageBitmap(result.getBitmap());
        Animation myFadeInAnimation = AnimationUtils.loadAnimation(mImageView.getContext(), R.anim.image_fadein);
        mImageView.startAnimation(myFadeInAnimation);
      }
    } else {
      Log.d("rgai", "            stopped while running...so skip setting image");
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
