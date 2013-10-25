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
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.test.R;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class SimpleEmailSettingActivity extends Activity implements TextWatcher {

  private EditText email;
  private EditText pass;
  private EditText imap;
  private EditText smtp;
  private Spinner securityType;
  private Spinner messageAmount;
  private Map<String, String> domainMap;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle); //To change body of generated methods, choose Tools | Templates.

    domainMap = new HashMap<String, String>();
    domainMap.put("vipmail.hu", "indamail.hu");
    domainMap.put("csinibaba.hu", "indamail.hu");
    domainMap.put("totalcar.hu", "indamail.hu");
    domainMap.put("index.hu", "indamail.hu");
    domainMap.put("velvet.hu", "indamail.hu");
    domainMap.put("torzsasztal.hu", "indamail.hu");
    domainMap.put("lamer.hu", "indamail.hu");

    setContentView(R.layout.account_settings_simple_mail_layout);
    
    messageAmount = (Spinner)findViewById(R.id.initial_emails_num);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.initial_emails_num,
            android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    messageAmount.setAdapter(adapter);
    
    securityType = (Spinner)findViewById(R.id.security_type);
    adapter = ArrayAdapter.createFromResource(this,
            R.array.security_types, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
    securityType.setAdapter(adapter);
    
    email = (EditText)findViewById(R.id.email_address);
    pass = (EditText)findViewById(R.id.password);
    imap = (EditText)findViewById(R.id.imap_server);
    smtp = (EditText)findViewById(R.id.smtp_server);
    
    email.addTextChangedListener(this);
    
    imap.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
      public void afterTextChanged(Editable arg0) {}

      public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
        AccountSettingsList.validateUriField(imap, text.toString());
      }
    });
    smtp.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
      public void afterTextChanged(Editable arg0) {}

      public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
        AccountSettingsList.validateUriField(smtp, text.toString());
      }
    });
    
    
    Bundle b = getIntent().getExtras();
    if (b != null && b.getParcelable("account") != null) {
      EmailAccountAndr acc = (EmailAccountAndr)b.getParcelable("account");
      email.setText(acc.getEmail());
      pass.setText(acc.getPassword());
      imap.setText(acc.getImapAddress());
      smtp.setText(acc.getSmtpAddress());
      securityType.setSelection(AccountSettingsList.getSpinnerPosition(securityType.getAdapter(), acc.isSsl() ? "SSL" : "None"));
      messageAmount.setSelection(AccountSettingsList.getSpinnerPosition(messageAmount.getAdapter(), acc.getMessageLimit()));
    }
    
  }
  
  private void autoFillImapSmtpField(CharSequence email) {
    String m = email.toString();
    int pos = m.indexOf("@");
    if (pos != -1) {
      String domain = m.substring(pos + 1).toLowerCase();
      if (domainMap.containsKey(domain)) {
        domain = domainMap.get(domain);
      }
      imap.setText("imap."+domain);
      smtp.setText("smtp."+domain);
    }
  }
  
  private boolean isSsl() {
    return ((String)securityType.getSelectedItem()).equals("SSL") ? true : false;
  }
  
  public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
    AccountSettingsList.validateEmailField(email, text.toString());
    autoFillImapSmtpField(text);
  }

  public void afterTextChanged(Editable e) {}
  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
  
//  public MessageProvider.Type getType() {
//    return MessageProvider.Type.EMAIL;
//  }
//
//  public AccountAndr getAccount() {
//    String m = email.getText().toString();
//    String p = pass.getText().toString();
//    String im = imap.getText().toString();
//    String sm = smtp.getText().toString();
//    boolean ssl = this.isSsl();
//    int num = Integer.parseInt((String)messageAmount.getSelectedItem());
//    return new EmailAccountAndr(m, p, im, sm, ssl, num);
//  }
  
  
}
