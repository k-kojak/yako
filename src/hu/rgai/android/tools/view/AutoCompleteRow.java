package hu.rgai.android.tools.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import hu.rgai.android.intent.beens.RecipientItem;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AutoCompleteRow extends RelativeLayout {

  private RecipientItem user;
  
  public AutoCompleteRow(Context context) {
    super(context);
  }

  public AutoCompleteRow(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AutoCompleteRow(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setRecipient(RecipientItem user) {
    this.user = user;
  }
  
  public RecipientItem getRecipient() {
    return user;
  }

}
