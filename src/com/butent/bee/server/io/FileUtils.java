package com.butent.bee.server.io;

import com.google.common.collect.Lists;

import com.butent.bee.server.Config;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.io.FileNameUtils.Component;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;
import com.butent.bee.shared.utils.Wildcards;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;

/**
 * Contains utility functions, necessary for operations with files, for example search, read
 * contents or save.
 */

public final class FileUtils {

  public static final String EXT_CLASS = "class";
  public static final String EXT_JAVA = "java";
  public static final String EXT_PROPERTIES = "properties";

  public static final Filter DIRECTORY_FILTER = new DirectoryFilter();
  public static final Filter FILE_FILTER = new NormalFileFilter();
  public static final Filter INPUT_FILTER = new InputFileFilter();

  public static final Charset UTF_8 = Charset.forName(BeeConst.CHARSET_UTF8);

  private static int defaultBufferSize = 4096;
  private static Charset defaultCharset = UTF_8;

  private static BeeLogger logger = LogUtils.getLogger(FileUtils.class);

  public static void closeQuietly(Closeable closeable) {
    if (closeable == null) {
      return;
    }

    try {
      closeable.close();
    } catch (IOException ex) {
      logger.warning(ex);
    }
  }

  public static boolean deleteFile(String name) {
    Assert.notEmpty(name);
    File fl = new File(name);
    boolean ok = !fl.exists();

    if (!ok && fl.isFile()) {
      ok = fl.delete();
    }
    return ok;
  }

  public static String fileToString(File fl) {
    return fileToString(fl, defaultCharset);
  }

  public static String fileToString(File fl, Charset cs) {
    Assert.notNull(fl);
    if (!fl.exists()) {
      logger.warning(fl.getAbsolutePath(), "file not found");
      return null;
    }

    try {
      return streamToString(new FileInputStream(fl), cs);
    } catch (FileNotFoundException ex) {
      logger.error(ex);
      return null;
    }
  }

  public static String fileToString(String fileName) {
    return fileToString(fileName, defaultCharset);
  }

  public static String fileToString(String fileName, Charset cs) {
    Assert.notEmpty(fileName);
    File fl = new File(fileName.trim());
    return fileToString(fl, cs);
  }

  public static List<File> findFiles(String search, Collection<File> defaultRoots,
      Collection<? extends Filter> requiredFilters, String defaultExtension, boolean recurse,
      boolean all) {
    if (BeeUtils.isEmpty(search)) {
      return null;
    }
    if (isFile(search.trim())) {
      return Lists.newArrayList(new File(search.trim()));
    }

    String pfx = FileNameUtils.getPrefix(search);
    String path = FileNameUtils.getPathNoEndSeparator(search);
    String stem = FileNameUtils.getBaseName(search);
    String ext = FileNameUtils.getExtension(search);

    List<File> roots = new ArrayList<>();

    if (!BeeUtils.isEmpty(pfx)) {
      roots.addAll(Config.getDirectories(pfx));
    }

    if (roots.isEmpty()) {
      if (defaultRoots != null && !defaultRoots.isEmpty()) {
        roots.addAll(defaultRoots);
      } else {
        roots.addAll(Config.getDefaultSearchDirectories());
      }
    }

    List<Filter> filters = new ArrayList<>();
    if (requiredFilters != null) {
      filters.addAll(requiredFilters);
    }

    if (Wildcards.isFsPattern(path)) {
      filters.add(new WildcardFilter(path, Component.PATH));
    }
    if (Wildcards.isFsPattern(stem)) {
      filters.add(new WildcardFilter(stem, Component.BASE_NAME));
    }
    if (Wildcards.isFsPattern(ext)) {
      filters.add(new WildcardFilter(ext, Component.EXTENSION));
    } else if (!BeeUtils.isEmpty(defaultExtension)) {
      filters.add(new ExtensionFilter(defaultExtension));
    }

    return findFiles(roots, filters, recurse, all);
  }

  public static List<File> findFiles(Collection<File> directories,
      Collection<? extends Filter> filters, boolean recurse, boolean all) {
    Assert.notEmpty(directories);
    List<File> files = new ArrayList<>();

    for (File dir : directories) {
      files.addAll(findFiles(dir, filters, recurse));
      if (!all && !files.isEmpty()) {
        break;
      }
    }
    return files;
  }

  public static List<File> findFiles(File dir, Collection<? extends Filter> filters) {
    return findFiles(dir, filters, true);
  }

