package hu.rgai.android.tools;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import hu.rgai.android.beens.fbintegrate.FacebookIntegrateItem;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookFriendProvider {

  public FacebookFriendProvider() {
  }

  public List<FacebookIntegrateItem> getFacebookFriends(final Activity activity) {
    getUserDataWithFql(activity);

    return null;
  }

  private static void getUserDataWithFql(final Activity activity) {
    String fql = "SELECT uid, name, username FROM user WHERE uid IN (SELECT uid2 FROM friend WHERE uid1 = me())";

    Bundle params = new Bundle();
    params.putString("q", fql);
    final FacebookIdSaver fbs = new FacebookIdSaver();
    Session session = Session.getActiveSession();
    Request request = new Request(session, "/fql", params, HttpMethod.GET,
            new Request.Callback() {
      public void onCompleted(Response response) {
        try {
          GraphObject go = response.getGraphObject();
          JSONObject jso = go.getInnerJSONObject();
          JSONArray arr = jso.getJSONArray("data");
          for (int i = 0; i < (arr.length()); i++) {
            JSONObject json_obj = arr.getJSONObject(i);

            //ezek adják vissza a szükséges adatokat
            String uid = json_obj.getString("uid");
            String name = json_obj.getString("name");
            String username = json_obj.getString("username");

            FacebookIntegrateItem fbii = new FacebookIntegrateItem(name, username, uid);
//            if (fbii.getName().contains("Zsibrita")) {
              fbs.integrate(activity, fbii);
//            }
            
          }
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });

    Request.executeAndWait(request);
  }
}
