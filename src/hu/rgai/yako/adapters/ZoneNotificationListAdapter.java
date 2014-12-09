package hu.rgai.yako.adapters;




import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.GpsZoneDAO;
import hu.rgai.yako.sql.ZoneNotificationDAO;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class ZoneNotificationListAdapter extends CursorAdapter {

  private static LayoutInflater inflater = null;
  private Cursor accounts = null;
  private boolean mUpdate = false;
  private GpsZone mGpsZone= null;
  private Map<String, Boolean> checkState = null;


  public ZoneNotificationListAdapter(Context context, Cursor cursorOfAccounts, GpsZone gpsZone, boolean update) {
    super(context, cursorOfAccounts, false);
    accounts = cursorOfAccounts;
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mUpdate = update;
    mGpsZone = gpsZone; 
    checkState = new HashMap <String,Boolean>();
    for(int i = 0; i < accounts.getCount();i++) {
      accounts.moveToPosition(i);
      accounts.getString(2);
      checkState.put(accounts.getString(2), false);
    }
  }

  static class ViewHolder {  
    protected TextView accountName;  
    protected CompoundButton accountSwitch;  
  } 


  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    // TODO Auto-generated method stub

    final ViewHolder holder = (ViewHolder)view.getTag();

//    holder.accountSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){
//      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//
//        System.out.println("isChecked : ? " + isChecked );  
//        holder.accountSwitch.setChecked(isChecked);
//        checkState.put(holder.accountName.toString(), isChecked);
//        System.out.println("na mivan " + checkState.get(holder.accountName.toString()));
//      }               
//    });
    
   
    holder.accountSwitch.setOnClickListener(new View.OnClickListener() {

      public void onClick(View v) {
          if (((CompoundButton) v).isChecked()) {
            checkState.put(holder.accountName.toString(), true);
          } else {
            checkState.put(holder.accountName.toString(), false);
            
          }
      }
  });

  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    // TODO Auto-generated method stub

    View view = inflater.inflate(R.layout.zone_notification_list_item, parent, false);

    ViewHolder holder = new ViewHolder();
    holder.accountName = (TextView) view.findViewById(R.id.account_textview);
    holder.accountSwitch = (CompoundButton) view.findViewById(R.id.account_switch);

    Account account = AccountDAO.cursorToAccount(cursor);
    holder.accountName.setText((String)account.getDisplayName().toString());

    if(!mUpdate) {
      holder.accountSwitch.setChecked(true);
      checkState.put(holder.accountName.toString(), true);
    } else { 
      int zoneId = GpsZoneDAO.getInstance(mContext).getZoneIdByAlias(mGpsZone.getAlias());
      boolean isChecked = ZoneNotificationDAO.getInstance(mContext).getNotificationCheckedByZoneAndAccount(zoneId, account.getDatabaseId());
      holder.accountSwitch.setChecked(isChecked);
      checkState.put(holder.accountName.toString(), isChecked);
    }

    view.setTag(holder);

    return view;
  }

  public Map<String, Boolean> getAllState() {
    return checkState;
  }

}
