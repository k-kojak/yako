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
import hu.rgai.android.test.settings.FacebookSettingActivity;
import java.util.Date;
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

  public List<FacebookIntegrateItem> getFacebookFriends(final Activity activity, final FacebookSettingActivity.ToastHelper th) {
    getUserDataWithFql(activity, th);

    return null;
  }

  private static void getUserDataWithFql(final Activity activity, final FacebookSettingActivity.ToastHelper th) {
    String fql = "SELECT uid, name, username, pic, pic_big FROM user WHERE uid IN (SELECT uid2 FROM friend WHERE uid1 = me())";

    Bundle params = new Bundle();
    params.putString("q", fql);
    final FacebookIdSaver fbs = new FacebookIdSaver();
    Session session = Session.getActiveSession();
    Request request = new Request(session, "/fql", params, HttpMethod.GET,
            new Request.Callback() {
      public void onCompleted(Response response) {
        try {
          GraphObject go = response.getGraphObject();
          if (go == null) {
            th.showToast("Unable to update contact list due to Facebook error");
            return;
          }
          JSONObject jso = go.getInnerJSONObject();
          JSONArray arr = jso.getJSONArray("data");
          int count = arr.length();
          
          // display some statistics
          Date start = new Date();
          int toastInterval = 20;
          long nextToast = start.getTime() + toastInterval * 1000;
          for (int i = 0; i < count; i++) {
            // count statistics
            String timeleftStr = "";
            if (new Date().getTime() > nextToast) {
              int diff = (int)(new Date().getTime() - start.getTime());
              int timeLeftInSec = (int)((diff / (i + 1)) * (count - i)) / 1000;
              Log.d("rgai", "remaining synctime -> " + timeLeftInSec);
              int timeLeftInMin = timeLeftInSec / 60;
              if (timeLeftInMin > 0) timeLeftInSec -= timeLeftInMin * 60;
              timeleftStr = (timeLeftInMin > 0 ? timeLeftInMin + " min" + (timeLeftInMin > 1 ? "s " : " ") : "") + timeLeftInSec + " sec" + (timeLeftInSec > 1 ? "s" : "");
              String toastStr = "Still updating, " + timeleftStr + " left";
              th.showToast(toastStr);
//              Toast.makeText(activity, timeleftStr + , Toast.LENGTH_LONG).show();
              nextToast += toastInterval * 1000;
            }
            JSONObject json_obj = arr.getJSONObject(i);

            //ezek adják vissza a szükséges adatokat
            String uid = json_obj.getString("uid");
            String name = json_obj.getString("name");
            String username = json_obj.getString("username");

            FacebookIntegrateItem fbii = new FacebookIntegrateItem(name, username, uid, json_obj.getString("pic"), json_obj.getString("pic_big"));
            fbs.integrate(activity, fbii);
//            Log.d("rgai", "Integrating user ("+ (i+1) +"/"+ arr.length() +") -> " + name);
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
