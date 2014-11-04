package hu.rgai.yako.view.extensions;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import hu.rgai.yako.beens.EmailMessageRecipient;
import hu.rgai.yako.beens.MessageRecipient;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Person;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.tools.ProfilePhotoProvider;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChipsMultiAutoCompleteTextView extends MultiAutoCompleteTextView implements OnItemClickListener,
        View.OnFocusChangeListener {

  private static final String EMAIL_PATTERN =
          "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                  + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

  private ArrayList<MessageRecipient> recipients;
  private static int CHIP_TEXT_DIMENSION_IN_DIP = 55;
  private OnChipChangeListener mChipListener = null;

  /* Constructor */
  public ChipsMultiAutoCompleteTextView(Context context) {
    super(context);
    init(context);
  }
  /* Constructor */

  public ChipsMultiAutoCompleteTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }
  /* Constructor */

  public ChipsMultiAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }
  /* set listeners for item click and text change */

  public void addOnChipChangeListener(OnChipChangeListener listener) {
    mChipListener = listener;
  }

  public void init(Context context) {
    recipients = new ArrayList<MessageRecipient>();
    setOnItemClickListener(this);
    addTextChangedListener(textWather);
    setOnFocusChangeListener(this);
  }


  private TextWatcher textWather = new TextWatcher() {
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      if (count == 0) {
        recipients.clear();
        Spannable sp = getText();
        ChipsMultiAutoCompleteTextView.ImageSpanExt[] annsToRemove = sp.getSpans(0, getText().length(), ChipsMultiAutoCompleteTextView.ImageSpanExt.class);
        for (ImageSpanExt anAnnsToRemove : annsToRemove) {
          recipients.add((MessageRecipient) anAnnsToRemove.getSpecVal());
        }
        callChipChangeListener();
      } else {
        if (s.length() != 0 && s.charAt(s.length() - 1) == ',') {
          parseRecipientsField();
        }

      }
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
  };

  private void parseRecipientsField() {
    List<String> validEmails = getEmailRecipients(this.getText());
    removeRecipientDuplicates(validEmails);
    if (!validEmails.isEmpty()) {
      addHandRecipientsToAll(validEmails);
      setChips2();
    }

  }

  private void removeRecipientDuplicates(List<String> handMails) {
    Iterator<String> it = handMails.iterator();
    while (it.hasNext()) {
      String m = it.next();
      for (MessageRecipient mr : recipients) {
        if (mr.getType().equals(MessageProvider.Type.EMAIL) || mr.getType().equals(MessageProvider.Type.GMAIL)) {
          EmailMessageRecipient emr = (EmailMessageRecipient) mr;
          if (emr.getEmail().toLowerCase().equals(m)) {
            it.remove();
          }
        }
      }
    }
  }

  private void addHandRecipientsToAll(List<String> validEmails) {
    for (String m : validEmails) {
      recipients.add(MessageRecipient.Helper.personToRecipient(new Person(-1, m, m, MessageProvider.Type.EMAIL)));
    }
  }

  private List<String> getEmailRecipients(CharSequence s) {
    List<String> emails = new LinkedList<String>(Arrays.asList(s.toString().split(",")));
    // trimming
    for (int i = 0; i < emails.size(); i++) {
      emails.set(i, emails.get(i).trim());
    }

    Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    Iterator<String> it = emails.iterator();
    while (it.hasNext()) {
      String mail = it.next();
      Matcher matcher = pattern.matcher(mail);
      if (!matcher.matches()) {
        it.remove();
      }
    }

    return emails;
  }

  private void callChipChangeListener() {
    if (mChipListener != null) {
      mChipListener.onChipListChange();
    }
  }

  /*This function has whole logic for chips generate*/
  public void setChips2() {
    callChipChangeListener();
    // removing spans
//    recipients.clear();
    Spannable sp = getText();
    ChipsMultiAutoCompleteTextView.ImageSpanExt[] annsToRemove = sp.getSpans(0, getText().length(), ChipsMultiAutoCompleteTextView.ImageSpanExt.class);
    for (ImageSpanExt anAnnsToRemove : annsToRemove) {
      Log.d("rgai", "ez van a listaban -> " + anAnnsToRemove.getSpecVal());
      sp.removeSpan(anAnnsToRemove);
//      recipients.add((UserItem)annsToRemove[i].getSpecVal());
    }

    String newText = "";
    int ind = 0;
    for (MessageRecipient u : recipients) {
      if (ind > 0) {
        newText += ",";
      }
      if (u.getContactId() == -1) {
        newText += u.getDisplayData();
      } else {
        newText += u.getDisplayName();
      }

      ind++;
    }

    SpannableStringBuilder ssb = new SpannableStringBuilder(newText + ",");

    int x = 0;
// loop will generate ImageSpan for every country name separated by comma
    for (MessageRecipient u : recipients) {
// inflate chips_edittext layout
      LayoutInflater lf = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
      ViewGroup vg = (ViewGroup) this.getParent();
      TextView textView = (TextView) lf.inflate(R.layout.chips_edittext, vg, false);
      String displayText;
      if (u.getContactId() == -1) {
        displayText = u.getDisplayData();
      } else {
        displayText = u.getDisplayName();
      }
      textView.setText(displayText);
//          setFlags(textView, u.getText()); // set flag image
      setImageIcon(textView, u.getContactId()); // set flag image
      // capture bitmapt of genreated textview
      int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
      textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
      textView.measure(spec, spec);

      textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
      Bitmap b = Bitmap.createBitmap(textView.getWidth(), textView.getHeight(), Bitmap.Config.ARGB_8888);
//          Bitmap b = Bitmap.createBitmap(70, 70, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(b);

      canvas.translate(-textView.getScrollX(), -textView.getScrollY());
      textView.draw(canvas);
      textView.setDrawingCacheEnabled(true);
      Bitmap cacheBmp = textView.getDrawingCache();
      Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
      Log.d("rgai", "viewBmp.getWidth() -> " + viewBmp.getWidth());
      Log.d("rgai", "viewBmp.getHeight() -> " + viewBmp.getHeight());

      textView.destroyDrawingCache(); // destory drawable
      // create bitmap drawable for imagespan
      BitmapDrawable bmpDrawable = new BitmapDrawable(viewBmp);
      bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(), bmpDrawable.getIntrinsicHeight());
      // create and set imagespan
//          ValamiKlassz vk = new ValamiKlassz("asder");
//          ssb.setSpan(vk, x + c.length() - 1, x + c.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
      ChipsMultiAutoCompleteTextView.ImageSpanExt ise = new ChipsMultiAutoCompleteTextView.ImageSpanExt(bmpDrawable, u);
      ssb.setSpan(ise, x, x + displayText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//          ssb.
//          ssb.set
//          Annotation a = new Annotation("kulcs", "ertek -> " + Math.random());
//          ssb.setSpan(a, x, x, Spannable.);
// hiding user info
//        StyleSpan bold = new StyleSpan(Typeface.BOLD);
//        ssb.setSpan(bold, 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
      x = x + displayText.length() + 1;
    }

// set chips span
    setText(ssb);

//      ImageSpanExt[] anns = ((Spannable)getText()).getSpans(0, getText().length(), ImageSpanExt.class);
//      Log.d("rgai", "length of annotations -> " + anns.length);
//      for (int i = 0; i < anns.length; i++) {
//        Log.d("rgai", "---> " + anns[i].getSpecVal());
//      }
    ChipsMultiAutoCompleteTextView.ImageSpanExt[] anns = getText().getSpans(0, getText().length(),
            ChipsMultiAutoCompleteTextView.ImageSpanExt.class);
    Log.d("rgai", "length of annotations -> " + anns.length);
    for (ImageSpanExt ann : anns) {
      Log.d("rgai", "---> " + ann.getSpecVal());
    }
// move cursor to last
    setSelection(getText().length());
//    }
  }


  @Override
  public void onFocusChange(View v, boolean hasFocus) {
    if (!hasFocus) {
      parseRecipientsField();
    }
  }

  private class ImageSpanExt extends ImageSpan {

    private Object specVal;

    public ImageSpanExt(Drawable d, Object source) {
      super(d);
      this.specVal = source;
    }

    public Object getSpecVal() {
      return specVal;
    }

  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    recipients.add(((AutoCompleteRow) view).getRecipient());
    setChips2(); // call generate chips when user select any item from auto complete
  }

  public List<MessageRecipient> getRecipients() {
    return recipients;
  }

  public void addRecipient(MessageRecipient ri) {
    recipients.add(ri);
    setChips2();
  }

  public void setImageIcon(TextView textView, int id) {
    Bitmap bitmap = ProfilePhotoProvider.getImageToUser(this.getContext(), id).getBitmap();
    int px = dipToPixels(CHIP_TEXT_DIMENSION_IN_DIP);
    Log.d("rgai", CHIP_TEXT_DIMENSION_IN_DIP + " DIP = " + px + " PX");
    BitmapDrawable bd = new BitmapDrawable(null, Bitmap.createScaledBitmap(bitmap, px, px, true));
//    }
    textView.setCompoundDrawablesWithIntrinsicBounds(null, null, bd, null);
  }

  private int dipToPixels(float dipValue) {
    DisplayMetrics metrics = this.getResources().getDisplayMetrics();
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
  }

  public interface OnChipChangeListener {
    public void onChipListChange();
  }

}
