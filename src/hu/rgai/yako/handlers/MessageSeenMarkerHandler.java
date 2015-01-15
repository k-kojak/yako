
package hu.rgai.yako.handlers;

import android.content.Context;
import android.widget.Toast;
import hu.rgai.android.test.R;
import hu.rgai.yako.view.fragments.MainActivityFragment;

public class MessageSeenMarkerHandler extends TimeoutHandler {

  public MainActivityFragment mFragment;

  public MessageSeenMarkerHandler(MainActivityFragment mFragment) {
    this.mFragment = mFragment;
  }
  
  @Override
  public void onTimeout(Context context) {
    Toast.makeText(mFragment.getActivity(), context.getString(R.string.timeout_while_marking_msg), Toast.LENGTH_LONG).show();
  }
  
  public void toastMessage(String s) {
    Toast.makeText(mFragment.getActivity(), s, Toast.LENGTH_LONG).show();
  }

  public void toastMessage(int resId) {
    Toast.makeText(mFragment.getActivity(), resId, Toast.LENGTH_LONG).show();
  }
  
  public void success() {
    mFragment.notifyAdapterChange();
  }

}
