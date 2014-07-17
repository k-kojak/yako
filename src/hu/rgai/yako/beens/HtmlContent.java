package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.Serializable;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class HtmlContent implements Parcelable, Serializable {

  public enum ContentType {
    TEXT("text/*"),
    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html");
    
    private final String mimeType;

    ContentType(String mimeType) {
      this.mimeType = mimeType;
    }
    
    public String getMimeName() {
      return mimeType;
    }
  };


  protected StringBuilder content;
  protected ContentType contentType;

  
  public static final Parcelable.Creator<HtmlContent> CREATOR = new Parcelable.Creator<HtmlContent>() {
    public HtmlContent createFromParcel(Parcel in) {
      return new HtmlContent(in);
    }

    public HtmlContent[] newArray(int size) {
      return new HtmlContent[size];
    }
  };
  
  public HtmlContent(Parcel in) {
    this.content = new StringBuilder(in.readString());
    this.contentType = ContentType.valueOf(in.readString());
  }

  public HtmlContent() {
    content = new StringBuilder();
    contentType = ContentType.TEXT_PLAIN;
  }

  public HtmlContent(String content, ContentType contentType) {
    this.content = new StringBuilder(content);
    this.contentType = contentType;
  }

  public StringBuilder getContent() {
    return content;
  }

  public ContentType getContentType() {
    return contentType;
  }
  
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    out.writeString(content.toString());
    out.writeString(contentType.toString());
  }
  
}
