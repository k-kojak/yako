
package hu.rgai.yako.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ZoneListAdapter extends CursorAdapter {

//  private final List<Account> accounts;
  private static LayoutInflater inflater = null;
  private MainActivity mActivity;


  public ZoneListAdapter(MainActivity context, Cursor c) {
    super(context, c, false);
    mActivity = context;
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }


  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = inflater.inflate(R.layout.zone_list_item, parent, false);
    return v;
  }


  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    if (view == null) {
      throw new RuntimeException("view is null at " + this.getClass().getSimpleName());
    }

    TextView alias = (TextView) view.findViewById(R.id.alias);
    TextView radius = (TextView) view.findViewById(R.id.radius);
    ImageView discard = (ImageView) view.findViewById(R.id.discard);

    final GpsZone zone = GpsZoneDAO.cursorToGpsZone(cursor);

    // Setting all values in listview
    alias.setText(zone.getAlias());
    radius.setText("(" + String.valueOf(zone.getRadius()) + "m)");
    discard.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new AlertDialog.Builder(mActivity)
                .setTitle("Delete zone")
                .setMessage("Are you sure want to delete zone "+ zone.getAlias() +"?")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    GpsZoneDAO.getInstance(mContext).removeZoneByAlias(zone.getAlias());
                    mActivity.loadZoneListAdapter();
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
  }
}
