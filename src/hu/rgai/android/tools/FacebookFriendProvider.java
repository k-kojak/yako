package hu.rgai.android.tools;

import android.app.Activity;
import android.util.Log;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import hu.rgai.android.beens.fbintegrate.FacebookIntegrateItem;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookSessionAccount;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookFriendProvider {
  
//  private FacebookSessionAccount account;

  public FacebookFriendProvider(FacebookSessionAccount account) {
//    this.account = account;
  }
  
  public List<FacebookIntegrateItem> getFacebookFriends(final Activity activity) {
    Log.d("rgai", "getting facebook friends");
    Session.openActiveSession(null, true, new Session.StatusCallback() {
      public void call(Session sn, SessionState ss, Exception excptn) {
        if (sn.isOpened()) {
          Log.d("rgai", "SESSION IS OPENED WHEN RETRIEVING FRIND LIST");
          Request.newMyFriendsRequest(sn, new Request.GraphUserListCallback() {

            public void onCompleted(List<GraphUser> list, Response rspns) {
//              Log.d("rgai", "friend list size -> " + list.size());
              List <FacebookIntegrateItem> fbi = new LinkedList<FacebookIntegrateItem>();
              for (GraphUser gu : list) {
                Log.d("rgai", gu.getName() + " - " + gu.getFirstName() + " - " + gu.getUsername());
                fbi.add(new FacebookIntegrateItem(gu.getName(), gu.getUsername(), gu.getId()));
              }
              FacebookIdSaver fbs = new FacebookIdSaver();
              for (FacebookIntegrateItem fbii : fbi) {
                fbs.integrate(activity, fbii);
              }
            }
          }).executeAsync();
        } else {
          Log.d("rgai", "SESSION IS CLOSED WHEN RETRIEVING FRIND LIST");
        }
      }
    });
    
    return null;
  }
}
