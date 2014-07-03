
package hu.rgai.yako.handlers;

import android.content.Context;
import android.widget.Toast;
import hu.rgai.yako.view.fragments.MainActivityFragment;

public class MessageSeenMarkerHandler extends TimeoutHandler {

  public MainActivityFragment mFragment;

  public MessageSeenMarkerHandler(MainActivityFragment mFragment) {
    this.mFragment = mFragment;
  }
  
  @Override
  public void timeout(Context context) {
    Toast.makeText(mFragment.getActivity(), "Timeout while marking messages", Toast.LENGTH_LONG).show();
  }
  
  public void toastMessage(String s) {
    Toast.makeText(mFragment.getActivity(), s, Toast.LENGTH_LONG).show();
  }
  
  public void success() {
    mFragment.notifyAdapterChange();
  }

}
