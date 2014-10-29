package hu.rgai.yako.view.extensions;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.GpsZone;

public class ZoneDisplayActionBarActivity extends ActionBarActivity {

  private boolean mChangeColor = false;
  private boolean mOverrideTitle = false;
  private boolean mOverrideSubtitle = false;

  protected void onCreate(Bundle savedInstanceState, boolean changeColor, boolean overrideTitle,
                          boolean overrideSubtitle) {

    super.onCreate(savedInstanceState);
    mChangeColor = changeColor;
    mOverrideTitle = overrideTitle;
    mOverrideSubtitle = overrideSubtitle;
  }


  @Override
  protected void onResume() {
    super.onResume();
    setActionBar();
  }

  
  protected void setActionBar() {
    if (mChangeColor || mOverrideTitle || mOverrideSubtitle) {
      GpsZone closest = YakoApp.getClosestZone(this, false);
      if (mChangeColor) {
        YakoApp.setActionBarColor(this, closest);
      }
      if (mOverrideTitle || mOverrideSubtitle) {
        YakoApp.setActionBarTitle(this, closest, mOverrideTitle, mOverrideSubtitle);
      }
    }
  }

}
