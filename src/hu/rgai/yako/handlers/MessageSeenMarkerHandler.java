
package hu.rgai.yako.handlers;

import android.widget.Toast;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.view.fragments.MainActivityFragment;
import java.util.TreeSet;

public class MessageSeenMarkerHandler extends TimeoutHandler {

  public MainActivityFragment mFragment;

  public MessageSeenMarkerHandler(MainActivityFragment mFragment) {
    this.mFragment = mFragment;
  }
  
  @Override
  public void timeout() {
    Toast.makeText(mFragment.getActivity(), "Timeout while marking messages", Toast.LENGTH_LONG).show();
  }
  
  public void toastMessage(String s) {
    Toast.makeText(mFragment.getActivity(), s, Toast.LENGTH_LONG).show();
  }
  
  public void success(TreeSet<MessageListElement> messagesToMark, boolean seen) {
    for (MessageListElement mle : messagesToMark) {
      mle.setSeen(seen);
    }
    mFragment.notifyAdapterChange();
  }

}
