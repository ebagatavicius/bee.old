package com.butent.bee.server;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.butent.bee.server.io.FileUtils;
import com.butent.bee.server.io.Filter;
import com.butent.bee.server.io.WildcardFilter;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.io.FileNameUtils.Component;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Contains essential server configuration parameters like directory structure.
 */

public final class Config {

  private static BeeLogger logger = LogUtils.getLogger(Config.class);

  public static final File WAR_DIR;
  public static final File WEB_INF_DIR;

  public static final File ROOT_DIR;
  public static final File SOURCE_DIR;

  public static final File SCHEMA_DIR;
  public static final File CONFIG_DIR;
  public static final File LOCAL_DIR;
  public static final File LOG_DIR;

  public static final File IMAGE_DIR;

  public static final Map<String, File> DIRECTORY_SUBSTITUTES;

  private static boolean initialized;

  private static Properties properties;

  private static final Splitter VALUE_SPLITTER =
      Splitter.on(BeeConst.CHAR_COMMA).trimResults().omitEmptyStrings();

  private static List<Filter> fileBlacklist;

  private static List<String> textExtensions;

  static {
    File path = FileUtils.toFile(Config.class);

    while (path != null) {
      if (BeeUtils.same(path.getName(), "WEB-INF")) {
        break;
      }
      path = path.getParentFile();
    }
    Assert.notNull(path);

    WEB_INF_DIR = path;
    WAR_DIR = WEB_INF_DIR.getParentFile();

    ROOT_DIR = WAR_DIR.getParentFile();
    SOURCE_DIR = new File(ROOT_DIR, "src");

    SCHEMA_DIR = new File(WEB_INF_DIR, "schemas");
    CONFIG_DIR = new File(WEB_INF_DIR, "config");
    LOCAL_DIR = new File(WEB_INF_DIR, "local");

    LOG_DIR = new File(LOCAL_DIR, "logs");

    IMAGE_DIR = new File(WAR_DIR, Paths.IMAGE_DIR);

    DIRECTORY_SUBSTITUTES = new LinkedHashMap<>();

    DIRECTORY_SUBSTITUTES.put(BeeUtils.embrace("war"), WAR_DIR);
    DIRECTORY_SUBSTITUTES.put(BeeUtils.embrace("web"), WEB_INF_DIR);
    DIRECTORY_SUBSTITUTES.put(BeeUtils.embrace("root"), ROOT_DIR);
    DIRECTORY_SUBSTITUTES.put(BeeUtils.embrace("src"), SOURCE_DIR);
    DIRECTORY_SUBSTITUTES.put(BeeUtils.embrace("schema"), SCHEMA_DIR);
    DIRECTORY_SUBSTITUTES.put(BeeUtils.embrace("config"), CONFIG_DIR);
    DIRECTORY_SUBSTITUTES.put(BeeUtils.embrace("local"), LOCAL_DIR);
    DIRECTORY_SUBSTITUTES.put(BeeUtils.embrace("log"), LOG_DIR);
    DIRECTORY_SUBSTITUTES.put(BeeUtils.embrace("image"), IMAGE_DIR);
  }

  public static String getConfigPath(String resource) {
    Assert.notEmpty(resource);

    if (FileUtils.isInputFile(CONFIG_DIR, resource)) {
      return new File(CONFIG_DIR, resource).getPath();
    }
    return null;
  }

  public static List<File> getDefaultSearchDirectories() {
    return Lists.newArrayList(LOCAL_DIR, CONFIG_DIR, SCHEMA_DIR, WAR_DIR, SOURCE_DIR);
  }

  public static List<File> getDirectories(String pfx) {
    if (BeeUtils.isEmpty(pfx)) {
      return null;
    }

    List<File> directories = new ArrayList<>();
    File dir;

    for (int i = 0; i < pfx.length(); i++) {
      switch (pfx.charAt(i)) {
        case 'c':
          dir = CONFIG_DIR;
          break;
        case 's':
          dir = SOURCE_DIR;
          break;
        case 'l':
          dir = LOCAL_DIR;
          break;
        case 'w':
          dir = WAR_DIR;
          break;
        case 'x':
          dir = SCHEMA_DIR;
          break;
        default:
          dir = null;
      }
      if (dir != null && !directories.contains(dir)) {
        directories.add(dir);
      }
    }
    return directories;
  }

  public static List<Filter> getFileBlacklist() {
    if (fileBlacklist == null) {
      fileBlacklist = new ArrayList<>();
      for (String expr : getList("FileBlacklist")) {
        fileBlacklist.add(new WildcardFilter(expr, Component.NAME));
      }
    }
    return fileBlacklist;
  }

