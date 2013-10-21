package hu.rgai.android.tools.view;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.text.Annotation;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.test.R;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChipsMultiAutoCompleteTextView extends MultiAutoCompleteTextView implements OnItemClickListener {

  private final String TAG = "ChipsMultiAutoCompleteTextview";
  private ArrayList<RecipientItem> recipients;
  private static int chipTextImgDimension = 50;

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

  public ChipsMultiAutoCompleteTextView(Context context, AttributeSet attrs,
          int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }
  /* set listeners for item click and text change */

  public void init(Context context) {
    recipients = new ArrayList<RecipientItem>();
    setOnItemClickListener(this);
    addTextChangedListener(textWather);
  }
  /*TextWatcher, If user type any country name and press comma then following code will regenerate chips */
//  private TextWatcher textWather = new TextWatcher() {
//    @Override
//    public void onTextChanged(CharSequence s, int start, int before, int count) {
//      Log.d("rgai", "'" + s + "'");
//      Log.d("rgai", start + " - " + before + " - " + count);
//      if (count == 0 && before == 1) {
////        if (s.charAt(start) == ',') {
//          setChips2(); // generate chips
////        }
//      }
//    }
//
//    @Override
//    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//    }
//
//    @Override
//    public void afterTextChanged(Editable s) {
//    }
//  };
  private TextWatcher textWather = new TextWatcher() {
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      if (count == 0) {
        recipients.clear();
        Log.d("rgai", "toroltunk!");
        Spannable sp = ((Spannable)getText());
        ChipsMultiAutoCompleteTextView.ImageSpanExt[] annsToRemove = sp.getSpans(0, getText().length(), ChipsMultiAutoCompleteTextView.ImageSpanExt.class);
        for (int i = 0; i < annsToRemove.length; i++) {
          recipients.add((RecipientItem)annsToRemove[i].getSpecVal());
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
  /*This function has whole logic for chips generate*/

  public void setChips2() {
    
    // removing spans
//    recipients.clear();
    Spannable sp = ((Spannable)getText());
    ChipsMultiAutoCompleteTextView.ImageSpanExt[] annsToRemove = sp.getSpans(0, getText().length(), ChipsMultiAutoCompleteTextView.ImageSpanExt.class);
    for (int i = 0; i < annsToRemove.length; i++) {
      Log.d("rgai", "ez van a listaban -> " + annsToRemove[i].getSpecVal());
      sp.removeSpan(annsToRemove[i]);
//      recipients.add((UserItem)annsToRemove[i].getSpecVal());
    }
    
    String newText = "";
    int ind = 0;
    for (RecipientItem u : recipients) {
      if (ind > 0) {
        newText += ",";
      }
      newText += u.getDisplayName();
      ind++;
    }
    
//    if (getText().toString().contains(",")) {

      SpannableStringBuilder ssb = new SpannableStringBuilder(newText + ",");
// split string wich comma
      String chips[] = newText.split(",");
//      String chips[] = {"a","b","c","d"};
//      String t = "";
//      for (int i = 0; i < chips.length; i++) {
//        chips[i] = chips[i].trim() + " ";
//        if (i > 0) {
//        }
//        t += chips[i];
//        t += ", ";
//      }
//      Log.d("rgai", "the original text -> " + getText().toString().trim());
//      Log.d("rgai", "the text -> " + t + " ("+ t.length() +")");
//      SpannableStringBuilder ssb = new SpannableStringBuilder(t);
      int x = 0;
// loop will generate ImageSpan for every country name separated by comma
      for (RecipientItem u : recipients) {
//        c = c.trim();
// inflate chips_edittext layout
          LayoutInflater lf = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
          TextView textView = (TextView) lf.inflate(R.layout.chips_edittext, null);
          textView.setText(u.getDisplayName()); // set text
//          setFlags(textView, u.getText()); // set flag image
          setImageIcon(textView, u.getImgUri(), u.getContactId()); // set flag image
  // capture bitmapt of genreated textview
          int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
          textView.measure(spec, spec);
          textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
          Bitmap b = Bitmap.createBitmap(textView.getWidth(), textView.getHeight(), Bitmap.Config.ARGB_8888);
          Canvas canvas = new Canvas(b);
          canvas.translate(-textView.getScrollX(), -textView.getScrollY());
          textView.draw(canvas);
          textView.setDrawingCacheEnabled(true);
          Bitmap cacheBmp = textView.getDrawingCache();
          Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
          textView.destroyDrawingCache(); // destory drawable
  // create bitmap drawable for imagespan
          BitmapDrawable bmpDrawable = new BitmapDrawable(viewBmp);
          bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(), bmpDrawable.getIntrinsicHeight());
  // create and set imagespan
//          ValamiKlassz vk = new ValamiKlassz("asder");
//          ssb.setSpan(vk, x + c.length() - 1, x + c.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
          ChipsMultiAutoCompleteTextView.ImageSpanExt ise = new ChipsMultiAutoCompleteTextView.ImageSpanExt(bmpDrawable, u);
          ssb.setSpan(ise, x, x + u.getDisplayName().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//          ssb.
//          ssb.set
//          Annotation a = new Annotation("kulcs", "ertek -> " + Math.random());
//          ssb.setSpan(a, x, x, Spannable.);
// hiding user info
//        StyleSpan bold = new StyleSpan(Typeface.BOLD);
//        ssb.setSpan(bold, 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        x = x + u.getDisplayName().length() + 1;
      }
      
// set chips span
      setText(ssb);
      
//      ImageSpanExt[] anns = ((Spannable)getText()).getSpans(0, getText().length(), ImageSpanExt.class);
//      Log.d("rgai", "length of annotations -> " + anns.length);
//      for (int i = 0; i < anns.length; i++) {
//        Log.d("rgai", "---> " + anns[i].getSpecVal());
//      }
      ChipsMultiAutoCompleteTextView.ImageSpanExt[] anns = ((Spannable)getText()).getSpans(0, getText().length(), ChipsMultiAutoCompleteTextView.ImageSpanExt.class);
      Log.d("rgai", "length of annotations -> " + anns.length);
      for (int i = 0; i < anns.length; i++) {
        Log.d("rgai", "---> " + anns[i].getSpecVal());
      }
// move cursor to last
      setSelection(getText().length());
//    }
  }
  
  public void setChips() {
    // removing spans
    Spannable sp = ((Spannable)getText());
    ChipsMultiAutoCompleteTextView.ImageSpanExt[] annsToRemove = sp.getSpans(0, getText().length(), ChipsMultiAutoCompleteTextView.ImageSpanExt.class);
    for (int i = 0; i < annsToRemove.length; i++) {
      Log.d("rgai", "ez van a listaban -> " + annsToRemove[i].getSpecVal());
      sp.removeSpan(annsToRemove[i]);
    }
//    if (getText().toString().contains(",")) {

      SpannableStringBuilder ssb = new SpannableStringBuilder(getText());
// split string wich comma
      String chips[] = getText().toString().trim().split(",");
//      String chips[] = {"a","b","c","d"};
//      String t = "";
//      for (int i = 0; i < chips.length; i++) {
//        chips[i] = chips[i].trim() + " ";
//        if (i > 0) {
//        }
//        t += chips[i];
//        t += ", ";
//      }
//      Log.d("rgai", "the original text -> " + getText().toString().trim());
//      Log.d("rgai", "the text -> " + t + " ("+ t.length() +")");
//      SpannableStringBuilder ssb = new SpannableStringBuilder(t);
      int x = 0;
// loop will generate ImageSpan for every country name separated by comma
      for (String c : chips) {
//        c = c.trim();
// inflate chips_edittext layout
          LayoutInflater lf = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
          TextView textView = (TextView) lf.inflate(R.layout.chips_edittext, null);
          textView.setText(c); // set text
//          setFlags(textView, c); // set flag image
  // capture bitmapt of genreated textview
          int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED) ;
          textView.measure(spec, spec);
          textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
          Bitmap b = Bitmap.createBitmap(textView.getWidth(), textView.getHeight(), Bitmap.Config.ARGB_8888);
          Canvas canvas = new Canvas(b);
          canvas.translate(-textView.getScrollX(), -textView.getScrollY());
          textView.draw(canvas);
          textView.setDrawingCacheEnabled(true);
          Bitmap cacheBmp = textView.getDrawingCache();
          Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
          textView.destroyDrawingCache(); // destory drawable
  // create bitmap drawable for imagespan
          BitmapDrawable bmpDrawable = new BitmapDrawable(viewBmp);
          bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(), bmpDrawable.getIntrinsicHeight());
  // create and set imagespan
//          ValamiKlassz vk = new ValamiKlassz("asder");
//          ssb.setSpan(vk, x + c.length() - 1, x + c.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
          ChipsMultiAutoCompleteTextView.ImageSpanExt ise = new ChipsMultiAutoCompleteTextView.ImageSpanExt(bmpDrawable, "Nyenyere -> '" + c + "'");
          ssb.setSpan(ise, x, x + c.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//          ssb.
//          ssb.set
//          Annotation a = new Annotation("kulcs", "ertek -> " + Math.random());
//          ssb.setSpan(a, x, x, Spannable.);
// hiding user info
//        StyleSpan bold = new StyleSpan(Typeface.BOLD);
//        ssb.setSpan(bold, 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        x = x + c.length() + 1;
      }
      
// set chips span
      setText(ssb);
      
//      ImageSpanExt[] anns = ((Spannable)getText()).getSpans(0, getText().length(), ImageSpanExt.class);
//      Log.d("rgai", "length of annotations -> " + anns.length);
//      for (int i = 0; i < anns.length; i++) {
//        Log.d("rgai", "---> " + anns[i].getSpecVal());
//      }
      ChipsMultiAutoCompleteTextView.ImageSpanExt[] anns = ((Spannable)getText()).getSpans(0, getText().length(), ChipsMultiAutoCompleteTextView.ImageSpanExt.class);
      Log.d("rgai", "length of annotations -> " + anns.length);
      for (int i = 0; i < anns.length; i++) {
        Log.d("rgai", "---> " + anns[i].getSpecVal());
      }
// move cursor to last
      setSelection(getText().length());
//    }
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

    public void setSpecVal(String specVal) {
      this.specVal = specVal;
    }
    
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    recipients.add(((AutoCompleteRow)view).getRecipient());
//    TextView tv = (TextView) view.findViewById(R.id.name);
    Log.d("rgai", "parent child number -> " + parent.getChildCount());
//    Log.d("rgai", "parent nth child -> " + parent.getChildAt(position - 1).toString());
    Log.d("rgai", "position -> " + position);
    Log.d("rgai", "id -> " + id);
    Log.d("rgai", "parent -> " + parent.getClass().toString());
    Log.d("rgai", view.toString());
    Log.d("rgai", view.getClass().toString());
//    Log.d("rgai", tv.getText().toString());
    setChips2(); // call generate chips when user select any item from auto complete
  }
  

  public void setImageIcon(TextView textView, Uri imgUri, int id) {
    InputStream is = null;
    Bitmap bm = null;
    BitmapDrawable bd;
    
    Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, id);
    Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
    Cursor cursor = this.getContext().getContentResolver().query(photoUri,
          new String[] {Contacts.Photo.PHOTO}, null, null, null);
     if (cursor == null) {
         is = null;
     }
     try {
         if (cursor.moveToFirst()) {
             byte[] data = cursor.getBlob(0);
             if (data != null) {
                 is = new ByteArrayInputStream(data);
             }
         }
     } finally {
       cursor.close();
     }

    
    if (imgUri != null) {
//      imgUri = Uri.parse(imgUri.toString().substring(0,imgUri.toString().length() - 6));
//      is = ContactsContract.Contacts.openContactPhotoInputStream(this.getContext().getContentResolver(), imgUri);
//      is = ContactsContract.Contacts.this.getContext().getContentResolver(), imgUri);
//      is = ContactsContract.Contacts.
    }
    
    if (is != null) {
      Bitmap bitmap = BitmapFactory.decodeStream(is);
      bd = new BitmapDrawable(null, Bitmap.createScaledBitmap(bitmap, chipTextImgDimension, chipTextImgDimension, true));
    } else {
      bm = BitmapFactory.decodeResource(getResources(), R.drawable.android);
      bd = new BitmapDrawable(null, bm);
    }
    textView.setCompoundDrawablesWithIntrinsicBounds(null, null, bd, null);
  }
}
