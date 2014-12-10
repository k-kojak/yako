package hu.rgai.yako.tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;
import hu.rgai.android.test.R;

import java.util.HashMap;

public class TooltipFactory {

  private static final String TOOLTIP = "TOOLTIP_";
//  private static HashMap<String, Boolean> inProgress = new HashMap<>();
  private static final int borderColor = 0xffffffff;
  public enum Tooltip {
    NAV_DRAWER_MENU(0xff000000, 0xffffffff, null, 0, R.string.tooltip_nav_drawer, true, "1"),
    NEW_MESSAGE_MENU(0xff000000, 0xffffffff, NAV_DRAWER_MENU, R.id.message_send_new, R.string.tooltip_new_message, false, "1"),
    ACCOUNTS_MENU(0xff000000, 0xffffffff, NEW_MESSAGE_MENU, R.id.accounts, R.string.tooltip_account, false, "1")
    ;

    private final int mColor;
    private final int mBgColor;
    private final Tooltip mNext;
    private final int mTargetView;
    private final int mTextResId;
    private final boolean mInCenter;
    private final String mGroup;

    private Tooltip(int color, int bgColor, Tooltip next, int targetView, int textResId, boolean inCenter, String group) {
      mColor = color;
      mBgColor = bgColor;
      mNext = next;
      mTargetView = targetView;
      mTextResId = textResId;
      mInCenter = inCenter;
      mGroup = group;
    }
  }


  public static void display(final ToolTipRelativeLayout layout, final Activity c, final Tooltip tooltip,
                             boolean firstCall) {
//    if (firstCall && inProgress.containsKey(tooltip.mGroup) && inProgress.get(tooltip.mGroup)) {
////      return;
//    } else {
//      inProgress = true;
//    }
    if (!shouldShowTooltip(c, tooltip.mGroup)) {
      return;
    }
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        LayoutInflater inflater = c.getLayoutInflater();
        View v = inflater.inflate(R.layout.tooltip_view, layout, false);
        TextView tv = (TextView)v.findViewById(R.id.text);

        ToolTip toolTip = new ToolTip()
                .withColor(borderColor)
                .withContentView(v)
                .withCloseOnClick(false)
                .withCenter(tooltip.mInCenter)
                .withShadow();

        final ToolTipView ttv;
        if (tooltip.mTargetView != 0) {
          ttv = layout.showToolTipForViewResId(c, toolTip, tooltip.mTargetView);
        } else {
          ttv = layout.showToolTipInCenter(c, toolTip);
        }

        v.setBackgroundColor(tooltip.mBgColor);
        tv.setTextColor(tooltip.mColor);
        tv.setText(c.getString(tooltip.mTextResId));
        if (tooltip.mNext == null) {
          v.findViewById(R.id.next).setVisibility(View.GONE);
          v.findViewById(R.id.close).setVisibility(View.VISIBLE);
          v.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              ttv.remove();
              tooltipViewed(c, tooltip.mGroup);
            }
          });
        } else {
          v.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              display(layout, c, tooltip.mNext, false);
              ttv.remove();
            }
          });
        }
      }
    }, 500);
  }


  private static boolean shouldShowTooltip(Context context, String key) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Boolean viewed = prefs.getBoolean(TOOLTIP + key, true);
    return !viewed;
  }

  private static void tooltipViewed(Context context, String key) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(TOOLTIP + key, true);
    editor.commit();
  }



}