  public static List<ExtendedProperty> getInfo() {
    List<ExtendedProperty> lst = new ArrayList<>();

    for (Map.Entry<String, File> entry : DIRECTORY_SUBSTITUTES.entrySet()) {
      if (entry.getValue() != null) {
        lst.add(new ExtendedProperty(entry.getKey(), entry.getValue().getAbsolutePath()));
      }
    }

    if (fileBlacklist == null) {
      PropertyUtils.addExtended(lst, "File Blacklist", "not initialized");
    } else {
      int cnt = fileBlacklist.size();
      PropertyUtils.addExtended(lst, "File Blacklist", cnt);
      for (int i = 0; i < cnt; i++) {
        PropertyUtils.addExtended(lst, "blacklist pattern", BeeUtils.progress(i + 1, cnt),
            fileBlacklist.get(i));
      }
    }

    if (textExtensions == null) {
      PropertyUtils.addExtended(lst, "Text extensions", "not initialized");
    } else {
      int cnt = textExtensions.size();
      PropertyUtils.addExtended(lst, "Text extensions", cnt);
      for (int i = 0; i < cnt; i++) {
        PropertyUtils.addExtended(lst, "text extension", BeeUtils.progress(i + 1, cnt),
            textExtensions.get(i));
      }
    }

    Set<String> keys = properties.stringPropertyNames();
    int size = keys.size();
    PropertyUtils.addExtended(lst, "Properties", size);

    int idx = 0;
    for (String key : keys) {
      PropertyUtils.addExtended(lst,
          key, BeeUtils.progress(++idx, size), properties.getProperty(key));
    }
    return lst;
  }

  public static List<String> getList(String key) {
    String values = getProperty(key);
    if (BeeUtils.isEmpty(values)) {
      return new ArrayList<>();
    }
    return Lists.newArrayList(VALUE_SPLITTER.split(values));
  }

  public static String getLocalPath(String resource) {
    Assert.notEmpty(resource);

    if (FileUtils.isInputFile(LOCAL_DIR, resource)) {
      return new File(LOCAL_DIR, resource).getPath();
    }
    return null;
  }

  public static String getPath(String resource) {
    return getPath(resource, true);
  }

  public static String getPath(String resource, boolean warn) {
    Assert.notEmpty(resource);

    String path = getLocalPath(resource);
    if (BeeUtils.isEmpty(path)) {
      path = getConfigPath(resource);
    }

    if (warn && BeeUtils.isEmpty(path)) {
      logger.warning(resource, "resource not found");
    }
    return path;
  }

  public static String getProperty(String key) {
    Assert.notEmpty(key);
    return properties.getProperty(key);
  }

  public static String getSchemaPath(String resource) {
    Assert.notEmpty(resource);

    if (FileUtils.isInputFile(SCHEMA_DIR, resource)) {
      return new File(SCHEMA_DIR, resource).getPath();
    }
    return null;
  }

  public static List<String> getTextExtensions() {
    if (textExtensions == null) {
      textExtensions = getList("TextExtensions");
    }
    return textExtensions;
  }

  public static void init() {
    properties = loadProperties("server.properties");
  }

  public static boolean isInitialized() {
    return initialized;
  }

  public static boolean isText(File file) {
    if (file == null) {
      return false;
    }
    String ext = FileNameUtils.getExtension(file.getName());
    if (BeeUtils.isEmpty(ext)) {
      return false;
    }

    boolean ok = false;
    for (String x : getTextExtensions()) {
      if (BeeUtils.same(x, ext)) {
        ok = true;
        break;
      }
    }
    return ok;
  }

  public static boolean isVisible(File file) {
    if (file == null) {
      return false;
    }

    boolean ok = true;
    for (Filter filter : getFileBlacklist()) {
      if (filter.accept(file) && filter.accept(file, file.getName())) {
        ok = false;
        break;
      }
    }
    return ok;
  }

  public static Properties loadProperties(String name) {
    Properties def = readProperties(new File(CONFIG_DIR, name));
    Properties loc = readProperties(new File(LOCAL_DIR, name));

    Properties result;
    if (isEmpty(def)) {
      result = new Properties();
    } else {
      result = new Properties(def);
    }

    if (!isEmpty(loc)) {
      for (String key : loc.stringPropertyNames()) {
        result.setProperty(key, loc.getProperty(key));
      }
    }
    return result;
  }

  public static void setInitialized(boolean initialized) {
    Config.initialized = initialized;
  }

  public static String substitutePath(String input) {
    if (!BeeUtils.isEmpty(input) && input.startsWith(BeeConst.STRING_LEFT_BRACE)
        && input.contains(BeeConst.STRING_RIGHT_BRACE)) {

      String path = input;

      for (Map.Entry<String, File> entry : DIRECTORY_SUBSTITUTES.entrySet()) {
        path = substituteDirectory(path, entry.getKey(), entry.getValue());
        if (!path.startsWith(BeeConst.STRING_LEFT_BRACE)) {
          break;
        }
      }

      return path;

    } else {
      return input;
    }
  }

  private static int getSize(Properties props) {
    if (props == null) {
      return 0;
    }
    return props.stringPropertyNames().size();
  }

  private static boolean isEmpty(Properties props) {
    return getSize(props) <= 0;
  }

  private static Properties readProperties(File file) {
    if (!FileUtils.isInputFile(file)) {
      return null;
    }
    Properties props = new Properties();
    FileUtils.loadProperties(props, file);
    return props;
  }

  private static String substituteDirectory(String input, String key, File directory) {
    if (BeeUtils.same(input, key)) {
      return directory.getAbsolutePath();
    } else if (BeeUtils.isPrefix(input, key)) {
      return new File(directory, BeeUtils.removePrefix(input, key)).getAbsolutePath();
    } else {
      return input;
    }
  }

  private Config() {
  }
}
