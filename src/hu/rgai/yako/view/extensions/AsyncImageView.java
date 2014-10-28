
package hu.rgai.yako.view.extensions;

import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.AsyncImageLoadProvider;
import hu.rgai.yako.beens.Person;
import hu.rgai.yako.workers.AsyncImageLoader;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import hu.rgai.yako.tools.AndroidUtils;

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

  public void setImageBitmap(YakoApp yApp, AsyncImageLoadProvider loadProvider, Person from) {
    // stop current worker thread anyway!
    if (mLoader != null && mLoader.get() != null) {
      mLoader.get().stop();
    }
    // if bitmap in cache, just display it
    boolean storedPexists = Person.isPersonStoredInCache(from);
    if (storedPexists && loadProvider.isBitmapLoaded(from)) {
      this.setImageBitmap(loadProvider.getBitmap(from).getBitmap());
    } else {
      this.setImageBitmap(loadProvider.getDefaultBitmap(this.getContext()));
      
      AsyncImageLoader loader = new AsyncImageLoader(this, loadProvider);
      AndroidUtils.startAsyncTask(yApp, loader, from);
      mLoader = new WeakReference<AsyncImageLoader>(loader);
    }
  }
  
}
