
package hu.rgai.yako.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.GpsZoneDAO;

import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ZoneListAdapter extends BaseAdapter {

  private static LayoutInflater inflater = null;
  private List<GpsZone> mGpsZones = null;
  private MainActivity mActivity;


  public ZoneListAdapter(MainActivity context, List<GpsZone> gpsZones) {
    mActivity = context;
    mGpsZones = gpsZones;
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
    TextView radius = (TextView) view.findViewById(R.id.radius);
    ImageView discard = (ImageView) view.findViewById(R.id.discard);
    ImageView circle = (ImageView) view.findViewById(R.id.active_status);

    final GpsZone zone = (GpsZone)getItem(position);

    // Setting all values in listview
    alias.setText(zone.getAlias());
    radius.setText("(" + String.valueOf(zone.getRadius()) + "m)");
    circle.setBackgroundResource(zone.isActive() ? R.drawable.ic_green_circle_on : R.drawable.ic_green_circle_off);

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
}
