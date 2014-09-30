package hu.rgai.yako.tools;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.util.Log;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import hu.rgai.yako.beens.fbintegrate.FacebookIntegrateItem;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.view.activities.FacebookSettingActivity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

  private static Set<String> getContactsFacebookIds(final Activity activity) {

    Set<String> FacebookIds = new HashSet<String>();

    String selection = Data.MIMETYPE + " = ? "
            + " AND " + Data.DATA6 + " = ?";
    
    String[] selectionArgs = {
      ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE,
      Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME
    };
    
    Cursor cursor = activity.getContentResolver().query(Data.CONTENT_URI,
            new String[]{Data.DATA10},
            selection,
            selectionArgs,
            null);

    while (cursor.moveToNext()) {
      FacebookIds.add(cursor.getString(0));
    }

    return FacebookIds;

  }

  public void insertFriends(final Activity activity, final FacebookSettingActivity.ToastHelper th) {
    final Set<String> facebookIds = getContactsFacebookIds(activity);
    
    Bundle params = new Bundle();
    params.putString("fields", "id, name, picture");
    final FacebookIdSaver fbs = new FacebookIdSaver();
    Session session = Session.getActiveSession();
    Request request = new Request(session, "/me/friends", params, HttpMethod.GET,
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
                      int diff = (int) (new Date().getTime() - start.getTime());
                      int timeLeftInSec = (int) ((diff / (i + 1)) * (count - i)) / 1000;
                      Log.d("rgai", "remaining synctime -> " + timeLeftInSec);
                      int timeLeftInMin = timeLeftInSec / 60;
                      if (timeLeftInMin > 0) {
                        timeLeftInSec -= timeLeftInMin * 60;
                      }
                      timeleftStr = (timeLeftInMin > 0 ? timeLeftInMin + " min" + (timeLeftInMin > 1 ? "s " : " ") : "") + timeLeftInSec + " sec" + (timeLeftInSec > 1 ? "s" : "");
                      String toastStr = "Still updating, " + timeleftStr + " left";
                      th.showToast(toastStr);
                      nextToast += toastInterval * 1000;
                    }
                    JSONObject json_obj = arr.getJSONObject(i);

                    if (!(facebookIds.contains(json_obj.getString("id")))) {

                      //ezek adják vissza a szükséges adatokat
                      String uid = json_obj.getString("id");
                      String name = json_obj.getString("name");
                      JSONObject picture_obj = json_obj.getJSONObject("picture").getJSONObject("data");
                      String picture = picture_obj.getString("url");

                      FacebookIntegrateItem fbii = new FacebookIntegrateItem(name, null, uid, picture, null);
                      fbs.integrate(activity, fbii);

                    } else {
//                      Log.d("rgai", "ez a contact id mar bennevan: " + json_obj.getString("uid") + ": " + json_obj.getString("name"));
                    }

//            Log.d("rgai", "Integrating user ("+ (i+1) +"/"+ arr.length() +") -> " + name);
                  }
                } catch (JSONException e) {
                  Log.d("rgai", "json exception", e);
                }
              }
            });

    Request.executeAndWait(request);
  }
}
