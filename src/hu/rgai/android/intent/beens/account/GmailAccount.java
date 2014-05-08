/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.intent.beens.account;

import hu.uszeged.inf.rgai.messagelog.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class GmailAccount extends EmailAccount {
  
  public GmailAccount(String email, String password, int messageLimit) {
    super(email, password, "imap.gmail.com", "smtp.gmail.com", default_imap_port, default_smtp_port, true, messageLimit);
    this.accountType = MessageProvider.Type.GMAIL;
  }
  
  @Override
  public String toString() {
    return "GmailAccount{" + "email=" + email + ", password=" + password + '}';
  }
  
}
