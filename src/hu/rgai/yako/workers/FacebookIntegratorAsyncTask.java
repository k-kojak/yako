
package hu.rgai.yako.workers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.view.activities.FacebookSettingActivity;
import hu.rgai.yako.tools.FacebookFriendProvider;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tamas Kojedzinszky
 */
  public class FacebookIntegratorAsyncTask extends TimeoutAsyncTask<String, String, String> {

    public static boolean isRunning = false;
    private Activity activity;

    public FacebookIntegratorAsyncTask(Activity activity) {
      super(null);
      this.activity = activity;
    }

    @Override
    protected String doInBackground(String... params) {
      String content = null;
      isRunning = true;

      // getting my facebook profile image
      String url = String.format("https://graph.facebook.com/%s/picture", params[0]);

      InputStream inputStream = null;
      try {
        inputStream = new URL(url).openStream();
      } catch (MalformedURLException ex) {
        Logger.getLogger(FacebookSettingActivity.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(FacebookSettingActivity.class.getName()).log(Level.SEVERE, null, ex);
      }
      Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
      StoreHandler.saveUserFbImage(activity, bitmap);



      FacebookFriendProvider fbfp = new FacebookFriendProvider();
      fbfp.insertFriends(activity, new FacebookSettingActivity.ToastHelper() {
        public void showToast(String msg) {
          publishProgress(msg);
        }
      });


      return content;
    }

    @Override
    protected void onPostExecute(String result) {
      Toast.makeText(activity, "Update complete.", Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onProgressUpdate(String... values) {
      Toast.makeText(activity, values[0], Toast.LENGTH_LONG).show();
    }
  }