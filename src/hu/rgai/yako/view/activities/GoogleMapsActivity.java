package hu.rgai.yako.view.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.*;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.sql.GpsZoneDAO;


/**
 * Created by kojak on 9/23/2014.
 */
public class GoogleMapsActivity extends FragmentActivity {

  private GoogleMap mMap;
  private Marker mMarker = null;
  private Circle mCircle = null;
  private Button mCancel;
  private Button mSave;

  private int mRadiusValue = 50;
  private TextView mRadiusText;
  private SeekBar mSeekBar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.google_maps_layout);

    mRadiusText = (TextView)findViewById(R.id.radius_text);
    mSeekBar = (SeekBar)findViewById(R.id.radius_seekbar);
    mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mRadiusValue = (progress * 10 + 20);
        setRadiusText(mRadiusValue);
        insertCircle();
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });
    mSeekBar.setProgress((mRadiusValue - 20) / 10);
    setRadiusText(mRadiusValue);


    mCancel = (Button)findViewById(R.id.cancel);
    mSave = (Button)findViewById(R.id.save);
    mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

    mCancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });

    mSave.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mMarker == null || !mMarker.isVisible()) {
          Toast.makeText(GoogleMapsActivity.this, "Select a location first", Toast.LENGTH_SHORT).show();
        } else {

          final EditText input = new EditText(GoogleMapsActivity.this);
          final AlertDialog alertD = new AlertDialog.Builder(GoogleMapsActivity.this)
                  .setTitle("Zone alias")
                  .setMessage("Please set an alias for selected zone")
                  .setView(input)
                  .setPositiveButton(android.R.string.ok, null)
                  .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                      dialog.dismiss();
                    }
                  })
                  .create();
          alertD.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
              Button b = alertD.getButton(AlertDialog.BUTTON_POSITIVE);
              b.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                  String value = input.getText().toString().trim();
                  if (value.length() == 0) {
                    Toast.makeText(GoogleMapsActivity.this, "Alias is empty",
                            Toast.LENGTH_SHORT).show();
                  } else {
                    if (GpsZoneDAO.getInstance(GoogleMapsActivity.this).isZoneAliasExists(value)) {
                      Toast.makeText(GoogleMapsActivity.this, "Alias already exists: " + value,
                              Toast.LENGTH_SHORT).show();
                    } else {
                      GpsZoneDAO.getInstance(GoogleMapsActivity.this).saveZone(
                              new GpsZone(value,
                                      mMarker.getPosition().latitude,
                                      mMarker.getPosition().longitude,
                                      mRadiusValue));
                      alertD.dismiss();
                      finish();
                    }
                  }
                }
              });
            }
          });
          alertD.show();
        }
      }
    });

    mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
      @Override
      public void onMapClick(LatLng latLng) {
        if (mMarker == null) {
          mMarker = mMap.addMarker(new MarkerOptions()
                  .position(latLng)
                  .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_custom_map_marker)));
        } else {
          mMarker.setPosition(latLng);
        }
        insertCircle();
      }
    });
  }

  private void insertCircle() {
    if (mMarker != null) {
      if (mCircle == null) {
        CircleOptions circleOptions = new CircleOptions()
                .center(mMarker.getPosition())
                .radius(mRadiusValue)
                .strokeWidth(3.0f)
                .fillColor(Color.argb(128, 255, 50, 50));
        mCircle = mMap.addCircle(circleOptions);
      } else {
        mCircle.setCenter(mMarker.getPosition());
        mCircle.setRadius(mRadiusValue);
      }
    }
  }

  private void setRadiusText(int radValue) {
    mRadiusText.setText("Accuracy: " + radValue + "m");
  }

}