  public static List<File> findFiles(File dir, Collection<? extends Filter> filters,
      boolean recurse) {
    Assert.notNull(dir);

    List<File> found = new ArrayList<>();
    if (!dir.isDirectory() || !Config.isVisible(dir)) {
      return found;
    }

    boolean ok;
    for (File entry : dir.listFiles()) {
      if (!Config.isVisible(entry)) {
        continue;
      }

      ok = true;
      if (filters != null) {
        for (Filter filter : filters) {
          if (!filter.accept(entry) || !filter.accept(dir, entry.getName())) {
            ok = false;
            break;
          }
        }
      }
      if (ok) {
        found.add(entry);
      }

      if (recurse && entry.isDirectory()) {
        found.addAll(findFiles(entry, filters, recurse));
      }
    }
    return found;
  }

  public static List<File> findFiles(File dir, Filter... filters) {
    Assert.notNull(filters);
    return findFiles(dir, Lists.newArrayList(filters));
  }

  public static byte[] getBytes(File fl) {
    Assert.notNull(fl);
    if (!isInputFile(fl)) {
      logger.severe(fl.getAbsolutePath(), "not an input file");
      return null;
    }

    if (fl.length() > Integer.MAX_VALUE) {
      logger.severe(fl.getAbsolutePath(), "file is too big:", fl.length());
      return null;
    }

    byte[] result = new byte[(int) fl.length()];

    FileInputStream fis = null;
    try {
      fis = new FileInputStream(fl);
      fis.read(result);
    } catch (IOException ex) {
      logger.error(ex, fl.getAbsolutePath());
      result = null;
    } finally {
      closeQuietly(fis);
    }

    return result;
  }

  public static String getCanonicalPath(File fl) {
    Assert.notNull(fl);
    String path;

    try {
      path = fl.getCanonicalPath();
    } catch (IOException ex) {
      logger.error(ex);
      path = BeeConst.STRING_EMPTY;
    }
    return path;
  }

  public static List<ExtendedProperty> getCharsets() {
    List<ExtendedProperty> lst = new ArrayList<>();
    PropertyUtils.addExtended(lst, "Default Charset", Charset.defaultCharset());

    SortedMap<String, Charset> charsets = Charset.availableCharsets();
    PropertyUtils.addExtended(lst, "Available Charsets", "Cnt", charsets.size());

    int i = 0;
    for (String key : charsets.keySet()) {
      i++;
      Charset cs = charsets.get(key);

      PropertyUtils.addChildren(lst,
          BeeUtils.joinWords(BeeUtils.progress(i, charsets.size()), key),
          "Name", cs.name(),
          "Aliases", cs.aliases(),
          "Can Encode", cs.canEncode(),
          "Display Name", cs.displayName(),
          "Registered", cs.isRegistered());
    }
    return lst;
  }

  public static File getDirectory(File parent, String child) {
    Assert.notNull(parent);
    Assert.notEmpty(child);
    File dir = new File(parent, child);

    if (isDirectory(dir)) {
      return dir;
    }
    return null;
  }

  public static List<Property> getFileInfo(File fl) {
    Assert.notNull(fl);

    List<Property> lst = new ArrayList<>();
    if (!fl.exists()) {
      PropertyUtils.addProperty(lst, "Exists", false);
      return lst;
    }

    PropertyUtils.addProperties(lst,
        "Can Execute", fl.canExecute(),
        "Can Read", fl.canRead(),
        "Can Write", fl.canWrite(),
        "Absolute Path", fl.getAbsolutePath(),
        "Canonical Path", getCanonicalPath(fl),
        "Free Space", fl.getFreeSpace(),
        "Name", fl.getName(),
        "Parent", fl.getParent(),
        "Path", fl.getPath(),
        "Total Space", fl.getTotalSpace(),
        "Usable Space", fl.getUsableSpace(),
        "Absolute", fl.isAbsolute(),
        "Directory", fl.isDirectory(),
        "File", fl.isFile(),
        "Hidden", fl.isHidden(),
        "Last Modified", new DateTime(fl.lastModified()),
        "Length", fl.length(),
        "URI", fl.toURI());

    return lst;
  }

  public static FileReader getFileReader(String fileName) {
    Assert.notEmpty(fileName);
    File fl = new File(fileName.trim());

    if (!fl.exists()) {
      logger.warning(fileName, "file not found");
      return null;
    }

    FileReader fr = null;

    try {
      fr = new FileReader(fl);
    } catch (IOException ex) {
      logger.error(ex, fileName);
    }
    return fr;
  }

  public static String[] getFiles(File dir) {
    return getFiles(dir, null);
  }

  public static String[] getFiles(File dir, FilenameFilter filter) {
    Assert.notNull(dir);
    return (filter == null) ? dir.list() : dir.list(filter);
  }

