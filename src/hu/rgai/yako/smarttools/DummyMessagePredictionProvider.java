package hu.rgai.yako.smarttools;

import android.util.Log;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Source;
import android.content.Context;

/**
 * Created by kojak on 9/25/2014.
 */
public class DummyMessagePredictionProvider implements MessagePredictionProvider {
  
  static int timeLimitToRank = 24 * 60 * 60 * 1000;
  @Override
  public double predictMessage(Context context, MessageListElement message) {
    
    if (message.isSeen())
      return 0;
    if (message.getDate().getTime() < Calendar.getInstance().getTime().getTime() - timeLimitToRank)
      return 0;
    String content = "";
    // this is how you can get the actual content...
    if (message.getMessageType().equals(MessageProvider.Type.EMAIL)
            || message.getMessageType().equals(MessageProvider.Type.GMAIL)) {
      // this case the fullMessage is a single FullSimple message with infos you need
      FullSimpleMessage fullMessage = (FullSimpleMessage) message.getFullMessage();
      Source source = new Source(fullMessage.getContent().getContent());
      content = source.getRenderer().toString();
      //content = fullMessage.getContent().getContent().toString();
    }
    // SMS or Facebook
    else {
      // this case the fullMessage is a TreeSet of FullSimple messages with infos you need (thread items)
      content = message.getTitle();
    }
    /**
     * GpsZone might have an unitialized, default <code>distance</code> and <code>proximity</code> values.
     * That means location cannot be set, so there will be no active (NEAR, CLOSEST) locations.
     */
    List<GpsZone> gpsZones = YakoApp.getSavedGpsZones(context);
    GpsZone closest = GpsZone.getClosest(gpsZones);
    List<String> rankedTerms = new LinkedList<String>();
    
    

    fillRankWords(closest, rankedTerms);
    

    return getRank(content, rankedTerms);
  }
  
  private double getRank(String content, List<String> rankedTerms) {
    double counter = 1;

    
    for (String term : rankedTerms) {
      Pattern pattern = Pattern.compile(term);
      Matcher  matcher = pattern.matcher(content);
      while (matcher.find())
        ++counter;
    }
    double rank = 1 - 1/counter;
    return rank;
  }
  
  private void fillRankWords(GpsZone closest, List<String> rankedTerms) {
    String s = "";
    if (closest != null) {      
      if (closest.getZoneType().equals(GpsZone.ZoneType.REST)) {
        s = "rest";
      } else if (closest.getZoneType().equals(GpsZone.ZoneType.WORK)) {
        s = "work";
        rankedTerms.add("főnök");
        rankedTerms.add("meeting");
        rankedTerms.add("megbeszélés");
        rankedTerms.add("munka");
        rankedTerms.add("hiba");
        rankedTerms.add("határidő");
        rankedTerms.add("program");
        rankedTerms.add("állás");
        rankedTerms.add("gyorsan");
        rankedTerms.add("kell");
        rankedTerms.add("pénz");
      } else if (closest.getZoneType().equals(GpsZone.ZoneType.SILENT)) {
        s = "silent";
        rankedTerms.add("család");
        rankedTerms.add("gyerek");
        rankedTerms.add("apa");
        rankedTerms.add("anya");
        rankedTerms.add("tesó");
        rankedTerms.add("báty");
        rankedTerms.add("nővér");
        rankedTerms.add("szeret");
        rankedTerms.add("csaj");
        rankedTerms.add("nő");
        rankedTerms.add("dota");
        rankedTerms.add("játék");
      }
    }
    rankedTerms.add("!");
    rankedTerms.add("\\?");
    rankedTerms.add("fontos");
    rankedTerms.add("sürgős");
    rankedTerms.add("vigyázz");
    rankedTerms.add("veszély");
    rankedTerms.add("kv");
    rankedTerms.add("kv");
    rankedTerms.add("kávé");
    rankedTerms.add("kávé");

  }

}