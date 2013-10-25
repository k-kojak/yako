/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.test.settings;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import com.actionbarsherlock.app.SherlockFragment;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.test.R;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookSettingActivity extends Activity implements TextWatcher {

  private EditText email;
  private EditText pass;
  private Spinner messageAmount;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle); //To change body of generated methods, choose Tools | Templates.
    
    setContentView(R.layout.account_settings_facebook_layout);
    
    messageAmount = (Spinner)findViewById(R.id.initial_items_num);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.initial_emails_num,
            android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
    messageAmount.setAdapter(adapter);
    
    email = (EditText)findViewById(R.id.email_address);
    email.addTextChangedListener(this);
    pass = (EditText)findViewById(R.id.password);
    
    
    Bundle b = getIntent().getExtras();
    if (b != null && b.getParcelable("account") != null) {
      FacebookAccountAndr acc = (FacebookAccountAndr)b.getParcelable("account");
      email.setText(acc.getUserName());
      pass.setText(acc.getPassword());
      messageAmount.setSelection(AccountSettingsList.getSpinnerPosition(messageAmount.getAdapter(), acc.getMessageLimit()));
    }
    
  }
  
  public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
    AccountSettingsList.validateEmailField(email, text.toString());
  }

  public void afterTextChanged(Editable e) {}
  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

//  public MessageProvider.Type getType() {
//    return MessageProvider.Type.FACEBOOK;
//  }
//  
//  public FacebookAccountAndr getAccount() {
//    String m = email.getText().toString();
//    String p = pass.getText().toString();
//    int num = Integer.parseInt((String)messageAmount.getSelectedItem());
//    return new FacebookAccountAndr(num, m, p);
//  }
  
}
