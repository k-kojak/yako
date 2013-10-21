package hu.rgai.android.tools;

import hu.rgai.android.beens.fbintegrate.FacebookIntegrateItem;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookFriendProvider {
  
  private FacebookAccount account;

  public FacebookFriendProvider(FacebookAccount account) {
    this.account = account;
  }
  
  public List<FacebookIntegrateItem> getFacebookFriends() {
    List<FacebookIntegrateItem> friends = null;
    friends = new LinkedList<FacebookIntegrateItem>();
    friends.add(new FacebookIntegrateItem("test 17024", "testike.1", "1111111111"));
    friends.add(new FacebookIntegrateItem("test 218", "testike.2", "2222222222"));
    
    return friends;
  }
  
}
