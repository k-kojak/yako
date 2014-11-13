package hu.rgai.yako.smarttools;

import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Source;
import android.content.Context;

/**
 * Created by kojak on 9/25/2014.
 */
public class DummyMessagePredictionProvider implements MessagePredictionProvider {
  
  static int timeLimitToRank = 24 * 60 * 60 * 1000;
  static Map<GpsZone.ZoneType,LinkedList<Pattern>> zoneTermListMap = new HashMap<GpsZone.ZoneType, LinkedList<Pattern>>();
  
  static {
    fillRankWords();
  }
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

    return getRank(content, closest);
  }
  
  private static double getRank(String content, GpsZone closest) {
    double counter = 1;
    content = content.toLowerCase();
    if (closest != null) {      
      LinkedList<Pattern> termPatternList = zoneTermListMap.get(closest.getZoneType());
      if (termPatternList == null)
        return 0;
      for (Pattern termPattern : termPatternList) {
        Matcher  matcher = termPattern.matcher(content);
        while (matcher.find())
          ++counter;
      }
      return 1 - 1/counter;
    } else {
      return 0;
    }
    
  }
  
  private static void fillRankWords() {
    LinkedList<Pattern> workTermList = new LinkedList<Pattern>();
    LinkedList<Pattern> restTermList = new LinkedList<Pattern>();
    LinkedList<Pattern> silentTermList = new LinkedList<Pattern>();
        
    fillWorkTermList(workTermList);

    fillRestTermList(restTermList);
    zoneTermListMap.put(GpsZone.ZoneType.WORK, workTermList);
    zoneTermListMap.put(GpsZone.ZoneType.SILENT, silentTermList);
    zoneTermListMap.put(GpsZone.ZoneType.REST, restTermList);
  }

  private static void fillRestTermList(LinkedList<Pattern> restTermList) {
    restTermList.add(Pattern.compile(".*" + "család" + ".*"));
    restTermList.add(Pattern.compile(".*" + "gyerek" + ".*"));
    restTermList.add(Pattern.compile(".*" + "apa" + ".*"));
    restTermList.add(Pattern.compile(".*" + "anya" + ".*"));
    restTermList.add(Pattern.compile(".*" + "tesó" + ".*"));
    restTermList.add(Pattern.compile(".*" + "báty" + ".*"));
    restTermList.add(Pattern.compile(".*" + "nővér" + ".*"));
    restTermList.add(Pattern.compile(".*" + "szeret" + ".*"));
    restTermList.add(Pattern.compile(".*" + "csaj" + ".*"));
    restTermList.add(Pattern.compile(".*" + "nő" + ".*"));
    restTermList.add(Pattern.compile(".*" + "dota" + ".*"));
    restTermList.add(Pattern.compile(".*" + "játék" + ".*"));
    restTermList.add(Pattern.compile(".*" + "bor" + ".*"));
    restTermList.add(Pattern.compile(".*" + "sör" + ".*"));
    restTermList.add(Pattern.compile(".*" + "pálinka" + ".*"));
    restTermList.add(Pattern.compile(".*" + "pia" + ".*"));
    restTermList.add(Pattern.compile(".*" + "whisky" + ".*"));
    restTermList.add(Pattern.compile(".*" + "szesz" + ".*"));
    restTermList.add(Pattern.compile(".*" + "szex" + ".*"));
    restTermList.add(Pattern.compile(".*" + "!" + ".*"));
    restTermList.add(Pattern.compile(".*" + "\\?" + ".*"));
    restTermList.add(Pattern.compile(".*" + "fontos" + ".*"));
    restTermList.add(Pattern.compile(".*" + "sürgős" + ".*"));
    restTermList.add(Pattern.compile(".*" + "vigyázz" + ".*"));
    restTermList.add(Pattern.compile(".*" + "veszély" + ".*"));
    restTermList.add(Pattern.compile(".*" + "kv" + ".*"));
    restTermList.add(Pattern.compile(".*" + "kv" + ".*"));
    restTermList.add(Pattern.compile(".*" + "kávé" + ".*"));
    restTermList.add(Pattern.compile(".*" + "kávé" + ".*"));
  }

  private static void fillWorkTermList(LinkedList<Pattern> workTermList) {
    workTermList.add(Pattern.compile(".*" + "főnök" + ".*"));
    workTermList.add(Pattern.compile(".*" + "meeting" + ".*"));
    workTermList.add(Pattern.compile(".*" + "megbeszélés" + ".*"));
    workTermList.add(Pattern.compile(".*" + "munka" + ".*"));
    workTermList.add(Pattern.compile(".*" + "hiba" + ".*"));
    workTermList.add(Pattern.compile(".*" + "határidő" + ".*"));
    workTermList.add(Pattern.compile(".*" + "program" + ".*"));
    workTermList.add(Pattern.compile(".*" + "állás" + ".*"));
    workTermList.add(Pattern.compile(".*" + "gyorsan" + ".*"));
    workTermList.add(Pattern.compile(".*" + "kell" + ".*"));
    workTermList.add(Pattern.compile(".*" + "pénz" + ".*"));
    workTermList.add(Pattern.compile(".*" + "yako" + ".*"));
    workTermList.add(Pattern.compile(".*" + "!" + ".*"));
    workTermList.add(Pattern.compile(".*" + "\\?" + ".*"));
    workTermList.add(Pattern.compile(".*" + "fontos" + ".*"));
    workTermList.add(Pattern.compile(".*" + "sürgős" + ".*"));
    workTermList.add(Pattern.compile(".*" + "vigyázz" + ".*"));
    workTermList.add(Pattern.compile(".*" + "veszély" + ".*"));
    workTermList.add(Pattern.compile(".*" + "kv" + ".*"));
    workTermList.add(Pattern.compile(".*" + "kv" + ".*"));
    workTermList.add(Pattern.compile(".*" + "kávé" + ".*"));
    workTermList.add(Pattern.compile(".*" + "kávé" + ".*"));
  }

}