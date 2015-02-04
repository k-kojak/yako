
package hu.rgai.yako.view.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.config.Settings;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class SystemPreferences extends PreferenceActivity {
  
  public static final String KEY_PREF_NOTIFICATION = "pref_notification";
  public static final String KEY_PREF_NOTIFICATION_SOUND = "pref_notification_sound";
  public static final String KEY_PREF_NOTIFICATION_VIBRATION = "pref_notification_vibration";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState); //To change body of generated methods, choose Tools | Templates.
    addPreferencesFromResource(R.xml.preferences);
  }

  @Override
  public void finish() {
    setResult(Settings.ActivityRequestCodes.PREFERENCES_REQUEST_CODE);
    super.finish();
  }

}
