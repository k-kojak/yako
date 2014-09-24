package hu.rgai.yako.view.extensions;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

public class LinearListView extends LinearLayout {

  private OnItemClickListener mListener = null;
  private boolean mSingleSelect = false;

  public LinearListView(Context context) {
    super(context);
  }

  public LinearListView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public LinearListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setIsSingleSelect(boolean isSingleSelect) {
    mSingleSelect = isSingleSelect;
  }

  public void setAdapter(final ListAdapter adapter) {
    this.removeAllViews();
    for (int i = 0; i < adapter.getCount(); i++) {
      final int position = i;
      View v = adapter.getView(i, null, this);
      addView(v);
      v.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          if (mListener != null) {
            mListener.onItemClick(adapter.getItem(position), position);
          }
        }
      });
    }
  }

  public void setItemChecked(int position, boolean checked) {
    if (mSingleSelect) {
      for (int i = 0; i < getChildCount(); i++) {
        getChildAt(i).setActivated(false);
      }
    }
    getChildAt(position).setActivated(checked);
  }

  public void setOnItemClickListener(OnItemClickListener listener) {
    mListener = listener;
  }

  public interface OnItemClickListener {
    public void onItemClick(Object item, int position);
  }

}
