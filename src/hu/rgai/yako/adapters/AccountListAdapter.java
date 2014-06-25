
package hu.rgai.yako.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.AccountDAO;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AccountListAdapter extends CursorAdapter {

//  private final List<Account> accounts;
  private static LayoutInflater inflater = null;
  private int mLayoutRes;


  public AccountListAdapter(Context context, Cursor c) {
    super(context, c, false);
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }


  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = inflater.inflate(R.layout.account_list_item, parent, false);
    return v;
  }


  @Override
  public void bindView(View view, Context context, Cursor cursor) {

    if (view == null) {
      throw new RuntimeException("view is null at " + this.getClass().getSimpleName());
    }

    TextView name = (TextView) view.findViewById(R.id.name);
    TextView type = (TextView) view.findViewById(R.id.type);
    ImageView icon = (ImageView) view.findViewById(R.id.img);

    Account account = AccountDAO.cursorToAccount(cursor);

    // Setting all values in listview
    name.setText((String)account.getDisplayName());
    type.setText((String)account.getAccountType().toString());
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
}
