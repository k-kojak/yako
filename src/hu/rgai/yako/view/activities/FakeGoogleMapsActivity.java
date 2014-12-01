package hu.rgai.yako.view.activities;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.sql.GpsZoneDAO;
import hu.rgai.yako.view.extensions.ZoneDisplayActionBarActivity;

import java.util.List;


/**
 * Created by kojak on 9/23/2014.
 */
public class FakeGoogleMapsActivity extends ZoneDisplayActionBarActivity {

  public static final String ACTION_FAKE_ZONE_SET = "hu.rgai.yako.extra_fake_zone_set";
  public static final String EXTRA_START_LOC = "hu.rgai.yako.extra_start_loc";

  private GoogleMap mMap;
  private MapFragment mMapFragment;
  private Marker mMarker = null;
  private Circle mCircle = null;

  private GpsZone mZoneToEdit = null;

  private String mNewZoneAlias = null;
  private GpsZone.ZoneType mNewZoneType = null;
  private LatLng mInitLatLng = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState, true, false, true);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    setContentView(R.layout.fake_google_maps_layout);

    List<GpsZone> zones = GpsZoneDAO.getInstance(this).getAllZones();

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      if (extras.containsKey(EXTRA_START_LOC)) {
        mInitLatLng = extras.getParcelable(EXTRA_START_LOC);
      }
    }


    getSupportActionBar().setTitle("Set fake zone");

//    mRadiusText = (TextView)findViewById(R.id.radius_text);
//    mSeekBar = (SeekBar)findViewById(R.id.radius_seekbar);
//    mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//      @Override
//      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        mRadiusValue = (progress * 10 + 20);
//        setRadiusText(mRadiusValue);
//        insertCircle();
//      }
//
//      @Override
//      public void onStartTrackingTouch(SeekBar seekBar) {}
//
//      @Override
//      public void onStopTrackingTouch(SeekBar seekBar) {}
//    });


    mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
    mMap = mMapFragment.getMap();


    mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
      @Override
      public void onMapClick(LatLng latLng) {
        placeMarkerOnMap(latLng);
      }
    });

    if (mInitLatLng != null) {
      mMap.animateCamera (CameraUpdateFactory.newLatLngZoom (mInitLatLng, 13.0f));
    }

    for (GpsZone z : zones) {
      putZoneOnMap(z);
    }

  }

  private void putZoneOnMap(GpsZone zone) {
    int icon = R.drawable.ic_custom_fake_map_marker;
    float iconAlpha = 0.7f;
    int circleColor = 0x22000000;
    float circleStroke = 1.0f;

    LatLng latLng = new LatLng(zone.getLat(), zone.getLong());
    mMap.addMarker(new MarkerOptions()
            .position(latLng)
            .icon(BitmapDescriptorFactory.fromResource(icon))
            .alpha(iconAlpha));

    CircleOptions circleOptions = new CircleOptions()
            .center(latLng)
            .radius(zone.getRadius())
            .strokeWidth(circleStroke)
            .fillColor(circleColor);
    mMap.addCircle(circleOptions);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.fake_google_maps_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.save:
        saveFakeZone();
        return true;
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void saveFakeZone() {
    if (mMarker == null || !mMarker.isVisible()) {
      Toast.makeText(FakeGoogleMapsActivity.this, "Select a fake location first", Toast.LENGTH_SHORT).show();
    } else {
      saveFakeLocationAndFinish();
    }
  }

  private void saveFakeLocationAndFinish() {
    LatLng fakeLatLng = new LatLng(mMarker.getPosition().latitude, mMarker.getPosition().longitude);
    YakoApp.setFakeLocation(fakeLatLng);
    setResult(RESULT_OK, new Intent(ACTION_FAKE_ZONE_SET));
    finish();
  }


  private void placeMarkerOnMap(LatLng latLng) {
    if (mMarker == null) {
      mMarker = mMap.addMarker(new MarkerOptions()
              .position(latLng)
              .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_custom_map_marker)));
    } else {
      mMarker.setPosition(latLng);
    }
  }


}
