package com.butent.bee.server.utils;

import com.butent.bee.shared.modules.administration.AdministrationConstants;

import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlUtils {

  public static String cleanHtml(String dirtyHtml) {
    if (dirtyHtml != null) {
      return Jsoup.clean(dirtyHtml, "http:", Whitelist.relaxed()
          .addTags("font")
          .addAttributes("font", "face", "size")
          .addAttributes(":all", "style", "color")
          .preserveRelativeLinks(true), new Document.OutputSettings().prettyPrint(false));
    }
    return dirtyHtml;
  }

  public static Map<String, String> getFileReferences(String html) {
    Map<String, String> files = new HashMap<>();
    Pattern pattern = Pattern.compile("src=\"(" + AdministrationConstants.FILE_URL
        + "/([a-f0-9]{40}))\"");
    Matcher matcher = pattern.matcher(html);

    while (matcher.find()) {
      files.put(matcher.group(2), matcher.group(1));
    }
    return files;
  }

  public static boolean hasHtml(String content) {
    if (content != null) {
      return !Jsoup.isValid(content, Whitelist.none());
    }
    return false;
  }

  public static String stripHtml(String content) {
    if (content != null) {
      return new HtmlToPlainText().getPlainText(Jsoup.parse(content));
    }
    return content;
  }

  public static String cleanXml(String dirtyXml) {
    return Jsoup.parse(dirtyXml, "", Parser.xmlParser()).toString();
  }

  private HtmlUtils() {
  }
}