  public static List<Property> getRootsInfo() {
    List<Property> lst = new ArrayList<>();

    File[] roots = File.listRoots();
    int n = ArrayUtils.length(roots);
    if (n <= 0) {
      PropertyUtils.addProperty(lst, "Roots", BeeUtils.bracket(n));
      return lst;
    }

    for (int i = 0; i < n; i++) {
      PropertyUtils.addProperty(lst, "Root", BeeUtils.progress(i + 1, n));
      lst.addAll(getFileInfo(roots[i]));
    }
    return lst;
  }

  public static boolean isDirectory(File dir) {
    if (dir == null) {
      return false;
    } else {
      return dir.exists() && dir.isDirectory();
    }
  }

  public static boolean isDirectory(String dirName) {
    Assert.notEmpty(dirName);
    File dir = new File(dirName);

    return isDirectory(dir);
  }

  public static boolean isFile(String name) {
    Assert.notEmpty(name);
    return new File(name).exists();
  }

  public static boolean isFile(File parent, String child) {
    Assert.notNull(parent);
    Assert.notEmpty(child);
    return new File(parent, child).exists();
  }

  public static boolean isInputFile(File fl) {
    if (fl == null) {
      return false;
    } else {
      return fl.exists() && fl.isFile() && fl.length() > 0 && fl.canRead();
    }
  }

  public static boolean isInputFile(File parent, String child) {
    Assert.notNull(parent);
    Assert.notEmpty(child);
    File fl = new File(parent, child);

    return isInputFile(fl);
  }

  public static boolean isInputFile(String fileName) {
    Assert.notEmpty(fileName);
    File fl = new File(fileName);

    return isInputFile(fl);
  }

  public static int loadProperties(Properties prp, File fl) {
    return loadProperties(prp, fl, UTF_8);
  }

  public static int loadProperties(Properties prp, File fl, Charset cs) {
    Assert.notNull(prp);
    Assert.notNull(fl);
    Assert.notNull(cs);
    if (!fl.exists()) {
      logger.warning(fl.getAbsolutePath(), "file not found");
      return -1;
    }

    int size = prp.size();
    int cnt = 0;
    InputStreamReader fr = null;

    try {
      fr = new InputStreamReader(new FileInputStream(fl), cs);
      prp.load(fr);
      cnt = prp.size() - size;
      logger.debug(cnt, "properties loaded from", fl.getAbsolutePath());
    } catch (IOException ex) {
      logger.error(ex, fl.getAbsolutePath());
      cnt = -1;
    }

    closeQuietly(fr);
    return cnt;
  }

  public static Charset normalizeCharset(String name) {
    if (BeeUtils.isEmpty(name)) {
      return defaultCharset;
    }
    Charset cs = null;

    try {
      cs = Charset.forName(name);
    } catch (Exception ex) {
      logger.warning(ex, name);
      cs = null;
    }
    return BeeUtils.nvl(cs, defaultCharset);
  }

  public static Properties readProperties(File fl) {
    Properties prp = new Properties();
    loadProperties(prp, fl);
    return prp;
  }

  public static String saveToFile(String src, String dst) {
    return saveToFile(src, dst, defaultCharset);
  }

  public static String saveToFile(String src, String dst, Charset cs) {
    Assert.notEmpty(src);
    Assert.notEmpty(dst);

    OutputStreamWriter fw = null;
    String path;
    File file = new File(dst);

    try {
      File parent = file.getParentFile();
      if (parent != null && !parent.exists()) {
        parent.mkdirs();
      }
      fw = new OutputStreamWriter(new FileOutputStream(file), cs);
      fw.append(src);

      path = file.getCanonicalPath();

    } catch (IOException ex) {
      logger.error(ex, dst);
      path = null;
    }

    closeQuietly(fw);
    return path;
  }

  public static String streamToString(InputStream stream) {
    return streamToString(stream, defaultCharset);
  }

  public static String streamToString(InputStream stream, Charset cs) {
    Assert.notNull(stream);
    Assert.notNull(cs);

    InputStreamReader fr = null;
    StringBuilder sb = new StringBuilder();

    int size = defaultBufferSize;
    char[] arr = new char[size];
    int len;

    try {
      fr = new InputStreamReader(stream, cs);
      do {
        len = fr.read(arr, 0, size);
        if (len > 0) {
          sb.append(arr, 0, len);
        }
      } while (len > 0);
    } catch (IOException ex) {
      logger.error(ex);
    }

    closeQuietly(fr);
    return sb.toString();
  }

  public static File toFile(Class<?> clazz) {
    Assert.notNull(clazz);
    return toFile(clazz.getResource(FileNameUtils.addExtension(clazz.getSimpleName(), EXT_CLASS)));
  }

  public static File toFile(URL url) {
    Assert.notNull(url);
    return new File(url.getPath());
  }

  private FileUtils() {
  }
}
