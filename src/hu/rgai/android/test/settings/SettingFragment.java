package hu.rgai.android.test.settings;

import hu.rgai.android.beens.Account;
import hu.rgai.android.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface SettingFragment {
  public MessageProvider.Type getType();
  public Account getAccount();
}
