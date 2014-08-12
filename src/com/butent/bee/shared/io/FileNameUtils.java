package com.butent.bee.shared.io;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.io.File;
import java.util.Collection;

/**
 * Enables operations with file names, paths, extensions, read and change them.
 */

public final class FileNameUtils {
  /**
   * Contains a list of possible name components like prefix, path, extension and so on.
   */

  public enum Component {
    PREFIX, PATH, FULL_PATH, NAME, BASE_NAME, EXTENSION
  }

  public static final char EXTENSION_SEPARATOR = '.';
  public static final String EXTENSION_SEPARATOR_STR = Character.toString(EXTENSION_SEPARATOR);

  public static final char UNIX_SEPARATOR = '/';
  public static final char WINDOWS_SEPARATOR = '\\';
  private static final char SYSTEM_SEPARATOR = File.separatorChar;

  private static final String ILLEGAL_NAME_CHARS = "\"<>|:*?\\/";

  public static String addExtension(String name, String ext) {
    Assert.notEmpty(name);
    Assert.isTrue(isValidExtension(ext));

    if (name.endsWith(EXTENSION_SEPARATOR_STR)) {
      return name + ext;
    } else {
      return name + EXTENSION_SEPARATOR + ext;
    }
  }

  public static String defaultExtension(String name, String ext) {
    Assert.notEmpty(name);
    Assert.isTrue(isValidExtension(ext));

    if (hasExtension(name)) {
      return name;
    } else {
      return addExtension(name, ext);
    }
  }

  public static String forceExtension(String name, String ext) {
    Assert.notEmpty(name);
    Assert.isTrue(isValidExtension(ext));

    String old = getExtension(name);
    if (ext.equals(old)) {
      return name;
    } else if (BeeUtils.isEmpty(old)) {
      return addExtension(name, ext);
    } else {
      return addExtension(removeExtension(name), ext);
    }
  }

  public static String getBaseName(String filename) {
    return removeExtension(getName(filename));
  }

  public static String getComponent(String filename, Component component) {
    if (filename == null) {
      return null;
    }
    if (filename.isEmpty() || component == null) {
      return BeeConst.STRING_EMPTY;
    }

    switch (component) {
      case PREFIX:
        return getPrefix(filename);
      case FULL_PATH:
        return getFullPathNoEndSeparator(filename);
      case PATH:
        return getPathNoEndSeparator(filename);
      case NAME:
        return getName(filename);
      case BASE_NAME:
        return getBaseName(filename);
      case EXTENSION:
        return getExtension(filename);
    }
    Assert.untouchable();
    return null;
  }

