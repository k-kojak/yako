
package hu.rgai.yako.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.sql.GpsZoneDAO;

import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ZoneListAdapter extends BaseAdapter {

  private static LayoutInflater inflater = null;
  private final List<GpsZone> mGpsZones;
  private final MainActivity mActivity;
  private final boolean mIsZoneActivated;


  public ZoneListAdapter(MainActivity context, List<GpsZone> gpsZones, boolean zoneActivated) {
    mActivity = context;
    mGpsZones = gpsZones;
    mIsZoneActivated = zoneActivated;
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

  }

  @Override
  public int getCount() {
    return mGpsZones.size();
  }

  public Object getItem(int position) {
    return mGpsZones.get(position);
  }

  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View view, ViewGroup parent) {

    view = inflater.inflate(R.layout.zone_list_item, parent, false);

    TextView alias = (TextView) view.findViewById(R.id.alias);
    TextView distance = (TextView) view.findViewById(R.id.distance);
    ImageView discard = (ImageView) view.findViewById(R.id.discard);
    ImageView circle = (ImageView) view.findViewById(R.id.active_status);

    final GpsZone zone = (GpsZone)getItem(position);

    // Setting all values in listview
    alias.setText(zone.getAlias());
    if (zone.getDistance() >= 0 && mIsZoneActivated) {
      distance.setText("(" + formatDistance(zone.getDistance()) + ")");
    } else {
      distance.setText("");
    }

    int drawable;
    if (!mIsZoneActivated) {
      drawable = R.drawable.ic_unavailable;
    } else {
      if (zone.getProximity().equals(GpsZone.Proximity.CLOSEST)) {
        drawable = R.drawable.ic_online;
      } else if (zone.getProximity().equals(GpsZone.Proximity.NEAR)) {
        drawable = R.drawable.ic_nearby;
      } else if (zone.getProximity().equals(GpsZone.Proximity.FAR)) {
        drawable = R.drawable.ic_offline;
      } else {
        drawable = R.drawable.ic_unavailable;
      }
    }
    circle.setBackgroundResource(drawable);

    discard.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new AlertDialog.Builder(mActivity)
                .setTitle("Delete zone")
                .setMessage("Are you sure want to delete zone "+ zone.getAlias() +"?")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    GpsZoneDAO.getInstance(mActivity).removeZoneByAlias(zone.getAlias());
                    mActivity.loadZoneListAdapter(true);
                    mActivity.startLocationService(true);
                  }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                  }
                })
                .show();
      }
    });

    return view;

  }

  private String formatDistance(int meters) {
    String s;
    if (meters < 1000) {
      s = meters + "m";
    } else if (meters < 10 * 1000) {
      s = String.format("%.1f", meters / 1000.0) + "km";
    } else {
      s = Math.round(meters / 1000.0) + "km";
    }

    return s;
  }

}
