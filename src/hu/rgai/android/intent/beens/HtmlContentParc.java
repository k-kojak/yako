
package hu.rgai.android.intent.beens;

import android.os.Parcel;
import android.os.Parcelable;
import hu.uszeged.inf.rgai.messagelog.beans.HtmlContent;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class HtmlContentParc extends HtmlContent implements Parcelable {

  public static final Parcelable.Creator<HtmlContentParc> CREATOR = new Parcelable.Creator<HtmlContentParc>() {
    public HtmlContentParc createFromParcel(Parcel in) {
      return new HtmlContentParc(in);
    }

    public HtmlContentParc[] newArray(int size) {
      return new HtmlContentParc[size];
    }
  };
  
  public HtmlContentParc(Parcel in) {
    this.content = new StringBuilder(in.readString());
    this.contentType = HtmlContent.ContentType.valueOf(in.readString());
  }
  
  public HtmlContentParc(HtmlContent htmlContent) {
    this(htmlContent.getContent().toString(), htmlContent.getContentType());
  }
  
  public HtmlContentParc(String content, ContentType contentType) {
    super(content, contentType);
  }
  
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    out.writeString(content.toString());
    out.writeString(contentType.toString());
  }

}
