
package hu.rgai.yako.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.messageproviders.MessageProvider;
import java.util.TreeSet;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MainListDrawerFilterAdapter extends BaseAdapter {

  public TreeSet<Account> mAccounts;
  private static LayoutInflater mInflater = null;
  
  public MainListDrawerFilterAdapter(Context context, TreeSet<Account> accounts) {
    mAccounts = accounts;
    mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }
  
  public int getCount() {
    return mAccounts.size() + 1;
  }

  public Object getItem(int position) {
    if (position == 0) {
      return null;
    } else {
      position--;
      int i = 0;
      for (Account a : mAccounts) {
        if (i == position) {
          return a;
        }
        i++;
      }
      return null;
    }
  }

  public long getItemId(int position) {
    return position;
  }

  public View getView(int position, View view, ViewGroup parent) {
    if (view == null) {
      view = mInflater.inflate(R.layout.mainlist_navigation_drawer_list_item, parent, false);
    }

    TextView name = (TextView) view.findViewById(R.id.name);
    TextView type = (TextView) view.findViewById(R.id.type);
    ImageView icon = (ImageView) view.findViewById(R.id.img);

    Account account = (Account)getItem(position);
    
    if (account == null) {
      name.setText(R.string.all);
      type.setVisibility(View.GONE);
      icon.setVisibility(View.INVISIBLE);
    } else {
      type.setVisibility(View.VISIBLE);
      icon.setVisibility(View.VISIBLE);
      
      // Setting all values in listview
      name.setText(account.getDisplayName());
      type.setText(account.getAccountType().toString());
      if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
        icon.setImageResource(R.drawable.fb);
      } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
        icon.setImageResource(R.drawable.gmail_icon);
      } else if (account.getAccountType().equals(MessageProvider.Type.SMS)) {
        icon.setImageResource(R.drawable.ic_sms3);
      } else if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
        EmailAccount eacc = (EmailAccount)account;
        String dom = eacc.getEmail().substring(eacc.getEmail().indexOf("@") + 1);
        int dotIndex = dom.indexOf(".");
        if (dotIndex == -1) {
          dom = "";
        } else {
          dom = dom.substring(0, dotIndex);
        }
        icon.setImageResource(Settings.EmailUtils.getResourceIdToEmailDomain(dom));
      }
    }
    
    return view;
  }

}
