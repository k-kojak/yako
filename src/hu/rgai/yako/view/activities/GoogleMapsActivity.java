package hu.rgai.yako.view.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;
import hu.rgai.android.test.R;
import hu.rgai.yako.adapters.ZoneNotificationListAdapter;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.GpsZoneDAO;
import hu.rgai.yako.sql.ZoneNotificationDAO;
import hu.rgai.yako.view.extensions.ZoneDisplayActionBarActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


/**
 * Created by kojak on 9/23/2014.
 */
public class GoogleMapsActivity extends ZoneDisplayActionBarActivity {

  public static final String ACTION_REFRESH_ZONE_LIST = "hu.rgai.yako.extra_refresh_zone_list";
  public static final String EXTRA_GPS_ZONE_DATA = "hu.rgai.yako.extra_gps_zone_data";
  public static final String EXTRA_START_LOC = "hu.rgai.yako.extra_start_loc";

  private GoogleMap mMap;
  private MapFragment mMapFragment;
  private Marker mMarker = null;
  private Circle mCircle = null;

  private int mRadiusValue = 50;
  private TextView mRadiusText;
  private SeekBar mSeekBar;

  private GpsZone mZoneToEdit = null;

  private String mNewZoneAlias = null;
  private GpsZone.ZoneType mNewZoneType = null;
  private LatLng mInitLatLng = null;
  private boolean mUpdating = false;
  private ZoneNotificationListAdapter mZoneNotListAdapter = null;
  private HashMap<String, Boolean> mCheckedStates = null; 
  private ZoneNotificationDAO mZoneNotDAO = null;




  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState, true, false, true);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    setContentView(R.layout.google_maps_layout);

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      if (extras.containsKey(EXTRA_GPS_ZONE_DATA)) {
        mZoneToEdit = extras.getParcelable(EXTRA_GPS_ZONE_DATA);
        mUpdating = true;
        mNewZoneAlias = mZoneToEdit.getAlias();
        mNewZoneType = mZoneToEdit.getZoneType();
      } else if (extras.containsKey(EXTRA_START_LOC)) {
        mInitLatLng = extras.getParcelable(EXTRA_START_LOC);
      }
    }
    String title;
    if (mUpdating) {
      title = "Updating " + mZoneToEdit.getAlias();
    } else {
      title = "Adding new zone";
    }
    getSupportActionBar().setTitle(title);

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
      public void onStartTrackingTouch(SeekBar seekBar) {}

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {}
    });


    mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
    mMap = mMapFragment.getMap();


    mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
      @Override
      public void onMapClick(LatLng latLng) {
        placeMarkerOnMap(latLng);
      }
    });


    if (mUpdating) {
      mRadiusValue = mZoneToEdit.getRadius();
      placeMarkerOnMap(new LatLng(mZoneToEdit.getLat(), mZoneToEdit.getLong()));
      mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
              new LatLng(mZoneToEdit.getLat(), mZoneToEdit.getLong()),
              16.0f));
    } else if (mInitLatLng != null) {
      mMap.animateCamera (CameraUpdateFactory.newLatLngZoom (mInitLatLng, 13.0f));
    }
    mSeekBar.setProgress((mRadiusValue - 20) / 10);
    setRadiusText(mRadiusValue);
    
    mZoneNotListAdapter = new ZoneNotificationListAdapter(getApplicationContext(), AccountDAO.getInstance(getApplicationContext()).getAllAccountsCursor(), mZoneToEdit, mUpdating);
    mCheckedStates = (HashMap<String, Boolean>) mZoneNotListAdapter.getAllState(); 
    mZoneNotDAO = ZoneNotificationDAO.getInstance(GoogleMapsActivity.this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.google_maps_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.save:
        saveZone();
        return true;
      case R.id.settings:
        showSettingsDialog(false);
        return true;
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void saveZone() {
    if (mMarker == null || !mMarker.isVisible()) {
      Toast.makeText(GoogleMapsActivity.this, "Select a location first", Toast.LENGTH_SHORT).show();
    } else {
      if (mUpdating) {
        saveZoneAndFinish();
      } else {
        if (mNewZoneType == null || mNewZoneAlias == null) {
          showSettingsDialog(true);
        }
      }
    }
  }

  private void saveZoneAndFinish() {
    GpsZone newZone = new GpsZone(mNewZoneAlias,
            mMarker.getPosition().latitude,
            mMarker.getPosition().longitude,
            mRadiusValue,
            mNewZoneType);
    boolean refreshNeeded = true;
    
    int oldZoneId = GpsZoneDAO.getInstance(GoogleMapsActivity.this).getZoneIdByAlias(mNewZoneAlias);
    boolean notificationChanged = false;
    
    if(mUpdating) {
      notificationChanged = checkNotificationChanged(oldZoneId);
    }
    
    if (!mUpdating) {
      long newZoneId = GpsZoneDAO.getInstance(GoogleMapsActivity.this).saveZone(newZone);
      saveNotificationsToZone(newZoneId);
    } else {
      if (!mZoneToEdit.equals(newZone) || notificationChanged ) {
        GpsZoneDAO.getInstance(GoogleMapsActivity.this).updateZone(mZoneToEdit.getAlias(), newZone);
        saveNotificationsToZone(oldZoneId);
      } else {
        refreshNeeded = false;
      }
    }
    if (refreshNeeded) {
      setResult(RESULT_OK, new Intent(ACTION_REFRESH_ZONE_LIST));
    }
    finish();
  }

  private boolean checkNotificationChanged(int zoneId) {
    Cursor notifications = mZoneNotDAO.getAllNotificationsToZoneCursor(zoneId);
    Cursor accounts = AccountDAO.getInstance(GoogleMapsActivity.this).getAllAccountsCursor();
    boolean notificationChanged = false;
    
    accounts.moveToFirst();
    notifications.moveToFirst();
    while (!accounts.isAfterLast()) {  
      if(mCheckedStates.get(accounts.getString(2)) != (notifications.getInt(3) == 1 ? true : false)) {
        notificationChanged = true;
        break;
      }
      notifications.moveToNext();
      accounts.moveToNext();
    }
    notifications.close();
    accounts.close();
    return notificationChanged;
    
//    boolean notificationChanged = false;
//    notifications.moveToFirst();
//    while (!notifications.isAfterLast()) {
//      System.out.println("gomb " + checkedButtons.get(i).isChecked() +  " : es " + notifications.getInt(3));
//      
//      if(checkedButtons.get(i).isChecked() != (notifications.getInt(3) == 1 ? true : false)) {
//        notificationChanged = true;
//        break;
//      }
//      i++;
//      notifications.moveToNext();
//    }
//    notifications.close();
//    return notificationChanged;
  }


  private void saveNotificationsToZone(long zoneId) {

    Cursor accounts = AccountDAO.getInstance(GoogleMapsActivity.this).getAllAccountsCursor();
    accounts.moveToFirst();
    while (!accounts.isAfterLast()) {
      if(!mUpdating){
      mZoneNotDAO.saveNotificationSettingToZone(accounts.getInt(0), zoneId, mCheckedStates.get(accounts.getString(2)));
      } else {
      mZoneNotDAO.updateNotificationSettingToZone(accounts.getInt(0), zoneId, mCheckedStates.get(accounts.getString(2)));
      }
      accounts.moveToNext();
    }
    accounts.close();
  }



  private void setZoneTypeSpinner(Spinner spinner, GpsZone.ZoneType zoneType) {
    int i = 0;
    int selected = 0;
    List<GpsZone.ZoneType> objects = new LinkedList<GpsZone.ZoneType>();
    for (GpsZone.ZoneType zt : GpsZone.ZoneType.values()) {
      objects.add(zt);
      if (zt.equals(zoneType)) {
        selected = i;
      }
      i++;
    }

    ZoneTypeAdapter adapter = new ZoneTypeAdapter(this, R.layout.gmaps_zonetype_spinner_selected, objects);
    adapter.setDropDownViewResource(R.layout.gmaps_zonetype_spinner_item);
    spinner.setAdapter(adapter);
    spinner.setSelection(selected);
  }

  private void placeMarkerOnMap(LatLng latLng) {
    if (mMarker == null) {
      mMarker = mMap.addMarker(new MarkerOptions()
              .position(latLng)
              .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_custom_map_marker)));
    } else {
      mMarker.setPosition(latLng);
    }
    insertCircle();
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

  private class ZoneTypeAdapter extends ArrayAdapter<GpsZone.ZoneType> {

    private List<GpsZone.ZoneType> mItems;
    private int mResource;

    public ZoneTypeAdapter(Context context, int resource, List<GpsZone.ZoneType> objects) {
      super(context, resource, objects);
      mResource = resource;
      mItems = objects;
    }

    @Override
    public GpsZone.ZoneType getItem(int position) {
      return mItems.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;

      if (v == null) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(mResource, parent, false);
      }

      GpsZone.ZoneType zt = mItems.get(position);
      ((TextView)v.findViewById(android.R.id.text1)).setText(zt.getDisplayName());
      return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
      LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View v = inflater.inflate(R.layout.gmaps_zonetype_spinner_item, parent, false);

      return getView(position, v, parent);
    }
  }

  private void showSettingsDialog(boolean finishAfterOk) {
    View dialogView = LayoutInflater.from(this).inflate(R.layout.gmaps_zone_adder, null, false);

    
    ListView mNotificationSettingsList = (ListView)dialogView.findViewById(R.id.account_notification_listview);
    mNotificationSettingsList.setAdapter(mZoneNotListAdapter);
  
    
    EditText input = (EditText)dialogView.findViewById(R.id.alias_edit);
    Spinner spinner = (Spinner)dialogView.findViewById(R.id.zone_category);
    setZoneTypeSpinner(spinner, mUpdating ? mNewZoneType : null);
    if (mUpdating) {
      input.setText(mNewZoneAlias);
    }
    AlertDialog alertD = new AlertDialog.Builder(GoogleMapsActivity.this)
            .setTitle("Zone settings")
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
              }
            })
            .create();
    alertD.setOnShowListener(new OnAliasShowListener(alertD, input, spinner, mCheckedStates, finishAfterOk));
    alertD.show();
  }

  public class OnAliasShowListener implements DialogInterface.OnShowListener {

    private AlertDialog mDialog;
    private EditText mInput;
    private Spinner mZoneCategory;
    private HashMap<String, Boolean> mCheckedButtons;
    private boolean mFinishAfterOk;

    public OnAliasShowListener(AlertDialog dialog, EditText input, Spinner zoneCategory, HashMap<String, Boolean> checkedButtons,  boolean finishAfterOk) {
      mDialog = dialog;
      mInput = input;
      mZoneCategory = zoneCategory;
      mCheckedButtons = checkedButtons;
      mFinishAfterOk = finishAfterOk;
    }

    @Override
    public void onShow(DialogInterface dialog) {
      Button b = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
      b.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View view) {
          onOkButtonClick();
        }
      });
    }

    private void onOkButtonClick() {
      String value = mInput.getText().toString().trim();
      if (value.length() == 0) {
        Toast.makeText(GoogleMapsActivity.this, "Alias is empty",
                Toast.LENGTH_SHORT).show();
      } else {
        boolean aliasExists = GpsZoneDAO.getInstance(GoogleMapsActivity.this).isZoneAliasExists(value);
        GpsZone.ZoneType zoneType = (GpsZone.ZoneType)mZoneCategory.getSelectedItem();
        if ((!mUpdating && aliasExists)
                || (mUpdating && aliasExists && !mInput.getText().toString().equals(mZoneToEdit.getAlias()))) {
          Toast.makeText(GoogleMapsActivity.this, "Alias already exists: " + value,
                  Toast.LENGTH_SHORT).show();
        } else {
          mNewZoneAlias = value;
          mNewZoneType = zoneType;
          mCheckedStates = (HashMap<String, Boolean>) mZoneNotListAdapter.getAllState();
          if (mFinishAfterOk) {
            saveZoneAndFinish();
          } else {
            mDialog.dismiss();
          }

        }
      }
    }
  }
}