  public static String getExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfExtension(filename);
    if (index == -1) {
      return BeeConst.STRING_EMPTY;
    } else {
      return filename.substring(index + 1);
    }
  }

  public static String getFullPath(String filename) {
    return doGetFullPath(filename, true);
  }

  public static String getFullPathNoEndSeparator(String filename) {
    return doGetFullPath(filename, false);
  }

  public static String getName(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfLastSeparator(filename);
    return (index >= 0) ? filename.substring(index + 1) : filename;
  }

  public static String getPath(String filename) {
    return doGetPath(filename, 1);
  }

  public static String getPathNoEndSeparator(String filename) {
    return doGetPath(filename, 0);
  }

  public static String getPrefix(String filename) {
    if (filename == null) {
      return null;
    }
    int len = getPrefixLength(filename);
    if (len < 0) {
      return null;
    }
    if (len > filename.length()) {
      return filename + UNIX_SEPARATOR;
    }
    return filename.substring(0, len);
  }

  public static int getPrefixLength(String filename) {
    if (filename == null) {
      return -1;
    }
    int len = filename.length();
    if (len == 0) {
      return 0;
    }
    char ch0 = filename.charAt(0);
    if (ch0 == ':') {
      return -1;
    }
    if (len == 1) {
      if (ch0 == '~') {
        return 2;
      }
      return isSeparator(ch0) ? 1 : 0;
    } else {
      if (ch0 == '~') {
        int posUnix = filename.indexOf(UNIX_SEPARATOR, 1);
        int posWin = filename.indexOf(WINDOWS_SEPARATOR, 1);
        if (posUnix == -1 && posWin == -1) {
          return len + 1;
        }
        posUnix = (posUnix == -1) ? posWin : posUnix;
        posWin = (posWin == -1) ? posUnix : posWin;
        return Math.min(posUnix, posWin) + 1;
      }
      char ch1 = filename.charAt(1);
      if (ch1 == ':') {
        ch0 = Character.toUpperCase(ch0);
        if (ch0 >= 'A' && ch0 <= 'Z') {
          if (len == 2 || !isSeparator(filename.charAt(2))) {
            return 2;
          }
          return 3;
        }
        return -1;

      } else if (isSeparator(ch0) && isSeparator(ch1)) {
        int posUnix = filename.indexOf(UNIX_SEPARATOR, 2);
        int posWin = filename.indexOf(WINDOWS_SEPARATOR, 2);
        if ((posUnix == -1 && posWin == -1) || posUnix == 2 || posWin == 2) {
          return -1;
        }
        posUnix = (posUnix == -1) ? posWin : posUnix;
        posWin = (posWin == -1) ? posUnix : posWin;
        return Math.min(posUnix, posWin) + 1;
      } else {
        return isSeparator(ch0) ? 1 : 0;
      }
    }
  }

  public static boolean hasExtension(String name) {
    return !BeeUtils.isEmpty(getExtension(name));
  }

  public static boolean hasSeparator(String filename) {
    if (filename == null) {
      return false;
    }
    return filename.indexOf(UNIX_SEPARATOR) >= 0 || filename.indexOf(WINDOWS_SEPARATOR) >= 0;
  }

  public static int indexOfExtension(String filename) {
    if (filename == null) {
      return -1;
    }
    int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
    int lastSeparator = indexOfLastSeparator(filename);
    return (lastSeparator > extensionPos) ? -1 : extensionPos;
  }

  public static int indexOfLastSeparator(String filename) {
    if (filename == null) {
      return -1;
    }
    int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
    int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
    return Math.max(lastUnixPos, lastWindowsPos);
  }

  public static boolean isExtension(String filename, Collection<String> extensions) {
    if (filename == null) {
      return false;
    }
    if (extensions == null || extensions.isEmpty()) {
      return indexOfExtension(filename) == -1;
    }
    String fileExt = getExtension(filename);
    for (String extension : extensions) {
      if (fileExt.equals(extension)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isExtension(String filename, String extension) {
    if (filename == null) {
      return false;
    }
    if (extension == null || extension.length() == 0) {
      return indexOfExtension(filename) == -1;
    }
    String fileExt = getExtension(filename);
    return fileExt.equals(extension);
  }

  public static boolean isExtension(String filename, String[] extensions) {
    if (filename == null) {
      return false;
    }
    if (extensions == null || extensions.length == 0) {
      return indexOfExtension(filename) == -1;
    }
    String fileExt = getExtension(filename);
    for (String extension : extensions) {
      if (fileExt.equals(extension)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isSystemWindows() {
    return SYSTEM_SEPARATOR == WINDOWS_SEPARATOR;
  }

  public static boolean isValidExtension(String ext) {
    if (BeeUtils.isEmpty(ext)) {
      return false;
    }
    boolean ok = true;

    for (int i = 0; i < ext.length(); i++) {
      if (!Character.isLetterOrDigit(ext.charAt(i))) {
        ok = false;
        break;
      }
    }
    return ok;
  }

  public static String removeExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfExtension(filename);
    if (index == -1) {
      return filename;
    } else {
      return filename.substring(0, index);
    }
  }

  public static String sanitize(String input, String illegalChars) {
    return sanitize(input, illegalChars, BeeConst.CHAR_SPACE);
  }

  public static String sanitize(String input, String illegalChars, char replacement) {
    if (BeeUtils.isEmpty(input)) {
      return BeeConst.STRING_EMPTY;
    }

    StringBuilder sb = new StringBuilder();
    char last = replacement;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (c < BeeConst.CHAR_SPACE || BeeUtils.contains(ILLEGAL_NAME_CHARS, c)
          || BeeUtils.contains(illegalChars, c)) {

        if (last != replacement) {
          sb.append(replacement);
          last = replacement;
        }

      } else {
        sb.append(c);
        last = c;
      }
    }

    return sb.toString().trim();
  }

  public static String separatorsToSystem(String path) {
    if (path == null) {
      return null;
    }
    if (isSystemWindows()) {
      return separatorsToWindows(path);
    } else {
      return separatorsToUnix(path);
    }
  }

  public static String separatorsToUnix(String path) {
    if (path == null || path.indexOf(WINDOWS_SEPARATOR) == -1) {
      return path;
    }
    return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
  }

  public static String separatorsToWindows(String path) {
    if (path == null || path.indexOf(UNIX_SEPARATOR) == -1) {
      return path;
    }
    return path.replace(UNIX_SEPARATOR, WINDOWS_SEPARATOR);
  }

  private static String doGetFullPath(String filename, boolean includeSeparator) {
    if (filename == null) {
      return null;
    }
    int prefix = getPrefixLength(filename);
    if (prefix < 0) {
      return null;
    }
    if (prefix >= filename.length()) {
      if (includeSeparator) {
        return getPrefix(filename);
      } else {
        return filename;
      }
    }
    int index = indexOfLastSeparator(filename);
    if (index < 0) {
      return filename.substring(0, prefix);
    }
    int end = index + (includeSeparator ? 1 : 0);
    if (end == 0) {
      end++;
    }
    return filename.substring(0, end);
  }

  private static String doGetPath(String filename, int separatorAdd) {
    if (filename == null) {
      return null;
    }
    int prefix = getPrefixLength(filename);
    if (prefix < 0) {
      return null;
    }
    int index = indexOfLastSeparator(filename);
    int endIndex = index + separatorAdd;
    if (prefix >= filename.length() || index < 0 || prefix >= endIndex) {
      return BeeConst.STRING_EMPTY;
    }
    return filename.substring(prefix, endIndex);
  }

  private static boolean isSeparator(char ch) {
    return (ch == UNIX_SEPARATOR) || (ch == WINDOWS_SEPARATOR);
  }

  private FileNameUtils() {
  }
}
