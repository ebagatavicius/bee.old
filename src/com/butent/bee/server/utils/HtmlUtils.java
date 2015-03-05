package com.butent.bee.server.utils;

import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;

import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlUtils {

  public static String cleanHtml(String dirtyHtml) {
    if (dirtyHtml != null) {
      return Jsoup.clean(dirtyHtml, "http:", Whitelist.relaxed().addAttributes(":all", "style")
          .preserveRelativeLinks(true));
    }
    return dirtyHtml;
  }

  public static Map<Long, String> getFileReferences(String html) {
    Map<Long, String> files = new HashMap<>();
    Pattern pattern = Pattern.compile("src=\"(" + AdministrationConstants.FILE_URL + "/(\\d+))\"");
    Matcher matcher = pattern.matcher(html);

    while (matcher.find()) {
      files.put(BeeUtils.toLong(matcher.group(2)), matcher.group(1));
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
