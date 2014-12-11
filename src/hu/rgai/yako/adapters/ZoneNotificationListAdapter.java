package hu.rgai.yako.adapters;


import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.sql.AccountDAO;

import java.util.HashMap;

public class ZoneNotificationListAdapter extends CursorAdapter {

  private static LayoutInflater inflater = null;
  private HashMap<String, Boolean> checkState = null;

  public ZoneNotificationListAdapter(Context context, Cursor cursorOfAccounts, HashMap<String, Boolean> checkState) {
    super(context, cursorOfAccounts, false);
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    
    this.checkState = new HashMap <>();
    this.checkState.putAll(checkState);
  }

  static class ViewHolder {  
    protected TextView accountName;  
    protected CompoundButton accountSwitch;  
  } 


  @Override
  public void bindView(View view, Context context, Cursor cursor) {

    final ViewHolder holder = (ViewHolder)view.getTag();
    final Account account = AccountDAO.cursorToAccount(cursor);
    holder.accountName.setText(account.getDisplayName());

    holder.accountSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        holder.accountSwitch.setChecked(isChecked);
        checkState.put(account.getDisplayName(), isChecked);
      }
    });
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {

    View view = inflater.inflate(R.layout.zone_notification_list_item, parent, false);

    Account account = AccountDAO.cursorToAccount(cursor);
    ViewHolder holder = new ViewHolder();
    holder.accountName = (TextView) view.findViewById(R.id.account_textview);
    holder.accountSwitch = (CompoundButton) view.findViewById(R.id.account_switch);
    holder.accountName.setText(account.getDisplayName());
    holder.accountSwitch.setChecked(checkState.get(account.getDisplayName()));

    view.setTag(holder);
    
    return view;
  }

  public void setPredefinedState(HashMap<String, Boolean> states) {
    checkState = states;
  }


  public HashMap<String, Boolean> getAllState() {
    HashMap<String, Boolean> allState = new HashMap<>();
    allState.putAll(checkState);
    return allState;
  }

}
