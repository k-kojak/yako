
package hu.rgai.yako.workers;

import android.os.AsyncTask;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import hu.rgai.yako.beens.BitmapResult;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.AsyncImageLoadProvider;
import hu.rgai.yako.beens.Person;

import java.lang.ref.WeakReference;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AsyncImageLoader extends AsyncTask<Person, Void, BitmapResult> {

  private WeakReference<ImageView> mImageView;
  private AsyncImageLoadProvider mLoadProvider;
  private volatile boolean running = true;

  public AsyncImageLoader(ImageView imageView, AsyncImageLoadProvider loadProvider) {
    this.mImageView = new WeakReference<ImageView>(imageView);
    this.mLoadProvider = loadProvider;
  }
  
  @Override
  protected BitmapResult doInBackground(Person... person) {
    // if task was stopped while waiting in thread pool
    if (!running) {
      return null;
    }
    Person p = null;
    if (person != null && person.length > 0) {
      p = person[0];
    }
    long s = System.currentTimeMillis();
    BitmapResult bmr = mLoadProvider.getBitmap(p);
//    Log.d("rgai", "time to get bitmap: " + (System.currentTimeMillis() - s) + " ms");
    return bmr;
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
  

  public synchronized void stop() {
    running = false;
  }
  
}
