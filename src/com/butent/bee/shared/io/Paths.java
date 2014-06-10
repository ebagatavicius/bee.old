package com.butent.bee.shared.io;

import com.google.common.collect.Lists;

import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.List;

public final class Paths {

  public static final String IMAGE_DIR = "images";
  
  public static final String FILE_ICON_DIR = "fileicons";
  public static final String PHOTO_DIR = "photo";

  public static final String FLAG_DIR = "flags";
  public static final String FLAG_EXT = "png";

  private static final String CSS_DIR = "css";
  private static final String CSS_EXT = "css";

  private static final String JS_DIR = "js";
  private static final String JS_EXT = "js";
  
  private static final String LANG_DIR = "lang";
  private static final String LANG_EXT = "png";

  private static final String SEGMENT_SEPARATOR = "/";
  
  public static String buildPath(String first, String second, String... rest) {
    List<String> segments = Lists.newArrayList(first, second);
    if (rest != null) {
      segments.addAll(Arrays.asList(rest));
    }
    return BeeUtils.join(SEGMENT_SEPARATOR, segments);
  }
  
  public static String ensureEndSeparator(String segment) {
    if (BeeUtils.isEmpty(segment)) {
      return segment;
    } else if (segment.trim().endsWith(SEGMENT_SEPARATOR)) {
      return segment.trim();
    } else {
      return segment.trim() + SEGMENT_SEPARATOR;
    }
  }
  
  public static String getFlagPath(String fileName) {
    if (BeeUtils.isEmpty(fileName)) {
      return null;
    } else {
      return normalizePath(buildPath(IMAGE_DIR, FLAG_DIR), fileName, FLAG_EXT);
    }
  }

  public static String getImagePath(String fileName) {
    if (BeeUtils.isEmpty(fileName)) {
      return null;
    } else {
      return buildPath(IMAGE_DIR, fileName);
    }
  }
  
  public static String getLangIconPath(String fileName) {
    if (BeeUtils.isEmpty(fileName)) {
      return null;
    } else {
      return normalizePath(buildPath(IMAGE_DIR, LANG_DIR), fileName, LANG_EXT);
    }
  }

  public static String getScriptPath(String fileName) {
    if (BeeUtils.isEmpty(fileName)) {
      return null;
    } else {
      return normalizePath(JS_DIR, fileName, JS_EXT);
    }
  }

  public static String getStyleSheetPath(String fileName) {
    if (BeeUtils.isEmpty(fileName)) {
      return null;
    } else {
      return normalizePath(CSS_DIR, fileName, CSS_EXT);
    }
  }

  public static String getStyleSheetUrl(String fileName, HasDateValue version) {
    if (version == null) {
      return getStyleSheetPath(fileName);
    } else {
      return CommUtils.addTimeStamp(getStyleSheetPath(fileName), version);
    }
  }
  
  public static boolean isAbsolute(String path) {
    return !BeeUtils.isEmpty(path) && path.startsWith(SEGMENT_SEPARATOR);
  }
  
  private static String normalizePath(String dir, String name, String ext) {
    if (FileNameUtils.hasSeparator(name)) {
      return FileNameUtils.defaultExtension(name, ext);
    } else {
      return buildPath(dir, FileNameUtils.defaultExtension(name, ext));
    }
  }
  
  private Paths() {
  }
}
