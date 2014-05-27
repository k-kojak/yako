
package hu.rgai.android.workers;

import android.util.Log;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestBatchedWorker extends BatchedTimeoutAsyncTask<String, Void, String> {

  public TestBatchedWorker() {
    super(null);
  }
  
  
  @Override
  protected void onBatchedPostExecute(String result) {
    Log.d("rgai2", "thread done: " + result);
  }

  @Override
  protected String doInBackground(String... params) {
    Log.d("rgai2", "started thread: " + params[0]);
    try {
      Thread.sleep((int)(Math.random() * 5000) + 5000);
    } catch (InterruptedException ex) {
      Logger.getLogger(TestBatchedWorker.class.getName()).log(Level.SEVERE, null, ex);
    }
    return params[0];
  }

}
